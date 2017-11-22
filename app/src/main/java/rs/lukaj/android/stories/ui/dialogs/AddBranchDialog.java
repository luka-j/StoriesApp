package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import rs.lukaj.android.stories.R;
import rs.lukaj.stories.runtime.State;

/**
 * Created by luka on 20.11.17..
 */
public class AddBranchDialog extends DialogFragment {

    private static State state; //this is so gonna break one day
    //(or as soon as someone turns off the screen on this dialog for long enough)

    public static AddBranchDialog newInstance(State variables) {
        AddBranchDialog.state = variables; //this is fugly, but so is requirement that everything must be parcelable
        return new AddBranchDialog();
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

        View         view       = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_branch, null, false);
        Spinner      variable   = view.findViewById(R.id.dialog_addbranch_variable);
        Spinner      op         = view.findViewById(R.id.dialog_addbranch_op);
        EditText     value      = view.findViewById(R.id.dialog_addbranch_value);
        Set<String>  varNameSet =state.getVariableNames();
        //varNameSet.remove("True");
        //varNameSet.remove("False"); //use constants from stories lib for this
        List<String> allVars    = new ArrayList<>(varNameSet);
        Collections.sort(allVars);
        ArrayAdapter<String> varsAdapter = new ArrayAdapter<>(getActivity(),
                                                              android.R.layout.simple_spinner_dropdown_item,
                                                              allVars);
        variable.setAdapter(varsAdapter);
        op.setAdapter(new ArrayAdapter<>(getActivity(),
                                         R.layout.support_simple_spinner_dropdown_item,
                                         new String[]{"=", "!=", "<", "<=", ">", ">="}));

        return builder.customView(view, false)
                .title(R.string.dialog_addbranch_title)
                .positiveText(R.string.add)
                .negativeText(R.string.cancel)
                .autoDismiss(true)
                .onPositive((materialDialog, dialogAction) ->
                                    callbacks.onFinishedAddBranch(variable.getSelectedItem().toString(),
                                                                  op.getSelectedItem().toString(),
                                                                  value.getText().toString()))
                .onNegative((m, d) -> callbacks.onCancelAddBranch())
                .show();
    }

    public interface Callbacks {
        void onFinishedAddBranch(String variable, String op, String value);
        default void onCancelAddBranch() {}
    }
}
