package se.kodsnack;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener,
        Response.Listener<JSONObject>, Response.ErrorListener {
    /* Logger tag. */
    private static final String TAG = PlayerService.class.getSimpleName();

    /**
     * ID of the notification shown while playing.
     */
    private static final int NOTIFICATION_ID = 0xbada55;

    // Actions when service is invoked from the notification.
    private static final String ACTION_STOP = "se.kodsnack.STOP";
    private static final String ACTION_TOGGLE_PLAYING = "se.kodsnack.TOGGLE_PLAYING";

    private MediaPlayer          mediaPlayer;         // MediaPlayer that plays the live stream.
    private boolean              isRunning;           // Whether this service is running or not.
    private boolean              isPreparing;         // Whether MediaPlayer is preparing a stream.
    private boolean              isPrepared;          // Whether MediaPlayer is done preparing.
    private LocalBinder          binder;              // For communication with clients.
    private List<PlayerCallback> callbacks;           // List of callbacks.
    private Handler              statusHandler;       // Handler for periodically fetching JSON.
    private RequestQueue         requestQueue;        // Request queue for network requests.
    private int                  updateFrequency;     // Handler's update frequency.
    private String               streamTitle;         // Title of the current stream.
    private RemoteViews          remoteViews;         // Views in the notification.
    private Notification.Builder notificationBuilder; // Builder for the notification.
    private NotificationManager  notificationManager; // Manager for displaying notification.
    private Request              statusRequest;       // Request for fetching current stream status.

    @Override
    public void onCreate() {
        mediaPlayer         = new MediaPlayer();
        binder              = new LocalBinder();
        callbacks           = new ArrayList<PlayerCallback>();
        statusHandler       = new Handler();
        requestQueue        = Volley.newRequestQueue(this);
        updateFrequency     = 0; // Do first request immediately.
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        statusRequest       = new JsonObjectRequest(getString(R.string.kodsnack_status_url), null,
                                                    this, this);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        // Check whether we were started with any action (from the notification).
        if (i != null && i.getAction() != null) {
            String action = i.getAction();
            if (action.equals(ACTION_STOP)) {
                stop();
                notificationManager.cancel(NOTIFICATION_ID);
                stopForeground(true);
                stopSelf();
            } else if (action.equals(ACTION_TOGGLE_PLAYING)) {
                togglePlaying();
            } else {
                Log.e(TAG, "Unknown action.");
            }
        }

        // If we're already running, don't do anything.
        if (isRunning) {
            return START_STICKY;
        }
        isRunning = true;

        // Intent for opening the activity when clicking the notification.
        Intent open = new Intent(this, ListenActivity.class);
        PendingIntent openActivity = PendingIntent.getActivity(this, 0, open,
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop = new Intent(this, PlayerService.class);
        stop.setAction(ACTION_STOP);
        // Intent for stopping the service when clicking the X button in the notification.
        PendingIntent pendingStop = PendingIntent.getService(this, 0, stop,
                                                             PendingIntent.FLAG_ONE_SHOT);
        Intent playPause = new Intent(this, PlayerService.class);
        playPause.setAction(ACTION_TOGGLE_PLAYING);
        // Intent for playing/pausing the stream when clicking that button in the notification.
        PendingIntent pendingToggle = PendingIntent.getService(this, 0, playPause,
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
        // Create custom notification and set the intents for it.
        remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        remoteViews.setOnClickPendingIntent(R.id.kill_button, pendingStop);
        remoteViews.setOnClickPendingIntent(R.id.play_pause_button, pendingToggle);
        notificationBuilder = new Notification.Builder(this)
                .setContent(remoteViews)
                .setOngoing(true)
                .setSmallIcon(R.drawable.kodsnack)
                .setContentIntent(openActivity);

        // Initialize the media player (except for preparing the stream).
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent i) {
        callbacks.clear();
        updateFrequency = 15000;
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            for (PlayerCallback callback : callbacks) {
                callback.onStopped();
            }
        }
        isRunning = false;
    }

    /**
     * Updates the persistent notification with the correct stream title
     * and button drawables.
     */
    private void updateNotification() {
        int res = R.drawable.play;
        if (mediaPlayer.isPlaying()) {
            res = R.drawable.pause;
        }
        remoteViews.setImageViewResource(R.id.play_pause_button, res);
        remoteViews.setTextViewText(R.id.subtitle, streamTitle);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Fetches the JSON status periodically.
     */
    private void fetchStatus() {
        statusHandler.postDelayed(new Runnable() {
            public void run() {
                // We only want to fetch again if we're running and either have some callbacks (i.e.
                // an fragment/activity is active) or we're currently playing.
                if (isRunning && (!callbacks.isEmpty() || mediaPlayer.isPlaying())) {
                    requestQueue.add(statusRequest);
                    statusHandler.postDelayed(this, updateFrequency);
                }
            }
        }, updateFrequency);
    }

    /**
     * Prepares the media player with the provided URL to a stream.
     *
     * @param url The URL to the stream.
     */
    public void prepareMedia(String url) {
        stop();
        prepare(url);
    }

    private void prepare(String url) {
        if (isRunning && !isPreparing && !isPrepared) {
            for (PlayerCallback callback : callbacks) {
                callback.onBuffering();
            }
            isPreparing = true;
            try {
                Log.d(TAG, "Preparing with: " + url);
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                // TODO: Error handling.
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * Toggle the playing state of the media player between play and pause.
     *
     * NOTE: This can *not* be called before the stream is prepared. Thus, a
     * client should wait for the onPrepared() callback before attempting to
     * call this.
     */
    public void togglePlaying() {
        if (mediaPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Starts the stream.
     *
     * NOTE: This can *not* be called before the stream is prepared!
     */
    private void play() {
        if (isPrepared) {
            mediaPlayer.start();
            isPreparing = false;

            for (PlayerCallback callback : callbacks) {
                callback.onPlaying();
            }
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
            updateNotification();
        }
    }

    /**
     * Pauses the stream.
     *
     * NOTE: This can *not* be called before the stream is prepared!
     */
    private void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();

            for (PlayerCallback callback : callbacks) {
                callback.onPaused();
            }
            updateNotification();
        }
    }

    /**
     * Stops the stream.
     *
     * NOTE: This can only be called when the stream is playing.
     */
    public void stop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            // TODO: Should we reset here?
            mediaPlayer.reset();
            isPrepared = false;

            for (PlayerCallback callback : callbacks) {
                callback.onStopped();
            }
            updateNotification();
        }
    }

    /**
     * Register a callback to be called when events happen.
     *
     * @param callback The callback to register.
     */
    public void registerPlayerCallback(PlayerCallback callback) {
        // Shorter update period when we have someone interested in updates.
        updateFrequency = 3000;
        // If this is the first callback, start fetching JSON again.
        if (callbacks.isEmpty()) {
            fetchStatus();
        }
        callbacks.add(callback);
        if (isPrepared) {
            callback.onPrepared();
        }
        if (mediaPlayer.isPlaying()) {
            callback.onPlaying();
        } else {
            callback.onPaused();
        }
    }

    /**
     * Remove a callback.
     *
     * @param callback The callback to remove.
     */
    public void unregisterPlayerCallback(PlayerCallback callback) {
        callbacks.remove(callback);
        if (callbacks.isEmpty()) {
            // TODO: Even more infrequent updates perhaps?
            updateFrequency = 15000;
        }
    }

    /* Callback received when we get a response from the JSON status request. */
    @Override
    public void onResponse(JSONObject status) {
        Log.d(TAG, status.toString());
        for (PlayerCallback callback : callbacks) {
            callback.onJsonInfo(status);
        }

        try {
            JSONObject icestats = status.getJSONObject("icestats");
            if (icestats.has("source")) {
                JSONObject source = icestats.getJSONObject("source");

                // Try to find a title or fall back to the server name as title.
                streamTitle = source.has("title") ? source.getString("title")
                                                   : source.getString("server_name");
                // Prepare MediaPlayer with stream URL.
                prepare(source.getString("listenurl"));
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            // TODO: We can't stop on error if we're not live streaming.
//            stop();
            for (PlayerCallback callback : callbacks) {
                callback.onError(e);
            }
        }
    }

    /* Callback received when the request for the JSON status failed. */
    @Override
    public void onErrorResponse(VolleyError volleyError) {
        Log.e(TAG, volleyError.toString());
        // <DEBUG> (with German thunk music...)
//        streamTitle = "Debug Title";
//        prepare("http://87.230.101.78:80/top100station.mp3");
        // </DEBUG>
        for (PlayerCallback callback : callbacks) {
            callback.onError(volleyError.getCause());
        }
    }

    /* Callback received when the MediaPlayer is done preparing (i.e. buffering) media. */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        isPreparing = false;
        isPrepared = true;
        for (PlayerCallback callback : callbacks) {
            callback.onPrepared();
        }
    }

    /* Callback from the MediaPlayer when it has an error inform us about. */
    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        isPreparing = false;
        isPrepared = false;
        mediaPlayer.reset();
        mediaPlayer.release();

        for (PlayerCallback callback : callbacks) {
            // TODO: Better error handling.
            callback.onError(new Exception("Something went wrong."));
        }

        // Signal that the error was handled.
        return true;
    }

    /* Callback from the MediaPlayer when it has some info to inform us about. */
    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            for (PlayerCallback callback : callbacks) {
                callback.onBuffering();
            }
            return true;
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            for (PlayerCallback callback : callbacks) {
                callback.onPlaying();
            }
            return true;
        }

        return false;
    }

    /**
     * Interface that clients wishing to receive callbacks should implement.
     */
    public interface PlayerCallback {
        /**
         * Called when the MediaPlayer is done preparing some media.
         */
        public void onPrepared();

        /**
         * Called when the prepared media has started playing.
         */
        public void onPlaying();

        /**
         * Called when the MediaPlayer has been paused.
         */
        public void onPaused();

        /**
         * Called when the MediaPlayer has been stopped.
         */
        public void onStopped();

        /**
         * Called when the MediaPlayer needs to buffer.
         */
        public void onBuffering();

        /**
         * Called when an error occurred.
         */
        public void onError(Throwable t);

        /**
         * Called when a new JSON info object is received.
         */
        public void onJsonInfo(JSONObject info);
    }

    /**
     * Subclass of Binder that allows us to sidestep the verbose IPC. Works
     * since both the service and all clients live in the same process.
     */
    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }
}
