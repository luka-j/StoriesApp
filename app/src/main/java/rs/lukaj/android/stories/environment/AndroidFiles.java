package rs.lukaj.android.stories.environment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import rs.lukaj.android.stories.controller.Runtime;
import rs.lukaj.android.stories.io.FileUtils;
import rs.lukaj.android.stories.io.BitmapUtils;
import rs.lukaj.stories.environment.FileProvider;

/**
 * Default FileProvider for books executing in this environment (i.e. on Android, using this app).
 * Differentiates between two possible root locations: either in application data directory, or on
 * sd card (internal), under folder stories. SD card directory is by default used for open-source
 * books copied to "My books" by the user and is freely accessible through any file manager,
 * and app data directory is used for "Downloaded" books which aren't (yet) copied to user's library.
 * When obtaining the book, app data directory has precedence over sd card.
 * Some methods assume there is a book in the Runtime - make sure to only use this class when handling
 * book data during the execution.
 * Created by luka on 5.8.17.
 */

public class AndroidFiles implements FileProvider {
    /**
     * Name of the placeholder chapter. Needed because by definition all books need to consist of at
     * least one chapter, and when we create a new book we have none.
     */
    public static final String PLACEHOLDER_CHAPTER_NAME = "0 _plAceholder_.ch";

    private static final String COVER_IMAGE_FILENAME = "cover.jpg";
    private static final String COVER_IMAGE_ALT_FILENAME = "cover.png"; //allowing both jpg and png; jpg is the default

    private static final File sd = new File(Environment.getExternalStorageDirectory(), "stories/");

    public static final File    SD_BOOKS        = new File(sd, "books/");
    private static final File   sdImages        = new File(sd, "images/"); //not utilized at the moment
    private static final String TAG             = "environment.Files";
    public static final String  SOURCE_DIR_NAME = "chapters";
    public static final String  IMAGE_DIR_NAME  = "images";
    private static final String AVATAR_DIR_NAME = "avatars";
    //need to resize the user's images in order to avoid huge file sizes
    private static final int BACKGROUND_WIDTH   = 1080; //size to which background images are resized by default
    private final int AVATAR_WIDTH = 320; //size to which character's avatars are resized by default

    private final File appData, appDataBooks, appDataImages;

    /**
     * Creates new AndroidFiles instance using provided Context. Context is not held anywhere and it won't leak.
     * @param context used for obtaining internal data directory.
     */
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

    /**
     * Removes the chapter for the given book, if it exists.
     * @param bookName name of the book which chapter is being removed
     * @param chapter chapter number, 1-based
     * @return true if chapter exists and is successfully removed, false otherwise
     */
    public boolean removeSource(String bookName, int chapter) {
        File srcDir = getSourceDirectory(bookName);
        String pre = chapter + " ";
        for(File src : srcDir.listFiles())
            if(src.getName().startsWith(pre) && src.getName().endsWith(".ch"))
                return src.delete();
        return false;
    }
    /**
     * Renames the chapter for the given book, if it exists.
     * @param bookName name of the book which chapter is being renamed
     * @param chapter chapter number, 1-based
     * @param renameTo new name for the chapter
     * @return true if chapter exists and is successfully renamed, false otherwise
     */
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


    public void unpackBook(int id, File bookZip, FileUtils.Callbacks callbacks) {
        FileUtils.unzip(id, bookZip, appDataBooks, callbacks);
    }
    public void unpackBookOnCurrentThread(File bookZip) throws IOException {
        FileUtils.unzipOnCurrentThread(new FileInputStream(bookZip), appDataBooks);
    }
    public void unpackBook(int id, InputStream bookStream, FileUtils.Callbacks callbacks) {
        FileUtils.unzip(id, bookStream, appDataBooks, callbacks);
    }

    @Override
    public File getAvatar(String bookName, String path) {
        return getImage(AVATAR_DIR_NAME + File.separator + path);
    }

    public File setAvatar(String path, InputStream avatar) throws IOException {
        return setImage(AVATAR_DIR_NAME + File.separator + path, avatar, AVATAR_WIDTH);
    }
    public File setBackground(String path, InputStream img) throws IOException {
        return setImage(path, img, BACKGROUND_WIDTH);
    }

    /**
     * Saves the image to the specified path and resizes it accordingly, preserving the aspect ratio.
     * @param path path to which to save the image
     * @param image stream containing the image
     * @param length length of the larger size to which it should be resized
     * @return File to which the image was saved
     * @throws IOException in case something goes wrong with the files, i.e. can't create the destination
     * file or the parent folders.
     */
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

    /**
     * Get books from the specific directory. Does not need a running Book.
     * @param dirType {@link #APP_DATA_DIR}, {@link #SD_CARD_DIR} or {@link #APP_DATA_DIR}|{@link #SD_CARD_DIR}
     * @return books in the specified directory
     */
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

    /**
     * Creates a new book on sd card and a placeholder chapter.
     * @param bookName name of the new book. Must be a valid file name.
     * @throws IOException in case some of the files cannot be created.
     */
    public void createBook(String bookName) throws IOException {
        File book = new File(SD_BOOKS, bookName);
        if(!book.mkdirs()) throw new IOException("Cannot create book directory");
        File source = new File(book, SOURCE_DIR_NAME);
        if(!source.mkdir()) throw new IOException("Cannot create book/chapters directory");
        File initialChapter = new File(source, PLACEHOLDER_CHAPTER_NAME);
        if(!initialChapter.createNewFile()) throw new IOException("Cannot create placeholder chapter");
    }

    /**
     * Creates a new chapter inside the existing book.
     * @param bookName name of the book this chapter should belong to.
     * @param chapterNo ordinal of the new chapter, 1-based
     * @param chapterName name of the new chapter. Mustn't include characters which are forbidden inside file names.
     * @throws IOException in case book doesn't exist, or any of the files cannot be created.
     */
    public void createChapter(String bookName, int chapterNo, String chapterName) throws IOException {
        File sources = getSourceDirectory(bookName);
        if(sources == null) throw new IOException("Source dir doesn't exist");
        File chapter = new File(sources, chapterNo + " " + chapterName + ".ch");
        if(!chapter.createNewFile()) throw new IOException("Cannot create new chapter " + chapterName);

        File oldPlaceholder = new File(sources, PLACEHOLDER_CHAPTER_NAME);
        if(oldPlaceholder.exists()) oldPlaceholder.delete();
    }
}
