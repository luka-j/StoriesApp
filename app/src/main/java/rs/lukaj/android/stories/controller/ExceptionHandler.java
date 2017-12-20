package rs.lukaj.android.stories.controller;

import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.net.SocketException;

import rs.lukaj.android.stories.network.Network;
import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.model.User;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.minnetwork.NetworkExceptionHandler;
import rs.lukaj.stories.exceptions.ExecutionException;
import rs.lukaj.stories.exceptions.InterpretationException;
import rs.lukaj.stories.exceptions.LoadingException;
import rs.lukaj.stories.exceptions.PreprocessingException;

/**
 * Created by luka on 26.8.17..
 */

public interface ExceptionHandler extends NetworkExceptionHandler {
    void handleInterpretationException(InterpretationException e);
    void handleExecutionException(ExecutionException e);
    void handleLoadingException(LoadingException e);
    void handleUnknownException(RuntimeException e);
    void handleBookIOException(IOException e);
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

        private void showErrorDialog(final @StringRes int title, final @StringRes int message) {
            showErrorDialog(hostActivity.getString(title), hostActivity.getString(message));
        }

        @Override
        public void handleInterpretationException(InterpretationException e) {
            showErrorDialog(R.string.interpretation_exception_title, R.string.interpretation_exception_text);
            Log.e(TAG, "Interpretation exception. Message: " + e.getMessage(), e);
        }

        @Override
        public void handleExecutionException(ExecutionException e) {
            showErrorDialog(R.string.execution_exception_title, R.string.execution_exception_text);
            Log.e(TAG, "Execution exception. Message: " + e.getMessage(), e);
        }

        @Override
        public void handleLoadingException(LoadingException e) {
            showErrorDialog(R.string.loading_exception_title, R.string.loading_exception_text);
            Log.e(TAG, "Loading exception. Message: " + e.getMessage(), e);
        }

        @Override
        public void handleUnknownException(RuntimeException e) {
            showErrorDialog(R.string.unknown_exception_title, R.string.unknown_exception_text);
            Log.e(TAG, "Unkown exception while executing", e);
        }

        public void handleBookIOException(IOException e) {
            showErrorDialog(R.string.ioex_dialog_title ,R.string.ioex_dialog_text);
            Log.e(TAG, "I/O exception. Message: " + e.getMessage(), e);
        }

        @Override
        public void handlePreprocessingException(PreprocessingException e) {
            showErrorDialog(R.string.ppex_dialog_title, R.string.ppex_dialog_text);
            Log.e(TAG, "Preprocessing exception. Message: " + e.getMessage(), e);
        }

        private boolean hasErrors = false;

        @Override
        public void handleUserNotLoggedIn() {
            User.logOut(hostActivity);
            showErrorDialog(R.string.error_session_expired_title, R.string.error_session_expired_text);
            //hostActivity.startActivity(new Intent(hostActivity, LoginActivity.class));
            //todo show login screen
            hasErrors=true;
        }

        @Override
        public void handleInsufficientPermissions(String message) {
            showErrorDialog(R.string.error_insufficient_permissions_title, R.string.error_insufficient_permissions_text);
            hasErrors=true;
        }

        @Override
        public void handleServerError(String message) {
            showErrorDialog(R.string.error_server_error_title, R.string.error_server_error_text);
            hasErrors=true;
        }

        @Override
        public void handleNotFound(final int code) {
            showErrorDialog(String.valueOf(code) + " " + hostActivity.getString(R.string.error_not_found_title),
                            hostActivity.getString(R.string.error_not_found_text));
            hasErrors=true;
        }

        @Override
        public void handleDuplicate() {
            showErrorDialog(R.string.error_duplicate_title, R.string.error_duplicate_text);
            hasErrors=true;
        }

        @Override
        public void handleBadRequest(String message) {
            showErrorDialog(R.string.error_bad_request_title, R.string.error_bad_request_text);
            hasErrors=true;
        }

        @Override
        public void handleJsonException() {
            showErrorDialog(R.string.error_json_title, R.string.error_json_text);
            hasErrors=true;
            finishedUnsuccessfully();
        }

        @Override
        public void handleMaintenance(String until) {
            showErrorDialog(R.string.error_maintenance_title, R.string.error_maintenance_text);
            hasErrors=true;
        }

        @Override
        public void handleUnreachable() {
            showErrorDialog(R.string.error_unreachable_title, R.string.error_unreachable_text);
            hasErrors=true;
        }

        @Override
        public void finished() {
            if(!hasErrors)
                hostActivity.runOnUiThread(this::finishedSuccessfully);
            else
                hostActivity.runOnUiThread(this::finishedUnsuccessfully);
            hasErrors=false;
        }

        @Override
        public void handleUnauthorized(String errorMessage) {
            showErrorDialog(R.string.error_unauthorized_title, R.string.error_unauthorized_text);
            hasErrors = true;
        }

        @Override
        public void handleUnknownHttpCode(int responseCode, String message) {
            showErrorDialog(hostActivity.getString(R.string.error_unknown_http_code_title),
                            hostActivity.getString(R.string.error_unknown_http_code_text, responseCode + ": " + message));
            if(responseCode >= 400) hasErrors = true;
        }

        @Override
        public void handleRateLimited(String retryAfter) {
            showErrorDialog(R.string.error_too_many_requests_title, R.string.error_too_many_requests_text);
            hasErrors = true;
        }

        @Override
        public void handleBadGateway() {
            showErrorDialog(R.string.error_bad_gateway_title, R.string.error_bad_gateway_text);
            hasErrors = true;
        }

        @Override
        public void handleGatewayTimeout() {
            showErrorDialog(R.string.error_gateway_timeout_title, R.string.error_gateway_timeout_text);
            hasErrors = true;
        }

        @Override
        public void handleEntityTooLarge() {
            showErrorDialog(R.string.error_entity_too_large_title, R.string.error_entity_too_large_text);
            hasErrors = true;
        }

        public void finishedSuccessfully() {
            Network.isOnline = true;
        }

        public void finishedUnsuccessfully() {
            ;
        }

        @Override
        public void handleIOException(final IOException ex) {
            hostActivity.runOnUiThread(() -> {
                if(ex instanceof SocketException) {
                    if(Utils.checkNetworkStatus(hostActivity))
                        handleSocketException((SocketException)ex);
                    else
                        handleOffline();
                } else if(ex instanceof FileIOException) {
                    handleFileException((FileIOException)ex);
                } else {
                    handleUnknownIOException(ex);
                }
            });
            hasErrors = true;
            hostActivity.runOnUiThread(this::finishedUnsuccessfully);
        }

        public void handleFileException(FileIOException ex) {
            showErrorDialog(R.string.error_fileex_title, R.string.error_fileex_text);
            Log.e(TAG, "Unexpected FileIOException", ex);
        }

        public void handleOffline() {
            if(Network.isOnline) {
                showErrorDialog(R.string.error_changed_offline_title, R.string.error_changed_offline_text);
                Network.isOnline = false;
            }
        }

        public void handleSocketException(SocketException ex) {
            //if(Network.isOnline) { //prevents this dialog from popping up multiple times. Should it?
                showErrorDialog(R.string.error_socketex_title, R.string.error_socketex_text);
                Log.e(TAG, "Unexpected SocketException", ex);
                Network.isOnline = false;
            //}
        }

        public void handleUnknownIOException(IOException ex) {
            showErrorDialog(R.string.error_unknown_ioex_title, R.string.error_unknown_ioex_text);
            Log.e(TAG, "Unexpected unknown IOException", ex);
        }
    }
}
