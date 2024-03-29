package rs.lukaj.android.stories.io;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.network.Books;
import rs.lukaj.minnetwork.Network;
import rs.lukaj.stories.environment.DisplayProvider;
import rs.lukaj.stories.exceptions.InterpretationException;

/**
 * Helpers for loading an publishing books.
 * Created by luka on 23.8.17.
 */

public class BookIO {

    /**
     * Max length of the longer side of the book cover
     */
    private static final int COVER_LENGTH   = 600;
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Loads all books from the specified directory on the background thread, sorted in ascending order by date
     * published. Puts them on provided {@link BookShelf}
     * @param files FileProvider used for book retrieval
     * @param display display to which to associate the book. You're probably looking for {@link rs.lukaj.android.stories.environment.NullDisplay}
     * @param callbacks where to put the loaded books
     * @param dirType either {@link AndroidFiles#SD_CARD_DIR} (for downloaded books) or
     *                       {@link AndroidFiles#APP_DATA_DIR} (for books copied to user's library)
     */
    public static void loadAllBooks(final AndroidFiles files, final DisplayProvider display,
                                     final BookShelf callbacks, int dirType) {
        if(files == null || display == null || callbacks == null) throw new NullPointerException();
        executor.submit(() -> {
            List<Book>  books  = new ArrayList<>();
            try {
                Set<String> titles = files.getBooks(dirType);
                for (String title : titles) {
                    try {
                        Book book = new Book(title, files, display);
                        if (book.getChapterCount() > 0)
                            books.add(book);
                    } catch (RuntimeException e) {
                        Log.e("BookIO", "Something bad happened", e);
                    }
                }
                Collections.sort(books, (o1, o2) -> {
                    if (o1.getDate() < o2.getDate()) return -1;
                    if (o1.getDate() > o2.getDate()) return 1;
                    return 0;
                });
            } finally {
                callbacks.replaceBooks(books);
            }
        });
    }

    /**
     * Publish the book to the server, on background thread. It resized the cover to appropriate size,
     * maintaining aspect ratio.
     * @param requestId request id which will be passed to {@link rs.lukaj.minnetwork.Network.NetworkCallbacks}
     * @param c Context used to retrieve user's data and auth token
     * @param book book to be uploaded
     * @param title title of the book (not necessarily the same as the working title)
     * @param genres genres of the book, comma separated
     * @param description description of the book
     * @param forkable whether to allow this book to be copied and edited by the others
     * @param handler ExceptionHandler to use to handle any exceptions that may occur in the process
     * @param callbacks when publishing is finished, the callbacks to notify
     */
    public static void publishBook(int requestId, Context c, Book book, String title, String genres,
                                   String description, boolean forkable, ExceptionHandler handler,
                                   Network.NetworkCallbacks<String> callbacks) {
        executor.submit(() -> {
            try {
                book.setDetails(title, description, genres, forkable);
                if(book.getCover() != null && book.getCover().isFile()) {
                    Bitmap scaledCover = BitmapUtils.resizeImage(book.getCover(), COVER_LENGTH);
                    scaledCover.compress(Bitmap.CompressFormat.JPEG, 90, new FileOutputStream(book.getCover()));
                }
                Books.uploadBook(requestId, c, book.getFiles(), book, handler, callbacks, executor);
            } catch (IOException e) {
                handler.handleIOException(e);
                handler.finished();
            } catch (InterpretationException e) {
                handler.handleInterpretationException(e);
                handler.finished();
            }
        });
    }
}
