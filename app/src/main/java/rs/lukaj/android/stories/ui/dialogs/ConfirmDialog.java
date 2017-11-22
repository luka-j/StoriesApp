package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Created by luka on 11.1.16..
 */
public class ConfirmDialog extends DialogFragment {
    private static final String ARG_TITLE    = "aTitle";
    private static final String ARG_MESSAGE  = "aMsg";
    private static final String ARG_POSITIVE = "aPositive";
    private static final String ARG_NEGATIVE = "aNegative";

    private Callbacks callbacks;
    @StringRes private int title, message, positiveText, negativeText;

    public static ConfirmDialog newInstance(@StringRes int title, @StringRes int message,
                                            @StringRes int positiveText, @StringRes int negativeText) {
        Bundle        args     = new Bundle();
        ConfirmDialog fragment = new ConfirmDialog();
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_MESSAGE, message);
        args.putInt(ARG_POSITIVE, positiveText);
        args.putInt(ARG_NEGATIVE, negativeText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (Callbacks) activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        title = args.getInt(ARG_TITLE);
        message = args.getInt(ARG_MESSAGE);
        positiveText = args.getInt(ARG_POSITIVE);
        negativeText = args.getInt(ARG_NEGATIVE);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());

        return builder.title(title)
                      .content(message)
                      .positiveText(positiveText)
                      .negativeText(negativeText)
                      .autoDismiss(true)
                      .onPositive((materialDialog, dialogAction) -> callbacks.onPositive(ConfirmDialog.this))
                      .onNegative((materialDialog, dialogAction) -> callbacks.onNegative(ConfirmDialog.this))
                      .show();
    }

    public interface Callbacks {
        void onPositive(DialogFragment dialog);
        void onNegative(DialogFragment dialog);
    }
}
