package se.kodsnack;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ListenActivity extends Activity implements PlayerService.PlayerCallback {
    /* Logger tag. */
    private static final String TAG = ListenActivity.class.getSimpleName();

    private ProgressBar     progressBar;    // Progress bar in the UI.
    private TextView        statusText;     // Status text in the UI.
    private ImageView       imageLogo;      // The big logo in the UI.
    private PlayerService   playerService;  // The service actually playing the stream.

    /**
     * The connection to the PlayerService.
     */
    private final ServiceConnection playerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            playerService = ((PlayerService.LocalBinder) service).getService();
            playerService.registerPlayerCallback(ListenActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            // TODO: Some error handling?
            playerService.unregisterPlayerCallback(ListenActivity.this);
            playerService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start PlayerService ...
        final Intent i = new Intent(this, PlayerService.class);
        startService(i);
        // ... and bind to it.
        bindService(i, playerConnection, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_listen);

        // Find views.
        progressBar = (ProgressBar) findViewById(R.id.loading_progressbar);
        statusText = (TextView) findViewById(R.id.offline_text);
        imageLogo = (ImageView) findViewById(R.id.logo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerService != null) {
            playerService.unregisterPlayerCallback(this);
            unbindService(playerConnection);
            playerService = null;
        }
    }

    /**
     * Hide progress bar and show status text when there is no stream.
     *
     * @param textId The resource id of the text to show.
     */
    private void showStatus(int textId) {
        progressBar.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(textId);
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.GONE);
    }

    /**
     * Updates the large image logo in the activity based on the title.
     *
     * @param title The title of the current stream.
     */
    private void updateUI(String title) {
        if (title.toLowerCase().contains("appsnack")) {
            imageLogo.setImageResource(R.drawable.appsnack_large);
        } else {
            imageLogo.setImageResource(R.drawable.kodsnack_large);
        }
    }

    /** CALLBACKS FROM SERVICE **/

    @Override
    public void onPrepared() {
        // Show text that show is live.
        showStatus(R.string.live);
    }

    @Override
    public void onPlaying() { }

    @Override
    public void onPaused() { }

    @Override
    public void onStopped() {
        showStatus(R.string.offline);
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
        showStatus(R.string.offline);
    }

    @Override
    public void updateTitle(String title) {
        updateUI(title);
    }
}
