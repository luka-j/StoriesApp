package rs.lukaj.android.stories.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.io.BitmapUtils;
import rs.lukaj.stories.environment.FileProvider;
import rs.lukaj.stories.exceptions.ExecutionException;
import rs.lukaj.stories.runtime.State;

import static android.view.View.NO_ID;

/**
 * Basically everything from StoryActivity that could be factored out to static helper methods, in order to
 * prevent the activity from becoming enormous and enormously cluttered.
 * Shared between StoryActivity and StoryEditorActivity
 * Created by luka on 1.9.17..
 */

public class StoryUtils {

    private static final Map<String, Integer> uiElementIds = new HashMap<>();
    private static final String TAG                        = "stories.storyutils";

    static {
        uiElementIds.put("avatar", R.id.story_editor_avatar);
        uiElementIds.put("narrative", R.id.story_text);
        uiElementIds.put("character", R.id.story_character_name);
        uiElementIds.put("answers", R.id.story_answers_scroll);
        uiElementIds.put("countdown", R.id.countdown_text);
    }

    static final String VAR_BACKGROUND = "_background_";

    //positions (guidelines) are specified in %
    //margins and paddings are specified in dp
    //image sizes are specified in dp
    //text sizes are specified in sp
    //background can either be in format #000000 - #ffffff for solid color or image name for background image
    //alignment follows a custom format - see alignFromState method

    static final String VAR_LEFT_TEXT_GUIDELINE   = "_narrative.left_";
    static final String VAR_TOP_TEXT_GUIDELINE    = "_narrative.top_";
    static final String VAR_BOTTOM_TEXT_GUIDELINE = "_narrative.bottom_";
    static final String VAR_RIGHT_TEXT_GUIDELINE  = "_narrative.right_";

    static final String VAR_LEFT_ANSWER_GUIDELINE   = "_answers.left_";
    static final String VAR_TOP_ANSWER_GUIDELINE    = "_answers.top_";
    static final String VAR_BOTTOM_ANSWER_GUIDELINE = "_answers.bottom_";
    static final String VAR_RIGHT_ANSWER_GUIDELINE  = "_answers.right_";

    static final String VAR_LEFT_CHARACTER_GUIDELINE   = "_cname.left_";
    static final String VAR_RIGHT_CHARACTER_GUIDELINE  = "_cname.right_";
    static final String VAR_BOTTOM_CHARACTER_GUIDELINE = "_cname.bottom_";

    static final String VAR_LEFT_COUNTDOWN_GUIDELINE   = "_countdown.left_";
    static final String VAR_RIGHT_COUNTDOWN_GUIDELINE  = "_countdown.right_";
    static final String VAR_BOTTOM_COUNTDOWN_GUIDELINE = "_countdown.bottom_";

    static final String VAR_BOTTOM_AVATAR_GUIDELINE = "_avatar.bottom_";
    static final String VAR_RIGHT_AVATAR_GUIDELINE  = "_avatar.right_";

    static final String VAR_AVATAR_SIZE = "_avatar.size_"; //in dp
    static final String VAR_AVATAR_ALIGNMENT = "_avatar.alignment_";

    static final String VAR_TEXT_BACKGROUND         = "_narrative.background_";
    static final String VAR_TEXT_VERTICAL_PADDING   = "_narrative.padding.vertical_";
    static final String VAR_TEXT_HORIZONTAL_PADDING = "_narrative.padding.horizontal_";
    static final String VAR_TEXT_ALIGNMENT          = "_narrative.alignment_";
    static final String VAR_NARRATIVE_COLOR         = "_narrative.color_";
    static final String VAR_NARRATIVE_TEXT_SIZE     = "_narrative.size_";

