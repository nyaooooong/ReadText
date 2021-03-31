package com.nrw.readtext

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class GoToDialogFragment : DialogFragment() {
    private lateinit var listener: GotoDialogListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface GotoDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment, position: String)
        fun getMaxLineCount():String;
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            val view = inflater.inflate(R.layout.dialog_goto, null);
            view.findViewById<EditText>(R.id.goto_position).hint = listener.getMaxLineCount()
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(R.string.go_to, DialogInterface.OnClickListener { _, _ ->
                                val position = this.dialog!!.findViewById<EditText>(R.id.goto_position).text.toString()
                                listener.onDialogPositiveClick(this, position)
                            })
                    .setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { _, _ -> this.dialog?.cancel() })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as GotoDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() + " must implement NoticeDialogListener"))
        }
    }
}
