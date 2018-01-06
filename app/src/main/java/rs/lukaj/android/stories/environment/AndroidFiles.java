package rs.lukaj.android.stories.environment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import rs.lukaj.android.stories.controller.Runtime;
import rs.lukaj.android.stories.io.FileUtils;
import rs.lukaj.android.stories.ui.BitmapUtils;
import rs.lukaj.stories.environment.FileProvider;

/**
 * Created by luka on 5.8.17..
 */

public class AndroidFiles implements FileProvider {
    public static final String PLACEHOLDER_CHAPTER_NAME = "0 _plAceholder_.ch";

    private static final String COVER_IMAGE_FILENAME = "cover.jpg";
    private static final String COVER_IMAGE_ALT_FILENAME = "cover.png";

    private static final File sd = new File(Environment.getExternalStorageDirectory(), "stories/");

    public static final File    SD_BOOKS        = new File(sd, "books/");
    private static final File   sdImages        = new File(sd, "images/");
    private static final String TAG             = "environment.Files";
    public static final String  SOURCE_DIR_NAME = "chapters";
    public static final String  IMAGE_DIR_NAME  = "images";
    private static final String AVATAR_DIR_NAME = "avatars";
    private static final int BACKGROUND_WIDTH   = 1080;

    private final File appData, appDataBooks, appDataImages;
    private int AVATAR_WIDTH = 320;

    public AndroidFiles(Context context) {
        appData = new File(context.getFilesDir(), "stories/");
        appDataBooks = new File(appData, "books/");
        appDataImages = new File(appData, IMAGE_DIR_NAME);

        if(!appDataBooks.isDirectory() && !appDataBooks.mkdirs()) Log.e(TAG, "Cannot create books dir in app data folder");
        if(!appDataImages.isDirectory() && !appDataImages.mkdirs()) Log.e(TAG, "Cannot create images dir in app data folder");
        if(!SD_BOOKS.isDirectory() && !SD_BOOKS.mkdirs()) Log.e(TAG, "Cannot create books dir on sdcard");
        if(!sdImages.isDirectory() && !sdImages.mkdirs()) Log.e(TAG, "Cannot create images dir on sdcard");
        Log.i(TAG, SD_BOOKS.getAbsolutePath());
    }

    @Override
    public File getImage(String imagePath) {
        if(imagePath == null || imagePath.isEmpty()) return null;
        File image = new File(sdImages, imagePath);
        if(image.isFile()) return image;

        Runtime rt = Runtime.getRuntime();
        File local = null;
        if(rt != null) local = rt.getCurrentBookRootDir();
        if(local != null) {
            if(imagePath.charAt(0) == File.separatorChar) imagePath = imagePath.substring(1);
            image = new File(local, IMAGE_DIR_NAME + File.separatorChar + imagePath);
            if(image.isFile()) return image;
        }

        image = new File(appDataImages, imagePath);
        if(image.isFile()) return image;

        return null;
    }

    public File getCover(String bookName) {
        File cover1 = new File(getRootDirectory(bookName), COVER_IMAGE_FILENAME);
        if(cover1.isFile()) return cover1;
        File cover2 = new File(getRootDirectory(bookName), COVER_IMAGE_ALT_FILENAME);
        if(cover2.isFile()) return cover2;
        return cover1;
    }

    public File getRootDirectory(String bookName) {
        if(SD_BOOKS != null) {
            File book = new File(SD_BOOKS, bookName);
            if (book.isDirectory()) return book;
        }

        File book = new File(appDataBooks, bookName);
        if(book.isDirectory()) return book;

        return null;
    }

    @Override
    public File getSourceDirectory(String bookName) {
        File root = getRootDirectory(bookName);
        if(root != null) {
            File src = new File(root, SOURCE_DIR_NAME);
            if(src.isDirectory()) return src;
        }
        return null;
    }

    @Override
    public File getSourceFile(String rootPath, String filePath) {
        return new File(getSourceDirectory(rootPath), filePath); //todo possible special location for shared include-files
    }

