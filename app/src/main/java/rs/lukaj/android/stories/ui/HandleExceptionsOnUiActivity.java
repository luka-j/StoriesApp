package rs.lukaj.android.stories.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.stories.exceptions.ExecutionException;

/**
 * Extension to AppCompatActivity which handles runtime exceptions in runOnUiThread using DefaultHandler
 * constructed with an instance of this class as a host. Because runOnUiThread is a final method, introduces
 * its on method, {@link #onUiThread(Runnable)} to do the job.
 *
 * Created by luka on 14.1.18.
 */
public abstract class HandleExceptionsOnUiActivity extends AppCompatActivity {
    protected ExceptionHandler.DefaultHandler exceptionHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        exceptionHandler = new ExceptionHandler.DefaultHandler(this);
    }

    public void onUiThread(Runnable run) { //it's idiotic runOnUiThread is final in AppCompatActivity
        runOnUiThread(() -> {
            try {
                run.run(); //uncaught exception handlers don't really work on UI thread
            } catch (ExecutionException e) {
                exceptionHandler.handleExecutionException(e);
            } catch (RuntimeException e) {
                exceptionHandler.handleUnknownException(e);
            }
        });
    }
}
