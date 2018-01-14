package rs.lukaj.android.stories.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.Utils;
import rs.lukaj.android.stories.controller.ExceptionHandler;
import rs.lukaj.android.stories.environment.AndroidFiles;
import rs.lukaj.android.stories.io.BookIO;
import rs.lukaj.android.stories.io.FileUtils;
import rs.lukaj.android.stories.io.Limits;
import rs.lukaj.android.stories.model.Book;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.minnetwork.Network;

/**
 * Created by luka on 29.12.17..
 */

public class PublishBookActivity extends AppCompatActivity implements Network.NetworkCallbacks<String>,
                                                                      FileUtils.Callbacks {

    public static final  String EXTRA_BOOK         = "extra.bookid";
    private static final int    INTENT_IMAGE       = 0;
    private static final String TAG                = "PublishBookActivity";
    private static final int REQUEST_PUBLISH       = 1;
    private static final String STATE_IMAGE_FILE_PATH = "publish.cover.path";
    private static final int REQUEST_COPY_COVER = 2;

    private EditText             title;
    private TextInputLayout      titleTil;
    private EditText             genres;
    private TextInputLayout      genresTil;
    private EditText             description;
    private TextInputLayout      descriptionTil;
    private CardView             publishBtn;
    private ImageView            coverImage;
    private CheckBox             opensourceBox;
    private CircularProgressView progressView;
    private File                 imageFile;
    private Book                 book;
    private String               bookId;
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
        imageFile = new AndroidFiles(this).getCover(bookId);
    }


    private void submit() {
        boolean error = false;
        String titleText = title.getText().toString(),
                genresText = genres.getText().toString(),
                descriptionText = description.getText().toString();
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
        } else if(Utils.occurrencesOf(genresText, ',') > Limits.MAX_GENRES_COUNT) {
            genresTil.setError(getString(R.string.error_too_many_genres));
            error = true;
        } else { genresTil.setError(null); }
        if(descriptionText.length() > Limits.BOOK_DESC_MAX_LENGTH) {
            descriptionTil.setError(getString(R.string.error_too_long));
            error = true;
        } else { descriptionTil.setError(null); }
        if (!error) {
            if (editing) {
                //todo edit book
            } else {

                BookIO.publishBook(REQUEST_PUBLISH, this, book, titleText, genresText, descriptionText,
                                   opensourceBox.isChecked(), exceptionHandler, this);
            }
            publishBtn.setVisibility(View.GONE);
            progressView.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        title = findViewById(R.id.publish_book_title_input);
        titleTil =  findViewById(R.id.publish_book_title_til);
        if(book.getTitle() != null) title.setText(book.getTitle());
        genres =  findViewById(R.id.publish_book_genres_input);
        genresTil =  findViewById(R.id.publish_book_genres_til);
        if(book.getGenres() != null) genres.setText(Utils.listToString(book.getGenres()));
        description = findViewById(R.id.publish_book_desc_input);
        descriptionTil = findViewById(R.id.publish_book_desc_til);
        if(book.getDescription() != null) description.setText(book.getDescription());
        publishBtn =  findViewById(R.id.button_add);
        progressView =  findViewById(R.id.publish_book_cpv);
        coverImage =  findViewById(R.id.add_course_image);
        if(imageFile.isFile())
            coverImage.setImageBitmap(BitmapUtils.loadImage(imageFile, getResources().getDimensionPixelSize(R.dimen.addview_image_width)));
        opensourceBox =  findViewById(R.id.publish_book_forkable_checkbox);
        opensourceBox.setChecked(true);
    }


    private void initMediaListeners() {
        coverImage.setOnClickListener(v -> {
            Intent camera  = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent gallery = new Intent(Intent.ACTION_PICK);
            gallery.setType("image/*");
            camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
            Intent chooserIntent = Intent.createChooser(camera,
                                                        getString(R.string.choose_cover));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{gallery});
            startActivityForResult(chooserIntent, INTENT_IMAGE);
        });
    }

    private void setupViewsForEditing() {
        title.setText(book.getTitle());
        genres.setText(Utils.listToString(book.getGenres()));
        if (book.hasCover())
            coverImage.setImageBitmap(BitmapUtils.loadImage(book.getCover(), getResources().getDimensionPixelSize(R.dimen.addview_image_width)));
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_IMAGE_FILE_PATH, imageFile.getAbsolutePath());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getString(STATE_IMAGE_FILE_PATH) != null) {
            imageFile = new File(savedInstanceState.getString(STATE_IMAGE_FILE_PATH));
            coverImage.setImageBitmap(BitmapUtils.loadImage(imageFile,
                                                            getResources().getDimensionPixelSize(R.dimen.addview_image_width)));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == INTENT_IMAGE) {
                if (data != null && data.getData() != null) { //ako je data==null, fotografija je napravljena kamerom, nije iz galerije
                    //u Marshmallow-u i kasnijim je data != null, ali je data.getData() == null
                    try {
                        FileUtils.copy(REQUEST_COPY_COVER, getContentResolver().openInputStream(data.getData()), imageFile, this);
                    } catch (FileNotFoundException ex) {
                        Log.e(TAG, "Cannot resolve selected cover", ex);
                        InfoDialog.newInstance(getString(R.string.error_cannot_resolve_uri_title),
                                               getString(R.string.error_cannot_resolve_uri_text))
                                  .show(getFragmentManager(), "publish.error.cannotresolveuri");
                    }
                } else {
                    coverImage.setImageBitmap(BitmapUtils.loadImage(imageFile,
                                                                    getResources().getDimensionPixelSize(R.dimen.addview_image_width)));
                }
            }
        }
    }



    @Override
    public void onRequestCompleted(int i, Network.Response<String> response) {
        exceptionHandler.finished();
        //todo save bookId; on second publish, replace it
    }

    @Override
    public void onExceptionThrown(int i, Throwable throwable) {
        if(throwable instanceof Exception)
            exceptionHandler.handleUnknownNetworkException((Exception)throwable);
        if(throwable instanceof Error)
            throw (Error)throwable;
    }

    @Override
    public void onFileOperationCompleted(int operationId) {
        if(operationId == REQUEST_COPY_COVER)
            coverImage.setImageBitmap(BitmapUtils.loadImage(imageFile,
                                                            getResources().getDimensionPixelSize(R.dimen.addview_image_width)));
    }

    @Override
    public void onIOException(int operationId, IOException ex) {
        Log.e(TAG, "Cannot resolve selected cover", ex);
        InfoDialog.newInstance(getString(R.string.error_cannot_resolve_uri_title),
                               getString(R.string.error_cannot_resolve_uri_text))
                  .show(getFragmentManager(), "publish.error.cannotresolveuri");
    }
}
