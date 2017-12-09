package rs.lukaj.android.stories.controller;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.stories.exceptions.ExecutionException;
import rs.lukaj.stories.exceptions.InterpretationException;
import rs.lukaj.stories.exceptions.LoadingException;
import rs.lukaj.stories.exceptions.PreprocessingException;

/**
 * Created by luka on 26.8.17..
 */

public interface ExceptionHandler {
    void handleInterpretationException(InterpretationException e);
    void handleExecutionException(ExecutionException e);
    void handleLoadingException(LoadingException e);
    void handleUnknownException(RuntimeException e);
    void handleIOException(IOException e);
    void handlePreprocessingException(PreprocessingException e);

    class DefaultHandler implements ExceptionHandler {
        public static final String TAG_DIALOG = "stories.dialog.error";
        public static final String TAG = "stories.exception";

        private AppCompatActivity hostActivity;

        public DefaultHandler(AppCompatActivity host) {
            this.hostActivity = host;
        }

        private void showErrorDialog(final String title, final String message) {
                hostActivity.runOnUiThread(() -> {
                    try {
                        InfoDialog dialog = InfoDialog.newInstance(title, message);
                        if (hostActivity instanceof InfoDialog.Callbacks)
                            dialog.registerCallbacks((InfoDialog.Callbacks) hostActivity);
                        dialog.show(hostActivity.getFragmentManager(), TAG_DIALOG);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Illegal state while displaying exception dialog! Is host Activity destroyed?");
                        //in some awkward cases, library's Runtime continues running after Thread is interrupted
                        //and ignores executor shutdown, so the StoryActivity is unable to retrieve the app Runtime
                        //which doesn't exist anymore. It will then attempt to display an error message, which
                        //will fail because the activity is already destroyed, throwing IllegalStateException
                        //todo fix threading mess
                        //on a side note, BgBus has a slew of race conditions and threading issues, and yet it
                        //fared well; this is comparatively minor, as there should be no visible consequences
                    }
                });
        }

        @Override
        public void handleInterpretationException(InterpretationException e) {
            showErrorDialog(hostActivity.getString(R.string.interpretation_exception_title),
                            hostActivity.getString(R.string.interpretation_exception_text));
            Log.e(TAG, "Interpretation exception. Message: " + e.getMessage(), e);
        }

        @Override
        public void handleExecutionException(ExecutionException e) {
            showErrorDialog(hostActivity.getString(R.string.execution_exception_title),
                            hostActivity.getString(R.string.execution_exception_text));
            Log.e(TAG, "Execution exception. Message: " + e.getMessage(), e);
        }

        @Override
        public void handleLoadingException(LoadingException e) {
            showErrorDialog(hostActivity.getString(R.string.loading_exception_title),
                            hostActivity.getString(R.string.loading_exception_text));
            Log.e(TAG, "Loading exception. Message: " + e.getMessage(), e);
        }

        @Override
        public void handleUnknownException(RuntimeException e) {
            showErrorDialog(hostActivity.getString(R.string.unknown_exception_title),
                            hostActivity.getString(R.string.unknown_exception_text));
            Log.e(TAG, "Unkown exception while executing", e);
        }

        public void handleIOException(IOException e) {
            showErrorDialog(hostActivity.getString(R.string.ioex_dialog_title),
                            hostActivity.getString(R.string.ioex_dialog_text));
            Log.e(TAG, "I/O exception. Message: " + e.getMessage(), e);
        }

        @Override
        public void handlePreprocessingException(PreprocessingException e) {
            showErrorDialog(hostActivity.getString(R.string.ppex_dialog_title),
                            hostActivity.getString(R.string.ppex_dialog_text));
            Log.e(TAG, "Preprocessing exception. Message: " + e.getMessage(), e);
        }
    }
}
