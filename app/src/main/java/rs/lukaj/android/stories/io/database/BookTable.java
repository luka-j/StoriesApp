package rs.lukaj.android.stories.io.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.List;

import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.model.Book;

import static rs.lukaj.android.stories.io.Limits.BOOK_TITLE_MAX_LENGTH;
import static rs.lukaj.android.stories.io.Limits.GENRES_MAX_LENGTH;
import static rs.lukaj.android.stories.io.Limits.USER_NAME_MAX_LENGTH;
import static rs.lukaj.android.stories.io.database.BookTable.BookEntry.COLUMN_AUTHOR;
import static rs.lukaj.android.stories.io.database.BookTable.BookEntry.COLUMN_AUTHOR_ID;
import static rs.lukaj.android.stories.io.database.BookTable.BookEntry.COLUMN_DATE;
import static rs.lukaj.android.stories.io.database.BookTable.BookEntry.COLUMN_GENRES;
import static rs.lukaj.android.stories.io.database.BookTable.BookEntry.COLUMN_ID;
import static rs.lukaj.android.stories.io.database.BookTable.BookEntry.COLUMN_TITLE;
import static rs.lukaj.android.stories.io.database.Database.CREATE;
import static rs.lukaj.android.stories.io.database.Database.DROP;
import static rs.lukaj.android.stories.io.database.Database.INSERT;
import static rs.lukaj.android.stories.io.database.Database.PRIMARY;
import static rs.lukaj.android.stories.io.database.Database.SEP;
import static rs.lukaj.android.stories.io.database.Database.TAG;
import static rs.lukaj.android.stories.io.database.Database.TYPE_INT8;
import static rs.lukaj.android.stories.io.database.Database.TYPE_TEXT;
import static rs.lukaj.android.stories.io.database.Database.TYPE_VARCHAR;
import static rs.lukaj.android.stories.io.database.Database.VALS;
import static rs.lukaj.android.stories.io.database.Database.getContext;

/**
 * Created by luka on 7.8.17..
 */

public class BookTable {

    static final String TABLE_NAME       = "books";
    static final String SQL_CREATE_TABLE =
            CREATE + TABLE_NAME + " (" +
            COLUMN_ID + TYPE_TEXT + PRIMARY + SEP +
            COLUMN_TITLE + TYPE_VARCHAR + "(" + BOOK_TITLE_MAX_LENGTH + ")" + SEP +
            COLUMN_AUTHOR_ID + TYPE_TEXT + SEP +
            COLUMN_AUTHOR + TYPE_VARCHAR + "(" + USER_NAME_MAX_LENGTH + ")" + SEP +
            COLUMN_GENRES + TYPE_VARCHAR + "(" + GENRES_MAX_LENGTH + ")" + SEP +
            COLUMN_DATE + TYPE_INT8 +
            ")"; //todo this is overkill: need only id and title (fix?)

    static final String SQL_DELETE_TABLE = DROP + TABLE_NAME;
    /**
     * Order: id, title, author id, author name, genres, date
     */
    static final String SQL_INSERT = INSERT + TABLE_NAME + " (" +
                                     COLUMN_ID + SEP +
                                     COLUMN_TITLE + SEP +
                                     COLUMN_AUTHOR_ID + SEP +
                                     COLUMN_AUTHOR + SEP +
                                     COLUMN_GENRES + SEP +
                                     COLUMN_DATE +
                                     ")" + VALS + "(?, ?, ?, ?, ?, ?)";

    public static abstract class BookEntry implements BaseColumns {
        public static final String COLUMN_ID     = _ID;
        public static final String COLUMN_TITLE  = "title";
        public static final String COLUMN_AUTHOR_ID = "author_id";
        public static final String COLUMN_AUTHOR = "author";
        public static final String COLUMN_GENRES = "genres";
        public static final String COLUMN_DATE   = "date";
    }

    public static class BookCursor extends CursorWrapper {

        public BookCursor(Cursor cursor) {
            super(cursor);
        }

        public Book getBook() {
            if (isBeforeFirst() || isAfterLast()) {
                return null;
            }
            return new Book(getString(getColumnIndex(COLUMN_TITLE)),
                            getContext());
        }
    }

    public static class BookLoader extends SQLiteCursorLoader {

        public BookLoader(Context context) {
            super(context);
        }

        @Override
        protected Cursor loadCursor() {
            return new BookTable(getContext()).allBooks();
        }
    }

    private Database helper;
    public BookTable(Context c) {
        this.helper = Database.getInstance(c);
    }

    public void insertBook(long id, String title, long authorId, String author, List<String> genres, long date) {
        ContentValues cv = new ContentValues(6);
        cv.put(COLUMN_ID, id);
        cv.put(COLUMN_TITLE, title);
        cv.put(COLUMN_AUTHOR_ID, authorId);
        cv.put(COLUMN_AUTHOR, author);
        cv.put(COLUMN_GENRES, Utils.listToString(genres));
        cv.put(COLUMN_DATE, date);
        SQLiteDatabase db   = helper.getWritableDatabase();
        long           code = db.insert(TABLE_NAME, null, cv);
    }

    public void removeBook(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long code = db.delete(TABLE_NAME,
                              COLUMN_ID + "=" + id,
                              null);
        Log.i(TAG, "removed book, status: " + code);
    }

    public void insertBooks(Iterable<Book> books) {
        SQLiteDatabase  db   = helper.getWritableDatabase();
        SQLiteStatement stmt = db.compileStatement(SQL_INSERT);
        db.beginTransaction();

        for (Book book : books) {
            stmt.bindString(1, book.getId());       //id
            stmt.bindString(2, book.getName());  //title
            stmt.bindString(3, book.getAuthorId()); //author id
            if(book.getAuthor() != null)
                stmt.bindString(4, book.getAuthor()); //author name
            else
                stmt.bindNull(4);
            stmt.bindString(5, Utils.listToString(book.getGenres())); //genres
            stmt.bindLong(6, book.getDate());   //date
            stmt.executeInsert();
            stmt.clearBindings();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public BookCursor allBooks() {
        SQLiteDatabase db = helper.getReadableDatabase();
        return new BookCursor(db.query(TABLE_NAME, null, null, null, null, null, "date desc"));
    }

    public Book getBook(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        BookCursor cursor = new BookCursor(db.query(TABLE_NAME, null, COLUMN_ID + "=" + id, null, null, null, null));
        if(cursor.getCount() <= 0) return null;
        else {
            cursor.moveToFirst();
            return cursor.getBook();
        }
    }
}