    public boolean removeSource(String bookName, int chapter) {
        File srcDir = getSourceDirectory(bookName);
        String pre = chapter + " ";
        for(File src : srcDir.listFiles())
            if(src.getName().startsWith(pre) && src.getName().endsWith(".ch"))
                return src.delete();
        return false;
    }
    public boolean renameSource(String bookName, int chapter, String renameTo) {
        File srcDir = getSourceDirectory(bookName);
        String pre = chapter + " ";
        String newName = chapter + " " + renameTo + ".ch";
        for(File src : srcDir.listFiles())
            if(src.getName().startsWith(pre) && src.getName().endsWith(".ch"))
                return src.renameTo(new File(src.getParent(), newName));
        return false;
    }

    @Override
    public boolean imageExists(String s) {
        if(s.charAt(0) == File.separatorChar) s = s.substring(1);

        File onSd = new File(sdImages, s);
        File onPrivate = new File(appDataImages, s);
        File onLocal = null;
        File currSourceDir = null;
        Runtime rt = Runtime.getRuntime();
        if(rt != null) currSourceDir = rt.getCurrentBookRootDir();
        if(currSourceDir != null) onLocal = new File(currSourceDir, IMAGE_DIR_NAME + File.separatorChar + s);

        return onSd.isFile() || onPrivate.isFile() || (onLocal != null && onLocal.isFile());
    }

    public void unpackBook(File bookZip) throws IOException {
        FileUtils.unzip(bookZip, appDataBooks);
    }
    public void unpackBook(InputStream bookStream) throws IOException {
        FileUtils.unzip(bookStream, appDataBooks);
    }

    public File getAvatar(String path) {
        return getImage(AVATAR_DIR_NAME + File.separator + path);
    }

    public File setAvatar(String path, InputStream avatar) throws IOException {
        return setImage(AVATAR_DIR_NAME + File.separator + path, avatar, AVATAR_WIDTH);
    }
    public File setBackground(String path, InputStream img) throws IOException {
        return setImage(path, img, BACKGROUND_WIDTH);
    }

    public File setImage(String path, InputStream image, int length) throws IOException {
        Runtime rt = Runtime.getRuntime();
        File file = new File(rt.getCurrentBookRootDir(), IMAGE_DIR_NAME + File.separator + path);
        if(!file.getParentFile().isDirectory())
            if(!file.getParentFile().mkdirs())
                throw new IOException("Cannot generate image directory!");

        Bitmap scaledBitmap = BitmapUtils.resizeImage(image, length);
        FileOutputStream fos = new FileOutputStream(file);
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        //in case of cartoon-ish characters, JPEG is suboptimal, but for now we're sticking with it
        return file;
    }

    public static final int APP_DATA_DIR = 1;
    public static final int SD_CARD_DIR = 1 << 2;
    public Set<String> getBooks(int dirType) {
        Set<String> books = new HashSet<>();
        if((dirType & SD_CARD_DIR) > 0 && SD_BOOKS != null)
            for(File f : SD_BOOKS.listFiles())
                if(f.isDirectory())
                    books.add(f.getName());
        if((dirType & APP_DATA_DIR) > 0)
            for(File f : appDataBooks.listFiles())
                if(f.isDirectory())
                    books.add(f.getName());
        return books;
    }

    public void createBook(String bookName) throws IOException {
        File book = new File(SD_BOOKS, bookName);
        if(!book.mkdirs()) throw new IOException("Cannot create book directory");
        File source = new File(book, SOURCE_DIR_NAME);
        if(!source.mkdir()) throw new IOException("Cannot create book/chapters directory");
        File initialChapter = new File(source, PLACEHOLDER_CHAPTER_NAME);
        if(!initialChapter.createNewFile()) throw new IOException("Cannot create placeholder chapter");
    }

    public void createChapter(String bookName, int chapterNo, String chapterName) throws IOException {
        File sources = getSourceDirectory(bookName);
        if(sources == null) throw new IOException("Source dir doesn't exist");
        File chapter = new File(sources, chapterNo + " " + chapterName + ".ch");
        if(!chapter.createNewFile()) throw new IOException("Cannot create new chapter " + chapterName);

        File oldPlaceholder = new File(sources, PLACEHOLDER_CHAPTER_NAME);
        if(oldPlaceholder.exists()) oldPlaceholder.delete();
    }
}
