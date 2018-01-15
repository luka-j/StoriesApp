package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * A yes-no-cancel dialog. Optionally, cancel (neutral) button can be left out.
 * Created by luka on 11.1.16.
 */
public class ConfirmDialog extends DialogFragment {
    private static final String ARG_TITLE    = "aTitle";
    private static final String ARG_MESSAGE  = "aMsg";
    private static final String ARG_POSITIVE = "aPositive";
    private static final String ARG_NEGATIVE = "aNegative";
    private static final String ARG_NEUTRAL  = "aNeutral";

    private Callbacks callbacks = null;
    @StringRes private int title, message, positiveText, negativeText, neutralText;

    public static ConfirmDialog newInstance(@StringRes int title, @StringRes int message,
                                            @StringRes int positiveText, @StringRes int negativeText) {
        return newInstance(title, message, positiveText, negativeText, 0);
    }

    public static ConfirmDialog newInstance(@StringRes int title, @StringRes int message,
                                            @StringRes int positiveText, @StringRes int negativeText,
                                            @StringRes int neutralText) {
        Bundle        args     = new Bundle();
        ConfirmDialog fragment = new ConfirmDialog();
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_MESSAGE, message);
        args.putInt(ARG_POSITIVE, positiveText);
        args.putInt(ARG_NEGATIVE, negativeText);
        args.putInt(ARG_NEUTRAL, neutralText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof Callbacks && callbacks == null)
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
        neutralText = args.getInt(ARG_NEUTRAL);
    }

    public ConfirmDialog registerCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        if(callbacks == null) callbacks = d -> {};

        builder.title(title)
               .content(message)
               .positiveText(positiveText)
               .negativeText(negativeText)
               .autoDismiss(true)
               .onPositive((m,d) -> callbacks.onPositive(this))
               .onNegative((m, d) -> callbacks.onNegative(this));
        if(neutralText != 0)
            builder.neutralText(neutralText).onNeutral((m, d) -> callbacks.onNeutral(this));

        return builder.show();
    }

    public interface Callbacks {
        void onPositive(DialogFragment dialog);
        default void onNegative(DialogFragment dialog) {}
        default void onNeutral(DialogFragment dialog) {}
    }
}
