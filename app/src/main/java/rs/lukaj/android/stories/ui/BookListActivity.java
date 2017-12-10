package rs.lukaj.android.stories.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import rs.lukaj.android.stories.R;
import rs.lukaj.android.stories.ui.dialogs.InfoDialog;
import rs.lukaj.android.stories.ui.dialogs.InputDialog;

public class BookListActivity extends AppCompatActivity implements InputDialog.Callbacks {
    private static final int PERM_REQ_STORAGE = 0;

    private static final int TAB_POS_DOWNLOADED = 0;
    private static final int TAB_POS_MY_BOOKS   = 1;

    private static final String TAG_NEW_BOOK_TITLE = "diagNewBook";

    private Toolbar   toolbar;
    private FloatingActionButton fab;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        }


        viewPager = findViewById(R.id.book_viewpager);
        setupViewPager(viewPager);

        tabLayout = findViewById(R.id.book_tabs);
        tabLayout.setupWithViewPager(viewPager);
        setTabListener();

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            if(tabLayout.getSelectedTabPosition() == TAB_POS_MY_BOOKS)
                InputDialog.newInstance(R.string.new_book, getString(R.string.new_book_title),
                                        R.string.create, 0, "", "",
                                        true)
                           .show(getFragmentManager(), TAG_NEW_BOOK_TITLE);
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(BookListFragment.newInstance(BookListFragment.TYPE_DOWNLOADED),
                            getString(R.string.tab_downloaded_books));
        adapter.addFragment(BookListFragment.newInstance(BookListFragment.TYPE_FORKED_CREATED),
                            getString(R.string.tab_my_books));
        viewPager.setAdapter(adapter);
    }

    private void setTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getPosition() == TAB_POS_DOWNLOADED) {
                    fab.setImageResource(R.drawable.ic_file_download_white_24dp);
                    //fab.setVisibility(View.GONE);
                } else if(tab.getPosition() == TAB_POS_MY_BOOKS) {
                    fab.setImageResource(R.drawable.ic_add_white_24dp);
                }
            }
        });
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
                }
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList      = new ArrayList<>();
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

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
