package rs.lukaj.android.stories.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.android.stories.model.Book;

/**
 * Created by luka on 3.9.17..
 */

public class BookEditorFragment extends Fragment {

    private static final String KEY_BOOK_NAME = "eBookName";  //todo random (or id-esque) book name, and save this for title

    private Book            book;
    private AndroidFiles    files;
    private RecyclerView recycler;
    private ChaptersAdapter adapter;
    private ExceptionHandler handler = new ExceptionHandler.DefaultHandler((AppCompatActivity) getActivity());

    public static BookEditorFragment newInstance(String bookName) {
        Bundle args = new Bundle();
        args.putString(KEY_BOOK_NAME, bookName);
        BookEditorFragment fragment = new BookEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        files = new AndroidFiles(getContext());
        String name = getArguments().getString(KEY_BOOK_NAME);
        if(files.getRootDirectory(name) != null)
            book = new Book(name, files, new NullDisplay());
        else {
            try {
                files.createBook(name);
                book = new Book(name, files, new NullDisplay());
                book.setAuthor(Book.AUTHOR_ID_ME);
            } catch (IOException e) {
                handler.handleIOException(e);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book_editor, container, false);
        recycler = v.findViewById(R.id.chapters_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChaptersAdapter();
        recycler.setAdapter(adapter);

        return v;
    }

    public void createChapter(String name) {
        int count = book.getChapterCount();
        try {
            files.createChapter(book.getName(), count+1, name);
            book.addChapter(name);
            adapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace(); // TODO: 4.9.17
        }
    }

    private class ChapterHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
                                                                           View.OnLongClickListener,
                                                                           View.OnCreateContextMenuListener {
        private final TextView titleTextView;
        private final TextView descriptionTextView;

        private int chapterNumber;

        public ChapterHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

            titleTextView = itemView.findViewById(R.id.card_chapter_name);
            descriptionTextView = itemView.findViewById(R.id.card_chapter_description);
        }

        public void bindBook(int chapterNumber) {
            this.chapterNumber = chapterNumber+1;
            titleTextView.setText(book.getChapterName(chapterNumber));
            descriptionTextView.setText(book.getChapterDescription(chapterNumber));
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getContext(), StoryEditorActivity.class);
            intent.putExtra(StoryEditorActivity.EXTRA_BOOK_NAME, book.getName());
            intent.putExtra(StoryEditorActivity.EXTRA_CHAPTER_NO, chapterNumber);
            startActivity(intent);
        }

        @Override
        public boolean onLongClick(View v) {
            adapter.selectedChapter = chapterNumber;
            return false;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            //todo delete, add description
        }

    /*@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if(permission >= Group.PERM_MODIFY)
            getActivity().getMenuInflater().inflate(R.menu.context_course, menu);
    }*/
    }

    private class ChaptersAdapter extends RecyclerView.Adapter<BookEditorFragment.ChapterHolder> {

        private int       selectedChapter;

        @Override
        public ChapterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.card_chapter,
                                               parent,
                                               false);
            return new ChapterHolder(view);
        }

        @Override
        public void onBindViewHolder(ChapterHolder holder, int position) {
            holder.bindBook(position);
        }

        @Override
        public int getItemCount() {
            return book.getChapterCount();
        }
    }
}
