package rs.lukaj.android.stories.model;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.stories.environment.DisplayProvider;
import rs.lukaj.stories.exceptions.InterpretationException;
import rs.lukaj.stories.exceptions.LoadingException;
import rs.lukaj.stories.runtime.State;

/**
 * Created by luka on 6.8.17..
 */
//this is well within the limits of LGPL: https://www.gnu.org/licenses/lgpl-java.html
public class Book {
    public static final String AUTHOR_ID_ME = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";
    private static final String TAG              = "stories.model.book";

    private static final String KEY_ID           = "id";
    private static final String KEY_TITLE        = "title";
    private static final String KEY_AUTHOR_NAME  = "author";
    private static final String KEY_AUTHOR_ID    = "authorId";
    private static final String KEY_GENRES       = "genres";
    private static final String KEY_DATE         = "date";
    private static final String KEY_CHAPTERS     = "chapters";
    private static final String KEY_CHAPTER_DESC = "chaptersDesc";
    private static final String KEY_IS_FORKABLE  = "forkable";
    private static final String KEY_DESCRIPTION  = "description";

    private rs.lukaj.stories.runtime.Book book;

    private AndroidFiles files;

    private String       id;
    private String       title;
    private String       author;
    private String       description;
    private List<String> genres = new ArrayList<>();
    private List<String> chapterNames = new ArrayList<>();
    private List<String> chapterDescriptions = new ArrayList<>();
    private long         date;
    private String       authorId;
    private File         image;
    private boolean      isForkable;
    //these exist only when book is downloaded from internet
    private double  rating = -1, ranking = -1;
    private boolean loaded = false;
    private int noOfChapters = -1;
    private boolean hasCover = true;

