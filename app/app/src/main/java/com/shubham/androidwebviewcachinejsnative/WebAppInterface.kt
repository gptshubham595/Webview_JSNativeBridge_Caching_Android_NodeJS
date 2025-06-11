package com.shubham.androidwebviewcachinejsnative

import android.webkit.JavascriptInterface

interface WebAppInterface {

    @JavascriptInterface
    fun onMessageFromWeb(message: String)

    @JavascriptInterface
    fun webToNative(actionName: String, callBackMethod: String, jsonObject: String)

    @JavascriptInterface
    fun showAlert(message: String)

    @JavascriptInterface
    fun closeWebView()

    @JavascriptInterface
    fun openContacts()
}