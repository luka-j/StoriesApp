package rs.lukaj.android.stories.ui;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.controller.Runtime;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.ui.dialogs.AddBranchDialog;
import rs.lukaj.android.stories.ui.dialogs.AddInputDialog;
import rs.lukaj.android.stories.ui.dialogs.ConfirmDialog;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;
import rs.lukaj.android.stories.ui.dialogs.SetVariableDialog;
import rs.lukaj.stories.environment.DisplayProvider;
import rs.lukaj.stories.exceptions.InterpretationException;
import rs.lukaj.stories.exceptions.LoadingException;
import rs.lukaj.stories.exceptions.PreprocessingException;
import rs.lukaj.stories.parser.lines.Answer;
import rs.lukaj.stories.parser.lines.AnswerLike;
import rs.lukaj.stories.parser.lines.AssignStatement;
import rs.lukaj.stories.parser.lines.Directive;
import rs.lukaj.stories.parser.lines.GotoStatement;
import rs.lukaj.stories.parser.lines.IfStatement;
import rs.lukaj.stories.parser.lines.LabelStatement;
import rs.lukaj.stories.parser.lines.Line;
import rs.lukaj.stories.parser.lines.Narrative;
import rs.lukaj.stories.parser.lines.Nop;
import rs.lukaj.stories.parser.lines.Question;
import rs.lukaj.stories.parser.lines.Speech;
import rs.lukaj.stories.parser.lines.Statement;
import rs.lukaj.stories.parser.lines.TextInput;
import rs.lukaj.stories.runtime.Chapter;
import rs.lukaj.stories.runtime.State;

import static rs.lukaj.android.stories.ui.MainActivity.ONBOARDING_ENABLED;
import static rs.lukaj.android.stories.ui.MainActivity.PREFS_DEMO_PROGRESS;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_AVATAR_SIZE;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_BACKGROUND;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_CHARACTER_BACKGROUND;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_CHARACTER_HORIZONTAL_PADDING;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_CHARACTER_VERTICAL_MARGINS;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_CHARACTER_VERTICAL_PADDING;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_TEXT_BACKGROUND;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_TEXT_HORIZONTAL_PADDING;
import static rs.lukaj.android.stories.ui.StoryUtils.VAR_TEXT_VERTICAL_PADDING;
import static rs.lukaj.android.stories.ui.StoryUtils.isValidCharacterName;
import static rs.lukaj.android.stories.ui.StoryUtils.setAnswerPropsFromState;
import static rs.lukaj.android.stories.ui.StoryUtils.setAvatarSize;
import static rs.lukaj.android.stories.ui.StoryUtils.setBackgroundFromState;
import static rs.lukaj.android.stories.ui.StoryUtils.setPaddingFromState;
import static rs.lukaj.android.stories.ui.StoryUtils.setVerticalMarginsFromState;

/**
 * Created by luka on 2.9.17.
 */
