package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView


class Viewer : AppCompatActivity() {
    var myWebView: WebView? = null

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.viewer_layout)
//        myWebView = findViewById<View>(R.id.my_text) as WebView
//        val txt = intent.getStringExtra("key")
//
//
//        print(txt)
//        myWebView!!.settings.javaScriptEnabled = true
////        myWebView!!.webViewClient = WebViewClient()
//        myWebView!!.webChromeClient = WebChromeClient()
////        myWebView!!.loadData(txt.toString(), "text/javascript", "UTF-8")
//        myWebView!!.settings.domStorageEnabled = true
//        myWebView!!.settings.allowContentAccess =true
//        myWebView!!.settings.domStorageEnabled = true
//        myWebView!!.settings.allowFileAccess =true
//        myWebView!!.settings.allowFileAccessFromFileURLs
//        myWebView!!.settings.allowUniversalAccessFromFileURLs
//        myWebView!!.addJavascriptInterface(this, "App");
////        myWebView!!.loadData(txt.toString(), "text/html", null);
//        if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
//            WebView.setWebContentsDebuggingEnabled(true)
//        }
//        myWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
//        myWebView!!.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//        myWebView!!.settings.loadsImagesAutomatically = true;
//        myWebView!!.loadUrl("http://localhost:8000/index.html")

    }

}



