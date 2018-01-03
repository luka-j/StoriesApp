package rs.lukaj.android.stories.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.ui.dialogs.ConfirmDialog;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;

/**
 * Created by luka on 3.9.17..
 */

public class BookEditorActivity extends SingleFragmentActivity implements InputDialog.Callbacks,
                                                                          ConfirmDialog.Callbacks{
    public static final String EXTRA_BOOK_NAME = "eBookName";

    private static final String TAG_ADD_CHAPTER = "dialog.addchapter";
    private BookEditorFragment fragment;
    private FloatingActionButton fab;

    @Override
    protected Fragment createFragment() {
        fragment = BookEditorFragment.newInstance(getIntent().getStringExtra(EXTRA_BOOK_NAME));
        return fragment;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_fab_fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fab = findViewById(R.id.add_chapter);
        fab.setOnClickListener(v -> InputDialog.newInstance(R.string.add_chapter_title, getString(R.string.add_chapter_text),
                                                            R.string.add, 0, "", "",
                                                            Limits.CHAPTER_NAME_MAX_LENGTH, true)
                                       .show(getFragmentManager(), TAG_ADD_CHAPTER));
    }

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        if(dialog.getTag().equals(TAG_ADD_CHAPTER))
            fragment.createChapter(s);
        else
            fragment.onFinishedInput(dialog, s); //todo maaybe make this a bit cleaner
    }

    @Override
    public void onPositive(DialogFragment dialog) {
        fragment.onPositive(dialog);
    }
}
