package rs.lukaj.android.stories.ui;

import android.app.DialogFragment;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.model.User;
import rs.lukaj.android.stories.ui.dialogs.ConfirmDialog;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;
import rs.lukaj.stories.exceptions.InterpretationException;

/**
 * Displays chapter list. Has context menu for all items. By default shows only chapters, but OptionsMenu
 * provides an option to display all source files for a book.
 * Created by luka on 3.9.17.
 */

public class BookEditorFragment extends Fragment implements InputDialog.Callbacks,
                                                            ConfirmDialog.Callbacks {

    private static final String KEY_BOOK_NAME           = "eBookName";
    private static final String TAG_DIAG_ADD_DESC       = "dialog.addchdesc";
    private static final String TAG_DIAG_RENAME_CHAPTER = "dialog.renamech";
    private static final String TAG_DIAG_REMOVE_CHAPTER = "dialog.removech";

    private Book            book;
    private AndroidFiles    files;
    private RecyclerView    recycler;
    private ChaptersAdapter adapter;
    private View            firstChapterHolder, secondChapterHolder, lastChapterHolder;
    private ExceptionHandler handler = new ExceptionHandler.DefaultHandler((AppCompatActivity) getActivity());
    private boolean showingIncludeFiles = false;

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
        setHasOptionsMenu(true);

        files = new AndroidFiles(getContext());
        String name = getArguments().getString(KEY_BOOK_NAME);
        if(files.getRootDirectory(name) != null)
            book = new Book(name, files, new NullDisplay());
        else {
            try {
                files.createBook(name);
                book = new Book(name, files, new NullDisplay());
                if(User.isLoggedIn(getContext()) && User.getLoggedInUser(getContext()).getId() != null)
                    book.setAuthor(User.getLoggedInUser(getContext()).getId());
                else
                    book.setAuthor(Book.AUTHOR_ID_ME);
            } catch (IOException e) {
                handler.handleBookIOException(e);
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
        registerForContextMenu(recycler);

        return v;
    }

    public View getLastItem() {
        return lastChapterHolder;
    }
    public View getMiddleItem() {
        return secondChapterHolder;
    }
    public View getFirstItem() {
        return firstChapterHolder;
    }

    public void createChapter(String name) {
        int count = book.getChapterCount();
        try {
            files.createChapter(book.getName(), count+1, name);
            book.addChapter(name);
            adapter.notifyDataSetChanged();
        } catch (IOException e) {
            handler.handleBookIOException(e);
        } catch (InterpretationException e) {
            handler.handleInterpretationException(e);
        }
    }

    //ovo je rađeno u nedelju popodne, prilično je hacky
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_book_editor, menu);
        if(showingIncludeFiles) {
            menu.removeItem(R.id.menu_item_show_includefiles);
        } else {
            menu.removeItem(R.id.menu_item_hide_includefiles);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        getActivity().invalidateOptionsMenu();
        switch (item.getItemId()) {
            case R.id.menu_item_show_includefiles:
                File bookRoot = files.getSourceDirectory(book.getName());
                File[] includeFiles = bookRoot
                        .listFiles((dir, name) -> !(name.endsWith(".ch") && Character.isDigit(name.charAt(0))));
                adapter.includeFiles = Arrays.asList(includeFiles);
                adapter.notifyDataSetChanged();
                showingIncludeFiles = true;
                return true;

            case R.id.menu_item_hide_includefiles:
                adapter.includeFiles = new ArrayList<>(); //because asList doesn't permit removing its elements
                adapter.notifyDataSetChanged();
                showingIncludeFiles = false;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_add_chapter_desc:
                InputDialog.newInstance(R.string.dialog_addchdesc_title, getString(R.string.dialog_addchdesc_text),
                                        R.string.set, R.string.cancel, book.getChapterDescription(adapter.selectedChapter-1),
                                        "", Limits.CHAPTER_DESC_MAX_LENGTH, false)
                           .registerCallbacks(this)
                           .show(getActivity().getFragmentManager(), TAG_DIAG_ADD_DESC);
                return true;
            case R.id.menu_item_rename_chapter:
                InputDialog.newInstance(R.string.dialog_renamech_title, getString(R.string.dialog_renamech_text),
                                        R.string.rename, R.string.cancel, book.getChapterName(adapter.selectedChapter-1),
                                        "", Limits.CHAPTER_NAME_MAX_LENGTH, true)
                           .registerCallbacks(this)
                           .show(getActivity().getFragmentManager(), TAG_DIAG_RENAME_CHAPTER);
                return true;
            case R.id.menu_item_remove_chapter:
                ConfirmDialog.newInstance(R.string.dialog_removech_title, R.string.dialog_removech_text,
                                          R.string.remove, R.string.cancel)
                             .registerCallbacks(this)
                             .show(getActivity().getFragmentManager(), TAG_DIAG_REMOVE_CHAPTER);
                return true;
            case R.id.menu_item_edit_code:
                Intent i = new Intent(getContext(), CodeEditorActivity.class);
                i.putExtra(CodeEditorActivity.EXTRA_BOOK_NAME, book.getName());
                i.putExtra(CodeEditorActivity.EXTRA_CHAPTER_NO, adapter.selectedChapter);
                startActivity(i);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onPositive(DialogFragment dialog) {
        if(dialog.getTag().equals(TAG_DIAG_REMOVE_CHAPTER)) {
            try {
                book.removeChapter(adapter.selectedChapter-1);
                files.removeSource(book.getName(), adapter.selectedChapter);
                adapter.notifyDataSetChanged();
            } catch (InterpretationException e) {
                handler.handleInterpretationException(e);
            } catch (IOException e) {
                handler.handleBookIOException(e);
            }
        }
    }

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        try {
            switch (dialog.getTag()) {
                case TAG_DIAG_ADD_DESC:
                    book.setChapterDescription(adapter.selectedChapter-1, s);
                    break;
                case TAG_DIAG_RENAME_CHAPTER:
                    book.renameChapter(adapter.selectedChapter-1, s);
                    files.renameSource(book.getName(), adapter.selectedChapter, s);
                    break;
            }
            adapter.notifyDataSetChanged();
        } catch (InterpretationException e) {
            handler.handleInterpretationException(e);
        } catch (IOException e) {
            handler.handleBookIOException(e);
        }
    }

    private class ChapterHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
                                                                           View.OnLongClickListener,
                                                                           View.OnCreateContextMenuListener {
        private final TextView titleTextView;
        private final TextView descriptionTextView;

        private int chapterNumber;
        private File file; //in case this isn't a chapter

        public ChapterHolder(View itemView) {
            super(itemView);
            lastChapterHolder = itemView;
            if(firstChapterHolder == null) firstChapterHolder = itemView;
            else if(firstChapterHolder != null && secondChapterHolder == null) secondChapterHolder = itemView;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

            titleTextView = itemView.findViewById(R.id.card_chapter_name);
            descriptionTextView = itemView.findViewById(R.id.card_chapter_description);
        }

        public void bindChapter(int chapterNumber) {
            this.chapterNumber = chapterNumber+1;
            this.file = null;
            titleTextView.setText(book.getChapterName(chapterNumber));
            descriptionTextView.setText(book.getChapterDescription(chapterNumber));
        }

        public void bindIncludeFile(int index, File file) {
            this.chapterNumber = index;
            this.file = file;
            titleTextView.setText(file.getName());
            descriptionTextView.setText(R.string.includefile_desc);
        }

        @Override
        public void onClick(View v) {
            if(file == null) {
                Intent intent = new Intent(getContext(), StoryEditorActivity.class);
                intent.putExtra(StoryEditorActivity.EXTRA_BOOK_NAME, book.getName());
                intent.putExtra(StoryEditorActivity.EXTRA_CHAPTER_NO, chapterNumber);
                startActivity(intent);
            } else {
                Intent i = new Intent(getContext(), CodeEditorActivity.class);
                i.putExtra(CodeEditorActivity.EXTRA_BOOK_NAME, book.getName());
                i.putExtra(CodeEditorActivity.EXTRA_FILEPATH, file.getAbsolutePath());
                startActivity(i);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            adapter.selectedChapter = chapterNumber;
            return false;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            if(file == null)
                getActivity().getMenuInflater().inflate(R.menu.context_chapter, menu);
        }


    /*@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if(permission >= Group.PERM_MODIFY)
            getActivity().getMenuInflater().inflate(R.menu.context_course, menu);
    }*/
    }

    private class ChaptersAdapter extends RecyclerView.Adapter<BookEditorFragment.ChapterHolder> {

        private int        selectedChapter;
        private List<File> includeFiles = new ArrayList<>();

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
            if(position < book.getChapterCount())
                holder.bindChapter(position);
            else
                holder.bindIncludeFile(position, includeFiles.get(position - book.getChapterCount()));
        }

        @Override
        public int getItemCount() {
            return book.getChapterCount() + includeFiles.size();
        }
    }
}
