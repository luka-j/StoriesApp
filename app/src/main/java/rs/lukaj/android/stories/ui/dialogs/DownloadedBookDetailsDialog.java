package rs.lukaj.android.stories.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.network.Books;
import rs.lukaj.minnetwork.Network;

import static rs.lukaj.minnetwork.Network.Response.RESPONSE_OK;

/**
 * Created by luka on 3.1.18..
 */

public class DownloadedBookDetailsDialog extends DialogFragment implements Network.NetworkCallbacks<String> {

    private static final String ARG_ID             = "id"; //use id to get rating
    private static final String ARG_TITLE          = "aTitle";
    private static final String ARG_GENRES         = "aGenres";
    private static final String ARG_AUTHOR         = "aAuthor";
    private static final String ARG_DATE_PUBLISHED = "aDate";
    private static final String ARG_CHAPTERS_COUNT = "aChaptersNo";
    private static final String ARG_DESCRIPTION    = "aDesc";
    private static final int REQUEST_RATING        = 0;

    public static DownloadedBookDetailsDialog newInstance(String id, String title, String genres, String author,
                                                          long date, int chaptersNo, String description) {

        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_GENRES, genres);
        args.putString(ARG_AUTHOR, author);
        args.putString(ARG_DATE_PUBLISHED, DateFormat.getDateInstance().format(new Date(date)));
        args.putString(ARG_CHAPTERS_COUNT, String.valueOf(chaptersNo));
        args.putString(ARG_DESCRIPTION, description);
        DownloadedBookDetailsDialog fragment = new DownloadedBookDetailsDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        exceptionHandler = new ExceptionHandler.DefaultHandler((AppCompatActivity)context);
    }

    private ExceptionHandler exceptionHandler;
    private LinearLayout            ratingLayout;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle                 args    = getArguments();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        View                   view    = getActivity().getLayoutInflater().inflate(R.layout.dialog_book_details, null, false);
        ratingLayout = view.findViewById(R.id.details_rating_layout);
        ratingLayout.setVisibility(View.GONE);
        view.findViewById(R.id.details_chaptersno_layout).setVisibility(View.VISIBLE);
        view.<TextView>findViewById(R.id.details_prop_title).setText(args.getString(ARG_TITLE));
        view.<TextView>findViewById(R.id.details_prop_genres).setText(args.getString(ARG_GENRES));
        view.<TextView>findViewById(R.id.details_prop_author).setText(args.getString(ARG_AUTHOR));
        view.<TextView>findViewById(R.id.details_prop_date_published).setText(args.getString(ARG_DATE_PUBLISHED));
        view.<TextView>findViewById(R.id.details_prop_chapters).setText(args.getString(ARG_CHAPTERS_COUNT));
        view.<TextView>findViewById(R.id.details_prop_description).setText(args.getString(ARG_DESCRIPTION));
        Books.getAverageRating(REQUEST_RATING, args.getString(ARG_ID), exceptionHandler, this);
        return builder.customView(view, true)
                      .title(R.string.dialog_details_title)
                      .positiveText(R.string.close)
                      .autoDismiss(true)
                      .show();
    }

    @Override
    public void onRequestCompleted(int id, Network.Response<String> response) {
        if(id == REQUEST_RATING) {
            if (response.responseCode == RESPONSE_OK) {
                getActivity().runOnUiThread(() -> {
                    ratingLayout.setVisibility(View.VISIBLE);
                    ratingLayout.<TextView>findViewById(R.id.details_prop_rating)
                            .setText(response.responseData.trim() + " / 5");
                });
            }
        }
    }

    @Override
    public void onExceptionThrown(int id, Throwable throwable) {
        if(throwable instanceof IOException)
            exceptionHandler.handleIOException((IOException) throwable);
        else if(throwable instanceof Exception)
            exceptionHandler.handleUnknownNetworkException((Exception)throwable);
        else if(throwable instanceof Error)
            throw (Error)throwable;
    }
}
