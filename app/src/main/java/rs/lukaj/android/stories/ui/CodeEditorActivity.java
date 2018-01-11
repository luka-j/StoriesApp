package rs.lukaj.android.stories.ui;

import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.controller.FileIOException;
import rs.lukaj.android.stories.controller.Runtime;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.android.stories.io.FileUtils;
import rs.lukaj.android.stories.ui.dialogs.ConfirmDialog;
import rs.lukaj.stories.Utils;
import rs.lukaj.stories.exceptions.InterpretationException;
import rs.lukaj.stories.parser.Parser;
import rs.lukaj.stories.parser.lines.Answer;
import rs.lukaj.stories.parser.lines.AssignStatement;
import rs.lukaj.stories.parser.lines.Directive;
import rs.lukaj.stories.parser.lines.EndChapter;
import rs.lukaj.stories.parser.lines.GotoStatement;
import rs.lukaj.stories.parser.lines.IfStatement;
import rs.lukaj.stories.parser.lines.LabelStatement;
import rs.lukaj.stories.parser.lines.Line;
import rs.lukaj.stories.parser.lines.Narrative;
import rs.lukaj.stories.parser.lines.Nop;
import rs.lukaj.stories.parser.lines.PictureAnswer;
import rs.lukaj.stories.parser.lines.ProcedureLabelStatement;
import rs.lukaj.stories.parser.lines.Question;
import rs.lukaj.stories.parser.lines.ReturnStatement;
import rs.lukaj.stories.parser.lines.Speech;
import rs.lukaj.stories.parser.lines.TextInput;
import rs.lukaj.stories.runtime.Chapter;

import static rs.lukaj.android.stories.ui.MainActivity.ONBOARDING_ENABLED;

public class CodeEditorActivity extends AppCompatActivity implements ConfirmDialog.Callbacks {

    public static final String EXTRA_BOOK_NAME            = "code.extra.bookname";
    public static final String EXTRA_CHAPTER_NO           = "code.extra.chapterno";
    private static final String TAG_CONFIRM_SAVE          = "code.dialog.confirmsave";
    private static final String TAG_CONFIRM_EXIT_AND_SAVE = "code.dialog.saveandexit";
    private static final String TAG_CONFIRM_EXIT          = "code.dialog.exit";
    private static final String SHOWCASE_TUTORIAL         = "code.demo.tutorial";
    private static final String SHOWCASE_ERROR            = "code.showcase.error";

    private Toolbar  toolbar;
    private EditText editor;
    private Button   colon, question, answer, ifstmt, gotostmt, input, comment, halt;
    private Handler handler;
    private ExceptionHandler.DefaultHandler exceptionHandler;

    private Chapter chapter;
    private Runtime rt;
    private File         source;
    private AndroidFiles files;
    private Showcase showcaseHelper;