    static final String VAR_ANSWER_BACKGROUND         = "_answer.background_";
    static final String VAR_ANSWER_TEXT_SIZE          = "_answer.size_";
    static final String VAR_ANSWER_MARGINS            = "_answer.margins_";
    static final String VAR_ANSWER_TEXT_COLOR         = "_answer.text.color_";
    static final String VAR_ANSWER_VERTICAL_PADDING   = "_answer.padding.vertical_";
    static final String VAR_ANSWER_HORIZONTAL_PADDING = "_answer.padding.horizontal_";
    static final String VAR_ANSWERS_ALIGNMENT         = "_answers.alignment_";

    static final String VAR_CHARACTER_BACKGROUND         = "_cname.background_";
    static final String VAR_CHARACTER_VERTICAL_PADDING   = "_cname.padding.vertical_";
    static final String VAR_CHARACTER_HORIZONTAL_PADDING = "_cname.padding.horizontal_";
    static final String VAR_CHARACTER_VERTICAL_MARGINS   = "_cname.margins.vertical_";
    static final String VAR_CHARACTER_ALIGNMENT          = "_cname.alignment_";
    static final String VAR_CHARACTER_COLOR              = "_cname.color_";
    static final String VAR_CHARACTER_TEXT_SIZE          = "_cname.size_";

    static final String VAR_COUNTDOWN_BACKGROUND       = "_countdown.background_";
    static final String VAR_COUNTDOWN_VERTICAL_MARGINS = "_countdown.margins.vertical_";
    static final String VAR_COUNTDOWN_INTERVAL         = "_countdown.interval_";
    static final String VAR_COUNTDOWN_FORMAT           = "_countdown.format_";
    static final String VAR_COUNTDOWN_COLOR            = "_countdown.color_";
    static final String VAR_COUNTDOWN_ALIGNMENT        = "_countdown.alignment_";
    static final String VAR_COUNTDOWN_SIZE             = "_countdown.size_";


    private static float  answerTextSize          = 16f;
    private static float  answerVerticalMargins   = 6;
    private static float  answerVerticalPadding   = 6;
    private static float  answerHorizontalPadding = 10;
    private static int    answerTextColor         = Color.parseColor("#ffffff");
    private static String answerBackground        = "#FF5252";

    private static Map<View, String> previousBackground = new HashMap<>();
    private static Map<View, String> previousTextColor = new HashMap<>();

    static void setBackground(Resources resources, FileProvider files, View view, String bg, String def, boolean cache) {
        if (bg == null) {
            if(def == null) return;
            bg = def;
        }
        if (bg.isEmpty()) return;
        if (bg.equals(previousBackground.get(view))) return;
        if (bg.startsWith("#")) {
            try {
                view.setBackgroundColor(Color.parseColor(bg));
            } catch (IllegalArgumentException e) {
                //throw new ExecutionException("Invalid color format", e); //just ignore it
            }
        } else {
            File bgImage = files.getImage(bg);
            if (bgImage == null || !bgImage.isFile()) return;
            Bitmap bm = BitmapUtils.loadImage(bgImage, view.getWidth());
            view.setBackgroundDrawable(new BitmapDrawable(resources, bm));
        }
        if (cache) previousBackground.put(view, bg);
    }

    static void setTextColorFromState(State variables, TextView view, String variable, String def) {
        String color;
        if (variable == null || !variables.hasVariable(variable)) {
            if(def == null) return;
            color = def;
        } else {
            color = variables.getString(variable);
        }
        if(color.isEmpty()) return;
        if (color.equals(previousTextColor.get(view))) return;
        if(!color.startsWith("#")) color = "#" + color;
        try {
            view.setTextColor(Color.parseColor(color));
        } catch (IllegalArgumentException e) {
            //throw new ExecutionException("Invalid color format", e); just ignore it
            Log.e(TAG, "Invalid color format: " + color);
        }
        previousTextColor.put(view, color);
    }

