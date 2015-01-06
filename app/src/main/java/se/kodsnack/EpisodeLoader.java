package se.kodsnack;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import se.kodsnack.util.AtomParser;
import se.kodsnack.util.Episode;

/**
 * Subclass of {@link android.support.v4.content.AsyncTaskLoader} that
 * fetches and parses the Kodsnack Atom feed in the background.
 *
 * @author Erik Jansson<erikjansson90@gmail.com>
 */
public class EpisodeLoader extends AsyncTaskLoader<List<Episode>> {
    /** Logger tag. */
    private static final String TAG = EpisodeLoader.class.getSimpleName();

    private List<Episode> episodes; // The list of episodes from the Atom feed.

    public EpisodeLoader(Context context) {
        super(context);
    }

    @Override
    public List<Episode> loadInBackground() {
        // Check if we have already fetched the episodes.
        if (episodes != null) {
            return episodes;
        }

        try {
            episodes = AtomParser.parse("http://feedpress.me/kodsnack");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (XmlPullParserException | ParseException e) {
            e.printStackTrace();
        }

        return episodes;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}
