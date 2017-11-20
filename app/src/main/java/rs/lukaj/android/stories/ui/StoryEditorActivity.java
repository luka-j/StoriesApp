package rs.lukaj.android.stories.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.controller.Runtime;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.ui.dialogs.AddBranchDialog;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;
import rs.lukaj.stories.environment.DisplayProvider;
import rs.lukaj.stories.exceptions.*;
import rs.lukaj.stories.parser.lines.*;
import rs.lukaj.stories.runtime.Chapter;
import rs.lukaj.stories.runtime.State;

import static rs.lukaj.android.stories.ui.StoryUtils.*;

/**
 * Created by luka on 2.9.17..
 */

public class StoryEditorActivity extends AppCompatActivity implements DisplayProvider, InputDialog.Callbacks,
                                                                      AddBranchDialog.Callbacks {
    public static final String EXTRA_BOOK_NAME = "eBookName";
    public static final String EXTRA_CHAPTER_NO = "eChapterNo";

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int    UI_ANIMATION_DELAY = 300;
    private static final long HIDE_UI_GRACE_PERIOD = 300;

    private static final int INTENT_PICK_AVATAR     = 1;
    private static final int INTENT_PICK_BACKGROUND = 2;
    private static final String DIALOG_QUESTION_VAR = "tagQVar";
    private static final String DIALOG_ADD_BRANCH   = "tagaddBranch";

    private ConstraintLayout layout;

    private final Handler  mHideHandler       = new Handler();
    private final Runnable mHidePart2Runnable = () -> layout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                                                                           | View.SYSTEM_UI_FLAG_FULLSCREEN
                                                                           | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                                           | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                                           | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                                           | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

    private String title;
    private EditText character, narrative;
    private ImageView avatar;
    private ImageView changeBackground, restartScene;
    private ImageView nextScene, previousScene, addSceneLeft;
    private ImageView addStatement, showBranches, makeQuestion, addBranch;
    private ScrollView answersScroll;
    private LinearLayout answersLayout, branchesLayout;

    private List<EditText> answers;
    private String questionVar;

    private AndroidFiles files;
    private ExceptionHandler exceptionHandler;
    private Chapter    chapter;

    private ArrayList<IfStatement> activeBranches = new ArrayList<>();
    private Line       currentLine;
    private int         executionPosition = 0;
    private List<Line>  execution         = new ArrayList<>();


    private View.OnFocusChangeListener onCharacterFocusChanged = (v, hasFocus) -> {
        if(!hasFocus) {
            File img = chapter.getState().getImage(character.getText().toString(), files);
            if(img != null && img.isFile()) {
                avatar.setImageBitmap(Utils.loadImage(img, avatar.getWidth()));
            }
        }
    },

    addAnswer = (v2, hasFocus) -> {
        if(hasFocus) {
            int                 tag = (int) v2.getTag();
            StoryEditorActivity ac  = StoryEditorActivity.this;
            if (answersLayout.getChildCount() - 1 == tag) {
                EditText ans2 = new EditText(ac);
                StoryUtils.generateAnswer(ac, files, ans2, null, "", tag + 1);
                ans2.setOnFocusChangeListener(ac.addAnswer);
                answersLayout.addView(ans2);
                answers.add(ans2);
            }
        }
    };

    private View.OnClickListener
    onAvatarTap              = v -> openImageChooser(INTENT_PICK_AVATAR, R.string.choose_avatar),

    onChangeBackgroundTap = v -> openImageChooser(INTENT_PICK_BACKGROUND, R.string.choose_background),

    onNextScene = v -> {
        if(executionPosition == execution.size())
            execution.add(null);
        List<Line> lines = makeLine(); //todo maybe optimize not to remake line if nothing was touched ?
        currentLine = lines.get(0);
        execution.set(executionPosition, currentLine);
        executionPosition++;
        for(int i=1; i<lines.size(); i++)
            execution.add(executionPosition++, lines.get(i));
        currentLine = null;
        while(execution.size() < executionPosition && !isShowableLine(execution.get(executionPosition))) {
            if(execution.get(executionPosition) instanceof IfStatement)
                activeBranches.add((IfStatement)execution.get(executionPosition));
            executionPosition++;
        }
        setupViews();
    },

    onPreviousScene     = v -> {
        while(executionPosition >= 0 && !isShowableLine(execution.get(executionPosition))) {
            if(currentLine instanceof IfStatement)
                activeBranches.remove(activeBranches.size()-1);
            currentLine = execution.get(--executionPosition);
        }
        setupViews();
    },

    onAddSceneLeft = v -> {
        execution.add(executionPosition, null);
        currentLine = null;
        setupViews();
    },

    onRestartScene      = v -> resetViews(),

    enterCode      = v -> {
        //show current line's code?
        //create arbitrary statements
    },

    onRemoveBranch = v -> {
        activeBranches.set((int)v.getTag(), null);
        branchesLayout.removeViewAt((int)v.getTag()); //todo test if this really works
    },

    onAddBranch = v -> AddBranchDialog.newInstance(chapter.getState()).show(getFragmentManager(), DIALOG_ADD_BRANCH),

    onShowBranches = v -> {
        if(branchesLayout.getVisibility() == View.VISIBLE) {
            branchesLayout.setVisibility(View.GONE);
            for(int i=0; i<activeBranches.size(); i++) {
                if(activeBranches.get(i) == null)
                    activeBranches.remove(i--);
            }
        } else {
            branchesLayout.setVisibility(View.VISIBLE);
            for(int i=0; i<branchesLayout.getChildCount(); i++)
                if(branchesLayout.getChildAt(i) instanceof TextView)
                    branchesLayout.removeViewAt(i--);
            for(int i=activeBranches.size()-1; i>=0; i--) {
                LinearLayout stmtView = new LinearLayout(this);
                stmtView.setTag(i);
                stmtView.setOrientation(LinearLayout.HORIZONTAL);
                TextView tv = new TextView(this);
                //tv.setPadding(12, 16, 12, 16);
                tv.setText(activeBranches.get(i).generateStatement());
                tv.setGravity(Gravity.START);
                ImageView remove = new ImageView(this);
                remove.setImageResource(R.drawable.ic_delete_black_24dp);
                remove.setTag(i);
                //set gravity - right
                remove.setOnClickListener(onRemoveBranch);
                stmtView.addView(tv);
                branchesLayout.addView(stmtView, 0);
            }
        }
    },

    onMakeQuestion  = v -> {
        Context  c    = StoryEditorActivity.this;
        EditText ans1 = new EditText(c);
        answers = new ArrayList<>(4);
        StoryUtils.generateAnswer(c, files, ans1, null, "", 0);
        ans1.setOnFocusChangeListener(addAnswer);
        answers.add(ans1);
        answersLayout.removeAllViews();
        answersLayout.addView(ans1);
        InputDialog.newInstance(R.string.make_question_var_title, getString(R.string.make_question_var_text),
                                R.string.ok, R.string.cancel, "",
                                getString(R.string.make_question_var_example), false)
        .show(getSupportFragmentManager(), DIALOG_QUESTION_VAR);
    };

    private List<Line> makeLine() {
        try {
            List<Line> lines = new ArrayList<>();
            int linenum = executionPosition, indent = getIndent();
            String text = narrative.getText().toString();
            String character = this.character.getText().toString();
            if (answers != null && !answers.isEmpty()) {
                Question q = new Question(chapter, questionVar, text, character, linenum, indent);
                lines.add(q);
                for(int i=0; i<answers.size(); i++) {
                    lines.add(new Answer(chapter, questionVar + i + 1, answers.get(i).getText().toString(),
                                         linenum+1+i, indent + 2));
                }
                lines.add(new Nop(chapter, linenum + answers.size(), indent));
            } else if(character.trim().isEmpty()) {
                lines.add(new Narrative(chapter, text, linenum, indent));
            } else {
                lines.add(new Speech(chapter, character, text, linenum, indent));
            }
            return lines;
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
            return new ArrayList<>();
        }
    }

    //todo figure out saving/files

    private void openImageChooser(int requestCode, @StringRes int chooserTitle) {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, getString(chooserTitle));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, requestCode);
    }

    private long lastHideTriggered;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        files = new AndroidFiles(this);
        exceptionHandler = new ExceptionHandler.DefaultHandler(this);

        setContentView(R.layout.activity_story_editor);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        title = getIntent().getStringExtra(EXTRA_BOOK_NAME);

        layout = findViewById(R.id.story_layout);
        answersScroll = findViewById(R.id.story_answers_scroll);
        answersLayout = findViewById(R.id.story_answers);
        character = findViewById(R.id.story_character_name);
        character.setOnFocusChangeListener(onCharacterFocusChanged);
        narrative = findViewById(R.id.story_text);
        avatar = findViewById(R.id.story_editor_avatar);
        avatar.setOnClickListener(onAvatarTap);
        changeBackground = findViewById(R.id.story_editor_change_background);
        changeBackground.setOnClickListener(onChangeBackgroundTap);
        nextScene = findViewById(R.id.story_editor_next);
        nextScene.setOnClickListener(onNextScene);
        previousScene = findViewById(R.id.story_editor_previous);
        previousScene.setOnClickListener(onPreviousScene);
        addSceneLeft = findViewById(R.id.story_editor_add_left);
        addSceneLeft.setOnClickListener(onAddSceneLeft);
        restartScene = findViewById(R.id.story_editor_restart);
        restartScene.setOnClickListener(onRestartScene);
        addStatement = findViewById(R.id.story_editor_code);
        addStatement.setOnClickListener(enterCode);
        showBranches = findViewById(R.id.story_editor_branching);
        showBranches.setOnClickListener(onShowBranches);
        makeQuestion = findViewById(R.id.story_make_question);
        makeQuestion.setOnClickListener(onMakeQuestion);
        branchesLayout = findViewById(R.id.story_editor_branches_layout);
        addBranch = findViewById(R.id.story_editor_add_branch);
        addBranch.setOnClickListener(onAddBranch);


        Book book = Runtime.loadBook(title, files, this, exceptionHandler).getCurrentBook();
        chapter = book.getUnderlyingBook().getChapter(getIntent().getIntExtra(EXTRA_CHAPTER_NO, 1));

        layout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(System.currentTimeMillis() - lastHideTriggered > HIDE_UI_GRACE_PERIOD)
                delayedHide(100);
            lastHideTriggered = System.currentTimeMillis();
            //this originally triggered only on keyboard closing. Having failed to make it work reliably,
            //it just hides everything every time anything is changed
            //edit: introduced grace period in case this method gets called extremely often
        });
    }

    private boolean isShowableLine(Line line) {
        return !(line instanceof Statement) && !(line instanceof AnswerLike);
    }

    private int getIndent() {
        if(executionPosition == 0) return 0;
        if(activeBranches.isEmpty()) return execution.get(executionPosition-1).getIndent();
        else return activeBranches.get(activeBranches.size()-1).getIndent() + 2;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
        try {
            Line ahead = chapter.compile();
            if(ahead == null) return;

            if(isShowableLine(ahead)) currentLine = ahead;
            while(ahead.getNextLine() != null) {
                execution.add(ahead);
                int last = activeBranches.size()-1;
                while(!activeBranches.isEmpty() && ahead.getIndent() <= activeBranches.get(last).getIndent())
                    activeBranches.remove(last);
                if(ahead instanceof IfStatement) activeBranches.add((IfStatement) ahead);

                ahead = ahead.getNextLine();
                if(isShowableLine(ahead)) currentLine = ahead;
            }

            if(!activeBranches.isEmpty())
                showBranches.setImageResource(R.drawable.ic_source_branch_red);
        } catch (IOException e) {
            exceptionHandler.handleIOException(e);
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
        } catch (PreprocessingException e) {
            exceptionHandler.handlePreprocessingException(e);
        } catch (LoadingException e) {
            exceptionHandler.handleLoadingException(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                InputStream stream = (this.getContentResolver().openInputStream(data.getData()));
                String imageName = ""; //todo (when figure out files/dir structure)
                AssignStatement stmt = null;

                switch (requestCode) {
                    case INTENT_PICK_AVATAR:
                        avatar.setImageBitmap(Utils.loadImage(stream, avatar.getWidth()));
                        stmt = new AssignStatement(chapter, executionPosition,
                                                                   getIndent(),
                                                                   character.getText().toString(),
                                                                   imageName);

                        break;
                    case INTENT_PICK_BACKGROUND:
                        layout.setBackgroundDrawable(new BitmapDrawable(getResources(), Utils.loadImage(stream, layout.getMaxWidth())));
                        stmt = new AssignStatement(chapter, executionPosition,
                                                   getIndent(),
                                                   VAR_BACKGROUND,
                                                   imageName);

                        break;
                }

                if(executionPosition == execution.size()) execution.add(null);
                execution.add(executionPosition++, stmt);
            } catch (IOException e) {
                exceptionHandler.handleIOException(e);
            } catch (InterpretationException e) {
                exceptionHandler.handleInterpretationException(e);
            }
        }
    }

    private void setVisuals() {
        State variables = chapter.getState();

        setBackgroundFromState(getResources(), files, variables, VAR_BACKGROUND, layout);
        setBackgroundFromState(getResources(), files, variables, VAR_TEXT_BACKGROUND, narrative);
        setBackgroundFromState(getResources(), files, variables, VAR_CHARACTER_BACKGROUND, character);

        setPaddingFromState(getResources(), variables, narrative, VAR_TEXT_VERTICAL_PADDING, VAR_TEXT_HORIZONTAL_PADDING);
        setPaddingFromState(getResources(), variables, character, VAR_CHARACTER_VERTICAL_PADDING, VAR_CHARACTER_HORIZONTAL_PADDING);
        setVerticalMarginsFromState(getResources(), variables, character, VAR_CHARACTER_VERTICAL_MARGINS);

        setAnswerPropsFromState(variables);
        if (variables.isNumeric(VAR_AVATAR_SIZE))
            setAvatarSize(getResources(), avatar, variables.getDouble(VAR_AVATAR_SIZE));
    }

    private void resetViews() {
        narrative.setText("");
        answersLayout.removeAllViews();
        answersLayout.addView(makeQuestion);
        makeQuestion.setVisibility(View.VISIBLE);
        answers = null;

    }

    private void setupViews() {
        resetViews();
        setVisuals();
        if(currentLine != null)
            currentLine.execute();
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(this::hide);
        mHideHandler.postDelayed(this::hide, delayMillis);
    }

    private File previousAvatar;
    private void setAvatar(File image) {
        if (image != null && image.equals(previousAvatar)) return;

        if (image != null) {
            avatar.setVisibility(View.VISIBLE);
            avatar.setImageBitmap(Utils.loadImage(image, avatar.getWidth()));
            previousAvatar = image;
        } else {
            avatar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        //todo serialize execution and save to file
        //add onDestroy just in case ?
        super.onBackPressed();
    }

    @Override
    public void showNarrative(String text) {
        narrative.setText(text);
    }

    @Override
    public void showSpeech(String character, File avatar, String text) {
        narrative.setText(text);
        this.character.setText(character);
        setAvatar(avatar);
    }

    @Override
    public int showQuestion(String question, String character, File avatar, double time, String... answers) {
        setAvatar(avatar);

        Context  c    = StoryEditorActivity.this;
        answersLayout.removeAllViews();
        if(answers == null) answers = new String[]{""};
        EditText ans = null;
        this.answers = new ArrayList<>(answers.length);
        for(String str : answers) {
            ans = new EditText(c);
            StoryUtils.generateAnswer(c, files, ans, null, str, 0);
            answersLayout.addView(ans);
            this.answers.add(ans);
        }
        ans.setOnFocusChangeListener(addAnswer);

        return -1;
    }

    @Override
    public int showPictureQuestion(String question, String character, File avatar, double time, File... answers) {
        return 0;
    }

    @Override
    public String showInput(String hint) {
        return null;
        // return value from ^^ somehow
        //figure out definite thread model
    }

    @Override
    public void onChapterBegin(int chapterNo, String chapterName) {

    }

    @Override
    public void onChapterEnd(int chapterNo, String chapterName) {

    }

    @Override
    public void onBookBegin(String bookName) {

    }

    @Override
    public void onBookEnd(String bookName) {

    }

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        if(DIALOG_QUESTION_VAR.equals(dialog.getTag())) {
            if(s != null && !s.isEmpty())
                questionVar = s;
            else {
                //todo handle missing input
            }
        }
    }

    @Override
    public void onFinishedAddBranch(String variable, String op, String value) {
        try {
            IfStatement ifs = new IfStatement(chapter, executionPosition, getIndent(),
                                              variable+op+value);
            execution.add(executionPosition++, ifs);
            Nop indent = new Nop(chapter, executionPosition, ifs.getIndent()+2);
            execution.add(executionPosition++, indent);
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
        }
    }
}
