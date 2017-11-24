package rs.lukaj.android.stories.ui;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.controller.Runtime;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;
import static rs.lukaj.android.stories.ui.StoryUtils.*;
import rs.lukaj.stories.environment.DisplayProvider;
import rs.lukaj.stories.runtime.State;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class StoryActivity extends AppCompatActivity implements DisplayProvider, InputDialog.Callbacks {
    public static final String EXTRA_BOOK_NAME = "eBookName";

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int    UI_ANIMATION_DELAY = 300;
    private static final String DEBUG_TAG          = "stories.debug";
    private static final String TAG_INPUT_DIALOG   = "stories.input";

    private final Handler  mHideHandler       = new Handler();
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            layout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                                         | View.SYSTEM_UI_FLAG_FULLSCREEN
                                         | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                         | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                         | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                         | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private AndroidFiles files;

    private boolean tapAnywhereToContinue = true;
    private boolean closeOnTap            = false;
    private TextView         character;
    private TextView         narrative;
    private TextView         countdown;
    private ImageView        avatar;
    private ConstraintLayout layout;
    private LinearLayout     answersLayout;
    private ScrollView       answersScroll;
    private Guideline        leftTextGuideline, rightTextGuideline, topTextGuideline, bottomTextGuideline;
    private Guideline        leftAnswersGuideline, rightAnswersGuideline, topAnswersGuideline, bottomAnswersGuideline;
    private Guideline        leftCharacterGuideline, rightCharacterGuideline, bottomCharacterGuideline;
    private Guideline        leftCountdownGuideline, rightCountdownGuideline, bottomCountdownGuideline;
    private Guideline        rightAvatarGuideline, bottomAvatarGuideline;

    private long   countdownInterval       = 100;
    private String countdownFormat         = "%.1fs";
    private int    countdownColor          = Color.parseColor("#ffffff");

    private View.OnClickListener advanceListener = v -> {
        if (tapAnywhereToContinue) {
            Runtime.getRuntime().advance();
            Log.d(DEBUG_TAG, "tap");
        }
        if (closeOnTap) { finish(); }
    };
    private volatile int selectedAnswer;

    private View.OnClickListener onAnswerClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selectedAnswer = (int) v.getTag();
            answersLayout.removeAllViews();
            answersScroll.setVisibility(View.GONE);
            Runtime.getRuntime().advance();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_story);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        files = new AndroidFiles(getApplicationContext());

        character = findViewById(R.id.story_character_name);
        narrative = findViewById(R.id.story_text);
        countdown = findViewById(R.id.countdown_text);
        avatar = findViewById(R.id.story_editor_avatar);
        layout = findViewById(R.id.story_layout);
        answersLayout = findViewById(R.id.story_answers);
        answersScroll = findViewById(R.id.story_answers_scroll);

        leftTextGuideline = findViewById(R.id.left_guideline_text);
        rightTextGuideline = findViewById(R.id.right_guideline_text);
        bottomTextGuideline = findViewById(R.id.bottom_guideline_text);
        topTextGuideline = findViewById(R.id.top_guideline_text);
        leftAnswersGuideline = findViewById(R.id.left_guideline_answers);
        rightAnswersGuideline = findViewById(R.id.right_guideline_answers);
        topAnswersGuideline = findViewById(R.id.top_guideline_answers);
        bottomAnswersGuideline = findViewById(R.id.bottom_guideline_answers);
        leftCharacterGuideline = findViewById(R.id.left_guideline_character);
        bottomCharacterGuideline = findViewById(R.id.bottom_guideline_character);
        rightCharacterGuideline = findViewById(R.id.right_guideline_character);
        leftCountdownGuideline = findViewById(R.id.left_guideline_countdown);
        bottomCountdownGuideline = findViewById(R.id.bottom_guideline_countdown);
        rightCountdownGuideline = findViewById(R.id.right_guideline_countdown);
        rightAvatarGuideline = findViewById(R.id.right_guideline_image);
        bottomAvatarGuideline = findViewById(R.id.bottom_guideline_image);

        answersScroll.setVisibility(View.GONE);
        layout.setOnClickListener(advanceListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Runtime.getRuntime().exit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
        Runtime.loadBook(getIntent().getStringExtra(EXTRA_BOOK_NAME),
                         files,
                         this,
                         new ExceptionHandler.DefaultHandler(this)).execute();
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

    //goal - set everything without resorting to ifs in this method (as it leads to  more cluttered code)
    private void setVisuals(State variables) {
        narrative.setVisibility(View.VISIBLE);
        if (variables == null) return;

        setBackgroundFromState(getResources(), files, variables, VAR_BACKGROUND, layout);
        setBackgroundFromState(getResources(), files, variables, VAR_TEXT_BACKGROUND, narrative);
        setBackgroundFromState(getResources(), files, variables, VAR_COUNTDOWN_BACKGROUND, countdown);
        setBackgroundFromState(getResources(), files, variables, VAR_CHARACTER_BACKGROUND, character);

        setPaddingFromState(getResources(), variables, narrative, VAR_TEXT_VERTICAL_PADDING, VAR_TEXT_HORIZONTAL_PADDING);
        setPaddingFromState(getResources(), variables, character, VAR_CHARACTER_VERTICAL_PADDING, VAR_CHARACTER_HORIZONTAL_PADDING);

        setVerticalMarginsFromState(getResources(), variables, character, VAR_CHARACTER_VERTICAL_MARGINS);
        setVerticalMarginsFromState(getResources(), variables, countdown, VAR_COUNTDOWN_VERTICAL_MARGINS);

        setGuidelineFromState(variables, VAR_LEFT_TEXT_GUIDELINE, leftTextGuideline);
        setGuidelineFromState(variables, VAR_RIGHT_TEXT_GUIDELINE, rightTextGuideline);
        setGuidelineFromState(variables, VAR_TOP_TEXT_GUIDELINE, topTextGuideline);
        setGuidelineFromState(variables, VAR_BOTTOM_TEXT_GUIDELINE, bottomTextGuideline);
        setGuidelineFromState(variables, VAR_LEFT_ANSWER_GUIDELINE, leftAnswersGuideline);
        setGuidelineFromState(variables, VAR_RIGHT_ANSWER_GUIDELINE, rightAnswersGuideline);
        setGuidelineFromState(variables, VAR_TOP_ANSWER_GUIDELINE, topAnswersGuideline);
        setGuidelineFromState(variables, VAR_BOTTOM_ANSWER_GUIDELINE, bottomAnswersGuideline);
        setGuidelineFromState(variables, VAR_LEFT_CHARACTER_GUIDELINE, leftCharacterGuideline);
        setGuidelineFromState(variables, VAR_RIGHT_CHARACTER_GUIDELINE, rightCharacterGuideline);
        setGuidelineFromState(variables, VAR_BOTTOM_CHARACTER_GUIDELINE, bottomCharacterGuideline);
        setGuidelineFromState(variables, VAR_LEFT_COUNTDOWN_GUIDELINE, leftCountdownGuideline);
        setGuidelineFromState(variables, VAR_RIGHT_COUNTDOWN_GUIDELINE, rightCountdownGuideline);
        setGuidelineFromState(variables, VAR_BOTTOM_COUNTDOWN_GUIDELINE, bottomCountdownGuideline);
        setGuidelineFromState(variables, VAR_BOTTOM_AVATAR_GUIDELINE, bottomAvatarGuideline);
        setGuidelineFromState(variables, VAR_RIGHT_AVATAR_GUIDELINE, rightAvatarGuideline);

        alignFromState(variables, character, VAR_CHARACTER_ALIGNMENT, leftCharacterGuideline.getId(), rightCharacterGuideline.getId(), bottomCharacterGuideline.getId(), bottomCharacterGuideline.getId());
        alignFromState(variables, narrative, VAR_TEXT_ALIGNMENT, leftTextGuideline.getId(), rightTextGuideline.getId(), bottomTextGuideline.getId(), topTextGuideline.getId());
        alignFromState(variables, answersScroll, VAR_ANSWERS_ALIGNMENT, leftAnswersGuideline.getId(), rightAnswersGuideline.getId(), bottomAnswersGuideline.getId(), topAnswersGuideline.getId());
        alignFromState(variables, countdown, VAR_COUNTDOWN_ALIGNMENT, leftCountdownGuideline.getId(), rightCountdownGuideline.getId(), bottomCountdownGuideline.getId(), bottomCountdownGuideline.getId());
        alignFromState(variables, avatar, VAR_AVATAR_ALIGNMENT, rightAvatarGuideline.getId(), rightAvatarGuideline.getId(), bottomAvatarGuideline.getId(), bottomAvatarGuideline.getId());

        setAnswerPropsFromState(variables);

        if (variables.isNumeric(VAR_AVATAR_SIZE))
            setAvatarSize(getResources(), avatar, variables.getDouble(VAR_AVATAR_SIZE));
        countdownInterval = variables.getOrDefault(VAR_COUNTDOWN_INTERVAL, countdownInterval).longValue();
        countdownFormat = variables.getOrDefault(VAR_COUNTDOWN_FORMAT, countdownFormat);
        countdownColor = getOrDefaultColor(variables, VAR_COUNTDOWN_COLOR, countdownColor);
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

    private CountDownTimer countDownTimer;

    private void startCountdown(double seconds) {
        countdown.setVisibility(View.VISIBLE);
        countdown.setTextColor(countdownColor);
        countDownTimer = new CountDownTimer((long) (seconds * 1000), countdownInterval) {
            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long millisUntilFinished) {
                countdown.setText(String.format(countdownFormat, millisUntilFinished / 1000.0));
            }

            @Override
            public void onFinish() {
            }
        }.start();
    }

    @Override
    public void showNarrative(final String text) {
        final Runtime rt = Runtime.getRuntime();
        runOnUiThread(() -> {
            tapAnywhereToContinue = true;
            setVisuals(rt.getState());
            setAvatar(null);
            narrative.setText(text);
            character.setVisibility(View.INVISIBLE);
        });
        rt.pause();
    }

    @Override
    public void showSpeech(final String character, final File avatar, final String text) {
        final Runtime rt = Runtime.getRuntime();
        runOnUiThread(() -> {
            tapAnywhereToContinue = true;
            setVisuals(rt.getState());
            setAvatar(avatar);
            StoryActivity.this.character.setVisibility(View.VISIBLE);
            StoryActivity.this.character.setText(character);
            narrative.setText(text);

        });
        rt.pause();
    }

    @Override
    public int showQuestion(final String question, final String character, final File avatar,
                            final double time, final String... answers) {
        final Runtime rt = Runtime.getRuntime();
        runOnUiThread(() -> {
            tapAnywhereToContinue = false;
            setVisuals(rt.getState());
            setAvatar(avatar);
            answersScroll.setVisibility(View.VISIBLE);
            selectedAnswer = -1;
            narrative.setText(question);
            if (character != null && !character.isEmpty()) {
                StoryActivity.this.character.setText(character);
                StoryActivity.this.character.setVisibility(View.VISIBLE);
            }
            for (int i = 0; i < answers.length; i++) {
                answersLayout.addView(generateAnswer(StoryActivity.this, files,
                                                     new TextView(StoryActivity.this), onAnswerClicked,
                                                     answers[i], i));
            }
            if (time != 0) {
                startCountdown(time);
            }
        });

        if (time == 0) {
            rt.pause();
        } else {
            rt.pauseFor((long) (time * 1000));
            countDownTimer.cancel();
            runOnUiThread(() -> countdown.setVisibility(View.GONE));
        }

        return selectedAnswer;
    }

    @Override
    public int showPictureQuestion(String question, String character, File avatar, double time, File... answers) {
        final Runtime rt = Runtime.getRuntime();
        runOnUiThread(() -> {
            tapAnywhereToContinue = false;
            setVisuals(rt.getState());
            //todo support picture questions?
        });

        rt.pause();
        return 0;
    }

    @Override
    public String showInput(final String hint) {
        final Runtime rt = Runtime.getRuntime();
        runOnUiThread(() -> {
            tapAnywhereToContinue = false;
            inputText = null;
            setVisuals(rt.getState());
            InputDialog.newInstance(R.string.input, hint, R.string.ok, 0, "", "", false)
                       .show(getFragmentManager(), TAG_INPUT_DIALOG);
        });

        rt.pause();
        return inputText;
    }

    @Override
    public void onChapterBegin(final int chapterNo, final String chapterName) {
        final Runtime rt = Runtime.getRuntime();
        Log.d(DEBUG_TAG, "Chapter begin: " + chapterNo + " " + chapterName);
        runOnUiThread(() -> {
            tapAnywhereToContinue = true;
            setVisuals(rt.getState());
            setAvatar(null);
            character.setText(chapterNo + ": " + rt.getCurrentBook().getChapterName(chapterNo - 1));
            character.setVisibility(View.VISIBLE);
            narrative.setVisibility(View.INVISIBLE);
        });
        rt.pause();
    }

    @Override
    public void onChapterEnd(int chapterNo, String chapterName) {
        Log.d(DEBUG_TAG, "Chapter ended");
        final Runtime rt = Runtime.getRuntime();
        runOnUiThread(() -> {
            tapAnywhereToContinue = true;
            setVisuals(rt.getState());
            setAvatar(null);
            character.setText(getString(R.string.end_of_chapter, rt.getCurrentBook().getChapterName(chapterNo - 1)));
            character.setVisibility(View.VISIBLE);
            narrative.setVisibility(View.INVISIBLE);
        });

        rt.pause();
    }

    @Override
    public void onBookBegin(final String bookName) {
        final Runtime rt = Runtime.getRuntime();
        Log.d(DEBUG_TAG, "Book " + bookName + " begin");
        runOnUiThread(() -> {
            tapAnywhereToContinue = true;
            setVisuals(rt.getState());
            setAvatar(files.getCover(bookName));
            character.setText(bookName);
            character.setVisibility(View.VISIBLE);
            narrative.setVisibility(View.INVISIBLE);
        });
        rt.pause();
    }

    @Override
    public void onBookEnd(String bookName) {
        Log.i(DEBUG_TAG, "Book ended");
        final Runtime rt = Runtime.getRuntime();
        runOnUiThread(() -> {
            tapAnywhereToContinue = true;
            closeOnTap = true;
            setVisuals(rt.getState());
            character.setText(getString(R.string.the_end));
            character.setVisibility(View.VISIBLE);
            narrative.setVisibility(View.INVISIBLE);
            setAvatar(null);
        });

        rt.pause();
    }

    private volatile String inputText;

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        final Runtime rt = Runtime.getRuntime();
        if (dialog.getTag().equals(TAG_INPUT_DIALOG)) {
            inputText = s;
            rt.advance();
        }
    }
}
