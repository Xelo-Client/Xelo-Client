package com.origin.launcher.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import com.origin.launcher.R

class LoadingDialog(context: Context) : Dialog(context) {

    private var messageView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        setContentView(view)

        setCancelable(false)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        messageView = view.findViewById(R.id.tv_message)
    }

    fun setMessage(message: CharSequence?) {
        messageView?.text = message
    }
}