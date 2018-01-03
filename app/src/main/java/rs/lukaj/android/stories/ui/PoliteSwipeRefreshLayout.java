package rs.lukaj.android.stories.ui;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Politely asks can he touch your stuff <s>before kidnapping them and raping you in the backseat of your car</s>
 *
 * @author AnubianNoob
 * @author luka
 * @since 3.9.2015.
 */
public class PoliteSwipeRefreshLayout extends SwipeRefreshLayout {

    private OnChildScrollUpListener scrollNeededListener;

    public PoliteSwipeRefreshLayout(Context context) {
        super(context);
    }

    public PoliteSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnChildScrollUpListener(OnChildScrollUpListener listener) {
        scrollNeededListener = listener;
    }

    @Override
    public boolean canChildScrollUp() {
        if (scrollNeededListener == null) {
            Log.w("PoliteSwipe", "Listener not defined!");
            return false;
        } else {
            return scrollNeededListener.canChildScrollUp();
        }
    }

    public interface OnChildScrollUpListener {
        boolean canChildScrollUp();
    }
}
