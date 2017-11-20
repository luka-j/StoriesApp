package rs.lukaj.android.stories;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by luka on 7.8.17..
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

    //gotta love java.util.zip
    public static void unzip(File zipFile, File targetDirectory) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry ze;
            int      count;
            byte[]   buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir  = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                                                    dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                try (FileOutputStream fout = new FileOutputStream(file)) {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                }
            /*long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);*/
            }
        }
    }

    public static Bitmap loadImage(File imageFile, int scaleTo) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        if (scaleTo > 0) {
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
            opts.inJustDecodeBounds = false;
            int larger = opts.outHeight > opts.outWidth ? opts.outHeight : opts.outWidth;
            opts.inSampleSize = larger / scaleTo;
        }
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
    }

    public static Bitmap loadImage(InputStream imageStream, int scaleTo) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(imageStream);
        bis.mark(bis.available());
        BitmapFactory.Options opts = new BitmapFactory.Options();
        if (scaleTo > 0) {
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(bis, null, opts);
            opts.inJustDecodeBounds = false;
            int larger = opts.outHeight > opts.outWidth ? opts.outHeight : opts.outWidth;
            opts.inSampleSize = larger / scaleTo;
            bis.reset();
        }
        return BitmapFactory.decodeStream(imageStream, null, opts);
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
}
