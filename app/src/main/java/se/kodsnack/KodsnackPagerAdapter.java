package se.kodsnack;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Subclass of {@link android.support.v4.app.FragmentPagerAdapter} that keeps
 * track of one {@link LiveFragment} and one {@link EpisodeListFragment} in a
 * {@link android.support.v4.view.ViewPager}.
 *
 * @author Erik Jansson<erikjansson90@gmail.com>
 */
public class KodsnackPagerAdapter extends FragmentPagerAdapter {
    private static final int NUM_FRAGMENTS = 2;

    private final Fragment[] fragments;         // List of fragments this adapter holds.
    private final String[]   fragmentTitles;    // Titles of the fragments'.

    public KodsnackPagerAdapter(FragmentManager fm) {
        super(fm);
        final LiveFragment liveFragment = new LiveFragment();
        final EpisodeListFragment listFragment = new EpisodeListFragment();

        fragments = new Fragment[NUM_FRAGMENTS];
        fragments[0] = liveFragment;
        fragments[1] = listFragment;

        fragmentTitles = new String[NUM_FRAGMENTS];
        fragmentTitles[0] = liveFragment.getTitle();
        fragmentTitles[1] = listFragment.getTitle();
    }

    @Override
    public Fragment getItem(int i) {
        if (i < NUM_FRAGMENTS) {
            return fragments[i];
        } else {
            throw new IllegalArgumentException("Fragment index too big.");
        }
    }

    @Override
    public int getCount() {
        return NUM_FRAGMENTS;
    }

    @Override
    public String getPageTitle(int i) {
        if (i < NUM_FRAGMENTS) {
            return fragmentTitles[i];
        } else {
            throw new IllegalArgumentException("Fragment index too big.");
        }
    }
}
