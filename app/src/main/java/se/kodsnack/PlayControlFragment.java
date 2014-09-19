package se.kodsnack;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import se.kodsnack.ui.PlayPauseButton;

/**
 * A {@link Fragment} subclass that handles the play control at the bottom
 * of the screen.
 */
public class PlayControlFragment extends Fragment implements PlayerService.PlayerCallback,
        PlayPauseButton.OnClickListener {
    /* Logger tag. */
    private static final String TAG = PlayControlFragment.class.getSimpleName();

    private ImageView       logo;               // Image of playing stream.
    private PlayPauseButton playPauseButton;    // The play/pause button.
    private TextView        titleText;          // Title of the stream.
    private PlayerService   playerService;      // Service actually playing the stream.

    /**
     * The connection to the PlayerService.
     */
    private final ServiceConnection playerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            playerService = ((PlayerService.LocalBinder) service).getService();
            playerService.registerPlayerCallback(PlayControlFragment.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            // TODO: Some error handling?
            playerService = null;
        }
    };

    public PlayControlFragment() {
        // Required empty public constructor.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Bind to player service.
        final Activity  activity    = getActivity();
        final Intent    i           = new Intent(activity, PlayerService.class);
        activity.bindService(i, playerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root       = inflater.inflate(R.layout.fragment_play_controls, container, false);

        // Find views.
        logo            = (ImageView)       root.findViewById(R.id.small_logo);
        playPauseButton = (PlayPauseButton) root.findViewById(R.id.play_pause_button);
        titleText       = (TextView)        root.findViewById(R.id.stream_title);

        // Register OnClickListener.
        playPauseButton.setOnClickListener(this);
        // Initialize play/pause button to be disabled (until we have a stream to play).
        playPauseButton.setEnabled(false);

        return root;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (playerService != null) {
            playerService.unregisterPlayerCallback(this);
            getActivity().unbindService(playerConnection);
        }
    }

    /* Callbacks from player service. */

    @Override
    public void onPrepared() {
        // Enable play button when prepared.
        playPauseButton.setEnabled(true);
    }

    @Override
    public void onPlaying() {
        playPauseButton.setPlaying(true);
    }

    @Override
    public void onPaused() {
        playPauseButton.setPlaying(false);
    }

    @Override
    public void onStopped() {
        playPauseButton.setPlaying(false);
        playPauseButton.setEnabled(false);
    }

    @Override
    public void onBuffering() {
        playPauseButton.setPlaying(false);
    }

    @Override
    public void onError(Throwable t) {
        playPauseButton.setPlaying(false);
        playPauseButton.setEnabled(false);
        // TODO?
    }

    @Override
    public void updateTitle(String title) {
        titleText.setText(title);
        // Replace logo with appsnack's if title says so.
        title = title.toLowerCase();
        int res = title.contains("appsnack") ? R.drawable.appsnack : R.drawable.kodsnack;
        logo.setImageResource(res);
    }

    @Override
    public void onClick(boolean playing) {
        playerService.togglePlaying();
    }
}
