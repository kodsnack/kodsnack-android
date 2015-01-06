package se.kodsnack;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

/**
 * The main activity that holds the {@link android.support.v4.view.ViewPager}
 * with the two fragments in it.
 *
 * @author Erik Jansson<erikjansson90@gmail.com>
 */
public class ListenActivity extends FragmentActivity {
    /* Logger tag. */
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = ListenActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerTabStrip tabs = (PagerTabStrip) findViewById(R.id.pager_tab_strip);

        viewPager.setAdapter(new KodsnackPagerAdapter(getSupportFragmentManager()));
        tabs.setTabIndicatorColorResource(R.color.alternate_foreground);
    }
}