//todo showcase, combine with Demo chapter 3: Hack
public class StoryEditorActivity extends AppCompatActivity implements DisplayProvider, InputDialog.Callbacks,
                                                                      AddBranchDialog.Callbacks,
                                                                      SetVariableDialog.Callbacks,
                                                                      ConfirmDialog.Callbacks,
                                                                      AddInputDialog.Callbacks {
    public static final String EXTRA_BOOK_NAME = "eBookName";
    public static final String EXTRA_CHAPTER_NO = "eChapterNo";

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int  UI_ANIMATION_DELAY = 300;
    private static final long HIDE_UI_GRACE_PERIOD = 300;

    private static final int INTENT_PICK_AVATAR      = 1;
    private static final int INTENT_PICK_BACKGROUND  = 2;
    private static final String DIALOG_QUESTION_VAR      = "diagQVar";
    private static final String DIALOG_ADD_BRANCH        = "diagaddBranch";
    private static final String DIALOG_ADD_LABEL            = "diagAddLabel";
    private static final String DIALOG_ADD_JUMP              = "diagAddJump";
    private static final String DIALOG_ADD_STATEMENT         = "diagAddStmt";
    private static final String DIALOG_ADD_INPUT             = "diagAddInput";
    private static final String DIALOG_SET_VARIABLE          = "diagSetVar";
    private static final String DIALOG_CONFIRM_EXIT          = "diagConfirmExit";
    private static final String DIALOG_INFO_NO_QUESTIONVAR   = "diagInfo_noqvar";
    private static final String SHOWCASE_DEMO_INTRO_BRANCHES = "StoryEditor.demo.intro&branches";
    private static final String SHOWCASE_DEMO_BRANCHES       = "StoryEditor.demo.branches";
    private static final String SHOWCASE_DEMO_REMOVED_BRANCH = "StoryEditor.demo.removedbranch";
    private static final String SHOWCASE_DEMO_NEXT           = "StoryEditor.demo.next";
    private static final String SHOWCASE_DEMO_GOTO           = "StoryEditor.demo.goto";
    private static final String SHOWCASE_DEMO_LABEL          = "StoryEditor.demo.label";
    private static final String SHOWCASE_DEMO_SECRET         = "StoryEditor.demo.secret";
    private static final String SHOWCASE_DEMO_OUTRO          = "StoryEditor.demo.outro";
    public static final String DEMO_PROGRESS_STORY_EDITOR   = "StoryEditor.finishededitor";

    private AndroidFiles files;
    private ExceptionHandler exceptionHandler;
    private Chapter    chapter;

    private final Directive DIRECTIVE_UNMODIFIABLE_LINE = new Directive(chapter, -1, 0, "!editor protected");
    private final Directive DIRECTIVE_HIDDEN_LINE = new Directive(chapter, -1, 0, "!editor hide");
    private final Directive DIRECTIVE_START_FROM = new Directive(chapter, -1, 0, "!editor start");

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
    private ImageView showCodeOps, showBranches, makeQuestion, addBranch, save, deleteScene;
    private ScrollView answersScroll;
    private LinearLayout answersLayout, branchesLayout, codeLayout;
    private Button setVariable, addLabel, addJump, addStatement, addInput;
    private TextView unrepresentableLineDescription;
    private TextView labelText, gotoText;

    private List<EditText> answers;
    private String questionVar;
    private boolean unmodifiable = false;


    private ArrayList<IfStatement> activeBranches = new ArrayList<>();
    private Line       currentLine;
    private GotoStatement jump = null;
    private int         executionPosition = 0;
    private List<Line>  execution         = new ArrayList<>();
    private Showcase showcaseHelper;
    private Handler handler;
    private Random random = new Random();

    private boolean isDemo, foundDemoSecret;
    private boolean recentlySaved = false, exitNoConfirm = false, sceneDeleted = false;

    private View.OnFocusChangeListener
            onCharacterFocusChanged = (v, hasFocus) -> {
        if(!hasFocus) {
            String chr = character.getText().toString();
            if(chr == null || chr.isEmpty()) return;
            File img = chapter.getState().getImage(character.getText().toString(), files);
            if(img != null && img.isFile()) {
                avatar.setImageBitmap(BitmapUtils.loadImage(img, avatar.getWidth()));
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
            onAvatarTap              = v -> {
        if(!unmodifiable)
            openImageChooser(INTENT_PICK_AVATAR, R.string.choose_avatar);
    },

    onChangeBackgroundTap = v -> openImageChooser(INTENT_PICK_BACKGROUND, R.string.choose_background),

    onNextScene = v -> {
        if(isLastScene() && isEmptyScene()) return;
        if(branchesLayout.getVisibility() == View.VISIBLE)
            Utils.click(showBranches);
        executionPosition += insertLinesToExecution();

        currentLine = null;
        while(execution.size() > executionPosition && !isShowableLine(execution.get(executionPosition))) {
            if(executionPosition < execution.size()) {
                while(!activeBranches.isEmpty() &&
                      execution.get(executionPosition).getIndent() <= activeBranches.get(activeBranches.size()-1).getIndent()) {
                    activeBranches.remove(activeBranches.size()-1);
                }
                if (execution.get(executionPosition) instanceof IfStatement)
                    activeBranches.add((IfStatement) execution.get(executionPosition));
            }
            executionPosition++;
        }
        while(executionPosition < execution.size() && !activeBranches.isEmpty() &&
              execution.get(executionPosition).getIndent() <= activeBranches.get(activeBranches.size()-1).getIndent())
            activeBranches.remove(activeBranches.size()-1);

        if(executionPosition < execution.size())
            currentLine = execution.get(executionPosition);
        setupViews();
    },

    onPreviousScene     = v -> {
        if(isFirstScene()) return;
        if(branchesLayout.getVisibility() == View.VISIBLE)
            Utils.click(showBranches);

        insertLinesToExecution();
        currentLine = execution.get(--executionPosition);

        int pos = executionPosition;
        while(pos > 0 && (pos == execution.size() || !isShowableLine(execution.get(pos)))) {
            currentLine = execution.get(--pos);
        }
        if(isShowableLine(execution.get(pos))) { //prevent going beyond first line in case first line isn't showable
            executionPosition = pos;
        } else {
            currentLine = execution.get(executionPosition);
            return;
        }

        setActiveBranches();
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
        activeBranches.set((int)v.getTag(), null); //this is temporary, to preserve indices; removing nulls on closing onActiveBranches
        branchesLayout.removeViewAt((int)v.getTag());
        if(isDemo && ONBOARDING_ENABLED) {
            showcaseHelper.showShowcase(SHOWCASE_DEMO_REMOVED_BRANCH, R.string.sc_editor_closebranches, true);
        }
    },

    onShowBranches = v -> {
        if(branchesLayout.getVisibility() == View.VISIBLE) {
            branchesLayout.setVisibility(View.GONE);
            for(int i=0; i<activeBranches.size(); i++) {
                if(activeBranches.get(i) == null)
                    activeBranches.remove(i--);
            }
            if(currentLine == null) insertLinesToExecution();
            currentLine.setIndent(getIndent());
            if(activeBranches.isEmpty()) showBranches.setImageResource(R.drawable.ic_source_branch);
            if(isDemo && ONBOARDING_ENABLED) {
                handler.postDelayed(() -> {
                    if (!foundDemoSecret) {
                        showcaseHelper.showShowcase(SHOWCASE_DEMO_NEXT,
                                                    nextScene,
                                                    R.string.sc_editor_next,
                                                    false,
                                                    true);
                    } else {
                        showcaseHelper.showSequence(SHOWCASE_DEMO_OUTRO, new View[]{null, deleteScene, addSceneLeft, changeBackground, makeQuestion, save},
                                                    new int[]{R.string.sc_editor_outro1, R.string.sc_editor_outro_delete,
                                                              R.string.sc_editor_outro_add, R.string.sc_editor_outro_bg,
                                                              R.string.sc_editor_outro_question,
                                                              R.string.sc_editor_outro_save}, false);
                        getSharedPreferences(PREFS_DEMO_PROGRESS, MODE_PRIVATE).edit().putBoolean(
                                DEMO_PROGRESS_STORY_EDITOR, true).apply();
                    }
                }, 600);
            }

        } else {
            branchesLayout.setVisibility(View.VISIBLE);
            if(codeLayout.getVisibility() == View.VISIBLE)
                codeLayout.setVisibility(View.GONE);
            for(int i=0; i<branchesLayout.getChildCount(); i++)
                if(branchesLayout.getChildAt(i) instanceof LinearLayout)
                    branchesLayout.removeViewAt(i--);
            View lastRemove = null, lastExpr = null;
            for(int i=activeBranches.size()-1; i>=0; i--) {
                LinearLayout stmtView = new LinearLayout(this);
                stmtView.setTag(i);
                stmtView.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams layParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layParams.setMargins(0, toDp(4), 0, toDp(4));
                stmtView.setLayoutParams(layParams);
                TextView tv = new TextView(this);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setText(activeBranches.get(i).generateStatement());
                tv.setPadding(0, 0, toDp(8), 0);
                LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tvParams.gravity = Gravity.CENTER_VERTICAL;
                tv.setLayoutParams(tvParams);
                lastExpr = tv;
                View filler = new View(this);
                LinearLayout.LayoutParams fill = new LinearLayout.LayoutParams(0, 0);
                fill.weight = 1;
                filler.setLayoutParams(fill);
                ImageView remove = new ImageView(this);
                remove.setImageResource(R.drawable.ic_delete_black_36dp);
                remove.setTag(i);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                remove.setLayoutParams(imgParams);
                remove.setOnClickListener(onRemoveBranch);
                lastRemove = remove;
                stmtView.addView(tv);
                //stmtView.addView(filler);
                stmtView.addView(remove);
                branchesLayout.addView(stmtView, 0);
            }
            if(isDemo && ONBOARDING_ENABLED) {
                boolean containsSecret = activeBranches.size()>0 && activeBranches.get(0).getExpression().literal.contains("secret");
                if(containsSecret) {
                    foundDemoSecret = true;
                    showcaseHelper.showShowcase(SHOWCASE_DEMO_SECRET, lastExpr, R.string.sc_editor_secret, true, true);
                } else {
                    showcaseHelper.showSequence(SHOWCASE_DEMO_BRANCHES, new View[]{branchesLayout, lastRemove},
                                                new int[]{R.string.sc_editor_branches1, R.string.sc_editor_branches2}, true);
                }
            }
        }
    },

    onAddBranch = v -> {
        //Utils.click(showBranches);
        AddBranchDialog.newInstance(chapter.getState()).show(getFragmentManager(), DIALOG_ADD_BRANCH);
    },

    onMakeQuestion  = v -> {
        if(unmodifiable) return;

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
                                getString(R.string.make_question_var_example), Limits.VAR_MAX_LENGTH, false)
        .show(getFragmentManager(), DIALOG_QUESTION_VAR);
    },

    onAddLabel = v -> InputDialog.newInstance(R.string.dialog_add_label_title,
                                              getString(R.string.dialog_add_label_text),
                                              R.string.add, R.string.cancel, "",
                                              getString(R.string.dialog_add_label_hint),
                                              Limits.STMT_MAX_LENGTH,
                                              false)
                                 .show(getFragmentManager(), DIALOG_ADD_LABEL),

    onAddJump = v -> InputDialog.newInstance(R.string.dialog_add_jump_title,
                                             getString(R.string.dialog_add_jump_text),
                                             R.string.add, R.string.cancel, "",
                                             getString(R.string.dialog_add_label_hint),
                                             Limits.STMT_MAX_LENGTH,
                                             false)
                                .show(getFragmentManager(), DIALOG_ADD_JUMP),

    onAddStatement = v -> InputDialog.newInstance(R.string.dialog_add_statement_title,
                                                  getString(R.string.dialog_add_statement_text),
                                                  R.string.add, R.string.cancel, "",
                                                  getString(R.string.dialog_add_statement_hint),
                                                  Limits.STMT_MAX_LENGTH,
                                                  false)
                                     .show(getFragmentManager(), DIALOG_ADD_STATEMENT),

    onAddInput = v-> new AddInputDialog().show(getFragmentManager(), DIALOG_ADD_INPUT),

    onSave = v -> {

        File source = chapter.getSourceFile();
        try (FileWriter fw = new FileWriter(source)) {
            insertLinesToExecution();
            for(Line line : execution) {
                fw.write(line.generateCode());
                fw.write('\n');
            }
            recentlySaved = true;
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            //todo better visual indicator, move saving to background thread ?
            //compile chapter to check whether jumps and other stuff are valid
        } catch (IOException e) {
            exceptionHandler.handleBookIOException(e);
        }
    },

    onDeleteScene = v -> {
        if(executionPosition == execution.size()) Utils.click(restartScene);
        else {
            execution.remove(executionPosition);
            sceneDeleted = true;
            Utils.click(nextScene); //do we want to move to next or previous?
        }
    };

    private TextWatcher characterWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            String name = editable.toString();
            if(name.isEmpty())
                avatar.setVisibility(View.INVISIBLE);
            else {
                avatar.setVisibility(View.VISIBLE);
                if (chapter.getState().hasVariable(name))
                    setAvatar(files.getAvatar(chapter.getBook().getName(), chapter.getState().getString(name)));
                else
                    setAvatar(null);
            }
        }
    };
    private InputFilter characterFilter  = (source, start, end, dest, dstart, dend) -> {
        if (source instanceof SpannableStringBuilder) {
            SpannableStringBuilder ret = (SpannableStringBuilder) source;
            for (int i = end - 1; i >= start; i--) {
                char ch = source.charAt(i);
                if (!isValidCharacterName(i, ch)) {
                    ret.delete(i, i + 1);
                }
            }
            return ret;
        } else {
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if(isValidCharacterName(i, ch))
                    ret.append(ch);
            }
            return ret;
        }
    };

    private List<Line> makeLine() {
        try {
            List<Line> lines = new ArrayList<>();
            if(unmodifiable) {
                lines.add(currentLine);
                return lines;
            }
            List<String> ansVars = new ArrayList<>();
            if(currentLine instanceof Question && questionVar == null) {
                questionVar = ((Question) currentLine).getVariable();
                for(AnswerLike ans : ((Question)currentLine).getDisplayedAnswers())
                    ansVars.add(ans.getVariable());
            }
            int linenum = executionPosition, indent = getIndent();
            String text = narrative.getText().toString();
            String character = this.character.getText().toString();
            if (answers != null && !answers.isEmpty()) {
                Question q = new Question(chapter, questionVar, text, character, linenum, indent);
                lines.add(q);
                for(int i=0; i<answers.size(); i++) {
                    lines.add(new Answer(chapter, i < ansVars.size() ? ansVars.get(i) : questionVar + "_ans" + (i + 1),
                                         answers.get(i).getText().toString(),
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
            questionVar = null;
            return lines;
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
            return new ArrayList<>();
        }
    }

    /**
     * Insert lines to execution and return number of lines inserted.
     * Replaces whatever is at current executionPosition with Line made according to current state on screen
     * @return number of lines inserted
     */
    private int insertLinesToExecution() {
        int pos = executionPosition;
        if(sceneDeleted) {
            sceneDeleted = false;
            return 0;
        }
        if(executionPosition == execution.size() && isEmptyScene()) return 0;

        if(pos == execution.size())
            execution.add(null);
        else if(execution.get(pos) instanceof Question) {
            int next = pos+1; //if current line is a question, we need to remove it before replacing it with something new
            while(next < execution.size() &&
                  execution.get(next).getIndent() > execution.get(pos).getIndent())
                execution.remove(next);
            if(execution.get(next) instanceof Nop) execution.remove(next); //cleaning up nops
        }
        List<Line> lines = makeLine(); //todo maybe optimize not to remake line if nothing was touched ?
        if(lines != null && !lines.isEmpty()) {
            currentLine = lines.get(0);
            execution.set(pos++, currentLine);
            for (int i = 1; i < lines.size(); i++) {
                execution.add(pos++, lines.get(i));
                try {
                    if(pos-2>=0)
                        execution.get(pos - 2).setNextLine(execution.get(pos - 1));
                } catch (InterpretationException e) {
                    exceptionHandler.handleInterpretationException(e);
                }
            }
            if(execution.size() < pos) {
                try {
                    execution.get(pos - 1).setNextLine(execution.get(pos));
                } catch (InterpretationException e) {
                    exceptionHandler.handleInterpretationException(e);
                }
            }

        }
        return pos-executionPosition;
    }

    private boolean isLastScene() {
        if(executionPosition >= execution.size()) return true;
        for(int i=executionPosition; i<execution.size(); i++)
            if(isShowableLine(execution.get(i)))
                return false;
        return true;
    }
    private boolean isFirstScene() {
        if(executionPosition <= 0) return true;
        if(executionPosition >= execution.size()) return false;
        for(int i=executionPosition; i>=0; i--)
            if(isShowableLine(execution.get(i)))
                return false;
        return true;
    }

    /**
     * Sets active branches for current line. Used when moving backwards.
     * currentLine mustn't be null, and executionPosition must be set so
     * execution.get(executionPosition) == currentLine.
     */
    private void setActiveBranches() {
        activeBranches.clear();
        int indent = currentLine.getIndent();
        int pos = executionPosition-1;
        while(pos >= 0 && indent > 0) {
            Line l = execution.get(pos--);
            if(l.getIndent() > indent) continue;
            if(l instanceof IfStatement && l.getIndent() < indent)
                activeBranches.add((IfStatement)l);
            if(l.getIndent() <= indent) indent = l.getIndent();
        }
        Collections.reverse(activeBranches);
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

        showcaseHelper = new Showcase(this);
        handler = new Handler();
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
        character.addTextChangedListener(characterWatcher);
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
        setVariable.setOnClickListener(v -> SetVariableDialog.newInstance(chapter.getState())
                                                             .show(getFragmentManager(), DIALOG_SET_VARIABLE));
        addLabel = findViewById(R.id.story_editor_btn_add_label);
        addLabel.setOnClickListener(onAddLabel);
        addJump = findViewById(R.id.story_editor_btn_goto);
        addJump.setOnClickListener(onAddJump);
        addStatement = findViewById(R.id.story_editor_btn_custom_stmt);
        addStatement.setOnClickListener(onAddStatement);
        addInput = findViewById(R.id.story_editor_btn_add_input);
        addInput.setOnClickListener(onAddInput);
        save = findViewById(R.id.story_editor_save);
        save.setOnClickListener(onSave);
        deleteScene = findViewById(R.id.story_editor_delete);
        deleteScene.setOnClickListener(onDeleteScene);
        unrepresentableLineDescription = findViewById(R.id.story_editor_special_line_desc);
        labelText = findViewById(R.id.story_editor_label_text);
        gotoText = findViewById(R.id.story_editor_goto_text);

        InputFilter[] filters    = character.getFilters();
        InputFilter[] newFilters = new InputFilter[filters.length + 1];
        System.arraycopy(filters, 0, newFilters, 0, filters.length);
        newFilters[newFilters.length - 1] = characterFilter;
        character.setFilters(newFilters);

        Book book = Runtime.loadBook(title, files, this, exceptionHandler).getCurrentBook();
        int chapterNo = getIntent().getIntExtra(EXTRA_CHAPTER_NO, 1);
        chapter = book.getUnderlyingBook().getChapter(chapterNo);
        isDemo = book.isDemo() && chapterNo == 3;
        try {
            book.getState().setVariable("__line__", 0);
            chapter.getState().saveToFile(book.getStateFile());
        } catch (IOException e) {
            exceptionHandler.handleBookIOException(e);
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
        }

        layout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(System.currentTimeMillis() - lastHideTriggered > HIDE_UI_GRACE_PERIOD)
                delayedHide(100);
            lastHideTriggered = System.currentTimeMillis();
            //this originally triggered only on keyboard closing. Having failed to make it work reliably,
            //it just hides everything every time anything is changed
            //edit: introduced grace period in case this method gets called extremely often
        });

        if(isDemo && ONBOARDING_ENABLED) {
            handler.postDelayed(() -> {
                showcaseHelper.showSequence(SHOWCASE_DEMO_INTRO_BRANCHES, new View[]{null, character, avatar, narrative, showBranches},
                                            new int[]{R.string.sc_editor_intro1, R.string.sc_editor_intro2, R.string.sc_editor_intro3,
                                                      R.string.sc_editor_intro4, R.string.sc_editor_intro_branches}, true);
            }, 300);
        }
    }

    private boolean isShowableLine(Line line) {
        return line != null && !(line instanceof Statement) && !(line instanceof AnswerLike)
                && !(line instanceof Directive) && !(line instanceof Nop) &&
                !line.getDirectives().contains(DIRECTIVE_HIDDEN_LINE);
    }

    /* Will not work while showBranches subwindow is open !! */
    private int getIndent() {
        if(executionPosition == 0) return 0;
        if(activeBranches.isEmpty()) /*return execution.get(executionPosition-1).getIndent();*/ //this is awkward for deleting branches
            return 0;
        else return activeBranches.get(activeBranches.size()-1).getIndent() + 2;
    }

    private boolean isEmptyScene() {
        return narrative.getText().length() == 0 &&
               unrepresentableLineDescription.getVisibility() != View.VISIBLE &&
               (answers == null || answers.isEmpty());
    }

    /**
     * Returns null if there is no preceding label to this scene (pretty often)
     */
    private LabelStatement getPrecedingLabel() {
        int pos = executionPosition-1;
        while(pos >= 0) {
            if (execution.get(pos) instanceof LabelStatement) return (LabelStatement) execution.get(pos);
            else if (isShowableLine(execution.get(pos))) return null;
            pos--;
        }
        return null;
    }
    private GotoStatement getFollowingGoto() {
        int pos = executionPosition+1;
        while(pos < execution.size()) {
            if (execution.get(pos) instanceof GotoStatement) return (GotoStatement) execution.get(pos);
            else if (isShowableLine(execution.get(pos))) return null;
            pos++;
        }
        return null;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
        try {
            Line ahead = chapter.compile(); //todo do parsing manually, to avoid setting jumps (?)
            if(ahead == null) return;
            Line startFrom=null; int starti=-1;

            if(isShowableLine(ahead)) currentLine = ahead;
            int i=0;
            while(ahead != null) {
                execution.add(ahead);
                int last = activeBranches.size()-1;
                while(!activeBranches.isEmpty() && ahead.getIndent() <= activeBranches.get(last).getIndent())
                    activeBranches.remove(last--);
                if(ahead instanceof IfStatement) activeBranches.add((IfStatement) ahead);

                ahead = ahead.getNextLine();
                i++;
                if(isShowableLine(ahead)) {
                    currentLine = ahead;
                    executionPosition = i;
                    if(ahead.getDirectives().contains(DIRECTIVE_START_FROM)) {
                        startFrom = ahead;
                        starti = executionPosition;
                    }
                }
            }

            if(startFrom != null) {
                currentLine = startFrom;
                executionPosition = starti;
            }

            setupViews();
        } catch (IOException e) {
            exceptionHandler.handleBookIOException(e);
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
                InputStream stream = this.getContentResolver().openInputStream(data.getData());
                //word of advice and a friendly reminder: do not fucking consume this stream
                AssignStatement stmt = null;
                String imageName;
                switch (requestCode) {
                    case INTENT_PICK_AVATAR:
                        imageName = character.getText().toString() + "_" + Integer.toString(random.nextInt(1295), 36);
                        while(files.getAvatar(chapter.getBook().getName(), imageName) != null)
                            imageName = character.getText().toString() + "_" + Integer.toString(random.nextInt(1295), 36);
                        //avatar.setImageBitmap(Utils.loadImage(stream, avatar.getWidth()));
                        stmt = new AssignStatement(chapter, executionPosition,
                                                                   getIndent(),
                                                                   character.getText().toString(),
                                                                   imageName);
                        File avatar = files.setAvatar(imageName, stream);
                        setAvatar(avatar);
                        break;
                    case INTENT_PICK_BACKGROUND:
                        imageName = "background_" + Integer.toString(random.nextInt(46655), 36);
                        while(files.imageExists(imageName))
                            imageName ="background_" + Integer.toString(random.nextInt(46655), 36);
                        stmt = new AssignStatement(chapter, executionPosition,
                                                   getIndent(),
                                                   VAR_BACKGROUND,
                                                   imageName);
                        File background = files.setBackground(imageName, stream);
                        layout.setBackgroundDrawable(new BitmapDrawable(getResources(), BitmapUtils.loadImage(background, layout.getMaxWidth())));
                        break;
                }

                if(stmt != null)
                    stmt.execute(); //this is the only Statement we execute - need it in state for future statements
                execution.add(executionPosition++, stmt);
            } catch (IOException e) {
                exceptionHandler.handleBookIOException(e);
            } catch (InterpretationException e) {
                exceptionHandler.handleInterpretationException(e);
            }
        }
    }

    //existence of this method proves the sheer idiocy of Android APIs sometimes
    private static void setDisabledTextView(TextView tv, boolean disable) {
        //tv.setFocusable(!disable); //this fucks stuff up. no idea why.
        tv.setClickable(!disable);
        tv.setEnabled(!disable);
        tv.setLongClickable(!disable);
        //tv.setCustomSelectionActionModeCallback(disable ? emptyActionMode : null); this actually breaks stuff
    }

    private void setVisuals() {
        restartScene.setVisibility(View.VISIBLE);
        narrative.setVisibility(View.VISIBLE);
        character.setVisibility(View.VISIBLE);
        characterWatcher.afterTextChanged(character.getText());
        unrepresentableLineDescription.setVisibility(View.GONE);
        setDisabledTextView(narrative, unmodifiable);
        setDisabledTextView(character, unmodifiable);
        if(!activeBranches.isEmpty())
            showBranches.setImageResource(R.drawable.ic_source_branch_red);
        else
            showBranches.setImageResource(R.drawable.ic_source_branch);
        LabelStatement label = getPrecedingLabel();
        if(label == null) labelText.setVisibility(View.INVISIBLE);
        else {
            labelText.setText(label.getLabel() + ":");
            labelText.setVisibility(View.VISIBLE);

            if(isDemo && ONBOARDING_ENABLED)
                showcaseHelper.showSequence(SHOWCASE_DEMO_LABEL, new View[]{null, labelText, showBranches},
                                            new int[]{R.string.sc_editor_label1, R.string.sc_editor_label2, R.string.sc_editor_label3},
                                            true);
        }
        GotoStatement gotos = getFollowingGoto();
        if(gotos == null) gotoText.setVisibility(View.INVISIBLE);
        else {
            gotoText.setText(">" + gotos.getTarget());
            gotoText.setVisibility(View.VISIBLE);
            if(isDemo && ONBOARDING_ENABLED)
                handler.postDelayed(() -> {
                    showcaseHelper.showSequence(SHOWCASE_DEMO_GOTO, new View[]{null, gotoText, previousScene},
                                                new int[]{R.string.sc_editor_goto1, R.string.sc_editor_goto2, R.string.sc_editor_goto3},
                                                true);
                }, 600);
        }

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
        answersScroll.setVisibility(View.VISIBLE);
        answersLayout.removeAllViews();
        answersLayout.addView(makeQuestion);
        makeQuestion.setVisibility(View.VISIBLE);
        answers = null;

    }

    private void setupViews() {
        unmodifiable = currentLine != null && currentLine.getDirectives().contains(DIRECTIVE_UNMODIFIABLE_LINE);

        resetViews();
        setVisuals();
        if (currentLine != null && isShowableLine(currentLine)) { currentLine.execute(); }
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
        if (image != null && image.isFile() && image.equals(previousAvatar)) return;

        if (image != null) {
            avatar.setVisibility(View.VISIBLE);
            avatar.setImageBitmap(BitmapUtils.loadImage(image, avatar.getWidth()));
            previousAvatar = image;
        } else {
            avatar.setImageResource(R.drawable.ic_empty_person);
        }
    }

    private void showUnrepresentableLine() {
        unrepresentableLineDescription.setVisibility(View.VISIBLE);
        narrative.setVisibility(View.GONE);
        character.setVisibility(View.GONE);
        answersScroll.setVisibility(View.GONE);
        avatar.setVisibility(View.GONE);
        restartScene.setVisibility(View.GONE);
        unmodifiable = true;
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
        narrative.setText(((Narrative)currentLine).getRawText());
        character.setText("");
    }

    @Override
    public void showSpeech(String character, File avatar, String text) {
        narrative.setText(((Speech)currentLine).getRawText());
        this.character.setText(character);
        setAvatar(avatar);
    }

    @Override
    public int showQuestion(String question, String character, File avatar, double time, String... answers) {
        setAvatar(avatar);
        this.character.setText(character);
        this.narrative.setText(((Question)currentLine).getRawText());

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
        unrepresentableLineDescription.setText(getString(R.string.story_editor_input_desc, ((TextInput)currentLine).getVariable()));
        showUnrepresentableLine();
        return "";
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
        unrepresentableLineDescription.setText(R.string.story_editor_endchapter_desc);
        showUnrepresentableLine();
    }

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        try {
            switch (dialog.getTag()) {
                case DIALOG_QUESTION_VAR:
                    if(s != null && !s.isEmpty())
                        questionVar = s;
                    else {
                        do {
                            questionVar = "q_" + Integer.toHexString(random.nextInt(4096));
                        } while(chapter.getState().hasVariable(questionVar));
                        InfoDialog.newInstance(getString(R.string.story_editor_no_questionvar_title),
                                               getString(R.string.story_editor_no_questionvar_text, questionVar))
                                  .show(getFragmentManager(), DIALOG_INFO_NO_QUESTIONVAR);
                    }
                    break;

                case DIALOG_ADD_LABEL:
                    if(s != null && !s.isEmpty()) {
                        LabelStatement label = new LabelStatement(chapter, s, executionPosition, getIndent());
                        execution.add(executionPosition++, label);
                        labelText.setText(s + ":");
                    }
                    break;
                case DIALOG_ADD_JUMP:
                    if(s != null && !s.isEmpty()) {
                        jump = new GotoStatement(chapter, executionPosition+1, getIndent(), s);
                        //note to self: this isn't a bug; we're adding this to execution later
                        gotoText.setText(">" + s);
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
            for(int i=executionPosition; i<execution.size() && execution.get(i).getIndent()>=ifs.getIndent(); i++)
                execution.get(i).setIndent(execution.get(i).getIndent()+2);
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


    private int toDp(int val) {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics());
    }

    @Override
    public void onInputAdded(String variable, String hint) {
        try {
            currentLine = new TextInput(chapter, variable, hint, executionPosition, getIndent());
        } catch (InterpretationException e) {
            exceptionHandler.handleInterpretationException(e);
        }
        unrepresentableLineDescription.setText(getString(R.string.story_editor_input_desc, variable));
        showUnrepresentableLine(); //we're tagging line as unmodifiable here - makeLine will just use the currentLine
        Utils.click(showCodeOps);
    }
}
