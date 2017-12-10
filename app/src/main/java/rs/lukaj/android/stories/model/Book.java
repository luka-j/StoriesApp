package rs.lukaj.android.stories.model;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.stories.environment.DisplayProvider;
import rs.lukaj.stories.exceptions.InterpretationException;
import rs.lukaj.stories.exceptions.LoadingException;
import rs.lukaj.stories.runtime.State;

/**
 * Created by luka on 6.8.17..
 */
//this is well within the limits of LGPL: https://www.gnu.org/licenses/lgpl-java.html
public class Book {
    public static final int AUTHOR_ID_ME = 0;

    private static final String KEY_ID           = "id";
    private static final String KEY_TITLE        = "title";
    private static final String KEY_AUTHOR_NAME  = "author";
    private static final String KEY_AUTHOR_ID    = "authorId";
    private static final String KEY_GENRES       = "genres";
    private static final String KEY_DATE         = "date";
    private static final String KEY_CHAPTERS     = "chapters";
    private static final String KEY_CHAPTER_DESC = "chaptersDesc";
    private static final String KEY_IS_FORKABLE  = "forkable";

    private rs.lukaj.stories.runtime.Book book;

    private AndroidFiles files;

    private long         id;
    private String       title;
    private String       author;
    private List<String> genres = new ArrayList<>();
    private List<String> chapterNames = new ArrayList<>();
    private List<String> chapterDescriptions = new ArrayList<>();
    private long         date;
    private long         authorId;
    private File         image;
    private boolean      isForkable;
    private boolean loaded = false;

    private void populateMetadata() {
        State bookInfo = book.getBookInfo();
        if(bookInfo != null) {
            this.id = bookInfo.getOrDefault(KEY_ID, -1).longValue();
            title = bookInfo.getString(KEY_TITLE);
            author = bookInfo.getString(KEY_AUTHOR_NAME);
            authorId = bookInfo.getOrDefault(KEY_AUTHOR_ID, -1).longValue();
            genres = bookInfo.getStringList(KEY_GENRES);
            chapterNames = bookInfo.getStringList(KEY_CHAPTERS);
            chapterDescriptions = bookInfo.getStringList(KEY_CHAPTER_DESC);
            date = bookInfo.getOrDefault(KEY_DATE, 0).longValue();
            isForkable = bookInfo.getBool(KEY_IS_FORKABLE);
        } else {
            this.id = (long) (Math.random() * Long.MIN_VALUE);
        }
        image = files.getCover(book.getName());
        loaded = true;
    }

    public Book(String name, AndroidFiles files, DisplayProvider display) {
        book = new rs.lukaj.stories.runtime.Book(name, files, display);
        this.files = files;
    }

    public Book(String name, Context context) {
        this(name, new AndroidFiles(context), new NullDisplay());
    }

    public Book(rs.lukaj.stories.runtime.Book book, AndroidFiles files) {
        this.book = book;
        this.files = files;
        populateMetadata();
    }

    public long getId() {
        if(!loaded) populateMetadata();
        return id;
    }

    public rs.lukaj.stories.runtime.Book getUnderlyingBook() {
        return book;
    }

    /**
     * This shall be favoured over {@link #getName()}. Not sure how to define "name" - it could be the id?
     * Main concern is allowing specific characters to appear in title which are invalid in file names.
     * @return
     */
    public String getTitle() {
        if(!loaded) populateMetadata();
        if(title == null || title.isEmpty()) return getName();
        return title;
    }

    public String getAuthor() {
        if(!loaded) populateMetadata();
        return author;
    }

    public List<String> getGenres() {
        if(!loaded) populateMetadata();
        return genres;
    }

    public long getDate() {
        if(!loaded) populateMetadata();
        return date;
    }

    public long getAuthorId() {
        if(!loaded) populateMetadata();
        return authorId;
    }

    public File getImage() {
        if(!loaded) populateMetadata();
        return image;
    }

    public String getChapterName(int number) {
        if(!loaded) populateMetadata();
        if(number >= chapterNames.size()) return book.getChapterNames().get(number);
        return chapterNames.get(number);
    }

    public String getChapterDescription(int number) {
        if(!loaded) populateMetadata();
        return number < chapterDescriptions.size() ? chapterDescriptions.get(number) : "";
    }

    public int getChapterCount() {
        if(!loaded) populateMetadata();
        return chapterNames.size();
    }

    public boolean isForkable() {
        if(!loaded) populateMetadata();
        return isForkable;
    }

    public String getName() {
        return book.getName();
    }

    public State getState() {
        return book.getState();
    }

    public File getStateFile() {
        return book.getStateFile();
    }

    public void addChapter(String name) throws InterpretationException, IOException {
        State bookInfo = book.getBookInfo();
        bookInfo.addToList(KEY_CHAPTERS, name);
        bookInfo.addToList(KEY_CHAPTER_DESC, ""); // ?
        bookInfo.saveToFile(new File(files.getRootDirectory(getName()), ".info"));
        populateMetadata();
    }

    public void setAuthor(long id) throws IOException {
        authorId = id;
        try {
            book.getBookInfo().setVariable(KEY_AUTHOR_ID, id);
            book.getBookInfo().saveToFile(book.getInfoFile());
        } catch (InterpretationException e) {
            throw new LoadingException("Error setting author", e);
        }
    }
}
