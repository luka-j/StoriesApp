package rs.lukaj.android.stories.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.io.BitmapUtils;
import rs.lukaj.android.stories.io.FileUtils;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.model.User;
import rs.lukaj.android.stories.network.Users;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;
import rs.lukaj.minnetwork.Network;

import static rs.lukaj.minnetwork.Network.Response.RESPONSE_OK;
import static rs.lukaj.minnetwork.Network.Response.RESPONSE_UNAUTHORIZED;

/**
 * Displays info about currently logged in user and provides an option to change password.
 * Created by luka on 3.1.18.
 */

public class UserInfoActivity extends AppCompatActivity implements InputDialog.Callbacks,
                                                                   Network.NetworkCallbacks<String>,
                                                                   FileUtils.Callbacks {
    private static final String TAG_DIALOG_CURRENT_PASSWORD  = "userinfo.dialog.currentpw";
    private static final String TAG_DIALOG_CHANGE_PASSWORD      = "userinfo.dialog.changepw";
    private static final String TAG_DIALOG_WRONG_PASSWORD       = "userinfo.infodialog.wrongpw";
    private static final String TAG_DIALOG_SUCCESSFULLY_CHANGED = "userinfo.infodialog.changedpw";
    private static final String STATE_IMAGE_FILE_PATH  = "userinfo.state.avatar";
    private static final int    REQUEST_REFRESH        = 0;
    private static final int    REQUEST_CHECK_PASSWORD = 1;
    private static final int REQUEST_CHANGE_PASSWORD   = 2;
    private static final int INTENT_SELECT_IMAGE       = 3;
    private static final int REQUEST_SET_AVATAR        = 4;
    private static final String TAG                    = "stories.userinfo";
    private static final int REQUEST_COPY_AVATAR       = 5;
    private Toolbar toolbar;
    private ImageView               avatar;
    private TextView                username;
    private TextView                email;
    private CardView                buttonChangePassword;
    private ExceptionHandler handler, handlerIgnore401;
    private File imageFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!User.isLoggedIn(this)) { onBackPressed(); return; }
        //Camera intent can't save to internal data dir
        imageFile = new File(AndroidFiles.SD_BOOKS.getParent(), User.AVATAR_FILENAME);

        setContentView(R.layout.activity_user_info);
        handler = new ExceptionHandler.DefaultHandler(this);
        handlerIgnore401 = new ExceptionHandler.DefaultHandler(this) {
            @Override
            public void handleUnauthorized(String errorMessage) {
            }
        };

        toolbar = findViewById(R.id.toolbar);
        avatar = findViewById(R.id.user_info_image);
        username = findViewById(R.id.user_info_username);
        email = findViewById(R.id.user_info_email);
        buttonChangePassword = findViewById(R.id.button_change_password);
        buttonChangePassword.setOnClickListener(v -> InputDialog.newInstance(R.string.dialog_checkpw_title, "",
                                                                             R.string.ok, R.string.cancel, "",
                                                                             "", Limits.USER_PASSWORD_MAX_LENGTH, false)
                                                                .show(getFragmentManager(), TAG_DIALOG_CURRENT_PASSWORD));
        if(!Utils.isOnline(this)) buttonChangePassword.setVisibility(View.GONE);

        avatar.setOnClickListener(v -> {
            Intent camera  = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent gallery = new Intent(Intent.ACTION_PICK);
            gallery.setType("image/*");
            camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
            Intent chooserIntent = Intent.createChooser(camera,
                                                        getString(R.string.choose_avatar));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{gallery});
            startActivityForResult(chooserIntent, INTENT_SELECT_IMAGE);
        });

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupViews();
        refreshData();
    }

    private void setupViews() {
        User me = User.getLoggedInUser(this);
        if (me.hasImage())
            Users.downloadMyAvatar(this, getResources().getDimensionPixelSize(R.dimen.avatar_size), avatar, handler);
        else
            avatar.setImageDrawable(getResources().getDrawable(R.drawable.default_user));
        username.setText(me.getUsername());
        email.setText(User.getMyEmail(this));
    }

    private void refreshData() {
        if(Utils.isOnline(this)) {
            Users.getMyDetails(REQUEST_REFRESH, this, handler, this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String currentPass;
    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        switch (dialog.getTag()) {
            case TAG_DIALOG_CURRENT_PASSWORD:
                currentPass = s;
                Users.checkPassword(REQUEST_CHECK_PASSWORD, this, s, handlerIgnore401, this);
                break;
            case TAG_DIALOG_CHANGE_PASSWORD:
                Users.changePassword(REQUEST_CHANGE_PASSWORD, this, currentPass, s, handler, this);
                break;
        }
    }
    @Override
    public void onRequestCompleted(int id, Network.Response<String> response) {
        switch (id) {
            case REQUEST_REFRESH:
                if (response.responseCode == RESPONSE_OK) {
                    try {
                        JSONObject json = new JSONObject(response.responseData);
                        User.getLoggedInUser(UserInfoActivity.this)
                            .setDetails(UserInfoActivity.this, json.getString("id"),
                                        json.getString("username"), json.getString("email"),
                                        json.getBoolean("hasImage"));
                        UserInfoActivity.this.runOnUiThread(UserInfoActivity.this::setupViews);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        handler.handleJsonException();
                    }
                }
                break;

            case REQUEST_CHECK_PASSWORD:
                runOnUiThread(() -> {
                    if (response.responseCode == RESPONSE_OK) {
                        InputDialog.newInstance(R.string.dialog_changepw_title, "", R.string.ok, R.string.cancel,
                                                "", "", Limits.USER_PASSWORD_MAX_LENGTH, false)
                                   .show(getFragmentManager(), TAG_DIALOG_CHANGE_PASSWORD);
                    } else if (response.responseCode == RESPONSE_UNAUTHORIZED) {
                        InfoDialog.newInstance(getString(R.string.dialog_wrong_password_title),
                                               getString(R.string.dialog_wrong_password_text))
                                  .show(getFragmentManager(), TAG_DIALOG_WRONG_PASSWORD);
                    }
                });
                break;

            case REQUEST_CHANGE_PASSWORD:
                if(response.responseCode == RESPONSE_OK) {
                    runOnUiThread(() -> InfoDialog.newInstance(getString(R.string.success), getString(R.string.dialog_password_changed_text))
                                                  .show(getFragmentManager(), TAG_DIALOG_SUCCESSFULLY_CHANGED));
                }
                break;

            case REQUEST_SET_AVATAR:
                if(response.responseCode == RESPONSE_OK) {
                    //todo this doesn't seem to work. If enough time, fix it (avatars are only an afterthought)
                    runOnUiThread(() -> Toast.makeText(this, R.string.toast_avatar_set, Toast.LENGTH_SHORT).show());
                }
        }
    }

    @Override
    public void onExceptionThrown(int i, Throwable throwable) {
        if(throwable instanceof IOException) handler.handleIOException((IOException) throwable);
        else if(throwable instanceof Exception) handler.handleUnknownNetworkException((Exception) throwable);
        else if(throwable instanceof Error) throw (Error)throwable;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_IMAGE_FILE_PATH, imageFile.getAbsolutePath());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getString(STATE_IMAGE_FILE_PATH) != null) {
            imageFile = new File(savedInstanceState.getString(STATE_IMAGE_FILE_PATH));
            avatar.setImageBitmap(BitmapUtils.loadImage(imageFile,
                                                        getResources().getDimensionPixelSize(R.dimen.addview_image_width)));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == INTENT_SELECT_IMAGE) {
                if (data != null && data.getData() != null) { //ako je data==null, fotografija je napravljena kamerom, nije iz galerije
                    //u Marshmallow-u i kasnijim je data != null, ali je data.getData() == null
                    try {
                        FileUtils.copy(REQUEST_COPY_AVATAR, getContentResolver().openInputStream(data.getData()), imageFile, this);
                    } catch (IOException e) {
                        e.printStackTrace();
                        InfoDialog.newInstance(getString(R.string.error_cannot_resolve_uri_title),
                                               getString(R.string.error_cannot_resolve_uri_text))
                                  .show(getFragmentManager(), "userinfo.error.cannotresolveuri");
                    }
                } else {
                    avatar.setImageBitmap(BitmapUtils.loadImage(imageFile,
                                                                getResources().getDimensionPixelSize(R.dimen.addview_image_width)));
                    Users.setAvatar(REQUEST_SET_AVATAR, this, imageFile, handler, this);
                }
            }
        }
    }


    @Override
    public void onFileOperationCompleted(int operationId) {
        if(operationId == REQUEST_COPY_AVATAR) {
            avatar.setImageBitmap(BitmapUtils.loadImage(imageFile,
                                                        getResources().getDimensionPixelSize(R.dimen.addview_image_width)));
            Users.setAvatar(REQUEST_SET_AVATAR, this, imageFile, handler, this);
        }
    }

    @Override
    public void onIOException(int operationId, IOException ex) {
        Log.e(TAG, "Cannot resolve selected cover", ex);
        InfoDialog.newInstance(getString(R.string.error_cannot_resolve_uri_title),
                               getString(R.string.error_cannot_resolve_uri_text))
                  .show(getFragmentManager(), "userinfo.error.cannotresolveuri");
    }
}
