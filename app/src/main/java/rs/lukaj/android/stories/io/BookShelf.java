package rs.lukaj.android.stories.io;

import java.util.List;

import rs.lukaj.android.stories.model.Book;

/**
 * Created by luka on 1.1.18..
 */

public interface BookShelf {
    void replaceBooks(List<Book> books);
    void addBooks(List<Book> books);
}