    private boolean containsErrors = false;
    private boolean isSaved = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_editor);
        handler = new Handler();
        showcaseHelper = new Showcase(this);
        files = new AndroidFiles(this);
        exceptionHandler = new ExceptionHandler.DefaultHandler(this);

        String bookName = getIntent().getStringExtra(EXTRA_BOOK_NAME);

        int    chapterNo = getIntent().getIntExtra(EXTRA_CHAPTER_NO, 1);
        rt = Runtime.loadBook(bookName, files, new NullDisplay(), exceptionHandler);

        chapter = rt.getCurrentBook().getUnderlyingBook().getChapter(chapterNo);
        source = chapter.getSourceFile();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true); enable after figuring out why extras in parent get lost

        editor = findViewById(R.id.code_editor_editor);
        editor.addTextChangedListener(new SyntaxHighlighter());
        try {
            editor.setText(FileUtils.loadFile(source));
        } catch (IOException e) {
            exceptionHandler.handleBookIOException(e);
        }

        colon = findViewById(R.id.editor_btn_colon);
        colon.setOnClickListener(v -> insert(":"));
        question = findViewById(R.id.editor_btn_question);
        question.setOnClickListener(v -> {
            insert("?[]");
            editor.setSelection(editor.getSelectionEnd()-1);
        });
        answer = findViewById(R.id.editor_btn_answer);
        answer.setOnClickListener(v -> {
            insert("*[]");
            editor.setSelection(editor.getSelectionEnd()-1);
        });
        ifstmt = findViewById(R.id.editor_btn_ifstmt);
        ifstmt.setOnClickListener(v -> {
            insert(":?");
            editor.setSelection(editor.getSelectionEnd()-1);
        });
        gotostmt = findViewById(R.id.editor_btn_gotostmt);
        gotostmt.setOnClickListener(v -> insert(":>"));
        input = findViewById(R.id.editor_btn_input);
        input.setOnClickListener(v -> {
            insert("[]");
            editor.setSelection(editor.getSelectionEnd()-1);
        });
        comment = findViewById(R.id.editor_btn_comment);
        comment.setOnClickListener(v -> insert("//"));
        halt = findViewById(R.id.editor_btn_halt);
        halt.setOnClickListener(v -> insert(";;"));

        if(ONBOARDING_ENABLED) {
            handler.postDelayed(() -> {
                showcaseHelper.showSequence(SHOWCASE_TUTORIAL,
                                            new View[]{null, null, colon, question, answer, ifstmt, gotostmt, input,
                                                       comment, halt, null},
                                            new int[]{R.string.sc_codetut_intro, R.string.sc_codetut_intro2,
                                                      R.string.sc_codetut_colon, R.string.sc_codetut_question,
                                                      R.string.sc_codetut_answer, R.string.sc_codetut_ifstmt,
                                                      R.string.sc_codetut_gotostmt, R.string.sc_codetut_input,
                                                      R.string.sc_codetut_comment, R.string.sc_codetut_halt,
                                                      R.string.sc_codetut_outro},
                                            false);
            }, 1200);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_code_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_item_save) {
            if(!containsErrors) {
                saveSource();
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            } else {
                ConfirmDialog.newInstance(R.string.saveq, R.string.confirm_save, R.string.save, R.string.dontsave)
                             .show(getFragmentManager(), TAG_CONFIRM_SAVE);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void insert(String text) {
        int start = Math.max(editor.getSelectionStart(), 0);
        int end = Math.max(editor.getSelectionEnd(), 0);
        editor.getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());
    }

    private void saveSource() {
        try(BufferedWriter out = new BufferedWriter(new FileWriter(source))) {
            out.write(editor.getText().toString());
            isSaved = true;
            if(exitAfterSave) onBackPressed();
        } catch (IOException e) {
            exceptionHandler.handleFileException(new FileIOException(source, e));
        }
    }

    private boolean exitAfterSave = false;
    @Override
    public void onPositive(DialogFragment dialog) {
        switch (dialog.getTag()) {
            case TAG_CONFIRM_SAVE:
                saveSource();
                break;
            case TAG_CONFIRM_EXIT_AND_SAVE:
                exitAfterSave = true;
                saveSource();
                break;
            case TAG_CONFIRM_EXIT:
                super.onBackPressed();
                break;
        }
    }

    @Override
    public void onNegative(DialogFragment dialog) {
        if(dialog.getTag().equals(TAG_CONFIRM_EXIT_AND_SAVE)) super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if(isSaved) {
            super.onBackPressed();
        } else if(!containsErrors) {
            ConfirmDialog.newInstance(R.string.exitq, R.string.confirm_save_or_exit, R.string.save_and_exit,
                                      R.string.dontsave, R.string.cancel)
                         .show(getFragmentManager(), TAG_CONFIRM_EXIT_AND_SAVE);
        } else {
            ConfirmDialog.newInstance(R.string.exitq, R.string.confirm_exit, R.string.exit, R.string.cancel)
                         .show(getFragmentManager(), TAG_CONFIRM_EXIT);
        }
    }


    private class SyntaxHighlighter implements TextWatcher {
        private static final int HIGHLIGHT_INTERVAL = 800;
        private static final int TYPING_INTERVAL    = 300;
        private static final String TAG             = "stories.highlighter";

        private long lastType;
        private boolean awaitingHighlight           = false;
        private Handler handler;
        private Editable editable;
        private Parser   parser;
        private int insertedNewlineAt = -1;

        public SyntaxHighlighter() {
            handler = new Handler();
            parser = new Parser(chapter);
        }
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            isSaved = false;
            //we're ignoring backspaces here, and looking at the last inserted character
            if(count > 0 && charSequence.charAt(start+count-1) == '\n') insertedNewlineAt = start+count-1;
            else insertedNewlineAt = -1;//this is supposed to be unreliable; if it makes trouble, find a better solution
        }

        @Override
        public void afterTextChanged(Editable editable) {
            lastType = System.currentTimeMillis();
            if(insertedNewlineAt >= 0) {
                if(editable.charAt(insertedNewlineAt) == '\n') {
                    int indent = getIndentEndline(editable, insertedNewlineAt-1);
                    editable.insert(insertedNewlineAt+1, Utils.generateIndent(indent));
                } else {
                    Log.e(TAG, "Captured newline in onTextChanged, but it isn't there in afterTextChanged!");
                }
            }
            this.editable = editable;
            if(!awaitingHighlight) {
                awaitingHighlight = true;
                handler.postDelayed(this::highlight, HIGHLIGHT_INTERVAL);
            }
        }

        //I'm too embarrassed to confess how much time I've spent on this
        private int getIndentEndline(Editable str, int endline) {
            if(str.charAt(endline) == '\n') return 0; //empty line
            while (endline > 0 && str.charAt(--endline) != '\n') ;
            int indent = 0;
            while(str.charAt(++endline) == ' ') indent++; //we don't support mixed indent here (and it shouldn't happen)
            return indent;
        }

        private void highlight() {
            if(System.currentTimeMillis() - lastType > TYPING_INTERVAL) {
                awaitingHighlight = false;
                int      starti = 0, linenum = 0;
                String[] lines  = editable.toString().split("\\n");
                containsErrors = false;
                for (String line : lines) {
                    try {
                        Line l = parser.parse(line, linenum++);
                        editable.setSpan(new ForegroundColorSpan(getColor(l)), starti, starti+line.length(),
                                         Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } catch (InterpretationException|RuntimeException e) {
                        editable.setSpan(new ForegroundColorSpan(Color.RED), starti, starti+line.length(),
                                         Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        containsErrors = true;
                        if(ONBOARDING_ENABLED) {
                            handler.postDelayed(() -> {
                                showcaseHelper.showShowcase(SHOWCASE_ERROR, R.string.sc_codeerror, true);
                            }, 800);
                        }
                    }
                    int commInx = line.indexOf("//"); //todo make this work for escaped slashes
                    if(commInx >= 0) {
                        editable.setSpan(new ForegroundColorSpan(Color.rgb(150, 150, 150)),
                                         starti+commInx, starti+line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    starti += line.length() + 1;
                }
            } else {
                handler.postDelayed(this::highlight, TYPING_INTERVAL);
            }
        }

        private int getColor(Line line) {
            if(line instanceof Answer) return Color.rgb(80, 180, 90);
            if(line instanceof AssignStatement) return Color.rgb(60, 100, 220);
            if(line instanceof Directive) return Color.rgb(160, 160, 200);
            if(line instanceof EndChapter) return Color.rgb(180, 120, 120);
            if(line instanceof GotoStatement) return Color.rgb(80, 160, 180);
            if(line instanceof IfStatement) return Color.rgb(220, 160, 80);
            if(line instanceof ProcedureLabelStatement) return Color.rgb(200, 180, 120);
            if(line instanceof LabelStatement) return Color.rgb(200, 200, 80);
            if(line instanceof Narrative) return Color.rgb(20, 20, 20);
            if(line instanceof Nop) return Color.rgb(150, 150, 150);
            if(line instanceof PictureAnswer) return Color.rgb(120, 180, 80);
            if(line instanceof Question) return Color.rgb(200, 80, 140);
            if(line instanceof ReturnStatement) return Color.rgb(60, 180, 200);
            if(line instanceof Speech) return Color.rgb(100, 40, 40);
            if(line instanceof TextInput) return Color.rgb(200, 140, 80);
            return Color.rgb(0, 0, 0);
        }
    }
}
