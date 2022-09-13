package com.vmb.mlkitscanner

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object CodeScanSuccessDialog {
    private var dialog: Dialog? = null

    interface DialogueCallBack {
        fun successDialogueCallBack()
    }

    fun ComingSoonDialog(
        context: Context, text: String, dialogueCallBack: DialogueCallBack

    ) {

        if (dialog != null) return

        dialog = Dialog(context, R.style.PauseDialog)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setGravity(Gravity.CENTER)
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        val window = dialog!!.window
        window!!.setGravity(Gravity.CENTER)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dialog?.setContentView(R.layout.scan_code_dialoge)
        dialog?.show()

        val mTitle = dialog?.findViewById<View>(R.id.title) as TextView
        val ok = dialog?.findViewById<View>(R.id.textOk) as TextView

        mTitle.text = text


        ok.setOnClickListener(android.view.View.OnClickListener {
            dialogueCallBack.successDialogueCallBack()
            dialog?.dismiss()
            dialog = null
        })


    }
}