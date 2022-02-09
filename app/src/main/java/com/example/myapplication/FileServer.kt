//package com.example.myapplication
//
//import android.content.Context
//import fi.iki.elonen.NanoHTTPD
//
//open class FileServer(private val context: Context, port: Int) : NanoHTTPD(port) {
//
//
//    override fun serve(session: IHTTPSession): Response {
//        val uri = session.uri.removePrefix("/").ifEmpty { "index.html" }
//        println("Loading $uri")
//        try {
//            val mime = when (uri.substringAfterLast(".")) {
//                "ico" -> "image/x-icon"
//                "css" -> "text/css"
//                "htm" -> "text/html"
//                "html" -> "text/html"
//                else -> "application/javascript"
//            }
//
//            return NanoHTTPD.newChunkedResponse(
//                Response.Status.OK,
//                mime,
//                context.assets.open("www/$uri") // prefix with www because your files are not in the root folder in assets
//            )
//        } catch (e: Exception) {
//            val message = "Failed to load asset $uri because $e"
//            println(message)
//            e.printStackTrace()
//            return NanoHTTPD.newFixedLengthResponse(message)
//        }
//    }
//}