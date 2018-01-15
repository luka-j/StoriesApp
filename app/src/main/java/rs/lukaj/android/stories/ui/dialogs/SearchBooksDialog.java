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

import rs.lukaj.android.stories.R;

/**
 * Dialog used for specifying search parameters.
 * Created by luka on 1.1.18.
 */

public class SearchBooksDialog extends DialogFragment {

    private static final String[] ORDER_METHODS_POS = {"best", "newest", "oldest", "worst"};

    private Callbacks callbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (Callbacks)activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        View                   view    = getActivity().getLayoutInflater().inflate(R.layout.dialog_search_books, null, false);
        EditText               title   = view.findViewById(R.id.search_title_input);
        EditText               genres  = view.findViewById(R.id.search_genres_input);
        Spinner                orderBy = view.findViewById(R.id.search_order_method);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                                                                             R.array.search_sorting_options,
                                                                             android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        orderBy.setAdapter(adapter);

        return builder.customView(view, true)
                      .title(R.string.dialog_search_title)
                      .positiveText(R.string.search)
                      .negativeText(R.string.cancel)
                      .autoDismiss(true)
                      .onPositive((m,d) -> callbacks.onInvokeSearch(title.getText().toString(), genres.getText().toString(),
                                                                    ORDER_METHODS_POS[orderBy.getSelectedItemPosition()]))
                      .show();
    }

    public interface Callbacks {
        void onInvokeSearch(String title, String genres, String order);
    }
}
