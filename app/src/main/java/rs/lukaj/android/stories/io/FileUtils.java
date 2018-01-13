package rs.lukaj.android.stories.io;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by luka on 27.11.17..
 */

public class FileUtils {
    private static final int STD_BUFFER = 4096;
    private static final int ZIP_BUFFER = STD_BUFFER;
    private static final int COPY_BUFFER = STD_BUFFER;
    private static final int UNZIP_BUFFER = 8192;

    private static ExecutorService executor = Executors.newCachedThreadPool();

    private static void copyImpl(InputStream src, File dst) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[COPY_BUFFER];
            int len;
            while ((len = src.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public static void copy(int id, InputStream src, File dst, Callbacks callbacks) {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.submit(() -> {
            try {
                copyImpl(src, dst);
                if(callbacks != null)
                    handler.post(() -> callbacks.onFileOperationCompleted(id));
            } catch (IOException ex) {
                if(callbacks != null)
                    handler.post(() -> callbacks.onIOException(id, ex));
            }
        });
    }

    private static void copyDirectoryImpl(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdirs();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectoryImpl(new File(sourceLocation, children[i]), new File(
                        targetLocation, children[i]));
            }
        } else {
            copyImpl(new FileInputStream(sourceLocation), targetLocation);
        }
    }

    public static void copyDirectory(int id, File sourceLocation, File targetLocation, Callbacks callbacks) {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.submit(() -> {
            try {
                copyDirectoryImpl(sourceLocation, targetLocation);
                handler.post(() -> callbacks.onFileOperationCompleted(id));
            } catch (IOException ex) {
                handler.post(() -> callbacks.onIOException(id, ex));
            }
        });
    }

    private static void deleteImpl(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                deleteImpl(c);
        }
        if (!f.delete())
            throw new IOException("Failed to delete file: " + f);
    }

    public static void delete(int id, File f, Callbacks callbacks) {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.submit(() -> {
            try {
                deleteImpl(f);
                handler.post(() -> callbacks.onFileOperationCompleted(id));
            } catch (IOException ex) {
                handler.post(() -> callbacks.onIOException(id, ex));
            }
        });
    }

    //gotta love java.util.zip
    public static void unzip(int id, File zipFile, File targetDirectory, Callbacks callbacks) {
        try {
            unzip(id, new FileInputStream(zipFile), targetDirectory, callbacks);
        } catch (IOException e) {
            callbacks.onIOException(id, e);
        }
    }

    public static void unzip(int id, InputStream zipStream, File targetDirectory, Callbacks callbacks) {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.submit(() -> {
            try {
                unzipOnCurrentThread(zipStream, targetDirectory);
                handler.post(() -> callbacks.onFileOperationCompleted(id));
            } catch (IOException ex) {
                handler.post(() -> callbacks.onIOException(id, ex));
            }
        });
    }

    public static void unzipOnCurrentThread(InputStream zipStream, File targetDirectory) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipStream))) {
            ZipEntry ze;
            int      count;
            byte[]   buffer = new byte[UNZIP_BUFFER];
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

    /**
     * Zips a file at a location and places the resulting zip file at the toLocation
     * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
     */
    public static void zipDirectoryAt(int id, File sourceFile, File toLocation, Callbacks callbacks) {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.submit(() -> {
            try(FileOutputStream    dest = new FileOutputStream(toLocation);
                ZipOutputStream     out  = new ZipOutputStream(new BufferedOutputStream(dest))) {
                BufferedInputStream origin;

                if (sourceFile.isDirectory()) {
                    zipSubFolder(out, sourceFile, sourceFile.getParent().length());
                } else {
                    byte data[] = new byte[ZIP_BUFFER];
                    FileInputStream fi = new FileInputStream(sourceFile);
                    origin = new BufferedInputStream(fi, ZIP_BUFFER);
                    ZipEntry entry = new ZipEntry(sourceFile.getName());
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, ZIP_BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                }
                handler.post(() -> callbacks.onFileOperationCompleted(id));
            } catch (IOException ex) {
                handler.post(() -> callbacks.onIOException(id, ex));
            }
        });
    }

    private static void zipSubFolder(ZipOutputStream out, File folder,
                                     int basePathLength) throws IOException {
        File[] fileList = folder.listFiles();
        BufferedInputStream origin;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                if (file.getName().startsWith(".fuse_hidden")) continue;
                //because java for some reason keeps references to deleted files, resulting in fuse creating this
                //might not be applicable to Android, but can't hurt
                byte data[] = new byte[ZIP_BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, ZIP_BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, ZIP_BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    public static String loadFile(File f) throws IOException {
        try(BufferedReader in = new BufferedReader(new FileReader(f))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while((line=in.readLine()) != null)
                builder.append(line).append('\n');
            return builder.toString();
        }
    }

    /**
     * Callbacks for file operations. Methods in these callbacks are always executed on UI thread.
     */
    public interface Callbacks {
        void onFileOperationCompleted(int operationId);
        void onIOException(int operationId, IOException ex);
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor       cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                                                        null);
            if (cursor != null && cursor.moveToFirst()) {

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) { cursor.close(); }
        }
        return null;
    }


    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     * @author luka
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else {
                    Log.i("FileUtils", "storage type: " + type);
                    File sdcard = getSDCard();
                    if(sdcard == null) return null;
                    return sdcard.getPath() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri)) { return uri.getLastPathSegment(); }

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    private static final String[] sdCardPaths = {"/storage/extSdCard/", "/storage/sdcard1/", "/storage/usbsdcard1/",
                                                 "/storage/ext_sd/", "/ext-storage/", "/ext-card/", "/mnt/sdcard1/"};

    /**
     * Guesses where sdcard might be. Sometimes works, sometimes doesn't.
     * @return File representing root of the sdcard if found, null otherwise
     * @author luka
     */
    private static File getSDCard() {
        File f;
        for(String s : sdCardPaths) if((f=new File(s)).exists()) return f;
        return null;
    }
}
