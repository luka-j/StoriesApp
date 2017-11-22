package rs.lukaj.android.stories.ui;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;

/**
 * Created by luka on 3.9.17..
 */

public class BookEditorActivity extends SingleFragmentActivity implements InputDialog.Callbacks{
    public static final String EXTRA_BOOK_NAME = "eBookName";

    private String bookName;
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
                                                    R.string.add, 0, "", "", true)
                                       .show(getFragmentManager(), ""));
    }

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        fragment.createChapter(s);
    }
}
