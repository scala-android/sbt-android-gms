package com.example

import android.app.Activity
import android.os.Bundle

class MainActivity extends Activity with TypedFindView {
    lazy val textview = findView(TR.text)

    /** Called when the activity is first created. */
    override def onCreate(savedInstanceState: Bundle): Unit = {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        textview.setText("Hello world, from Foo")
        println(R.xml.global_tracker)
        println(R.string.gcm_defaultSenderId)
        println(R.string.google_crash_reporting_api_key)
        println(R.string.google_app_id)
        println(R.string.default_web_client_id)
    }
}
