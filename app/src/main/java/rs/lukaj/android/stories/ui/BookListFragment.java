package rs.lukaj.android.stories.ui;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.io.Books;
import rs.lukaj.android.stories.model.Book;

/**
 * A placeholder fragment containing a simple view.
 */
public class BookListFragment extends Fragment implements Books.Callbacks {

    private static final String TAG          = "ui.MainActivityFragment";
    private static final int CARD_WIDTH_DP = 108;

    private RecyclerView recycler;
    private BooksAdapter adapter;

    private AndroidFiles files;
    private NullDisplay  display;

    public BookListFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book_list, container, false);
        recycler = v.findViewById(R.id.books_recycler_view);

        DisplayMetrics            displayMetrics = getResources().getDisplayMetrics();
        float                     dpWidth        = displayMetrics.widthPixels / displayMetrics.density;
        int                       noOfColumns    = (int) (dpWidth / CARD_WIDTH_DP);
        final LinearLayoutManager layoutManager  = new GridLayoutManager(getActivity(), noOfColumns);
        recycler.setLayoutManager(layoutManager);
        files = new AndroidFiles(getContext());
        display = new NullDisplay();
        registerForContextMenu(recycler);

        setData();

        return v;
    }

    public void setData() {
        if (adapter == null) {
            adapter = new BooksAdapter(new ArrayList<>());
            recycler.setAdapter(adapter);
        }
        Books.loadAllBooks(files, display, this);
    }

    @Override
    public void onBooksLoaded(List<Book> books) {
        Log.i(TAG, "Books loaded; size: " + books.size());
        adapter.books = books;
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit_book:
                Intent i = new Intent(getContext(), BookEditorActivity.class);
                i.putExtra(BookEditorActivity.EXTRA_BOOK_NAME, adapter.selectedBook.getName());
                startActivity(i);
                return true;
        }
        return super.onContextItemSelected(item);
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
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

            titleTextView = itemView.findViewById(R.id.card_book_title);
            genresTextView = itemView.findViewById(R.id.card_book_genres);
            coverImage = itemView.findViewById(R.id.card_book_image);
            itemView.setOnCreateContextMenuListener(this);
        }

        public void bindBook(Book book) {
            this.book = book;
            titleTextView.setText(book.getTitle());
            genresTextView.setText(Utils.listToString(book.getGenres()));
            if(book.getImage() != null)
                coverImage.setImageBitmap(Utils.loadImage(book.getImage(), coverImage.getWidth()));
            else
                coverImage.setImageBitmap(null);
        }

        @Override
        public void onClick(View v) {
            Intent intent  = new Intent(getContext(), StoryActivity.class);
            intent.putExtra(StoryActivity.EXTRA_BOOK_NAME, book.getName());
            startActivity(intent);
        }

        @Override
        public boolean onLongClick(View v) {
            adapter.selectedBook = book;
            return false; //this shouldn't consume the click
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(Menu.NONE, R.id.menu_edit_book, 1, R.string.edit_book);
        }

    /*@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if(permission >= Group.PERM_MODIFY)
            getActivity().getMenuInflater().inflate(R.menu.context_course, menu);
    }*/
    }

    private class BooksAdapter extends RecyclerView.Adapter<BookHolder> {

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
        }

        @Override
        public int getItemCount() {
            return books.size();
        }
    }

}