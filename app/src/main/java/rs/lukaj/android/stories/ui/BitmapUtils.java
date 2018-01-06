package rs.lukaj.android.stories.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by luka on 6.1.18..
 */

public class BitmapUtils {

    private static BitmapFactory.Options setOptsFromStream(BufferedInputStream bis, int sampleTo, boolean strictlyLarger)
            throws IOException {
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

    private static BitmapFactory.Options setOptsFromFile(File imageFile, int sampleTo, boolean strictlyLarger) {
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
}
