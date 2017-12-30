package rs.lukaj.android.stories.io;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.network.Books;
import rs.lukaj.minnetwork.Network;
import rs.lukaj.stories.environment.DisplayProvider;
import rs.lukaj.stories.exceptions.InterpretationException;

/**
 * Created by luka on 23.8.17..
 */

public class BookIO {

    public interface Callbacks {
        void onBooksLoaded(List<Book> books);
    }

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void loadAllBooks(final AndroidFiles files, final DisplayProvider display,
                                     final Callbacks callbacks, int dirType) {
        if(files == null || display == null || callbacks == null) throw new NullPointerException();
        executor.submit(() -> {
            Set<String> titles = files.getBooks(dirType);
            List<Book> books = new ArrayList<>();
            for(String title : titles) {
                Book book = new Book(title, files, display);
                if(book.getChapterCount() > 0)
                    books.add(book);
            }
            Collections.sort(books, (o1, o2) -> {
                if(o1.getDate() < o2.getDate()) return -1;
                if(o1.getDate() > o2.getDate()) return 1;
                return 0;
            });
            callbacks.onBooksLoaded(books);
        });
    }

    public static void publishBook(int requestId, Context c, Book book, String title, String genres, File cover,
                                   ExceptionHandler handler, Network.NetworkCallbacks<String> callbacks) {
        executor.submit(() -> {
            try {
                if(cover != null && cover.isFile()) {
                    book.getFiles().setCover(book.getName(), cover);
                }
                book.setDetails(title, genres);
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
