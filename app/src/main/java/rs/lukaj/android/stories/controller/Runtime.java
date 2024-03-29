package rs.lukaj.android.stories.controller;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.ui.StoryActivity;
import rs.lukaj.stories.environment.DisplayProvider;
import rs.lukaj.stories.exceptions.ExecutionException;
import rs.lukaj.stories.exceptions.InterpretationException;
import rs.lukaj.stories.exceptions.LoadingException;
import rs.lukaj.stories.exceptions.PreprocessingException;
import rs.lukaj.stories.runtime.State;

/**
 * Runtime which can load and execute the book. Book is by default executed on background thread and
 * methods from this class need to be used to pause (block) the thread when needed. Keep in mind
 * DisplayProvider methods need to be blocking, so that's probably the right spot for pausing the runtime thread.
 * Created by luka on 26.8.17..
 */
//this is a pretty awful class tbh
public class Runtime {
    private static final String VAL_BOOK_NAME = "_BOOK_NAME_";

    private static Runtime instance;

    /**
     * Used to get instance of currently executing book. To create a runtime, use
     * {@link #loadBook(String, AndroidFiles, DisplayProvider, ExceptionHandler) loadBook}
     * @return
     * @throws ExecutionException if no book is executing at the moment
     */
    public static Runtime getRuntime() {
        if(instance != null) return instance;
        else throw new ExecutionException("Attempting to get runtime of non-existant book!");
    }

    private Runtime(final String title, final AndroidFiles files, final ExceptionHandler handler) {
        this.files = files;
        this.handler = handler;
        this.bookTitle = title;
    }

    private static final boolean DEBUG = false;

    private static ExecutorService executor     = Executors.newSingleThreadExecutor();

    private String bookTitle;
    private AndroidFiles files;
    private ExceptionHandler handler;

    private Book         currentBook;
    private Future       task;

    private final Object lock = new Object();
    private rs.lukaj.stories.runtime.Runtime runtime;

    /**
     * Loads the book into runtime and sets the appropriate constants. This does _not_ execute
     * the book.
     * @param name name of the book
     * @param files FileProvider which should be associated with the book
     * @param display DisplayProvider which should be associated with the book
     * @param handler ExceptionHandler which should be used to handle any possible errors during book loading.
     * @see #execute()
     */
    public static Runtime loadBook(final String name, final AndroidFiles files,
                                   final DisplayProvider display, final ExceptionHandler handler) {
        instance = new Runtime(name, files, handler);
        try {
            instance.runtime = new rs.lukaj.stories.runtime.Runtime(files, display);
            instance.currentBook = new Book(instance.runtime.loadBook(instance.bookTitle));
            State state = instance.getState();
            if(state != null && !state.hasVariable(VAL_BOOK_NAME))
                state.setConstant(VAL_BOOK_NAME, instance.currentBook.getName());
        } catch (LoadingException e) {
            handler.handleLoadingException(e);
        } catch (ExecutionException e) {
            handler.handleExecutionException(e);
        } catch (InterpretationException e) {
            handler.handleInterpretationException(e);
        }
        return instance;
    }

    /**
     * Executes the book associated with this runtime in tight loop in background thread, i.e. without stopping
     * until it's done. This method should be used in conjunction with
     * {@link Runtime#pause()}/{@link Runtime#pauseFor(long)} and {@link Runtime#advance()} to pause the background
     * thread accordingly.
     */
    public void execute() {
        instance.task = executor.submit(() -> {
            try {
                instance.runtime.executeInTightLoop(DEBUG, false);
            } catch (InterpretationException e) {
                handler.handleInterpretationException(e);
            } catch (ExecutionException e) {
                handler.handleExecutionException(e);
            } catch (LoadingException e) {
                handler.handleLoadingException(e);
            } catch (PreprocessingException e) {
                handler.handlePreprocessingException(e);
            } catch (RuntimeException e) {
                handler.handleUnknownException(e);
            }
        });
    }

    /**
     *
     */
    public void executeEnding() {
        instance.task = executor.submit(() -> {
            try {
                currentBook.getState().setVariable("__chapter__", currentBook.getChapterCount());
                currentBook.getState().setVariable("__line__", Integer.MAX_VALUE);
                runtime.executeInTightLoop(false, false);
            } catch (InterpretationException e) {
                handler.handleInterpretationException(e);
            } catch (ExecutionException e) {
                handler.handleExecutionException(e);
            } catch (LoadingException e) {
                handler.handleLoadingException(e);
            } catch (PreprocessingException e) {
                handler.handlePreprocessingException(e);
            } catch (RuntimeException e) {
                handler.handleUnknownException(e);
            }
        });
    }

    public static void loadBook(String title, AppCompatActivity activity, DisplayProvider display) {
        loadBook(title, new AndroidFiles(activity), display, new ExceptionHandler.DefaultHandler(activity));
    }

    public static void loadBook(String title, StoryActivity activity) {
        loadBook(title, new AndroidFiles(activity), activity, new ExceptionHandler.DefaultHandler(activity));
    }

    public void exit() {
        task.cancel(true);
        executor.shutdownNow(); //cancelling future doesn't seem to do the job
        executor = Executors.newSingleThreadExecutor(); //we've just thrown out the executor; need a new one
        instance = null;
    }

    /**
     * Pauses the current thread using the instance-wide lock. Saves the book progress.
     * This needs to be called off the UI thread !!
     * i.e. use from DisplayProvider methods
     */
    public void pause() {
        if(Utils.isOnUiThread()) throw new IllegalThreadStateException("Attempt to pause ui thread!");
        synchronized (lock) {
            try {
                runtime.save(); //instead of wasting time, doing a state save here
                lock.wait();
            } catch (InterruptedException e) {
            } catch (IOException e) {
                handler.handleBookIOException(e);
            }
        }
    }

    /**
     * Pauses the current thread for a specific amount of time using the instance-wide lock.
     * This needs to be called off the UI thread !!
     * i.e. use from DisplayProvider methods
     * @param millis time which the thread needs to be paused for, in milliseconds
     */
    public void pauseFor(long millis) {
        if(Utils.isOnUiThread()) throw new IllegalThreadStateException("Attempt to pause ui thread!");
        synchronized (lock) {
            try {
                runtime.save();
                lock.wait(millis);
            } catch (InterruptedException e) {
            } catch (IOException e) {
                handler.handleBookIOException(e);
            }
        }
    }

    /**
     * Advances the background thread, notifying the lock it should continue.
     * This must be called off the runtime thread !
     * i.e. use from UI thread
     */
    public void advance() {
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * State of the currently executing book. If no book is executing at the moment, returns null.
     */
    @Nullable
    public State getState() {
        if(currentBook == null) return null;
        return currentBook.getState();
    }

    @Nullable
    public File getCurrentBookRootDir() {
        if(files == null) return null;
        File dir = files.getRootDirectory(currentBook.getName());
        if(dir == null || !dir.isDirectory()) return null;
        return dir;
    }

    public Book getCurrentBook() {
        return currentBook;
    }
}