    private void populateMetadata() {
        State bookInfo = book.getBookInfo();
        if(bookInfo != null) {
            this.id = bookInfo.getString(KEY_ID);
            title = bookInfo.getString(KEY_TITLE);
            author = bookInfo.getString(KEY_AUTHOR_NAME);
            authorId = bookInfo.getString(KEY_AUTHOR_ID);
            genres = bookInfo.getStringList(KEY_GENRES);
            description = bookInfo.getString(KEY_DESCRIPTION);
            chapterNames = bookInfo.getStringList(KEY_CHAPTERS);
            chapterDescriptions = bookInfo.getStringList(KEY_CHAPTER_DESC);
            date = bookInfo.getOrDefault(KEY_DATE, 0).longValue();
            isForkable = bookInfo.getBool(KEY_IS_FORKABLE);
        } else {
            this.id = UUID.randomUUID().toString();
        }
        if(this.id == null) {
            this.id = UUID.randomUUID().toString();
            try {
                bookInfo.setVariable(KEY_ID, this.id);
                bookInfo.saveToFile(book.getInfoFile());
            } catch (InterpretationException|IOException e) {
                //welp, not much we can do here
                Log.e(TAG, "Unexpected exception while manipulating state (saving generated UUID)", e);
            }
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

    public Book(rs.lukaj.stories.runtime.Book book) {
        this.book = book;
        this.files = (AndroidFiles)book.getFiles();
        populateMetadata();
    }

    /**
     * This doesn't create a playable book; rather, it only populates metadata.
     * @param json
     */
    public Book(JSONObject json) throws JSONException {
        id = json.getString("id");
        title = json.getString("title");
        description = json.getString("description");
        genres = Arrays.asList(json.getString("genres").split("\\s*,\\s*"));
        date = (long)(json.getDouble("createdAt")*1000);
        JSONObject author = json.getJSONObject("author");
        authorId = author.getString("id");
        this.author = author.getString("username");
        isForkable = json.getBoolean("forkable");
        noOfChapters = json.getInt("noOfChapters");
        ranking = json.getDouble("exploreRanking");
        rating = json.getDouble("averageRating");
        hasCover = json.getBoolean("hasCover");
        loaded = true;
    }

    /**
     * If this is a published & downloaded books, synonymous with {@link #getName()}.
     * If this is a not-yet-downloaded book, returns book's id ({@link #getName()} throws NPE,
     * since there is no 'real' book)
     * If this is a not-yet-published book, returns a random UUID.
     * @return
     */
    public String getId() {
        if(!loaded) populateMetadata();
        return id;
    }

    public rs.lukaj.stories.runtime.Book getUnderlyingBook() {
        return book;
    }

    public AndroidFiles getFiles() {
        return files;
    }

    public double getRating() {
        return rating;
    }

    public double getRanking() {
        return ranking;
    }

    /**
     * This is the title which should be displayed to the user.
     * It shall be favoured over {@link #getName()}. (Not sure how to define "name" - it could be the id?
     * Main concern is allowing specific characters to appear in title which are invalid in file names.)
     * @return book's (external) title, as specified by .info
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

    public String getAuthorId() {
        if(!loaded) populateMetadata();
        return authorId;
    }

    public String getDescription() {
        if(!loaded) populateMetadata();
        return description;
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
        if(noOfChapters > 0) return noOfChapters;
        if(!loaded) populateMetadata();
        return chapterNames.size();
    }

    public boolean isForkable() {
        if(!loaded) populateMetadata();
        return isForkable;
    }

    public void setDetails(String title, String description, String genres, boolean forkable)
            throws IOException, InterpretationException {
        State bookInfo = book.getBookInfo();
        bookInfo.setVariable(KEY_TITLE, title);
        bookInfo.setVariable(KEY_IS_FORKABLE, forkable);
        bookInfo.setVariable(KEY_DESCRIPTION, description);
        String[] genresList = genres.split("\\s*,\\s*");
        bookInfo.undeclareVariable(KEY_GENRES);
        for(String g : genresList)
            bookInfo.addToList(KEY_GENRES, g);
        bookInfo.saveToFile(book.getInfoFile());
    }

    public File getCover() {
        return files.getCover(getName());
    }

    public boolean hasCover() {
        if(book == null) return hasCover;
        File cover = getCover();
        return cover != null && cover.isFile();
    }
    /**
     * This is a string by which the book is internally identified. It doesn't have to be equal to the title,
     * which is how this book is represented externally. Name has some fundamental requirements title doesn't
     * impose, such as the need to be a valid directory name, and that it doesn't collide with names of other
     * books user might download (i.e. to be platform-unique).
     *
     * In current implementation, for published (and downloaded) books, this method is synonymous with {@link #getId()}.
     * For user's own books, this method returns a working title.
     * @return book's internal name
     */
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
        bookInfo.addToList(KEY_CHAPTER_DESC, "/"); // ?
        bookInfo.saveToFile(book.getInfoFile());
        populateMetadata();
    }

    public void renameChapter(int index, String name) throws InterpretationException, IOException {
        State bookInfo = book.getBookInfo();
        bookInfo.replaceInList(KEY_CHAPTERS, index-1, name);
        bookInfo.saveToFile(book.getInfoFile());
        populateMetadata();
    }

    public void setChapterDescription(int index, String description) throws InterpretationException, IOException {
        State bookInfo = book.getBookInfo();
        bookInfo.replaceInList(KEY_CHAPTER_DESC, index-1, description);
        bookInfo.saveToFile(book.getInfoFile());
        populateMetadata();
    }

    public void removeChapter(int index) throws InterpretationException, IOException {
        State bookInfo = book.getBookInfo();
        bookInfo.removeFromList(KEY_CHAPTERS, index);
        bookInfo.removeFromList(KEY_CHAPTER_DESC, index);
        bookInfo.saveToFile(book.getInfoFile());
        populateMetadata();
    }

    public void setAuthor(String id) throws IOException {
        authorId = id;
        try {
            book.getBookInfo().setVariable(KEY_AUTHOR_ID, id);
            book.getBookInfo().saveToFile(book.getInfoFile());
        } catch (InterpretationException e) {
            throw new LoadingException("Error setting author", e);
        }
    }
}
