package com.shubham.androidwebviewcachinejsnative

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterfaceImpl(private val activity: Activity) : WebAppInterface {

    @JavascriptInterface
    override fun onMessageFromWeb(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, "JS says: $message", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    override fun webToNative(actionName: String, callBackMethod: String, jsonObject: String) {
        Toast.makeText(activity, "$actionName called with callback $callBackMethod and data: $jsonObject", Toast.LENGTH_LONG).show()
    }

    @JavascriptInterface
    override fun showAlert(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    @JavascriptInterface
    override fun closeWebView() {
        activity.runOnUiThread {
            activity.finish()
        }
    }

    @JavascriptInterface
    override fun openContacts() {
        activity.runOnUiThread {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "No Contacts app found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}