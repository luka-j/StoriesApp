package network;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rs.lukaj.android.stories.model.User;
import rs.lukaj.minnetwork.*;
import rs.lukaj.minnetwork.Network;

import static network.Network.*;

/**
 * Created by luka on 17.12.17..
 */

public class TokenManager implements AuthTokenManager {
    private static final String TAG = "network.TokenManager";
    private static TokenManager instance;

    private Context context;
    private TokenManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public TokenManager getInstance(Context c) {
        if(instance == null) instance = new TokenManager(c);
        return instance;
    }

    @Override
    public String getToken() {
        return User.getToken(context);
    }

    @Override
    public void handleTokenError(Network.Response<?> response, NetworkExceptionHandler networkExceptionHandler)
            throws NotLoggedInException {
        if ("Expired".equals(response.errorMessage)) {
            try {
                URL refreshUrl = new URL(HOST + V1 + "users/" + User.getToken(context) + "/refresh");
                User.refreshToken(context, NetworkRequestBuilder.create(refreshUrl, NetworkRequestBuilder.VERB_POST)
                                                                .blocking(5, TimeUnit.SECONDS).responseData);
                Log.i(TAG, "token refreshed");
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex); //should NOT happen
            } catch (IOException ex) {
                networkExceptionHandler.handleIOException(ex);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                networkExceptionHandler.handleGatewayTimeout(); //well not really, but let's hope it won't happen
            }
        } else
            throw new NotLoggedInException(TokenManager.class, "handleTokenError(Response): server token error");
    }

    @Override
    public int getTokenStatus(Network.Response<?> response) {
        if ("Expired".equals(response.errorMessage)) return TOKEN_EXPIRED;
        if ("Invalid".equals(response.errorMessage)) return TOKEN_INVALID;
        return TOKEN_UNKNOWN;
    }

    @Override
    public void clearToken() {
        User.logOut(context);
    }
}
