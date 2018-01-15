package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import com.afollestad.materialdialogs.MaterialDialog;

import rs.lukaj.android.stories.R;

/**
 * Dialog showing 1-5 NumberPicker and prompting the user to rate the book.
 * Created by luka on 8.1.18.
 */

public class RateBookDialog extends DialogFragment {

    private Callbacks callbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof ConfirmDialog.Callbacks && callbacks == null)
            callbacks = (Callbacks)activity;
    }

    public RateBookDialog registerCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        View                   view = getActivity().getLayoutInflater().inflate(R.layout.dialog_rate_book, null, false);
        NumberPicker picker = view.findViewById(R.id.rate_book_numberpicker);
        picker.setMinValue(1);
        picker.setMaxValue(5);

        return builder.customView(picker, false)
                      .title(R.string.dialog_ratebook_title)
                      .positiveText(R.string.rate)
                      .negativeText(R.string.cancel)
                      .autoDismiss(true)
                      .onPositive((m,d) -> callbacks.onRatingSelected(this, picker.getValue()))
                      .show();
    }

    public interface Callbacks {
        void onRatingSelected(RateBookDialog dialog, int rating);
    }
}
