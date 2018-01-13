package rs.lukaj.android.stories.network;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.io.FileUtils;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.model.User;
import rs.lukaj.android.stories.ui.BitmapUtils;
import rs.lukaj.minnetwork.Network;
import rs.lukaj.minnetwork.NetworkExceptionHandler;
import rs.lukaj.minnetwork.NetworkRequestBuilder;

import static rs.lukaj.android.stories.network.Network.HOST;
import static rs.lukaj.android.stories.network.Network.V1;
import static rs.lukaj.minnetwork.Network.Response.RESPONSE_OK;
import static rs.lukaj.minnetwork.NetworkRequestBuilder.VERB_DELETE;
import static rs.lukaj.minnetwork.NetworkRequestBuilder.VERB_GET;
import static rs.lukaj.minnetwork.NetworkRequestBuilder.VERB_POST;
import static rs.lukaj.minnetwork.NetworkRequestBuilder.VERB_PUT;

/**
 * Created by luka on 20.12.17..
 */

public class Books {
    private static URL UPLOAD_BOOK, PROMOTED_BOOKS, PUSHED_BOOKS, SEARCH, EXPLORE, MY_PUBLISHED_BOOKS;

    static {
        try {
            UPLOAD_BOOK = new URL(HOST + V1 + "books");
            PROMOTED_BOOKS = new URL(HOST + V1 + "books/promoted");
            PUSHED_BOOKS = new URL(HOST + V1 + "books/pushed");
            SEARCH = new URL(HOST + V1 + "books/search");
            EXPLORE = new URL(HOST + V1 + "books/explore");
            MY_PUBLISHED_BOOKS = new URL(HOST + V1 + "books/publisher/me");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getBookContent(int requestId, Context c, String bookId, File saveTo,
                                      NetworkExceptionHandler handler, Network.NetworkCallbacks<File> callbacks) {
        try {
            NetworkRequestBuilder<String, File> req = NetworkRequestBuilder
                    .create(new URL(HOST + V1 + "books/" + bookId + "/"), VERB_GET, NetworkRequestBuilder.emptyMap, saveTo)
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
            throw new RuntimeException(e);
        }
    }

    public static void uploadBook(int requestId, Context c, AndroidFiles files, Book book,
                                  NetworkExceptionHandler handler, Network.NetworkCallbacks<String> callbacks,
                                  ExecutorService executor) {
        File bookDir = files.getRootDirectory(book.getName()),
                bookZip = new File(bookDir.getParent(), book.getName() + ".zip");
        FileUtils.zipDirectoryAt(0, bookDir, bookZip, new FileUtils.Callbacks() {
            @Override
            public void onFileOperationCompleted(int operationId) {
                NetworkRequestBuilder.create(UPLOAD_BOOK, VERB_POST, bookZip)
                                     .id(requestId)
                                     .auth(TokenManager.getInstance(c))
                                     .handler(handler)
                                     .executor(executor)
                                     .async(callbacks);
                bookZip.deleteOnExit();
            }

            @Override
            public void onIOException(int operationId, IOException ex) {
                handler.handleIOException(ex);
            }
        });
    }

    public static void exploreBooks(int requestId, int maxResults, double minRanking,
                                    NetworkExceptionHandler handler, Network.NetworkCallbacks<String> callbacks) {
        Map<String, String> params = new HashMap<>();
        params.put("limit", String.valueOf(maxResults));
        params.put("minRanking", String.valueOf(minRanking));
        NetworkRequestBuilder.create(EXPLORE, VERB_GET, params)
                             .id(requestId)
                             .handler(handler)
                             .async(callbacks);
    }

    public static void getBookCover(int requestId, String bookId, NetworkExceptionHandler exceptionHandler,
                                    int maxWidth, File saveTo, Network.NetworkCallbacks<File> callbacks) {
        Map<String, String> params = new HashMap<>();
        params.put("maxWidth", String.valueOf(maxWidth));
        try {
            NetworkRequestBuilder.create(new URL(HOST + V1 + "books/" + bookId + "/cover"), VERB_GET, params, saveTo)
                                 .id(requestId)
                                 .handler(exceptionHandler)
                                 .async(callbacks);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void downloadCover(Activity c, String bookId, ImageView putTo, int width, ExceptionHandler handler) {
        File cover = new File(c.getCacheDir(), "covers/" + bookId);
        if(!cover.getParentFile().isDirectory()) cover.getParentFile().mkdirs();
        if(cover.isFile()) {
            putTo.setImageBitmap(BitmapUtils.loadImage(cover, width));
        } else {
            Books.getBookCover(0, bookId, handler, width, cover, new Network.NetworkCallbacks<File>() {
                @Override
                public void onRequestCompleted(int i, Network.Response<File> response) {
                    if(response.responseCode == RESPONSE_OK)
                        c.runOnUiThread(() -> putTo.setImageBitmap(BitmapUtils.loadImage(cover, width)));
                }

                @Override
                public void onExceptionThrown(int i, Throwable throwable) {
                    if(throwable instanceof Exception) handler.handleUnknownNetworkException((Exception)throwable);
                    if(throwable instanceof Error) throw (Error)throwable;
                }
            });
        }
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

    public static void getMyPublishedBooks(int requestId, Context context, NetworkExceptionHandler exceptionHandler,
                                           Network.NetworkCallbacks<String> callbacks) {
        NetworkRequestBuilder.create(MY_PUBLISHED_BOOKS, VERB_GET)
                             .auth(TokenManager.getInstance(context))
                             .id(requestId)
                             .handler(exceptionHandler)
                             .async(callbacks);
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
