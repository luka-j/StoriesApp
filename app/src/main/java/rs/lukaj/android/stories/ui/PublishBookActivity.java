package rs.lukaj.android.stories.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.io.File;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.io.BookIO;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.minnetwork.Network;

/**
 * Created by luka on 29.12.17..
 */

public class PublishBookActivity extends AppCompatActivity implements Network.NetworkCallbacks<String> {

    public static final  String EXTRA_BOOK   = "extra.bookid";
    private static final int    INTENT_IMAGE = 0;
    private static final String TAG          = "PublishBookActivity";
    private static final int REQUEST_PUBLISH = 1;

    private EditText             title;
    private TextInputLayout      titleTil;
    private EditText             genres;
    private TextInputLayout      genresTil;
    private CardView             publishBtn;
    private ImageView            coverImage;
    private CheckBox             privateBox;
    private CircularProgressView progressView;
    private File imageFile;
    private Book book;
    private String bookId;
    private boolean         editing = false;

    private ExceptionHandler exceptionHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initExceptionHandler();
        initData();

        setContentView(R.layout.activity_publish_book);
        initToolbar();
        initViews();

        if (editing) {
            getSupportActionBar().setTitle(book.getTitle());
            setupViewsForEditing();
        }

        publishBtn.setOnClickListener(v -> submit());
        initMediaListeners();
    }


    private void initExceptionHandler() {
        exceptionHandler = new ExceptionHandler.DefaultHandler(this) {
            @Override
            public void finishedSuccessfully() {
                super.finishedSuccessfully();
                PublishBookActivity.this.onBackPressed();
            }
            @Override
            public void finishedUnsuccessfully() {
                super.finishedUnsuccessfully();
                progressView.setVisibility(View.GONE);
                publishBtn.setVisibility(View.VISIBLE);
            }
        };
    }

    private void initData() {
        bookId = getIntent().getStringExtra(EXTRA_BOOK);
        book = new Book(bookId, this);
    }


    private void submit() {
        boolean error = false;
        String titleText = title.getText().toString(),
                genresText = genres.getText().toString();
        if (titleText.isEmpty()) {
            titleTil.setError(getString(R.string.error_empty));
            error = true;
        } else if (titleText.length() >= Limits.BOOK_TITLE_MAX_LENGTH) {
            titleTil.setError(getString(R.string.error_too_long));
            error = true;
        } else { titleTil.setError(null); }
        if (genresText.length() >= Limits.GENRES_MAX_LENGTH) {
            genresTil.setError(getString(R.string.error_too_long));
            error = true;
        } else if(Utils.occurencesOf(genresText, ',') > Limits.MAX_GENRES_COUNT) {
            genresTil.setError(getString(R.string.error_too_many_genres));
            error = true;
        } else { genresTil.setError(null); }
        if (!error) {
            if (editing) {
                //todo edit book
            } else {
                BookIO.publishBook(REQUEST_PUBLISH, this, book, titleText, genresText, imageFile, exceptionHandler, this);
            }
            publishBtn.setVisibility(View.GONE);
            progressView.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        title = findViewById(R.id.publish_book_title_input);
        titleTil =  findViewById(R.id.publish_book_title_til);
        genres =  findViewById(R.id.publish_book_genres_input);
        genresTil =  findViewById(R.id.publish_book_genres_til);
        publishBtn =  findViewById(R.id.button_add);
        progressView =  findViewById(R.id.publish_book_cpv);
        coverImage =  findViewById(R.id.add_course_image);
        privateBox =  findViewById(R.id.publish_book_forkable_checkbox);
    }


    private void initMediaListeners() {
        coverImage.setOnClickListener(v -> {
            Intent camera  = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent gallery = new Intent(Intent.ACTION_PICK);
            gallery.setType("image/*");
            camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
            Intent chooserIntent = Intent.createChooser(camera,
                                                        getString(R.string.select_image));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{gallery});
            startActivityForResult(chooserIntent, INTENT_IMAGE);
        });
    }

    private void setupViewsForEditing() {
        title.setText(book.getTitle());
        genres.setText(Utils.listToString(book.getGenres()));
        if (book.hasCover())
            coverImage.setImageBitmap(Utils.loadImage(book.getCover(), coverImage.getWidth()));
        title.setSelection(title.getText().length());
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (NavUtils.getParentActivityIntent(this) != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onRequestCompleted(int i, Network.Response<String> response) {

    }

    @Override
    public void onExceptionThrown(int i, Throwable throwable) {

    }
}
