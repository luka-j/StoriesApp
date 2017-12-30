package rs.lukaj.android.stories.network;

import android.content.Context;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import rs.lukaj.android.stories.model.User;
import rs.lukaj.minnetwork.Network;
import rs.lukaj.minnetwork.NetworkExceptionHandler;
import rs.lukaj.minnetwork.NetworkRequestBuilder;
import static rs.lukaj.minnetwork.NetworkRequestBuilder.*;

import static rs.lukaj.android.stories.network.Network.*;

/**
 * Created by luka on 17.12.17..
 */

public class Users {
    private static URL REGISTER_URL, LOGIN_URL, MY_DETAILS, CHECK_PASSWORD, CHANGE_PASSWORD, SET_AVATAR, SEARCH,
    READING_LIST;
    static {
        try {
            REGISTER_URL =  new URL(HOST + V1 + "users/register");
            LOGIN_URL =  new URL(HOST + V1 + "users/login");
            MY_DETAILS = new URL(HOST + V1 + "users/me");
            CHECK_PASSWORD = new URL(HOST + V1 + "users/checkpw");
            CHANGE_PASSWORD = new URL(HOST + V1 + "users/changepw");
            SET_AVATAR = new URL(HOST + V1 + "users/avatar");
            SEARCH = new URL(HOST + V1 + "users/search");
            READING_LIST = new URL(HOST + V1 + "users/myReadingList");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void register(int requestId, String email, String user, String password,
                                NetworkExceptionHandler exceptionHandler, Network.NetworkCallbacks<String> callbacks) {
        Map<String, String> data = new HashMap<>();
        data.put("username", user);
        data.put("email", email);
        data.put("password", password);
        NetworkRequestBuilder.create(REGISTER_URL, VERB_POST, data)
                             .id(requestId)
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void login(int requestId, String email, String pass, NetworkExceptionHandler exceptionHandler,
                             Network.NetworkCallbacks<String> callbacks) {
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("pass", pass);
        NetworkRequestBuilder.create(LOGIN_URL, VERB_POST, data)
                             .id(requestId)
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void getMyDetails(int requestId, NetworkExceptionHandler exceptionHandler,
                                    Network.NetworkCallbacks<String> callbacks) {
        NetworkRequestBuilder.create(MY_DETAILS, VERB_GET)
                             .id(requestId)
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void checkPassword(int requestId, String password, NetworkExceptionHandler exceptionHandler,
                                     Network.NetworkCallbacks<String> callbacks) {
        Map<String, String> data = new HashMap<>();
        data.put("pass", password);
        NetworkRequestBuilder.create(CHECK_PASSWORD, VERB_PUT, data)
                             .id(requestId)
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void changePassword(int requestId, String oldPass, String newPass,
                                      NetworkExceptionHandler exceptionHandler, Network.NetworkCallbacks<String> callbacks) {
        Map<String, String> data = new HashMap<>();
        data.put("old", oldPass);
        data.put("new", newPass);
        NetworkRequestBuilder.create(CHANGE_PASSWORD, VERB_PUT, data)
                             .id(requestId)
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void setAvatar(int requestId, Context c, File avatar, NetworkExceptionHandler exceptionHandler,
                                 Network.NetworkCallbacks<String> callbacks) {
        NetworkRequestBuilder.create(SET_AVATAR, VERB_PUT, avatar)
                             .id(requestId)
                             .auth(TokenManager.getInstance(c))
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void getMyAvatar(int requestId, Context c, int maxWidth, File saveTo,
                                   NetworkExceptionHandler handler, Network.NetworkCallbacks<File> callbacks) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("maxWidth", String.valueOf(maxWidth));
            NetworkRequestBuilder.create(new URL(HOST + V1 + "users/myAvatar"), VERB_GET, data, saveTo)
                                 .id(requestId)
                                 .auth(TokenManager.getInstance(c))
                                 .handler(handler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getAvatar(int requestId, Context c, String id, int maxWidth, File saveTo,
                                 NetworkExceptionHandler exceptionHandler, Network.NetworkCallbacks<File> callbacks) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("maxWidth", String.valueOf(maxWidth));
            NetworkRequestBuilder.create(new URL(HOST + V1 + "users/" + id + "/avatar"), VERB_GET, data, saveTo)
                                 .id(requestId)
                                 .auth(TokenManager.getInstance(c))
                                 .handler(exceptionHandler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void searchUsers(int requestId, Context c, String username, int limit,
                                   NetworkExceptionHandler handler, Network.NetworkCallbacks<String> callbacks) {
        Map<String, String> data = new HashMap<>();
        data.put("name", username);
        data.put("limit", String.valueOf(limit));
        NetworkRequestBuilder.create(SEARCH, VERB_POST, data)
                             .id(requestId)
                             .auth(TokenManager.getInstance(c))
                             .handler(handler)
                             .async(callbacks);
    }

    public static void getReadingList(int requestId, Context c, NetworkExceptionHandler exceptionHandler,
                                      Network.NetworkCallbacks<String> callbacks) {
        NetworkRequestBuilder.create(READING_LIST, VERB_GET)
                             .id(requestId)
                             .auth(TokenManager.getInstance(c))
                             .handler(exceptionHandler)
                             .async(callbacks);
    }
}
