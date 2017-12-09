package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.afollestad.materialdialogs.MaterialDialog;

import rs.lukaj.android.stories.R;

/**
 * Created by luka on 9.12.17..
 */

public class AddInputDialog extends DialogFragment {


    private Callbacks callbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (Callbacks)activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());

        View     view     = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_input, null, false);
        EditText variable    = view.findViewById(R.id.dialog_addinput_variable);
        EditText hint = view.findViewById(R.id.dialog_addinput_hint);
        return builder.customView(view, true)
                      .title(R.string.dialog_addinput_title)
                      .positiveText(R.string.add)
                      .negativeText(R.string.cancel)
                      .autoDismiss(true)
                      .onPositive((materialDialog, dialogAction) ->
                                          callbacks.onInputAdded(variable.getText().toString(),
                                                                 hint.getText().toString()))
                      .onNegative((m, d) -> callbacks.onAddInputCancelled())
                      .show();
    }

    public interface Callbacks {
        void onInputAdded(String variable, String hint);
        default void onAddInputCancelled() {}
    }
}
