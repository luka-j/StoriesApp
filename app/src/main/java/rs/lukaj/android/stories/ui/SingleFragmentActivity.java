package rs.lukaj.android.stories.ui;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import rs.lukaj.android.stories.R;


/**
 * Created by Luka on 7/1/2015.
 */
public abstract class SingleFragmentActivity<T extends Fragment> extends AppCompatActivity {

    private Toolbar toolbar;
    private T fragment;

    protected abstract T createFragment();

    @LayoutRes
    protected int getLayoutResId() {
        return R.layout.activity_fragment;
    }

    protected boolean shouldCreateFragment() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        if (shouldCreateFragment()) {
            toolbar = findViewById(R.id.toolbar);
            if(toolbar != null)
                setSupportActionBar(toolbar);
            if (NavUtils.getParentActivityIntent(this) != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            FragmentManager fm = getSupportFragmentManager();
            fragment = (T)fm.findFragmentById(R.id.fragment_container);

            if (fragment == null) {
                fragment = createFragment();
                fm.beginTransaction()
                  .add(R.id.fragment_container, fragment)
                  .commit();
            }
        }
    }

    protected T getFragment() {
        return fragment;
    }
}

