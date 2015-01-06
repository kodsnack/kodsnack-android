package se.kodsnack;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;

import se.kodsnack.util.Episode;

/**
 * Subclass of {@link android.support.v4.app.ListFragment} that
 * displays {@link Episode}s from Kodsnack's Atom feed.
 *
 * @author Erik Jansson<erikjansson90@gmail.com>
 */
public class EpisodeListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<Episode>> {
    private EpisodeListAdapter episodeAdapter; // List adapter that holds the data.
    private PlayerService      playerService;  // The service playing the stream.

    /**
     * The connection to the PlayerService.
     */
    private final ServiceConnection playerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            playerService = ((PlayerService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            playerService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        episodeAdapter = new EpisodeListAdapter(getActivity());
        setListAdapter(episodeAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episode_list, container, false);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Episode episode = episodeAdapter.getItem(position);
        playerService.prepareMedia(episode.url);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Start PlayerService ...
        final Intent i = new Intent(activity, PlayerService.class);
        activity.startService(i);
        // ... and bind to it.
        activity.bindService(i, playerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (playerService != null) {
            getActivity().unbindService(playerConnection);
            playerService = null;
        }
    }

    /**
     * Returns the title of this fragment.
     *
     * @return The title of this fragment.
     */
    public String getTitle() {
        return "Avsnitt";
    }

    @Override
    public Loader<List<Episode>> onCreateLoader(int id, Bundle args) {
        return new EpisodeLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<Episode>> loader, List<Episode> data) {
        episodeAdapter.setData(data);
        episodeAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<Episode>> loader) { }
}
