package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rs.lukaj.android.stories.R;
import rs.lukaj.stories.runtime.State;

/**
 * Created by luka on 22.11.17..
 */

public class SetVariableDialog extends DialogFragment {
    private static State state; //this is so gonna break one day
    //(or as soon as someone turns off the screen on this dialog for long enough)

    public static SetVariableDialog newInstance(State variables) {
        SetVariableDialog.state = variables; //this is fugly, but so is requirement that everything must be parcelable
        return new SetVariableDialog();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(state == null)
            dismiss(); //workaround to make it break less
    }

    private Callbacks callbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (Callbacks)activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());

        View                 view     = getActivity().getLayoutInflater().inflate(R.layout.dialog_set_variable, null, false);
        AutoCompleteTextView variable = view.findViewById(R.id.dialog_setvar_variable);
        EditText             value    = view.findViewById(R.id.dialog_setvar_value);
        List<String>         allVars  = new ArrayList<>(state.getVariableNames());
        Collections.sort(allVars);
        ArrayAdapter<String> varsAdapter = new ArrayAdapter<>(getActivity(),
                                                              android.R.layout.simple_spinner_dropdown_item,
                                                              allVars);
        variable.setAdapter(varsAdapter);

        return builder.customView(view, false)
                      .title(R.string.dialog_setvar_title)
                      .positiveText(R.string.set)
                      .negativeText(R.string.cancel)
                      .autoDismiss(true)
                      .onPositive((materialDialog, dialogAction) ->
                                          callbacks.onFinishedSetVar(variable.getText().toString(),
                                                                     value.getText().toString()))
                      .onNegative((m, d) -> callbacks.onCancelSetVar())
                      .show();
    }

    public interface Callbacks {
        void onFinishedSetVar(String variable, String value);
        default void onCancelSetVar() {}
    }
}