    static void setTextSizeFromState(State variables, TextView view, String variable, Double def) {
        Double size;
        if (variable == null || !variables.hasVariable(variable)) {
            if(def == null) return;
            size = def;
        } else {
            size = variables.getDouble(variable);
        }
        if(size == null || Double.isNaN(size)) return;
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.floatValue());
    }

    static void setGuideline(Guideline guideline, Double value) {
        if (value == null || value < 0 || value > 1) return;
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
        lp.guidePercent = value.floatValue();
        guideline.requestLayout();
    }

    static void setAvatarSize(Resources resources, ImageView avatar, Double size) {
        if (size == null || size <= 0) return;
        final float            scale = resources.getDisplayMetrics().density;
        ViewGroup.LayoutParams lp    = avatar.getLayoutParams();
        lp.width = (int) (size * scale);
        lp.height = (int) (size * scale);
        avatar.requestLayout();
    }

    static void setGuidelineFromState(State variables, String key, Guideline guideline) {
        if (variables.isNumeric(key)) { setGuideline(guideline, variables.getDouble(key)); }
    }

    static void setBackgroundFromState(Resources resources, FileProvider files, State variables,
                                       String key, String def, View view) {
            setBackground(resources, files, view, variables.hasVariable(key) ? variables.getString(key) : def, def, true);
    }

    static void setPaddingFromState(Resources resources, State variables, TextView text, String vertical,
                                    String horizontal) {
        double      h     = variables.getOrDefault(horizontal, -1), v = variables.getOrDefault(vertical, -1);
        final float scale = resources.getDisplayMetrics().density;
        int         hval  = h < 0 ? text.getPaddingTop() : (int) (h * scale);
        int         vval  = v < 0 ? text.getPaddingLeft() : (int) (v * scale);
        text.setPadding(hval, vval, hval, vval);
    }

    static void setVerticalMarginsFromState(Resources resources, State variables, View view, String key) {
        if (variables.isNumeric(key)) {
            ConstraintLayout.LayoutParams lp     = (ConstraintLayout.LayoutParams) view.getLayoutParams();
            float                         scale  = resources.getDisplayMetrics().density;
            int                           margin = (int) (scale * variables.getDouble(key));
            lp.topMargin = margin;
            lp.bottomMargin = margin;
            view.requestLayout();
        }
    }

    static int getOrDefaultColor(State state, String key, int def) {
        try {
            return state.isAssigned(key) ? Color.parseColor(state.getString(key)) : def;
        } catch (StringIndexOutOfBoundsException|IllegalArgumentException e) {
            return def;
        }
    }

    private static Map<View, String> previousAlignment = new HashMap<>();

    /**
     * Format, roughly: {left|right|top|bot|bottom} {left|right|top|bot|bottom|guideline|none} {[view]|guideline|none}; ...
     * Not all are valid - e.g. it's nonsense to align left to top. Bot and bottom are synonyms.
     * In case value is invalid, this method does nothing.
     * @param state state from which to read property
     * @param view view to which this should be applied
     * @param key key under which property is stored
     * @param leftGuideline left guideline for the passed view
     * @param rightGuideline right guideline for the passed view
     * @param botGuideline bot guideline for the passed view
     * @param topGuideline top guideline for the passed view
     */
    static void alignFromState(State state, View view, String key,
                               int leftGuideline, int rightGuideline, int botGuideline, int topGuideline) {
        if(state.hasVariable(key)) {
            String value = state.getString(key).toLowerCase();
            if(previousAlignment.get(view).equals(value)) return;
            previousAlignment.put(view, value);

            String[] alignments = value.split("\\s*;\\s*");
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) view.getLayoutParams();
            for(String align : alignments) {
                String[] params = align.split("\\s+", 3);
                if (params.length != 3) continue;
                int id;
                if (!uiElementIds.containsKey(params[2])) {
                    if (!params[1].equals("guideline") && !params[1].equals("none")) continue;
                    id = 0;
                } else {
                    id = uiElementIds.get(params[2]);
                }
                switch (params[0]) { //alrighty, then...
                    case "left":
                        switch (params[1]) {
                            case "left":
                                lp.leftToLeft = id;
                                break;
                            case "right":
                                lp.leftToRight = id;
                                break;
                            case "guideline":
                                lp.leftToLeft = leftGuideline;
                                break;
                            case "none":
                                lp.leftToLeft = lp.leftToRight = NO_ID;
                                break;
                            default:
                                return;
                        }
                        break;
                    case "right":
                        switch (params[1]) {
                            case "left":
                                lp.rightToLeft = id;
                                break;
                            case "right":
                                lp.rightToRight = id;
                                break;
                            case "guideline":
                                lp.rightToRight = rightGuideline;
                                break;
                            case "none":
                                lp.rightToRight = lp.rightToLeft = NO_ID;
                                break;
                            default:
                                return;
                        }
                        break;
                    case "top":
                        switch (params[1]) {
                            case "top":
                                lp.topToTop = id;
                                break;
                            case "bot":
                            case "bottom":
                                lp.topToBottom = id;
                                break;
                            case "guideline":
                                lp.topToTop = topGuideline;
                                break;
                            case "none":
                                lp.topToBottom = lp.topToTop = NO_ID;
                                break;
                            default:
                                return;
                        }
                        break;
                    case "bot":
                    case "bottom":
                        switch (params[1]) {
                            case "top":
                                lp.bottomToTop = id;
                                break;
                            case "bot":
                            case "bottom":
                                lp.bottomToBottom = id;
                                break;
                            case "guideline":
                                lp.bottomToBottom = botGuideline;
                                break;
                            case "none":
                                lp.bottomToBottom = lp.bottomToTop = NO_ID;
                                break;
                            default:
                                return;
                        }
                        break;
                    default:
                        return;
                }
            }
            view.setLayoutParams(lp);
        }
    }

    static void setAnswerPropsFromState(State variables) {
        answerTextSize = variables.getOrDefault(VAR_ANSWER_TEXT_SIZE, answerTextSize).floatValue();
        answerBackground = variables.getOrDefault(VAR_ANSWER_BACKGROUND, answerBackground);
        answerVerticalMargins = variables.getOrDefault(VAR_ANSWER_MARGINS, answerVerticalMargins).floatValue();
        answerVerticalPadding = variables.getOrDefault(VAR_ANSWER_VERTICAL_PADDING, answerVerticalPadding).floatValue();
        answerHorizontalPadding = variables.getOrDefault(VAR_ANSWER_HORIZONTAL_PADDING, answerHorizontalPadding).floatValue();
        answerTextColor = getOrDefaultColor(variables, VAR_ANSWER_TEXT_COLOR, answerTextColor);
    }

    static TextView generateAnswer(Context context, AndroidFiles files, TextView answerView,
                                   View.OnClickListener listener, String text, int tag) {
        final float scale = context.getResources().getDisplayMetrics().density;

        answerView.setText(text);
        answerView.setTag(tag);
        answerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, answerTextSize);
        answerView.setPadding((int) (answerHorizontalPadding * scale), (int) (answerVerticalPadding * scale),
                       (int) (answerHorizontalPadding * scale), (int) (answerVerticalPadding * scale));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                     ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (answerVerticalMargins * scale);
        lp.bottomMargin = (int) (answerVerticalMargins * scale);
        try {
            answerView.setTextColor(answerTextColor);
        } catch (IllegalArgumentException e) {
            throw new ExecutionException("Invalid color format", e);
        }
        answerView.setLayoutParams(lp);
        if(listener != null)
            answerView.setOnClickListener(listener);
        setBackground(context.getResources(), files, answerView, answerBackground, answerBackground, false);
        return answerView;
    }

    static boolean isValidCharacterName(int i, char ch) {
        //if(i==0) { //todo fix this, dunno why is i wrong
          //  if(Character.isLetter(ch) || ch == '_')
            //    return true;
        //} else {
            if (ch != '?' && ch != '-' && ch != '+' && ch != '*' && ch != '/' && ch != '=' && ch != '(' && ch != ')'
                    && ch != '[' && ch != ']' && ch != ':')
                return true;
        //}
        return false;
    }
}
