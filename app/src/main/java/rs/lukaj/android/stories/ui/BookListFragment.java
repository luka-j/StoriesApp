package rs.lukaj.android.stories.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.android.stories.io.BookShelf;
import rs.lukaj.android.stories.io.FileUtils;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.model.User;
import rs.lukaj.android.stories.network.Books;
import rs.lukaj.android.stories.ui.dialogs.ConfirmDialog;
import rs.lukaj.android.stories.ui.dialogs.DownloadedBookDetailsDialog;
import rs.lukaj.android.stories.ui.dialogs.RateBookDialog;
import rs.lukaj.minnetwork.Network;
import rs.lukaj.stories.environment.DisplayProvider;

import static rs.lukaj.minnetwork.Network.Response.RESPONSE_OK;

/**
 * A placeholder fragment containing a simple view.
 */
public class BookListFragment extends Fragment implements BookShelf, RateBookDialog.Callbacks,
                                                          ConfirmDialog.Callbacks, Network.NetworkCallbacks<String>,
                                                          FileUtils.Callbacks {
    public static final int TYPE_DOWNLOADED = 0;
    public static final int TYPE_FORKED_CREATED = 1;
    public static final int TYPE_EXPLORE        = 2;
    public static final int TYPE_SEARCH_RESULTS   = 3;
    public static final int TYPE_READING_HISTORY = 4;
    public static final int TYPE_MY_PUBLISHED_BOOKS = 5;

    private static final String TAG                      = "ui.BookListFragment";
    private static final String ARG_TYPE                 = "ui.BookListFragment.type";
    private static final int    CARD_WIDTH_DP            = 108;
    private static final int    REQUEST_LOGIN_TO_PUBLISH = 0;

    private static final int INITIAL_EXPLORE_SIZE              = 24;
    private static final int EXPLORE_INFINITESCROLL_STEP       = 18;
    private static final String TAG_DETAILS_DIALOG             = "BookListFragment.dialog.details";
    private static final java.lang.String TAG_RATE_BOOK_DIALOG = "BookListFragment.dialog.ratebook";
    private static final String TAG_ALREADY_PUBLISHED_DIALOG   = "BookListFragment.dialog.alreadypublished";
    private static final int REQUEST_RATE_BOOK                 = 1;
    private static final int REQUEST_COPY_TO_SD                = 2;

    private RecyclerView recycler;
    private CircularProgressView progressView;
    private PoliteSwipeRefreshLayout swipe;
    private BooksAdapter adapter;
    private int              type;
    private AndroidFiles     files;
    private NullDisplay      display;
    private ExceptionHandler exceptionHandler;
    private Callbacks        callbacks;
    private View firstBookView;

    public BookListFragment() {
    }

    public static BookListFragment newInstance(int type) {
        BookListFragment fragment = new BookListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = (Callbacks)context;
        exceptionHandler = new ExceptionHandler.DefaultHandler((AppCompatActivity) getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_book_list, container, false);
        recycler = v.findViewById(R.id.books_recycler_view);
        progressView = v.findViewById(R.id.books_loading_cpv);
        swipe = v.findViewById(R.id.book_list_swipe);
        type = getArguments().getInt(ARG_TYPE);

        DisplayMetrics            displayMetrics = getResources().getDisplayMetrics();
        float                     dpWidth        = displayMetrics.widthPixels / displayMetrics.density;
        int                       noOfColumns    = (int) (dpWidth / CARD_WIDTH_DP);
        final LinearLayoutManager layoutManager  = new GridLayoutManager(getActivity(), noOfColumns);
        recycler.setLayoutManager(layoutManager);
        files = new AndroidFiles(getContext());
        display = new NullDisplay();
        registerForContextMenu(recycler);

        setData();

        swipe.setOnChildScrollUpListener(() -> adapter != null && adapter.getItemCount() > 0 && layoutManager.findFirstCompletelyVisibleItemPosition() > 0);
        swipe.setOnRefreshListener(this::setData);
        swipe.setColorSchemeResources(R.color.refresh_progress_1, R.color.refresh_progress_2, R.color.refresh_progress_3);
        return v;
    }

    void setData() {
        if(recycler == null) return; //too early

        if (adapter == null) {
            adapter = new BooksAdapter(new ArrayList<>());
            recycler.setAdapter(adapter);
        } else {
            adapter.lastFetchPosition = INITIAL_EXPLORE_SIZE;
            int sz = adapter.books.size();
            adapter.books.clear();
            adapter.notifyItemRangeRemoved(0, sz);
        }
        callbacks.retrieveData(files, display, this, INITIAL_EXPLORE_SIZE, -1, type);
        startLoading();
    }

    void startLoading() {
        progressView.setVisibility(View.VISIBLE);
    }
    void stopLoading() {
        progressView.setVisibility(View.GONE);
        swipe.setRefreshing(false);
    }

    @Override
    public void replaceBooks(List<Book> books) {
        Activity ac = getActivity();
        if(ac == null) return;
        ac.runOnUiThread(() -> {
            if(books != null) {
                //Log.i(TAG, "Books loaded; size: " + books.size());
                adapter.books = books;
                adapter.notifyDataSetChanged();
            }
            stopLoading();
        });
    }

    public void addBooks(List<Book> books) {
        Activity ac = getActivity();
        if(ac == null) return;
        ac.runOnUiThread(() -> {
            int currLen = adapter.books.size();
            adapter.books.addAll(books);
            adapter.notifyItemRangeInserted(currLen, books.size());
            stopLoading();
        });
    }

    public View getViewForShowcase() {
        return firstBookView;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getGroupId() != type) return false; //because fragments are a half-assed implementation
        switch (item.getItemId()) {
            case R.id.menu_item_edit_book:
                Intent i = new Intent(getContext(), BookEditorActivity.class);
                i.putExtra(BookEditorActivity.EXTRA_BOOK_NAME, adapter.selectedBook.getName());
                startActivity(i);
                return true;

            case R.id.menu_item_fork_book:
                FileUtils.copyDirectory(REQUEST_COPY_TO_SD, files.getRootDirectory(adapter.selectedBook.getName()),
                                        new File(AndroidFiles.SD_BOOKS, UUID.randomUUID().toString()), this);
                //yeah, I have just given up naming completely, too complicated and yet too useless to care
                startLoading();
                return true;

            case R.id.menu_item_publish_book:
                if(!User.isLoggedIn(getContext())) {
                    Intent loginActivity = new Intent(getContext(), LoginActivity.class);
                    startActivityForResult(loginActivity, REQUEST_LOGIN_TO_PUBLISH);
                } else {
                    if(!adapter.selectedBook.isAlreadyPublished())
                        publish(adapter.selectedBook);
                    else
                        ConfirmDialog.newInstance(R.string.dialog_alreadypublished_title,
                                                  R.string.dialog_alreadypublished_text,
                                                  R.string.publish, R.string.cancel)
                                     .registerCallbacks(this)
                                     .show(getActivity().getFragmentManager(), TAG_ALREADY_PUBLISHED_DIALOG);
                }
                return true;

            case R.id.menu_item_remove_book:
                callbacks.removeBook(adapter.selectedBook, this);
                return true;

            case R.id.menu_item_see_details:
                Book b = adapter.selectedBook;
                DownloadedBookDetailsDialog.newInstance(b.getId(), b.getTitle(), Utils.listToString(b.getGenres()),
                                                        b.getAuthor(), b.getDate(), b.getChapterCount(),
                                                        b.getDescription())
                                           .show(getActivity().getFragmentManager(), TAG_DETAILS_DIALOG);
                return true;

            case R.id.menu_item_rate_book:
                new RateBookDialog().registerCallbacks(this)
                                    .show(getActivity().getFragmentManager(), TAG_RATE_BOOK_DIALOG);
                return true;

        }
        return super.onContextItemSelected(item);
    }

    private void publish(Book book) {
        Intent i = new Intent(getContext(), PublishBookActivity.class);
        i.putExtra(PublishBookActivity.EXTRA_BOOK, book.getName());
        startActivity(i);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_LOGIN_TO_PUBLISH) {
            publish(adapter.selectedBook);
        }
    }

    @Override
    public void onRatingSelected(RateBookDialog dialog, int rating) {
        Books.rateBook(REQUEST_RATE_BOOK, getContext(), adapter.selectedBook.getId(), rating, exceptionHandler, this);
    }

    @Override
    public void onRequestCompleted(int id, Network.Response<String> response) {
        switch (id) {
            case REQUEST_RATE_BOOK:
                if(response.responseCode == RESPONSE_OK) {
                    Toast.makeText(getContext(), R.string.toast_book_rated, Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public void onExceptionThrown(int i, Throwable throwable) {
        if(throwable instanceof IOException)
            exceptionHandler.handleIOException((IOException) throwable);
        else if(throwable instanceof Exception)
            exceptionHandler.handleUnknownNetworkException((Exception)throwable);
        else if(throwable instanceof Error)
            throw (Error)throwable;
    }

    @Override
    public void onFileOperationCompleted(int operationId) {
        if(operationId == REQUEST_COPY_TO_SD) {
            callbacks.afterBookForked(adapter.selectedBook);
            stopLoading();
        }
    }

    @Override
    public void onIOException(int operationId, IOException ex) {
        exceptionHandler.handleBookIOException(ex);
        stopLoading();
    }

    @Override
    public void onPositive(DialogFragment dialog) {
        if(TAG_ALREADY_PUBLISHED_DIALOG.equals(dialog.getTag())) {
            publish(adapter.selectedBook);
        }
    }

    private class BookHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
                                                                        View.OnLongClickListener,
                                                                        View.OnCreateContextMenuListener {
        private final TextView titleTextView;
        private final TextView genresTextView;
        private final ImageView coverImage;

        private Book book;

        public BookHolder(View itemView) {
            super(itemView);
            if(firstBookView == null) firstBookView = itemView;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

            titleTextView = itemView.findViewById(R.id.card_book_title);
            genresTextView = itemView.findViewById(R.id.card_book_genres);
            coverImage = itemView.findViewById(R.id.card_book_image);
        }

        public void bindBook(Book book) {
            this.book = book;
            titleTextView.setText(book.getTitle());
            genresTextView.setText(Utils.listToString(book.getGenres()));
            int size = getResources().getDimensionPixelSize(R.dimen.card_image_width);
            if((type == TYPE_EXPLORE || type == TYPE_SEARCH_RESULTS || type == TYPE_READING_HISTORY) && book.hasCover())
                Books.downloadCover(getActivity(), book.getId(), coverImage, size, exceptionHandler);
            else if(book.getImage() != null)
                coverImage.setImageBitmap(BitmapUtils.loadImage(book.getImage(), size));
            else
                coverImage.setImageBitmap(null);
        }

        @Override
        public void onClick(View v) {
            callbacks.onBookClick(book, type);
        }

        @Override
        public boolean onLongClick(View v) {
            adapter.selectedBook = book;
            return false; //this shouldn't consume the click
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            callbacks.createContextMenu(menu, book, type);
        }
    }

    private class BooksAdapter extends RecyclerView.Adapter<BookHolder> {
        private int lastFetchPosition = INITIAL_EXPLORE_SIZE;

        private Book       selectedBook;
        private List<Book> books;

        public BooksAdapter(List<Book> books) {
            this.books = books;
        }

        @Override
        public BookHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.card_book,
                                               parent,
                                               false);
            return new BookHolder(view);
        }

        @Override
        public void onBindViewHolder(BookHolder holder, int position) {
            holder.bindBook(books.get(position));
            if(type == TYPE_EXPLORE && lastFetchPosition - position < 4) {
                callbacks.retrieveData(files, display, BookListFragment.this, EXPLORE_INFINITESCROLL_STEP,
                                       books.get(books.size()-1).getRanking(), type);
                lastFetchPosition += EXPLORE_INFINITESCROLL_STEP;
            }
        }

        @Override
        public int getItemCount() {
            return books.size();
        }
    }

    public interface Callbacks {
        void removeBook(Book book, BookListFragment fragment);
        void onBookClick(Book book, int fragmentType);
        void retrieveData(AndroidFiles files, DisplayProvider provider, BookShelf callbacks, int count,
                          double minRating, int type);
        void createContextMenu(ContextMenu menu, Book book, int type);
        void afterBookForked(Book book);
    }
}