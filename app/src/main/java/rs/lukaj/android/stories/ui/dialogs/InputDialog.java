package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import rs.lukaj.android.stories.Utils;

/**
 * Dialog prompting user for input.
 * Created by luka on 23.7.15.
 */
public class InputDialog extends DialogFragment {
    private Callbacks callbacks;
    private static final String ARG_TITLE    = "aTitle";
    private static final String ARG_POSITIVE = "aPositive";
    private static final String ARG_NEGATIVE = "aNegative";
    private static final String ARG_TEXT     = "aText";
    private static final String ARG_INITIAL  = "aInitialText";
    private static final String ARG_HINT     = "aHint";
    private static final String ARG_MAX_LEN  = "aMaxLen";
    private static final String ARG_FILE     = "aFile";

    public static InputDialog newInstance(@StringRes int title, String text, @StringRes int positiveText,
                                          @StringRes int negativeText, String initialText, String hint, int maxLen,
                                          boolean isFileInput) {
        InputDialog f    = new InputDialog();
        Bundle      args = new Bundle();
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_POSITIVE, positiveText);
        args.putInt(ARG_NEGATIVE, negativeText);
        args.putString(ARG_INITIAL, initialText);
        args.putString(ARG_TEXT, text);
        args.putString(ARG_HINT, hint);
        args.putInt(ARG_MAX_LEN, maxLen);
        args.putBoolean(ARG_FILE, isFileInput);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof Callbacks && callbacks == null)
            callbacks = (Callbacks) activity;
    }

    public InputDialog registerCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle                 args    = getArguments();
        String                 text    = args.getString(ARG_TEXT);
        if(callbacks == null) callbacks = (d,s) -> {};
        final boolean isFileInput = getArguments().getBoolean(ARG_FILE);
        int negativeId = args.getInt(ARG_NEGATIVE);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        //TextInputLayout til = new TextInputLayout(getActivity());
        final EditText input = new EditText(getActivity());
        input.requestFocus();
        input.setSelection(input.getText().length());
        //til.addView(input);
        builder.title(args.getInt(ARG_TITLE))
               .positiveText(args.getInt(ARG_POSITIVE))
               .inputRange(0, args.getInt(ARG_MAX_LEN))
               .input(args.getString(ARG_HINT),
                      args.getString(ARG_INITIAL),
                      false,
                      (materialDialog, charSequence) -> {
                          if (isFileInput && (Utils.contains(charSequence, '/') ||
                                              Utils.equals(charSequence, ".") || Utils.equals(charSequence, ".."))) {
                              materialDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                          }
                      })
               .cancelable(false)
               .onAny((dialog, which) -> callbacks.onFinishedInput(InputDialog.this,
                                                                   dialog.getInputEditText().getText().toString()));
        if(isFileInput)
            builder.alwaysCallInputCallback();
        if(negativeId != 0)
            builder.negativeText(negativeId);
        if(text != null)
            builder.content(text);
        return builder.show();
    }

    public interface Callbacks {
        void onFinishedInput(DialogFragment dialog, String s);
    }
}
