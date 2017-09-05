package com.cammy.locationupdates.fragments

import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import com.cammy.locationupdates.R
import kotlinx.android.synthetic.main.fragment_edittext_dialog.view.*


class AlertEditTextDialogFragment : DialogFragment() {

    var dialogListener: DialogInterface.OnClickListener? = null
    var mEditLayout: View? = null

    private var mOnClickListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, which ->
        if (dialogListener != null) {
            dialogListener!!.onClick(dialog, which)
        }
    }

    val userInput: CharSequence
        get() = mEditLayout?.user_input?.text!!


    override fun onCreateDialog(savedInstanceState: Bundle?): AppCompatDialog {
        mEditLayout = LayoutInflater.from(activity).inflate(R.layout.fragment_edittext_dialog, null)

        val titleText = arguments.getCharSequence(ARG_TITLE)
        val contentText = arguments.getCharSequence(ARG_CONTENT)
        val hintText = arguments.getCharSequence(ARG_HINT)
        val positiveButtonText = arguments.getCharSequence(ARG_POSITIVE_BUTTON_LABEL)
        val negativeButtonText = arguments.getCharSequence(ARG_NEGATIVE_BUTTON_LABEL)
        val inputType = arguments.getInt(ARG_INPUT_TYPE, InputType.TYPE_CLASS_TEXT)

        mEditLayout?.user_input?.setText(contentText)
        mEditLayout?.user_input?.hint = hintText
        mEditLayout?.user_input?.setRawInputType(inputType)
        mEditLayout?.title_text?.text = titleText.toString().toUpperCase()

        return AlertDialog.Builder(activity)
                .setView(mEditLayout)
                .setPositiveButton(positiveButtonText, mOnClickListener)
                .setNegativeButton(negativeButtonText, mOnClickListener)
                .create()
    }

    companion object {

        private val ARG_TITLE = "title"
        private val ARG_CONTENT = "content"
        private val ARG_HINT = "hint"
        private val ARG_POSITIVE_BUTTON_LABEL = "positiveButtonLabel"
        private val ARG_NEGATIVE_BUTTON_LABEL = "negativeButtonLabel"
        private val ARG_INPUT_TYPE = "inputType"

        fun newInstance(titleText: CharSequence,
                        contentText: CharSequence,
                        hintText: CharSequence,
                        positiveButtonText: CharSequence,
                        negativeButtonText: CharSequence): AlertEditTextDialogFragment {
            val alertDialogFragment = AlertEditTextDialogFragment()
            val arguments = Bundle()
            arguments.putCharSequence(ARG_TITLE, titleText)
            arguments.putCharSequence(ARG_CONTENT, contentText)
            arguments.putCharSequence(ARG_HINT, hintText)
            arguments.putCharSequence(ARG_POSITIVE_BUTTON_LABEL, positiveButtonText)
            arguments.putCharSequence(ARG_NEGATIVE_BUTTON_LABEL, negativeButtonText)
            alertDialogFragment.arguments = arguments

            return alertDialogFragment
        }

        fun newInstance(titleText: CharSequence,
                        contentText: CharSequence,
                        hintText: CharSequence,
                        positiveButtonText: CharSequence,
                        negativeButtonText: CharSequence,
                        inputType: Int): AlertEditTextDialogFragment {
            val alertDialogFragment = AlertEditTextDialogFragment()
            val arguments = Bundle()
            arguments.putCharSequence(ARG_TITLE, titleText)
            arguments.putCharSequence(ARG_CONTENT, contentText)
            arguments.putCharSequence(ARG_HINT, hintText)
            arguments.putCharSequence(ARG_POSITIVE_BUTTON_LABEL, positiveButtonText)
            arguments.putCharSequence(ARG_NEGATIVE_BUTTON_LABEL, negativeButtonText)
            arguments.putInt(ARG_INPUT_TYPE, inputType)
            alertDialogFragment.arguments = arguments

            return alertDialogFragment
        }
    }
}