package rs.lukaj.android.stories.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.constraint.ConstraintLayout;
import android.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
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
import rs.lukaj.android.stories.ui.dialogs.ConfirmDialog;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;
import rs.lukaj.android.stories.ui.dialogs.SetVariableDialog;
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
                                                                      AddBranchDialog.Callbacks,
                                                                      SetVariableDialog.Callbacks,
                                                                      ConfirmDialog.Callbacks {
    public static final String EXTRA_BOOK_NAME = "eBookName";
    public static final String EXTRA_CHAPTER_NO = "eChapterNo";

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int    UI_ANIMATION_DELAY = 300;
    private static final long HIDE_UI_GRACE_PERIOD = 300;

    private static final int INTENT_PICK_AVATAR      = 1;
    private static final int INTENT_PICK_BACKGROUND  = 2;
    private static final String DIALOG_QUESTION_VAR  = "tagQVar";
    private static final String DIALOG_ADD_BRANCH    = "tagaddBranch";
    private static final String DIALOG_ADD_LABEL     = "tagAddLabel";
    private static final String DIALOG_ADD_JUMP      = "tagAddJump";
    private static final String DIALOG_ADD_STATEMENT = "tagAddStmt";
    private static final String DIALOG_SET_VARIABLE  = "tagSetVar";
    private static final String DIALOG_CONFIRM_EXIT  = "tagConfirmExit";

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
    private ImageView showCodeOps, showBranches, makeQuestion, addBranch, save;
    private ScrollView answersScroll;
    private LinearLayout answersLayout, branchesLayout, codeLayout;
    private Button setVariable, addLabel, addJump, addStatement;

    private List<EditText> answers;
    private String questionVar;

    private AndroidFiles files;
    private ExceptionHandler exceptionHandler;
    private Chapter    chapter;

    private ArrayList<IfStatement> activeBranches = new ArrayList<>();
    private Line       currentLine;
    private GotoStatement jump = null;
    private int         executionPosition = 0;
    private List<Line>  execution         = new ArrayList<>();

    private boolean recentlySaved = false, exitNoConfirm = false;

    private View.OnFocusChangeListener onCharacterFocusChanged = (v, hasFocus) -> {
        if(!hasFocus) {
            String chr = character.getText().toString();
            if(chr == null || chr.isEmpty()) return;
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
        executionPosition += insertLinesToExecution();

        currentLine = null;
        while(execution.size() > executionPosition && !isShowableLine(execution.get(executionPosition))) {
            if(execution.get(executionPosition) instanceof IfStatement)
                activeBranches.add((IfStatement)execution.get(executionPosition));
            executionPosition++;
        }
        int currIndent = execution.get(executionPosition-(executionPosition==execution.size() ? 1 : 0)).getIndent();
        while(!activeBranches.isEmpty() && activeBranches.get(activeBranches.size()-1).getIndent() >= currIndent)
            activeBranches.remove(activeBranches.size()-1);

        if(executionPosition < execution.size())
            currentLine = execution.get(executionPosition);
        setupViews();
    },

    onPreviousScene     = v -> {
        if(executionPosition == 0) return;
        insertLinesToExecution();
        executionPosition--;

        while(executionPosition >= 0 && (executionPosition == execution.size() ||
                                         !isShowableLine(execution.get(executionPosition)))) {
            if(currentLine instanceof IfStatement && !activeBranches.isEmpty() &&
               currentLine.getIndent() >= activeBranches.get(activeBranches.size()-1).getIndent())
                activeBranches.remove(activeBranches.size()-1); //todo proper if handling when going backwards
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

    onShowCodeOps = v -> {
        if(codeLayout.getVisibility() == View.VISIBLE)
            codeLayout.setVisibility(View.GONE);
        else {
            codeLayout.setVisibility(View.VISIBLE);
            if(branchesLayout.getVisibility() == View.VISIBLE)
                branchesLayout.setVisibility(View.GONE);
        }
    },

    onRemoveBranch = v -> {
        activeBranches.set((int)v.getTag(), null);
        branchesLayout.removeViewAt((int)v.getTag()); //todo test if this really works
    },

    onShowBranches = v -> {
        if(branchesLayout.getVisibility() == View.VISIBLE) {
            branchesLayout.setVisibility(View.GONE);
            for(int i=0; i<activeBranches.size(); i++) {
                if(activeBranches.get(i) == null)
                    activeBranches.remove(i--);
            }
        } else {
            branchesLayout.setVisibility(View.VISIBLE);
            if(codeLayout.getVisibility() == View.VISIBLE)
                codeLayout.setVisibility(View.GONE);
            for(int i=0; i<branchesLayout.getChildCount(); i++)
                if(branchesLayout.getChildAt(i) instanceof LinearLayout)
                    branchesLayout.removeViewAt(i--);
            for(int i=activeBranches.size()-1; i>=0; i--) {
                LinearLayout stmtView = new LinearLayout(this);
                stmtView.setTag(i);
                stmtView.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams layParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                stmtView.setLayoutParams(layParams);
                TextView tv = new TextView(this);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                //tv.setPadding(12, 0, 12, 0);
                tv.setText(activeBranches.get(i).generateStatement());
                tv.setGravity(Gravity.START);
                LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tv.setLayoutParams(tvParams);
                ImageView remove = new ImageView(this); //fixme not showing
                remove.setImageResource(R.drawable.ic_delete_black_24dp);
                remove.setTag(i);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                remove.setLayoutParams(imgParams);
                //set gravity - right
                remove.setOnClickListener(onRemoveBranch);
                stmtView.addView(tv);
                branchesLayout.addView(stmtView, 0);
            }
        }
    },

    onAddBranch = v -> {
        onShowBranches.onClick(v);
        AddBranchDialog.newInstance(chapter.getState()).show(getFragmentManager(), DIALOG_ADD_BRANCH);
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
        .show(getFragmentManager(), DIALOG_QUESTION_VAR);
    },

    onAddLabel = v -> InputDialog.newInstance(R.string.dialog_add_label_title,
                                              getString(R.string.dialog_add_label_text),
                                              R.string.add, R.string.cancel, "",
                                              getString(R.string.dialog_add_label_hint),
                                              false)
                                 .show(getFragmentManager(), DIALOG_ADD_LABEL),

    onAddJump = v -> InputDialog.newInstance(R.string.dialog_add_jump_title,
                                             getString(R.string.dialog_add_jump_text),
                                             R.string.add, R.string.cancel, "",
                                             getString(R.string.dialog_add_label_hint),
                                             false)
                                .show(getFragmentManager(), DIALOG_ADD_JUMP),

    onAddStatement = v -> InputDialog.newInstance(R.string.dialog_add_statement_title,
                                                  getString(R.string.dialog_add_statement_text),
                                                  R.string.add, R.string.cancel, "",
                                                  getString(R.string.dialog_add_statement_hint),
                                                  false)
                                     .show(getFragmentManager(), DIALOG_ADD_STATEMENT),

    onSave = v -> {
        File source = chapter.getSourceFile();
        try (FileWriter fw = new FileWriter(source)) {
            for(Line line : execution) {
                fw.write(line.generateCode(line.getIndent()));
                fw.write('\n');
            }
            recentlySaved = true;
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            //todo better visual indicator, move saving to background thread ?
            //compile chapter to check whether jumps and other stuff is valid
        } catch (IOException e) {
            exceptionHandler.handleIOException(e);
        }
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
            if(jump != null) {
                lines.add(jump);
                jump = null;
            }
            recentlySaved = false;
            return lines;
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
            return new ArrayList<>();
        }
    }

    /**
     * Insert lines to execution and return number of lines inserted.
     * Replaces whatever is at current executionPosition with Line made according to current state on screen
     * @return
     */
    private int insertLinesToExecution() {
        int pos = executionPosition;
        if(pos == execution.size())
            execution.add(null);
        else if(execution.get(pos) instanceof Question) {
            int next = pos+1; //if current line is a question, we need to remove it before replacing it with something new
            while(execution.size() < next &&
                  execution.get(next).getIndent() > execution.get(pos).getIndent())
                execution.remove(next);
        }
        List<Line> lines = makeLine(); //todo maybe optimize not to remake line if nothing was touched ?
        currentLine = lines.get(0);
        execution.set(pos++, currentLine);
        for(int i=1; i<lines.size(); i++)
            execution.add(pos++, lines.get(i));
        return pos-executionPosition;
    }


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
        showCodeOps = findViewById(R.id.story_editor_code);
        showCodeOps.setOnClickListener(onShowCodeOps);
        showBranches = findViewById(R.id.story_editor_branching);
        showBranches.setOnClickListener(onShowBranches);
        makeQuestion = findViewById(R.id.story_make_question);
        makeQuestion.setOnClickListener(onMakeQuestion);
        branchesLayout = findViewById(R.id.story_editor_branches_layout);
        codeLayout = findViewById(R.id.story_editor_code_layout);
        addBranch = findViewById(R.id.story_editor_add_branch);
        addBranch.setOnClickListener(onAddBranch);
        setVariable = findViewById(R.id.story_editor_btn_set_variable);
        setVariable.setOnClickListener(v -> SetVariableDialog.newInstance(chapter.getState()).show(getFragmentManager(), DIALOG_SET_VARIABLE));
        addLabel = findViewById(R.id.story_editor_btn_add_label);
        addLabel.setOnClickListener(onAddLabel);
        addJump = findViewById(R.id.story_editor_btn_goto);
        addJump.setOnClickListener(onAddJump);
        addStatement = findViewById(R.id.story_editor_btn_custom_stmt);
        addStatement.setOnClickListener(onAddStatement);
        save = findViewById(R.id.story_editor_save);
        save.setOnClickListener(onSave);

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
        return line != null && !(line instanceof Statement) && !(line instanceof AnswerLike);
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
            Line ahead = chapter.compile(); //todo do parsing manually, to avoid setting jumps (?)
            if(ahead == null) return;

            if(isShowableLine(ahead)) currentLine = ahead;
            int i=0;
            while(ahead != null) {
                execution.add(ahead);
                if(ahead instanceof IfStatement) activeBranches.add((IfStatement) ahead);

                ahead = ahead.getNextLine();
                i++;
                if(isShowableLine(ahead)) {
                    currentLine = ahead;
                    executionPosition = i;
                    int last = activeBranches.size()-1;
                    while(!activeBranches.isEmpty() && ahead.getIndent() <= activeBranches.get(last).getIndent())
                        activeBranches.remove(last--);
                }
            }

            if(!activeBranches.isEmpty())
                showBranches.setImageResource(R.drawable.ic_source_branch_red);
            setupViews();
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
        if(exitNoConfirm || recentlySaved) super.onBackPressed();
        else {
            ConfirmDialog.newInstance(R.string.story_editor_confirm_exit_title,
                                      R.string.story_editor_confirm_exit_text,
                                      R.string.exit, R.string.stay)
                         .show(getFragmentManager(), DIALOG_CONFIRM_EXIT);
        }
    }

    @Override
    public void showNarrative(String text) {
        narrative.setText(text);
        character.setText("");
        //todo hide avatar, display it when something is entered in character field, i.e. attach a listener to
        //todo character and make it display avatar/default when it's not empty
    }

    @Override
    public void showSpeech(String character, File avatar, String text) {
        narrative.setText(text);
        this.character.setText(character);
        setAvatar(avatar); //todo if avatar == null, show default and allow the user to change it
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
        setupViews();
        return null;
        //todo indicate text input somehow
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
    public void signalEndChapter() {
        //todo represent halt somehow
    }

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        try {
            switch (dialog.getTag()) {
                case DIALOG_QUESTION_VAR:
                    if(s != null && !s.isEmpty())
                        questionVar = s;
                    else {
                        //todo handle missing input
                    }
                    break;

                case DIALOG_ADD_LABEL:
                    if(s != null && !s.isEmpty()) {
                        LabelStatement label = new LabelStatement(chapter, s, executionPosition, getIndent());
                        execution.add(executionPosition++, label);
                    }
                    break;
                case DIALOG_ADD_JUMP:
                    if(s != null && !s.isEmpty()) {
                        jump = new GotoStatement(chapter, executionPosition+1, getIndent(), s);
                    }
                    break;
                case DIALOG_ADD_STATEMENT:
                    if(s != null && !s.isEmpty()) {
                        Statement stmt = Statement.create(chapter, s, executionPosition, getIndent());
                        execution.add(executionPosition++, stmt);
                    }
                    break;
            }
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
        }
    }

    @Override
    public void onFinishedAddBranch(String variable, String op, String value) {
        try {
            IfStatement ifs = new IfStatement(chapter, executionPosition, getIndent(),
                                              variable+op+value);
            execution.add(executionPosition++, ifs);
            //Nop indent = new Nop(chapter, executionPosition, ifs.getIndent()+2);
            //execution.add(executionPosition++, indent);
            activeBranches.add(ifs);
            showBranches.setImageResource(R.drawable.ic_source_branch_red);
            onShowBranches.onClick(branchesLayout);
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
        }
    }

    @Override
    public void onFinishedSetVar(String variable, String value) {
        try {
            AssignStatement agn = new AssignStatement(chapter, executionPosition, getIndent(), variable, value);
            execution.add(executionPosition++, agn);
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
        }
    }


    @Override
    public void onPositive(DialogFragment dialog) {
        if(DIALOG_CONFIRM_EXIT.equals(dialog.getTag())) {
            exitNoConfirm = true;
            onBackPressed();
        }
    }

    @Override
    public void onNegative(DialogFragment dialog) {

    }
}
