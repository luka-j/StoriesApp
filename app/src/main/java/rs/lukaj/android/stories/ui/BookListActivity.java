package rs.lukaj.android.stories.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;

public class BookListActivity extends SingleFragmentActivity implements InputDialog.Callbacks {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_book_list;
    }

    @Override
    protected Fragment createFragment() {
        return new BookListFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("DENSITY", String.valueOf(getResources().getDisplayMetrics().density));

        if (ContextCompat.checkSelfPermission(this,
                                              Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                                              new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                              0);
        } //todo write callback

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> InputDialog.newInstance(R.string.new_book, getString(R.string.new_book_title),
                                                               R.string.create, 0, "", "",
                                                               true)
                                          .show(getFragmentManager(), ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFinishedInput(DialogFragment dialog, String s) {
        Intent i = new Intent(BookListActivity.this, BookEditorActivity.class);
        i.putExtra(BookEditorActivity.EXTRA_BOOK_NAME, s);
        startActivity(i);
    }
}
