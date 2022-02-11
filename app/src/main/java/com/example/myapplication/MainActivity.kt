package com.example.myapplication

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mjdev.libaums.UsbMassStorageDevice
import com.github.mjdev.libaums.fs.FileSystem
import com.github.mjdev.libaums.fs.UsbFile
import java.io.*
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.view.WindowManager
import com.github.mjdev.libaums.server.http.UsbFileHttpServerService
import com.github.mjdev.libaums.server.http.server.AsyncHttpServer
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView


class MainActivity : AppCompatActivity() {
    companion object {


        /**
         * Action string to request the permission to communicate with an UsbDevice.
         */
        private const val ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION"
        private val TAG = MainActivity::class.java.simpleName

        @SuppressLint("StaticFieldLeak")
        var myWebView: WebView? = null
    }


    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                val device =
                    intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        setupDevice()
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device =
                    intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                Log.d(TAG, "USB device attached")
                if (device != null) {
                    discoverDevice()
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                val device =
                    intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                Log.d(TAG, "USB device detached")

                // determine if connected device is a mass storage device
                if (device != null) {
                    if (currentDevice != -1) {
                        massStorageDevices[currentDevice].close()
                    }
                    discoverDevice()
                }
            }
        }
    }

    private lateinit var currentFs: FileSystem
    private lateinit var serviceIntent: Intent
    lateinit var massStorageDevices: Array<UsbMassStorageDevice>
    private var currentDevice = -1

    var serverService: UsbFileHttpServerService? = null
    var rootDirectory: UsbFile? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceIntent = Intent(this, UsbFileHttpServerService::class.java)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.viewer_layout)


        myWebView = findViewById<View>(R.id.my_text) as WebView
        display()
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter)
        discoverDevice()
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)


    }
    override fun onStart() {
        super.onStart()
    }


    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    /**
     * Searches for connected mass storage devices, and initializes them if it
     * could find some.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun discoverDevice() {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        massStorageDevices = UsbMassStorageDevice.getMassStorageDevices(this)
        if (massStorageDevices.isEmpty()) {
            Log.w(TAG, "no device found!")
            val actionBar = supportActionBar
            actionBar!!.title = "No device"
            return
        }
        currentDevice = 0
        val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
        if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
            Log.d(TAG, "received usb device via intent")
            // requesting permission is not needed in this case
            setupDevice()
        } else {
            // first request permission from user to communicate with the underlying UsbDevice
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(
                    ACTION_USB_PERMISSION
                ), 0
            )
            usbManager.requestPermission(
                massStorageDevices[currentDevice].usbDevice,
                permissionIntent
            )
        }
    }

    /**
     * Sets the device up and shows the contents of the root directory.
     */
    private fun setupDevice() {
        try {
            massStorageDevices[currentDevice].init()

            // we always use the first partition of the device
            currentFs = massStorageDevices[currentDevice].partitions[0].fileSystem.also {
                Log.d(TAG, "Capacity: " + it.capacity)
                Log.d(TAG, "Occupied Space: " + it.occupiedSpace)
                Log.d(TAG, "Free Space: " + it.freeSpace)
                Log.d(TAG, "Chunk size: " + it.chunkSize)
            }

            val root = currentFs.rootDirectory
            val actionBar = supportActionBar
            actionBar!!.title = currentFs.volumeLabel
//            listView.adapter = UsbFileListAdapter(this, root).apply { adapter = this }
            if (serverService != null) {
                startHttpServer(root)
            }
            rootDirectory = root

        } catch (e: IOException) {
            Log.e(TAG, "error setting up device", e)
        }
    }

    private fun startHttpServer(file: UsbFile?) {
        Log.d(TAG, "starting HTTP server")
        if (serverService == null) {
            Toast.makeText(this@MainActivity, "serverService == null!", Toast.LENGTH_LONG).show()
            return
        }
        if (serverService!!.isServerRunning) {
            Log.d(TAG, "Stopping existing server service")
            serverService!!.stopServer()
        }

        // now start the server
        try {
            serverService!!.startServer(file!!, AsyncHttpServer(8000))
            Toast.makeText(this@MainActivity, "HTTP server up and running", Toast.LENGTH_LONG)
                .show()
        } catch (e: IOException) {
            Log.e(TAG, "Error starting HTTP server", e)
            Toast.makeText(this@MainActivity, "Could not start HTTP server", Toast.LENGTH_LONG)
                .show()
        }
        if (file!!.isDirectory) {
            // only open activity when serving a file
            return
        }


    }
    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    private fun display(){
        myWebView!!.settings.javaScriptEnabled = true
        myWebView!!.webChromeClient = WebChromeClient()
        myWebView!!.settings.domStorageEnabled = true
        myWebView!!.settings.allowContentAccess = true
        myWebView!!.settings.domStorageEnabled = true
        myWebView!!.settings.allowFileAccess = true
        myWebView!!.settings.allowFileAccessFromFileURLs
        myWebView!!.settings.builtInZoomControls
        myWebView!!.settings.displayZoomControls
        myWebView!!.settings.supportZoom()
        myWebView!!.settings.allowUniversalAccessFromFileURLs
        myWebView!!.addJavascriptInterface(this, "App")
        if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        myWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
        myWebView!!.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        myWebView!!.settings.loadsImagesAutomatically = true
    }

    private var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "on service connected $name")
            Toast.makeText(this@MainActivity, "onServiceConnected", Toast.LENGTH_LONG).show()

            val binder = service as UsbFileHttpServerService.ServiceBinder
            serverService = binder.service
            if (rootDirectory != null && (serverService == null || !serverService!!.isServerRunning)) {
                Log.d(TAG, "on service connected - start server $name")
                startHttpServer(rootDirectory)
                myWebView!!.loadUrl("http://localhost:8000/index.html")
            } else {
                Log.d(
                    TAG,
                    "on service connected start service failed-  $rootDirectory $serverService"
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "on service disconnected $name")
            serverService = null
            rootDirectory = null
        }
    }
    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }
}