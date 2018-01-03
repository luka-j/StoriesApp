package rs.lukaj.android.stories;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Looper;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    private static BitmapFactory.Options setOptsFromStream(BufferedInputStream bis, int sampleTo, boolean strictlyLarger) throws IOException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        bis.mark(bis.available());
        if (sampleTo > 0) {
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(bis, null, opts);
            opts.inJustDecodeBounds = false;
            int larger = opts.outHeight > opts.outWidth ? opts.outHeight : opts.outWidth;
            double ratio = (double)larger/sampleTo;
            //if(!strictlyLarger) we err on the side of efficiency, maybe loading smaller image than sampleTo if
            //2^n-1<ratio<2^n (sampleSize is rounded down towards a power of 2)
            opts.inSampleSize = (int)( strictlyLarger ? Math.floor(ratio) : Math.ceil(ratio) );
            bis.reset();
        }
        return opts;
    }

    private static BitmapFactory.Options setOptsFromFile(File imageFile, int sampleTo,  boolean strictlyLarger) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        if (sampleTo > 0) {
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
            opts.inJustDecodeBounds = false;
            int larger = opts.outHeight > opts.outWidth ? opts.outHeight : opts.outWidth;
            double ratio = (double)larger/sampleTo;
            opts.inSampleSize = (int)( strictlyLarger ? Math.floor(ratio) : Math.ceil(ratio) );
        }
        return opts;
    }

    private static Bitmap scaleBitmap(Bitmap orig, BitmapFactory.Options opts, int scaleTo) {
        int    larger = opts.outHeight > opts.outWidth ? opts.outHeight : opts.outWidth;
        double scale  = (double)scaleTo / larger;
        return Bitmap.createScaledBitmap(orig, (int) (scale * opts.outWidth), (int) (scale * opts.outHeight), true);
    }

    public static Bitmap loadImage(InputStream imageStream, int scaleTo) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(imageStream);
        BitmapFactory.Options opts = setOptsFromStream(bis, scaleTo, true);
        return BitmapFactory.decodeStream(bis, null, opts);
    }

    public static Bitmap loadImage(File imageFile, int sampleTo) {
        BitmapFactory.Options opts = setOptsFromFile(imageFile, sampleTo, true);
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
    }

    public static Bitmap resizeImage(InputStream image, int resizeTo) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(image);
        BitmapFactory.Options opts = setOptsFromStream(bis, resizeTo, true);
        //we don't want to actually load whole bitmap if it's way too large, so we're sampling appropriately and then resizing
        //(sampling should always generate larger image than needed)
        Bitmap orig = BitmapFactory.decodeStream(bis, null, opts);
        return scaleBitmap(orig, opts, resizeTo);
    }

    public static Bitmap resizeImage(File image, int resizeTo) throws IOException {
        BitmapFactory.Options opts = setOptsFromFile(image, resizeTo, true);
        Bitmap orig = BitmapFactory.decodeFile(image.getAbsolutePath(), opts);
        return scaleBitmap(orig, opts, resizeTo);
    }

    public static boolean isOnUiThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
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
}
