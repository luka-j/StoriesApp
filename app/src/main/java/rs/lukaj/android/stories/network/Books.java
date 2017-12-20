package rs.lukaj.android.stories.network;

import android.content.Context;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import rs.lukaj.android.stories.model.User;
import rs.lukaj.minnetwork.*;
import rs.lukaj.minnetwork.Network;

import static rs.lukaj.android.stories.network.Network.*;
import static rs.lukaj.minnetwork.NetworkRequestBuilder.*;

/**
 * Created by luka on 20.12.17..
 */

public class Books {
    private static URL UPLOAD_BOOK, PROMOTED_BOOKS, PUSHED_BOOKS, SEARCH;

    static {
        try {
            UPLOAD_BOOK = new URL(HOST + V1 + "books");
            PROMOTED_BOOKS = new URL(HOST + V1 + "books/promoted");
            PUSHED_BOOKS = new URL(HOST + V1 + "books/pushed");
            SEARCH = new URL(HOST + V1 + "books/search");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getBookContent(int requestId, Context c, String bookId, File saveTo,
                                      NetworkExceptionHandler handler, Network.NetworkCallbacks<File> callbacks) {
        try {
            NetworkRequestBuilder<File> req = NetworkRequestBuilder
                    .create(new URL(HOST + V1 + "books/" + bookId + "/"), VERB_GET, saveTo)
                    .id(requestId)
                    .handler(handler);
            if(User.isLoggedIn(c)) req.auth(TokenManager.getInstance(c));
            //this does not _require_ an account (though it'd be nice)
            req.async(callbacks);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getBookInfo(int requestId, String bookId, NetworkExceptionHandler exceptionHandler,
                                   Network.NetworkCallbacks<String> callbacks) {
        try {
            NetworkRequestBuilder.create(new URL(HOST + V1 + "books/" + bookId + "/info"), VERB_GET)
                                 .id(requestId)
                                 .handler(exceptionHandler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void uploadBook(int requestId, Context c, File book, NetworkExceptionHandler exceptionHandler,
                                  Network.NetworkCallbacks<File> callbacks) {
        NetworkRequestBuilder.create(UPLOAD_BOOK, VERB_POST, book)
                             .id(requestId)
                             .auth(TokenManager.getInstance(c))
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void getPromotedBooks(int requestId, NetworkExceptionHandler exceptionHandler,
                                        Network.NetworkCallbacks<String> callbacks) {
        NetworkRequestBuilder.create(PROMOTED_BOOKS, VERB_GET)
                             .id(requestId)
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void getPushedBooks(int requestId, NetworkExceptionHandler exceptionHandler,
                                      Network.NetworkCallbacks<String> callbacks) {
        NetworkRequestBuilder.create(PUSHED_BOOKS, VERB_GET)
                             .id(requestId)
                             .handler(exceptionHandler)
                             .async(callbacks);
    }

    public static void removeBook(int requestId, String bookId, NetworkExceptionHandler exceptionHandler,
                                  Network.NetworkCallbacks<String> callbacks) {
        try {
            NetworkRequestBuilder.create(new URL(HOST + V1 + "books/" + bookId + "/"), VERB_DELETE)
                                 .id(requestId)
                                 .handler(exceptionHandler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    //order should be either oldest, newest, best or worst
    public static void searchBooks(int requestId, String title, String genres, String order, int limit,
                                   NetworkExceptionHandler handler, Network.NetworkCallbacks<String> callbacks) {
        Map<String, String> data = new HashMap<>();
        data.put("title", title);
        if(order != null && !order.isEmpty())
            data.put("order", order);
        data.put("genres", genres);
        data.put("limit", String.valueOf(limit));
        NetworkRequestBuilder.create(SEARCH, VERB_POST, data)
                             .id(requestId)
                             .handler(handler)
                             .async(callbacks);
    }

    public static void rateBook(int requestId, Context c, String bookId, int rating,
                                NetworkExceptionHandler handler, Network.NetworkCallbacks<String> callbacks) {
        Map<String, String> data = new HashMap<>();
        data.put("rating", String.valueOf(rating));
        try {
            NetworkRequestBuilder.create(new URL(HOST + V1 + "books/" + bookId + "/rate"), VERB_PUT, data)
                                 .id(requestId)
                                 .auth(TokenManager.getInstance(c))
                                 .handler(handler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getAverageRating(int requestId, String bookId, NetworkExceptionHandler exceptionHandler,
                                        Network.NetworkCallbacks<String> callbacks) {
        try {
            NetworkRequestBuilder.create(new URL(HOST + V1 + "books/" + bookId + "/ratings/average"), VERB_GET)
                                 .id(requestId)
                                 .handler(exceptionHandler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void getNumberOfRatings(int requestId, String bookId, NetworkExceptionHandler handler,
                                          Network.NetworkCallbacks<String> callbacks) {
        try {
            NetworkRequestBuilder.create(new URL(HOST + V1 + "books/" + bookId + "ratings/count"), VERB_GET)
                                 .id(requestId)
                                 .handler(handler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getBooksPublishedBy(int requestId, String userId, NetworkExceptionHandler handler,
                                           Network.NetworkCallbacks<String> callbacks) {
        try {
            NetworkRequestBuilder.create(new URL(HOST + V1 + "books/publisher/" + userId), VERB_GET)
                                 .id(requestId)
                                 .handler(handler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
