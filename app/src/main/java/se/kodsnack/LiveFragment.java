package se.kodsnack;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 */
public class LiveFragment extends Fragment implements PlayerService.PlayerCallback {
    private static final String TAG = LiveFragment.class.getSimpleName();

    private ProgressBar     progressBar;    // Progress bar in the UI.
    private TextView        statusText;     // Status text in the UI.
    private ImageView       imageLogo;      // The big logo in the UI.
    private PlayerService   playerService;  // The service actually playing the stream.
    private int             numListeners;   // The number of listeners to the stream.

    public LiveFragment() {
        // Required empty public constructor

        numListeners = -1;
    }

    /**
     * The connection to the PlayerService.
     */
    private final ServiceConnection playerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            playerService = ((PlayerService.LocalBinder) service).getService();
            playerService.registerPlayerCallback(LiveFragment.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            // TODO: Some error handling?
            playerService.unregisterPlayerCallback(LiveFragment.this);
            playerService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_live, container, false);

        // Find views.
        progressBar = (ProgressBar) root.findViewById(R.id.loading_progressbar);
        statusText = (TextView) root.findViewById(R.id.offline_text);
        imageLogo = (ImageView) root.findViewById(R.id.logo);

        return root;
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

    public void onDestroy() {
        super.onDestroy();
        if (playerService != null) {
            playerService.unregisterPlayerCallback(this);
            getActivity().unbindService(playerConnection);
            playerService = null;
        }
    }

    /**
     * Hide progress bar and show status text when there is no stream.
     *
     * @param text The status text to show.
     */
    private void showStatus(String text) {
        progressBar.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(text);
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.GONE);
    }

    private void updateStatusText(String status) {
        if (numListeners != -1) {
            status += "\n" + getString(R.string.num_listeners) + numListeners;
        }

        showStatus(status);
    }

    /** CALLBACKS FROM SERVICE **/

    @Override
    public void onPrepared() {
        // Show text that show is live.
        updateStatusText(getString(R.string.live));
    }

    @Override
    public void onPlaying() { }

    @Override
    public void onPaused() { }

    @Override
    public void onStopped() {
        numListeners = -1;
        updateStatusText(getString(R.string.offline));
    }

    @Override
    public void onBuffering() {
        showProgress();
    }

    @Override
    public void onError(Throwable t) {
        if (t != null) {
            Log.e(TAG, t.toString());
        }
        updateStatusText(getString(R.string.offline));
    }

    @Override
    public void onJsonInfo(JSONObject info) {
        try {
            JSONObject icestats = info.getJSONObject("icestats");
            if (icestats.has("source")) {
                JSONObject source = icestats.getJSONObject("source");

                // Try to find a title or fall back to the server name as title.
                String title = source.has("title") ? source.getString("title")
                        : source.getString("server_name");
                numListeners = source.has("listeners") ? source.getInt("listeners") : -1;
                updateStatusText(getString(R.string.live));

                // Update UI according to JSON info.
                if (title.toLowerCase().contains("appsnack")) {
                    imageLogo.setImageResource(R.drawable.appsnack_large);
                } else {
                    imageLogo.setImageResource(R.drawable.kodsnack_large);
                }
            }
        } catch (JSONException e) {
            numListeners = -1;
            updateStatusText(getString(R.string.offline));
            Log.w(TAG, e.toString());
        }
    }
}
