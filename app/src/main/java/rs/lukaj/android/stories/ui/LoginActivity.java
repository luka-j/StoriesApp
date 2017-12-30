package rs.lukaj.android.stories.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.io.IOException;
import java.net.SocketException;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.model.User;
import rs.lukaj.android.stories.network.Users;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.minnetwork.Network;

/**
 * Created by luka on 25.12.17..
 */

public class LoginActivity extends AppCompatActivity implements Network.NetworkCallbacks<String>, InfoDialog.Callbacks  {

    public static final int REQUEST_LOGIN = 0;

    private static final String TAG_DIALOG_ERROR   = "stories.dialog.errordialog";
    private static final String TAG_DIALOG_NO_NETWORK = "stories.LoginActivity.errorNetwork";
    private boolean requestInProgress              = false;

    private CardView             login;
    private TextView             register;
    private TextInputLayout      emailTil;
    private TextInputLayout      passwordTil;
    private EditText             email;
    private EditText             password;
    private CircularProgressView progressView;
    private ExceptionHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        handler = new ExceptionHandler.DefaultHandler(this) {
            @Override
            public void handleUnauthorized(String errorMessage) {} //we're handling that here
        };

        login = findViewById(R.id.button_login);
        register = findViewById(R.id.button_register);
        emailTil = findViewById(R.id.login_email_til);
        email = findViewById(R.id.login_email_input);
        passwordTil = findViewById(R.id.login_password_til);
        password = findViewById(R.id.login_password_input);
        progressView = findViewById(R.id.login_cpv);

        login.setOnClickListener(v -> {
            if(!requestInProgress)
                login();
        });
        register.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void login() {
        requestInProgress = true;
        Users.login(REQUEST_LOGIN, email.getText().toString(), password.getText().toString(), handler, this);
        login.setVisibility(View.GONE);
        register.setVisibility(View.GONE);
        forceShow(progressView);
    }

    private void forceShow(View v) {
        v.setVisibility(View.VISIBLE);
        v.bringToFront();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            v.getParent().requestLayout();
            if(v.getParent() instanceof View)
                ((View)v.getParent()).invalidate();
        }
    }

    @Override
    public void onInfoDialogClosed(InfoDialog dialog) {
        if(dialog.getTag().equals(TAG_DIALOG_ERROR))
            password.setText("");
    }

    @Override
    public void onRequestCompleted(int id, Network.Response<String> response) {
        runOnUiThread(() -> {
            if(id == REQUEST_LOGIN) {
                switch (response.responseCode) {
                    case Network.Response.RESPONSE_OK:
                        User.logIn(this, response.responseData);
                        //user logged in, all fine
                        break;
                    case Network.Response.RESPONSE_UNAUTHORIZED:
                        InfoDialog.newInstance(getString(R.string.wrong_creds_title),
                                               getString(R.string.wrong_creds))
                                  .registerCallbacks(LoginActivity.this)
                                  .show(getFragmentManager(), TAG_DIALOG_ERROR);
                        reshowButtons();
                        break;
                    default:
                        response.handleErrorCode(handler);
                        password.setText("");
                        reshowButtons();
                }
            }
        });
        requestInProgress = false;
    }

    private void reshowButtons() {
        runOnUiThread(() -> {
            progressView.setVisibility(View.GONE);
            forceShow(login);
            forceShow(register);
        });
    }

    @Override
    public void onExceptionThrown(int i, Throwable ex) {
        if(ex instanceof Error)
            throw new Error(ex);
        runOnUiThread(() -> {
            if(ex instanceof IOException) handler.handleIOException((IOException)ex);
            else {
                InfoDialog.newInstance(getString(R.string.error_login_unknownex_title),
                                       getString(R.string.error_login_unknownex_text))
                          .registerCallbacks(LoginActivity.this)
                          .show(getFragmentManager(), TAG_DIALOG_NO_NETWORK);
            }
            ex.printStackTrace();
            requestInProgress = false;
        });
    }
}
