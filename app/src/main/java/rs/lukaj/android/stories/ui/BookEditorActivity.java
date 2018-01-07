package rs.lukaj.android.stories.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.View;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

/**
 * Created by luka on 3.9.17.
 */

public class BookEditorActivity extends SingleFragmentActivity implements InputDialog.Callbacks {
    public static final String EXTRA_BOOK_NAME = "eBookName";
    public static final String SHOWCASE_BOOKEDITOR_INTRO = "bookeditor.showcase.intro";
    public static final String SHOWCASE_BOOKEDITOR_CODE = "bookeditor.showcase.code";

    private static final String TAG_ADD_CHAPTER = "dialog.addchapter";
    private BookEditorFragment   fragment;
    private FloatingActionButton fab;
    private Showcase             showcaseHelper;
    private Handler              handler;

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
        showcaseHelper = new Showcase(this);
        handler = new Handler();
        if(MaterialShowcaseView.hasAlreadyFired(this, SHOWCASE_BOOKEDITOR_INTRO)) {
            handler.postDelayed(() ->
            showcaseHelper.showShowcase(SHOWCASE_BOOKEDITOR_CODE, fragment.getFirstItem(), R.string.sc_bookeditor_code,
                                         false, true), 400);
        } else {
            handler.postDelayed(() ->
                showcaseHelper.showSequence(SHOWCASE_BOOKEDITOR_INTRO, new View[]{null, fab, fragment.getLastItem()},
                                            new int[]{R.string.sc_bookeditor_intro1, R.string.sc_bookeditor_intro2,
                                                      R.string.sc_bookeditor_intro3}, true),
                                400);
        }

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
    }
}
