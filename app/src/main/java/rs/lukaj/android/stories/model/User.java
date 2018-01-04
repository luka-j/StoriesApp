package rs.lukaj.android.stories.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import rs.lukaj.android.stories.network.Users;
import rs.lukaj.minnetwork.Network;

import static rs.lukaj.minnetwork.Network.Response.RESPONSE_OK;

/**
 * Created by luka on 17.12.17..
 */

public class User {
    public static final String AVATAR_FILENAME = "myAvatar.jpg";

    private static final String SHARED_PREFS_NAME = "userprefs";
    private static final String PREF_TOKEN        = "jwt";
    private static final String PREF_EMAIL        = "email";
    private static final String PREF_ID           = "id";
    private static final String PREF_USERNAME     = "username";
    private static final String PREF_HAS_IMAGE    = "hasImage";

    private static User loggedInInstance;

    private String token;

    private String id;
    private String username;
    private boolean hasImage;

    private User(String token) {
        this.token = token;
    }


    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean hasImage() {
        return hasImage;
    }

    public static boolean isLoggedIn(Context c) {
        return loggedInInstance != null || c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                                            .contains(PREF_TOKEN);
    }

    @Nullable
    public static User getLoggedInUser(Context c) {
        if(loggedInInstance != null) return loggedInInstance;
        SharedPreferences prefs = c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        if(prefs.contains(PREF_TOKEN)) {
            loggedInInstance = new User(prefs.getString(PREF_TOKEN, null));
            loggedInInstance.id = prefs.getString(PREF_ID, null);
            loggedInInstance.username = prefs.getString(PREF_USERNAME, null);
            loggedInInstance.hasImage = prefs.getBoolean(PREF_HAS_IMAGE, false);
        }
        return loggedInInstance;
    }

    public static User logIn(Context c, String token) {
        token = token.trim();
        SharedPreferences.Editor prefs = c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
        prefs.putString(PREF_TOKEN, token);
        prefs.apply();
        loggedInInstance = new User(token);
        Users.getMyDetails(0, c, null, new Network.NetworkCallbacks<String>() {
            @Override
            public void onRequestCompleted(int i, Network.Response<String> response) {
                if(response.responseCode == RESPONSE_OK) {
                    try {
                        JSONObject json = new JSONObject(response.responseData);
                        User.loggedInInstance.setDetails(c, json.getString("id"), json.getString("username"),
                                                         json.getString("email"), json.getBoolean("hasImage"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onExceptionThrown(int i, Throwable throwable) {}
        });
        return loggedInInstance;
    }

    public void setDetails(Context c, String id, String username, String email, boolean hasImage) {
        this.id = id;
        this.username = username;
        this.hasImage = hasImage;
        SharedPreferences.Editor prefs = c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
        prefs.putString(PREF_ID, id);
        prefs.putString(PREF_USERNAME, username);
        if(email != null) prefs.putString(PREF_EMAIL, email);
        prefs.putBoolean(PREF_HAS_IMAGE, hasImage);
        prefs.apply();
    }

    public static String getToken(Context c) {
        User user = getLoggedInUser(c);
        if(user == null) return null;
        return user.token;
    }

    public static boolean refreshToken(Context c, String newToken) {
        newToken = newToken.trim();
        User user = getLoggedInUser(c);
        if(user == null) return false;
        user.token = newToken;
        c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit().putString(PREF_TOKEN, newToken).apply();
        return true;
    }

    public static void logOut(Context c) {
        c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        if(loggedInInstance != null) loggedInInstance.token = null;
        loggedInInstance = null;
    }

    public static String getMyEmail(Context c) {
        return c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).getString(PREF_EMAIL, null);
    }
}
