package rs.lukaj.android.stories.ui;

import android.Manifest;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.environment.NullDisplay;
import rs.lukaj.android.stories.io.BookIO;
import rs.lukaj.android.stories.io.BookShelf;
import rs.lukaj.android.stories.io.FileUtils;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.model.User;
import rs.lukaj.android.stories.network.Books;
import rs.lukaj.android.stories.ui.dialogs.ConfirmDialog;
import rs.lukaj.android.stories.ui.dialogs.ExploreBookDetailsDialog;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;
import rs.lukaj.android.stories.ui.dialogs.SearchBooksDialog;
import rs.lukaj.minnetwork.Network;
import rs.lukaj.stories.environment.DisplayProvider;

import static rs.lukaj.android.stories.ui.BookListFragment.TYPE_DOWNLOADED;
import static rs.lukaj.android.stories.ui.BookListFragment.TYPE_EXPLORE;
import static rs.lukaj.android.stories.ui.BookListFragment.TYPE_FORKED_CREATED;
import static rs.lukaj.minnetwork.Network.Response.RESPONSE_OK;

public class MainActivity extends AppCompatActivity implements InputDialog.Callbacks,
                                                               BookListFragment.Callbacks,
                                                               ConfirmDialog.Callbacks,
                                                               ExploreBookDetailsDialog.Callbacks,
                                                               SearchBooksDialog.Callbacks,
                                                               Network.NetworkCallbacks {
    private static final String TAG                 = "stories.MainActivity";
    private static final int PERM_REQ_STORAGE = 0;

    public static final String PREFS_REMOVED_BOOKS = "removedBooks";
    public static final String PREFS_DEMO_PROGRESS = "demoProgress";
    public static final String DEMO_BOOK_NAME = "demo";
    public static final boolean ONBOARDING_ENABLED = true;

    private static final int TAB_POS_EXPLORE = 0;
    private static final int TAB_POS_DOWNLOADED = 1;
    private static final int TAB_POS_MY_BOOKS   = 2;
    private static final int TAB_COUNT = 3;
    private static final int DEFAULT_TAB_POS       = 1;

    private static final String TAG_NEW_BOOK_TITLE    = "diagNewBook";
    private static final String TAG_SEARCH_BOOKS      = "diagSearchBooks";
    private static final String TAG_REMOVE_BOOK       = "diagRemoveBook";
    private static final int REQUEST_EXPLORE_BOOKS    = 2;
    private static final int REQUEST_DOWNLOAD_BOOK    = 3;
    private static final int REQUEST_LOGIN_ACTIVITY   = 4;
    private static final int REQUEST_GET_PUSHED_BOOKS = 5;

    private static final String SHOWCASE_WELCOME           = "MainActivity.showcase.welcome";
    private static final String SHOWCASE_MY_BOOKS          = "MainActivity.demo.mybooks";
    private static final String SHOWCASE_AFTERDEMO         = "MainActivity.demo.afterdemo";
    private static final String SHOWCASE_EXPLORE           = "MainActivity.showcase.explore";
    private static final String SHOWCASE_CREATED_BOOK      = "MainActivity.showcase.createdbook";
    private static final String DEMO_PROGRESS_INTRO        = "mainactivity.finishedintro";
    private static final String DEMO_PROGRESS_CREATED_BOOK = "mainactivity.createdbook";

    private Toolbar   toolbar;
    private FloatingActionButton fab;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter adapter;
    private Showcase showcaseHelper;
    private AndroidFiles files ;
    private DisplayProvider display;
    private ExceptionHandler exceptionHandler;
    private Handler handler;

    private boolean playedDemo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        files = new AndroidFiles(this);
        display = new NullDisplay();
        exceptionHandler = new ExceptionHandler.DefaultHandler(this);
        showcaseHelper = new Showcase(this);
        handler = new Handler();

        setContentView(R.layout.activity_book_list);
        toolbar = findViewById(R.id.toolbar);
        if(toolbar != null)
            setSupportActionBar(toolbar);

        if (ContextCompat.checkSelfPermission(this,
                                              Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                                              new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                              PERM_REQ_STORAGE);
        } else {
            initActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERM_REQ_STORAGE:
                if(grantResults.length > 0
                   && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    InfoDialog.newInstance(getString(R.string.explain_perm_storage_title),
                                           getString(R.string.explain_perm_storage_text))
                              .registerCallbacks(d -> ActivityCompat.requestPermissions(this,
                                                                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                                        PERM_REQ_STORAGE))
                              .show(getFragmentManager(), "infoExplainStorage");
                } else if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initActivity();
                }
        }
    }

    private void initActivity() {
        viewPager = findViewById(R.id.book_viewpager);
        viewPager.setOffscreenPageLimit(2); //todo figure out lifecycle of inactive fragments
        setupViewPager(viewPager);

        tabLayout = findViewById(R.id.book_tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(DEFAULT_TAB_POS).select();
        setTabListener();

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            if(adapter.mFragmentList.get(tabLayout.getSelectedTabPosition()).getType() == TYPE_FORKED_CREATED)
                InputDialog.newInstance(R.string.new_book, getString(R.string.new_book_title),
                                        R.string.create, 0, "", "", Limits.BOOK_TITLE_MAX_LENGTH,
                                        true)
                           .show(getFragmentManager(), TAG_NEW_BOOK_TITLE);
            else if(adapter.mFragmentList.get(tabLayout.getSelectedTabPosition()).getType() == TYPE_EXPLORE) {
                new SearchBooksDialog().show(getFragmentManager(), TAG_SEARCH_BOOKS);
            }
        });

        if(!getSharedPreferences(PREFS_REMOVED_BOOKS, MODE_PRIVATE).contains(DEMO_BOOK_NAME)
                && !files.getBooks(TYPE_DOWNLOADED).contains(DEMO_BOOK_NAME)) {
            try {
                files.unpackBook(getResources().openRawResource(R.raw.demo));
                //this looks like a race condition, but I thought everything is executed on UI thread, which is strange
                if(adapter.mFragmentList.size() > TAB_POS_DOWNLOADED && adapter.mFragmentList.get(TAB_POS_DOWNLOADED) != null)
                    adapter.mFragmentList.get(TAB_POS_DOWNLOADED).setData();
            } catch (IOException e) {
                exceptionHandler.handleBookIOException(e);
            }
        } else {
            playedDemo = true;
        }

        handler.postDelayed(() -> {
                                View showcaseTarget = adapter.mFragmentList.get(TAB_POS_DOWNLOADED).getViewForShowcase();
                                showcaseHelper.showSequence(SHOWCASE_WELCOME,
                                                            new View[]{null, showcaseTarget},
                                                            new int[]{R.string.welcome, 0},
                                                            new int[]{R.string.sc_welcome1, R.string.sc_welcome2},
                                                            true);
                            }, 800); //need delay to allow for book to load
        Books.getPushedBooks(REQUEST_GET_PUSHED_BOOKS, exceptionHandler, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(playedDemo && ONBOARDING_ENABLED) {
            View demoBook = adapter.mFragmentList.get(TAB_POS_DOWNLOADED).getViewForShowcase();
            showcaseHelper.showShowcase(SHOWCASE_AFTERDEMO, demoBook, R.string.sc_afterdemo, false, true);
            getSharedPreferences(PREFS_DEMO_PROGRESS, MODE_PRIVATE).edit().putBoolean(DEMO_PROGRESS_INTRO, true).apply();
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(BookListFragment.newInstance(BookListFragment.TYPE_EXPLORE),
                            getString(R.string.tab_explore));
        adapter.addFragment(BookListFragment.newInstance(TYPE_DOWNLOADED),
                            getString(R.string.tab_downloaded_books));
        adapter.addFragment(BookListFragment.newInstance(BookListFragment.TYPE_FORKED_CREATED),
                            getString(R.string.tab_my_books));
        viewPager.setAdapter(adapter);
    }

    private void setTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                BookListFragment fragment = adapter.mFragmentList.get(tab.getPosition());
                switch (fragment.getType()) {
                    case TYPE_FORKED_CREATED:
                        fab.setImageResource(R.drawable.ic_add_white_24dp);
                        fab.setVisibility(View.VISIBLE);
                        View showcaseTarget = fragment.getViewForShowcase();
                        handler.postDelayed(() -> {
                            if(showcaseTarget != null && ONBOARDING_ENABLED)
                                showcaseHelper.showSequence(SHOWCASE_MY_BOOKS, new View[]{null, showcaseTarget},
                                                            new int[]{R.string.sc_mybooks1, R.string.sc_mybooks2}, true);

                            if(ONBOARDING_ENABLED && getSharedPreferences(PREFS_DEMO_PROGRESS, MODE_PRIVATE).contains(DEMO_PROGRESS_CREATED_BOOK))
                                showcaseHelper.showShowcase(SHOWCASE_CREATED_BOOK, showcaseTarget, R.string.sc_createdbook, true, true);
                        }, 300);
                        break;
                    case TYPE_EXPLORE:
                        fab.setImageResource(R.drawable.ic_search_white_24dp);
                        fab.setVisibility(View.VISIBLE);
                        if(ONBOARDING_ENABLED)
                            showcaseHelper.showShowcase(SHOWCASE_EXPLORE, R.string.sc_explore, true);
                        break;
                    case TYPE_DOWNLOADED:
                        fab.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if(!User.isLoggedIn(this)) {
            menu.removeItem(R.id.menu_item_user_details);
            menu.removeItem(R.id.menu_item_reading_history);
            menu.removeItem(R.id.menu_item_my_published_books);
            menu.removeItem(R.id.menu_item_logout);
        } else {
            menu.removeItem(R.id.menu_item_login);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                //todo - add option to reset showcase
                return true;
            case R.id.menu_item_user_details:
                startActivity(new Intent(this, UserInfoActivity.class));
                return true;
            case R.id.menu_item_login:
                startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_LOGIN_ACTIVITY);
                return true;
            case R.id.menu_item_reading_history:
                Intent rhi = new Intent(this, BookListActivity.class);
                rhi.putExtra(BookListActivity.EXTRA_FRAGMENT_TYPE, BookListFragment.TYPE_READING_HISTORY);
                startActivity(rhi);
                return true;
            case R.id.menu_item_my_published_books:
                Intent mpbi = new Intent(this, BookListActivity.class);
                mpbi.putExtra(BookListActivity.EXTRA_FRAGMENT_TYPE, BookListFragment.TYPE_MY_PUBLISHED_BOOKS);
                startActivity(mpbi);
                return true;
            case R.id.menu_item_logout:
                User.logOut(this);
                invalidateOptionsMenu();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_LOGIN_ACTIVITY) {
            invalidateOptionsMenu();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        switch (dialog.getTag()) {
            case TAG_NEW_BOOK_TITLE:
                adapter.mFragmentList.get(TAB_POS_MY_BOOKS).setData();
                getSharedPreferences(PREFS_DEMO_PROGRESS, MODE_PRIVATE).edit().putBoolean(DEMO_PROGRESS_CREATED_BOOK, true).apply();
                Intent i = new Intent(MainActivity.this, BookEditorActivity.class);
                i.putExtra(BookEditorActivity.EXTRA_BOOK_NAME, s);
                startActivity(i);
                break;
        }
    }

    private Book removingBook;
    private BookListFragment refreshAfterRemoval;
    @Override
    public void removeBook(Book book, BookListFragment fragment) {
        if(removingBook != null) return;
        removingBook = book;
        refreshAfterRemoval = fragment;
        ConfirmDialog.newInstance(R.string.confirm_remove_book_title, R.string.confirm_remove_book_text,
                                  R.string.remove, R.string.cancel)
                     .show(getFragmentManager(), TAG_REMOVE_BOOK);
    }

    @Override
    public void onBookClick(Book book, int fragmentType) {
        if(fragmentType == TYPE_DOWNLOADED || fragmentType == TYPE_FORKED_CREATED) {
            Intent intent = new Intent(this, StoryActivity.class);
            intent.putExtra(StoryActivity.EXTRA_BOOK_NAME, book.getName());
            if(book.isDemo()) playedDemo = true;
            startActivity(intent);
        } else if(fragmentType == TYPE_EXPLORE) {
            ExploreBookDetailsDialog.newInstance(book.getTitle(), Utils.listToString(book.getGenres()), book.getAuthor(),
                                                 book.getDate(), book.getRating(), book.getDescription())
            .show(getFragmentManager(), book.getId());
        }
    }

    private BookShelf retrieveToShelf;
    @Override
    public void retrieveData(AndroidFiles files, DisplayProvider display, BookShelf shelf, int count,
                             double minRanking, int type) {
        if(type == TYPE_DOWNLOADED)
            BookIO.loadAllBooks(files, display, shelf, AndroidFiles.APP_DATA_DIR);
        else if(type == TYPE_FORKED_CREATED)
            BookIO.loadAllBooks(files, display, shelf, AndroidFiles.SD_CARD_DIR);
        else if(type == TYPE_EXPLORE) {
            retrieveToShelf = shelf;
            Books.exploreBooks(REQUEST_EXPLORE_BOOKS, count, minRanking, exceptionHandler, this);
        }
    }

    //onContextItemSelected is handled in BookListFragment because of possibility there are some shared options
    //(i.e. option with same id for multiple types of fragment)
    @Override
    public void createContextMenu(ContextMenu menu, Book book, int type) {
        if(type == TYPE_FORKED_CREATED) {
            menu.add(type, R.id.menu_item_edit_book, 1, R.string.edit_book); //fun fact: onContextItemSelected is
            menu.add(type, R.id.menu_item_publish_book, 2, R.string.publish_book); //called for all fragments in
            menu.add(type, R.id.menu_item_remove_book, 3, R.string.remove_from_my_books); //an activity
        } else if(type == TYPE_DOWNLOADED) {
            int i=1;
            menu.add(type, R.id.menu_item_see_details, i++, R.string.see_details);
            if(book.isForkable())
                menu.add(type, R.id.menu_item_fork_book, i++, R.string.fork_book);
            menu.add(type, R.id.menu_item_remove_book, i++, R.string.remove_book);
        }
    }

    @Override
    public void afterBookForked(Book book) {
        tabLayout.getTabAt(TAB_POS_MY_BOOKS).select();
        List<Book> bookl = new ArrayList<>(1);
        bookl.add(book);
        adapter.mFragmentList.get(TAB_POS_MY_BOOKS).addBooks(bookl);
    }

    @Override
    public void onPositive(DialogFragment dialog) {
        if(TAG_REMOVE_BOOK.equals(dialog.getTag())) {
            try {
                FileUtils.delete(files.getRootDirectory(removingBook.getName()));
                refreshAfterRemoval.setData();
                getSharedPreferences(PREFS_REMOVED_BOOKS, MODE_PRIVATE)
                        .edit()
                        .putBoolean(removingBook.getName(), true)
                        .apply();
            } catch (IOException e) {
                exceptionHandler.handleBookIOException(e);
            } finally {
                removingBook = null;
                refreshAfterRemoval = null;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked") //because generics suck (can't override both Response<File> and Response<String>)
    public void onRequestCompleted(int id, Network.Response response) {
        List<Book> books = new ArrayList<>();
            switch (id) {
                case REQUEST_EXPLORE_BOOKS:
                    response = (Network.Response<String>) response; //this is truly only semantic (fu)
                    if(response.responseCode == RESPONSE_OK) {
                        try {
                            JSONArray jsonArray = new JSONArray((String) response.responseData);
                            for (int i = 0; i < jsonArray.length(); i++)
                                books.add(new Book(jsonArray.getJSONObject(i)));
                        } catch (JSONException e) {
                            exceptionHandler.handleJsonException();
                        }
                    }
                    retrieveToShelf.addBooks(books);
                    break;

                case REQUEST_DOWNLOAD_BOOK:
                    response = (Network.Response<File>) response;
                    if(response.responseCode == RESPONSE_OK) {
                        File book = (File)response.responseData;
                        try {
                            files.unpackBook((File) response.responseData);
                            ArrayList<Book> added = new ArrayList<>();
                            String filename = book.getName();
                            String bookName = filename.substring(0, filename.length()-4);
                            added.add(new Book(bookName, files, display));
                            adapter.mFragmentList.get(TAB_POS_DOWNLOADED).addBooks(added);
                        } catch (IOException e) {
                            exceptionHandler.handleIOException(e);
                        }
                    }

                case REQUEST_GET_PUSHED_BOOKS:
                    response = (Network.Response<String>) response;
                    Set<String> downloaded = files.getBooks(AndroidFiles.APP_DATA_DIR);
                    SharedPreferences deleted = getSharedPreferences(PREFS_REMOVED_BOOKS, MODE_PRIVATE);
                    try {
                        JSONArray json = new JSONArray((String) response.responseData);
                        for(int i=0; i<json.length(); i++) {
                            String name = json.getJSONObject(i).getString("id").trim();
                            if(!downloaded.contains(name) && !deleted.contains(name))
                                onDownloadBook(null); //this is kinda sneaky, but in case I don't finish everything by Jan15
                        }
                    } catch (JSONException e) {
                        exceptionHandler.handleJsonException();
                    }
            }
    }

    @Override
    public void onExceptionThrown(int i, Throwable throwable) {
        if(throwable instanceof IOException)
            exceptionHandler.handleIOException((IOException) throwable);
        else if(throwable instanceof Exception)
            exceptionHandler.handleUnknownNetworkException((Exception)throwable);
        else if(throwable instanceof Error)
            throw (Error)throwable;
    }

    @Override
    public void onInvokeSearch(String title, String genres, String order) {
        Intent i = new Intent(this, BookListActivity.class);
        i.putExtra(BookListActivity.EXTRA_FRAGMENT_TYPE, BookListFragment.TYPE_SEARCH_RESULTS);
        i.putExtra(BookListActivity.EXTRA_SEARCH_TITLE, title);
        i.putExtra(BookListActivity.EXTRA_SEARCH_GENRES, genres);
        i.putExtra(BookListActivity.EXTRA_SEARCH_ORDERBY, order);
        startActivity(i);
    }

    @Override
    public void onDownloadBook(DialogFragment dialog) {
        String bookName = dialog.getTag();
        File saveTo = new File(getFilesDir(), "download/" + bookName + ".zip");
        if(!saveTo.getParentFile().isDirectory()) saveTo.getParentFile().mkdirs();
        saveTo.deleteOnExit();
        Books.getBookContent(REQUEST_DOWNLOAD_BOOK, this, bookName, saveTo, exceptionHandler, this);
        Toast.makeText(this, R.string.toast_downloading_book, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if(adapter == null || adapter.mFragmentList.isEmpty() || tabLayout == null || tabLayout.getTabCount() < TAB_COUNT)
            super.onBackPressed();
        else switch (adapter.mFragmentList.get(tabLayout.getSelectedTabPosition()).getType()) {
            case TYPE_DOWNLOADED:
                moveTaskToBack(true);
                break;
            case TYPE_FORKED_CREATED:
            case TYPE_EXPLORE:
                tabLayout.getTabAt(DEFAULT_TAB_POS).select();
                break;
            default: super.onBackPressed();
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<BookListFragment> mFragmentList      = new ArrayList<>();
        private final List<String>   mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(BookListFragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
