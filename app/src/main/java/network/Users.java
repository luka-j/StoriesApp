package network;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import rs.lukaj.minnetwork.Network;
import rs.lukaj.minnetwork.NetworkExceptionHandler;
import rs.lukaj.minnetwork.NetworkRequestBuilder;
import static rs.lukaj.minnetwork.NetworkRequestBuilder.*;

import static network.Network.*;

/**
 * Created by luka on 17.12.17..
 */

public class Users {
    private static URL REGISTER_URL, LOGIN_URL, MY_DETAILS, CHECK_PASSWORD, CHANGE_PASSWORD;
    static {
        try {
            REGISTER_URL =  new URL(HOST + V1 + "users/register");
            LOGIN_URL =  new URL(HOST + V1 + "users/login");
            MY_DETAILS = new URL(HOST + V1 + "users/me");
            CHECK_PASSWORD = new URL(HOST + V1 + "users/checkpw");
            CHANGE_PASSWORD = new URL(HOST + V1 + "users/changepw");
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
}
