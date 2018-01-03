package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import rs.lukaj.android.stories.R;

/**
 * Created by luka on 1.1.18..
 */

public class ExploreBookDetailsDialog extends DialogFragment {

    private static final String ARG_TITLE = "aTitle";
    private static final String ARG_GENRES = "aGenres";
    private static final String ARG_AUTHOR = "aAuthor";
    private static final String ARG_DATE_PUBLISHED = "aDate";
    private static final String ARG_DOWNLOADS = "aDls";
    private static final String ARG_RATING = "aRating";
    private static final String ARG_DESCRIPTION = "aDesc";

    public static ExploreBookDetailsDialog newInstance(String title, String genres, String author, long dateMillis,
                                                       double rating, String description) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_GENRES, genres);
        args.putString(ARG_AUTHOR, author);
        args.putString(ARG_DATE_PUBLISHED, DateFormat.getDateInstance().format(new Date(dateMillis)));
        args.putString(ARG_RATING, String.valueOf(DecimalFormat.getInstance().format(rating)) + " / 5");
        args.putString(ARG_DESCRIPTION, description);
        ExploreBookDetailsDialog fragment = new ExploreBookDetailsDialog();
        fragment.setArguments(args);
        return fragment;
    }

    private Callbacks callbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (Callbacks)activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        View                   view    = getActivity().getLayoutInflater().inflate(R.layout.dialog_book_details, null, false);
        view.<TextView>findViewById(R.id.details_prop_title).setText(args.getString(ARG_TITLE));
        view.<TextView>findViewById(R.id.details_prop_genres).setText(args.getString(ARG_GENRES));
        view.<TextView>findViewById(R.id.details_prop_author).setText(args.getString(ARG_AUTHOR));
        view.<TextView>findViewById(R.id.details_prop_date_published).setText(args.getString(ARG_DATE_PUBLISHED));
        view.<TextView>findViewById(R.id.details_prop_rating).setText(args.getString(ARG_RATING));
        view.<TextView>findViewById(R.id.details_prop_description).setText(args.getString(ARG_DESCRIPTION));

        return builder.customView(view, true)
                      .title(R.string.dialog_details_title)
                      .positiveText(R.string.close)
                      .neutralText(R.string.download)
                      .autoDismiss(true)
                      .onNeutral((m,d) -> callbacks.onDownloadBook(this))
                      .show();
    }

    public interface Callbacks {
        void onDownloadBook(DialogFragment dialog);
    }
}
