package rs.lukaj.android.stories.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextMenu;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.io.BookShelf;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.network.Books;
import rs.lukaj.android.stories.network.Users;
import rs.lukaj.android.stories.ui.dialogs.ExploreBookDetailsDialog;
import rs.lukaj.minnetwork.Network;
import rs.lukaj.stories.environment.DisplayProvider;

import static rs.lukaj.android.stories.ui.BookListFragment.TYPE_READING_HISTORY;
import static rs.lukaj.android.stories.ui.BookListFragment.TYPE_SEARCH_RESULTS;
import static rs.lukaj.minnetwork.Network.Response.RESPONSE_OK;

/**
 * Created by luka on 5.1.18..
 */

public class BookListActivity extends SingleFragmentActivity<BookListFragment> implements BookListFragment.Callbacks,
                                                                        Network.NetworkCallbacks<String> {
    /**
     * Should be one of BookListFragment.TYPE_*
     */
    public static final String EXTRA_FRAGMENT_TYPE = "booklist.extra.type";

    public static final String EXTRA_SEARCH_TITLE = "booklist.extra.search.title";
    public static final String EXTRA_SEARCH_GENRES = "booklist.extra.search.genres";
    public static final String EXTRA_SEARCH_ORDERBY = "booklist.extra.search.orderby";

    private static final int REQUEST_SEARCH_BOOKS    = 0;
    private static final int REQUEST_READING_HISTORY = 1;

    private int type;
    private ExceptionHandler exceptionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exceptionHandler = new ExceptionHandler.DefaultHandler(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        switch (type) {
            case TYPE_SEARCH_RESULTS:
                setTitle(R.string.search_results);
                break;
            case TYPE_READING_HISTORY:
                setTitle(R.string.reading_history);
                break;
        }
    }

    @Override
    protected BookListFragment createFragment() {
        type = getIntent().getIntExtra(EXTRA_FRAGMENT_TYPE, TYPE_SEARCH_RESULTS);//this gets called before onCreate
        //(techincally, onCreate calls super which calls this)
        return BookListFragment.newInstance(type);
    }

    @Override
    public void removeBook(Book book, BookListFragment fragment) {
        //we aren't removing books here
    }

    @Override
    public void onBookClick(Book book, int fragmentType) {
        ExploreBookDetailsDialog.newInstance(book.getTitle(), Utils.listToString(book.getGenres()), book.getAuthor(),
                                             book.getDate(), book.getRating(), book.getDescription())
                                .show(getFragmentManager(), book.getId());
    }

    @Override
    public void retrieveData(AndroidFiles files, DisplayProvider provider, BookShelf callbacks, int count,
                             double minRating, int type) {
        Bundle ex = getIntent().getExtras();
        switch (type) {
            case TYPE_SEARCH_RESULTS:
                Books.searchBooks(REQUEST_SEARCH_BOOKS,
                                  ex.getString(EXTRA_SEARCH_TITLE),
                                  ex.getString(EXTRA_SEARCH_GENRES),
                                  ex.getString(EXTRA_SEARCH_ORDERBY),
                                  36,
                                  exceptionHandler,
                                  this);
                break;
            case TYPE_READING_HISTORY:
                Users.getReadingList(REQUEST_READING_HISTORY, this, exceptionHandler, this);
                break;
        }
    }

    @Override
    public void createContextMenu(ContextMenu menu, Book book, int type) {
        //no context menu
    }

    @Override
    public void onRequestCompleted(int id, Network.Response<String> response) {
        List<Book> books = new ArrayList<>();
        switch (id) {
            case REQUEST_SEARCH_BOOKS:
            case REQUEST_READING_HISTORY: //response is in the same format
                if (response.responseCode == RESPONSE_OK) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.responseData);
                        for (int i = 0; i < jsonArray.length(); i++)
                            books.add(new Book(jsonArray.getJSONObject(i)));
                    } catch (JSONException e) {
                        exceptionHandler.handleJsonException();
                    }
                }
                getFragment().replaceBooks(books);
                break;
        }
    }

    @Override
    public void onExceptionThrown(int i, Throwable throwable) {
        getFragment().replaceBooks(null);
        if(throwable instanceof IOException)
            exceptionHandler.handleIOException((IOException) throwable);
        else if(throwable instanceof Exception)
            exceptionHandler.handleUnknownNetworkException((Exception)throwable);
        else if(throwable instanceof Error)
            throw (Error)throwable;
    }
}
