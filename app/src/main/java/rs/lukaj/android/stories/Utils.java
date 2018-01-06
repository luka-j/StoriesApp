package rs.lukaj.android.stories;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Looper;
import android.view.View;

import java.util.List;

/**
 * Created by luka on 7.8.17.
 */

public class Utils {
    public static String listToString(List<?> list) {
        if(list == null || list.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(list.size()*8);
        for(Object e : list)
            sb.append(e).append(", ");
        sb.deleteCharAt(sb.length()-2);
        return sb.toString();
    }

    public static boolean isOnUiThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    public static int getTransparentColor(int color, int transparency) {
        int transparencyMask = 255 * (transparency)/100;
        int fullyTransparent = color & 0x00ffffff;
        return fullyTransparent | (transparencyMask << 24);
    }

    public static boolean contains(CharSequence sequence, char ch) {
        for(int i=sequence.length()-1; i>=0; i--)
            if(sequence.charAt(i) == ch)
                return true;
        return false;
    }

    public static boolean equals(CharSequence sequence, String string) {
        if(sequence == null && string == null) return true;
        if(sequence == null || string == null) return false;
        int len = sequence.length();
        if(string.length() != len) return false;
        else for(int i=0; i<len; i++)
            if(sequence.charAt(i) != string.charAt(i))
                return false;
        return true;
    }

    public static void click(View v) {
        if(Build.VERSION.SDK_INT >= 15) //people still use 4.0 ?
            v.callOnClick();
        else
            v.performClick();
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static int occurencesOf(String haystack, char needle) {
        int c = 0;
        for(int i=0; i<haystack.length(); i++)
            if(haystack.charAt(i) == needle)
                c++;
        return c;
    }

    public static void forceShow(View v) {
        v.setVisibility(View.VISIBLE);
        v.bringToFront();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            v.getParent().requestLayout();
            if(v.getParent() instanceof View)
                ((View)v.getParent()).invalidate();
        }
    }
}
