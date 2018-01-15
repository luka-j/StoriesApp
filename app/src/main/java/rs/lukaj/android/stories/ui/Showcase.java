package rs.lukaj.android.stories.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

/**
 * Helpers for displaying showcases. Relies on my fork of MaterialShowcaseView library.
 * Created by luka on 8.1.17.
 */

public class Showcase {
    public static final boolean DEBUG = false;

    public static final int     DISMISS_BUTTON_COLOR  = R.color.colorAccent;
    public static final int     BACKGROUND_COLOR      = R.color.colorPrimaryDark;
    public static final int     BACKGROUND_OPACITY    = 90;
    public static final int     DELAY                 = 200;
    public static final int     DISMISS_TEXT_SIZE_SP  = 20;
    public static final int     DISMISS_STYLE         = ShowcaseConfig.DISMISS_STYLE_BUTTON;
    public static final int     NOSHAPE_TOP_MARGIN_DP = 140;
    public static final int     NOSHAPE_BOT_MARGIN_DP = 40;
    public static final int     OVAL_PADDING          = 10;
    public static final int     CIRCLE_PADDING        = 20;
    public static final boolean RENDER_OVER_NAV       = true;

    private final Activity activity;
    private final ShowcaseConfig config;

    public Showcase(Activity activity) {
        this.activity = activity;
        Resources res = activity.getResources();
        config = new ShowcaseConfig();
        config.setDismissButtonColor(res.getColor(DISMISS_BUTTON_COLOR));
        config.setContentTextColor(res.getColor(R.color.showcase_text));
        config.setMaskColor(Utils.getTransparentColor(res.getColor(BACKGROUND_COLOR), BACKGROUND_OPACITY));
        config.setDelay(DELAY);
        config.setDismissTextSize(DISMISS_TEXT_SIZE_SP);
        config.setDismissStyle(DISMISS_STYLE);
        config.setContentTopMargin(NOSHAPE_TOP_MARGIN_DP);
        config.setContentBotMargin(NOSHAPE_BOT_MARGIN_DP);
        config.setRenderOverNavigationBar(RENDER_OVER_NAV);
    }

    private MaterialShowcaseView buildShowcase(@Nullable String showcaseId, @Nullable View target, boolean isOval,
                                               @StringRes int title, @StringRes int content,
                                               @StringRes int dismissText, @StringRes int skipText,
                                               boolean dimissOnTap, boolean targetTouchable) {
        if(!DEBUG && showcaseId != null && MaterialShowcaseView.hasAlreadyFired(activity, showcaseId)) {
            return new MaterialShowcaseView(activity) {
                @Override
                public boolean show(Activity activity) {
                    return false;
                }
            };
        }

        MaterialShowcaseView.Builder builder = new MaterialShowcaseView.Builder(activity)
                .setConfig(config)
                .setContentText(content)
                .setDismissText(dismissText)
                .setDismissOnTouch(dimissOnTap)
                .setTargetTouchable(targetTouchable);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setFadeDuration(800); //this is actually circular revel animation duration - original &
            // pre-lollipop only available (and default) animation was fade
        }
        if(target == null) builder.withoutShape();
        else {
            builder.setTarget(target);
            if(isOval) {
                builder.withOvalShape();
                builder.setShapePadding(OVAL_PADDING);
            } else {
                builder.withCircleShape();
                builder.setShapePadding(CIRCLE_PADDING);
            }
        }

        if(title != 0) builder.setTitleText(title);
        if(skipText != 0) builder.setSkipText(skipText);
        if(!DEBUG && showcaseId != null) builder.singleUse(showcaseId);

        return builder.build();
    }

    public void showShowcase(@Nullable String showcaseId, @Nullable View target, boolean isOval, @StringRes int title,
                             @StringRes int content, @StringRes int dismissText, boolean dimissOnTap,
                             boolean targetTouchable) {
        buildShowcase(showcaseId, target, isOval, title, content, dismissText, 0, dimissOnTap, targetTouchable).show(activity);
    }
    public void showShowcase(@Nullable String showcaseId, @StringRes int content, boolean dismissOnTap) {
        showShowcase(showcaseId, 0, content, dismissOnTap);
    }
    public void showShowcase(@Nullable String showcaseId, @StringRes int title, @StringRes int content, boolean dismissOnTap) {
        showShowcase(showcaseId, null, false, title, content, R.string.default_showcase_dismiss_text, dismissOnTap, false);
    }
    public void showShowcase(@Nullable String showcaseId, @Nullable View target, @StringRes int content,
                             boolean dismissOnTap, boolean targetTouchable) {
        showShowcase(showcaseId, target, true, content, dismissOnTap, targetTouchable);
    }
    public void showShowcase(@Nullable String showcaseId, @Nullable View target, boolean isOval,
                             @StringRes int content, boolean dismissOnTap, boolean targetTouchable) {
        showShowcase(showcaseId, target, isOval, 0, content, R.string.default_showcase_dismiss_text, dismissOnTap, targetTouchable);
    }
    public void showShowcase(@Nullable String showcaseId, @Nullable View target, boolean isOval, @StringRes int title,
                             @StringRes int content, boolean dimissOnTap, boolean targetTouchable) {
        showShowcase(showcaseId, target, isOval, title, content, R.string.default_showcase_dismiss_text, dimissOnTap, targetTouchable);
    }

    public void showSequence(@Nullable String sequenceId, View[] targets, @StringRes @Nullable int[] titles,
                             @StringRes int[] contents, @StringRes int dismissText, @StringRes int skipText,
                             boolean lastTargetTouchable) {
        if(MaterialShowcaseView.hasAlreadyFired(activity, sequenceId)) return;
        MaterialShowcaseSequence seq = new MaterialShowcaseSequence(activity);
        if(!DEBUG && sequenceId != null) seq.singleUse(sequenceId);
        int size = targets.length;
        for(int i=0; i<size; i++) {
            MaterialShowcaseView item = buildShowcase(null, targets[i], true, titles == null ? 0 : titles[i],
                                                      contents[i], dismissText, i==size-1?0:skipText, false,
                                                      i==size-1 && lastTargetTouchable);
            seq.addSequenceItem(item);
        }
        seq.start();
    }
    public void showSequence(@Nullable String sequenceId, View[] targets, @StringRes @Nullable int[] titles,
                             @StringRes int[] contents, boolean lastTargetTouchable) {
        showSequence(sequenceId, targets, titles, contents, R.string.default_showcase_dismiss_text, R.string.default_showcase_skip_text, lastTargetTouchable);
    }
    public void showSequence(@Nullable String sequenceId, View[] targets, @StringRes int[] contents, boolean lastTargetTouchable) {
        showSequence(sequenceId, targets, null, contents, lastTargetTouchable);
    }
}