package rs.lukaj.android.stories.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.model.User;
import rs.lukaj.android.stories.network.Users;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.minnetwork.Network;

/**
 * Created by luka on 25.12.17..
 */

public class RegisterActivity extends AppCompatActivity implements Network.NetworkCallbacks<String> {

    private static final int REQUEST_REGISTER               = 0;
    private static final String TAG                         = "RegisterActivity";

    private boolean requestInProgress = false;
    private CardView register;
    private TextInputLayout emailTil;
    private TextInputLayout usernameTil;
    private TextInputLayout passwordTil;
    private EditText email;
    private EditText username;
    private EditText password;

    private ExceptionHandler handler;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        handler = new ExceptionHandler.DefaultHandler(this) {
            @Override
            public void handleDuplicate() {}
        };

        emailTil = findViewById(R.id.register_email_til);
        usernameTil = findViewById(R.id.register_username_til);
        passwordTil = findViewById(R.id.register_password_til);
        register = findViewById(R.id.button_register);
        email = findViewById(R.id.register_email_input);
        username = findViewById(R.id.register_username_input);
        password = findViewById(R.id.register_password_input);
        ((TextView)findViewById(R.id.register_legal)).setMovementMethod(LinkMovementMethod.getInstance());

        register.setOnClickListener(v -> {
            if (!requestInProgress)
                register();
        });
    }

    private void register() {
        boolean hasErrors = false;
        if(email.getText().length() > Limits.USER_EMAIL_MAX_LENGTH) {
            emailTil.setError(getString(R.string.error_email_too_long));
            hasErrors = true;
        } else emailTil.setError(null);
        if(username.getText().length() > Limits.USER_NAME_MAX_LENGTH) {
            usernameTil.setError(getString(R.string.error_username_too_long, Limits.USER_NAME_MAX_LENGTH));
            hasErrors = true;
        } else usernameTil.setError(null);
        if(password.getText().length() > Limits.USER_PASSWORD_MAX_LENGTH) {
            passwordTil.setError(getString(R.string.error_password_too_long, Limits.USER_PASSWORD_MAX_LENGTH));
            hasErrors = true;
        } else passwordTil.setError(null);
        if(password.getText().length() < Limits.USER_PASSWORD_MIN_LENGTH) {
            passwordTil.setError(getString(R.string.error_password_too_short,Limits.USER_PASSWORD_MIN_LENGTH));
            hasErrors = true;
        } else passwordTil.setError(null);
        if(!Utils.isEmailValid(email.getText())) {
            emailTil.setError(getString(R.string.error_invalid_email));
            hasErrors = true;
        } else emailTil.setError(null);
        if(!hasErrors) {
            requestInProgress = true;
            Users.register(REQUEST_REGISTER, email.getText().toString(),
                           username.getText().toString(),
                           password.getText().toString(),
                           handler,this);
        }
    }

    @Override
    public void onRequestCompleted(int id, Network.Response<String> response) {
        runOnUiThread(() -> {
            emailTil.setError(null);
            if (id == REQUEST_REGISTER) {
                switch (response.responseCode) {
                    case Network.Response.RESPONSE_CREATED:
                        User.logIn(this, response.responseData);

                        break;
                    case Network.Response.RESPONSE_DUPLICATE:
                        if(response.errorMessage.equals("email"))
                            emailTil.setError(getString(R.string.email_duplicate));
                        if(response.errorMessage.equals("username"))
                            emailTil.setError(getString(R.string.username_duplicate));
                        else
                            handler.handleDuplicate(); //this shouldn't occur
                        break;
                }
            }
        });
        requestInProgress = false;
    }

    @Override
    public void onExceptionThrown(int i, Throwable ex) {
        if(ex instanceof Error)
            throw new Error(ex);
        if(ex instanceof IOException)
            handler.handleIOException((IOException)ex);
        else {
            runOnUiThread(() -> InfoDialog.newInstance(getString(R.string.error_unknown_ex_title),
                                               getString(R.string.error_unknown_ex_text))
                                  .show(getFragmentManager(), ""));
            Log.e(TAG, "Unknown Throwable caught", ex);
        }
        requestInProgress = false;
    }
}
