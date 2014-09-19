package se.kodsnack.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import se.kodsnack.R;

public class PlayPauseButton extends ImageButton {
    private boolean         isPlaying;      // Keeps track of the button's state.
    private Drawable        playDrawable;   // Drawable for the playing state.
    private Drawable        pauseDrawable;  // Drawable for the paused state.
    private OnClickListener listener;       // Listener to call on click.
    private ColorFilter     tint;           // Blue color filter to apply to play/pause drawable.

    public PlayPauseButton(Context context) {
        super(context);
        init(context, null);
    }

    public PlayPauseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PlayPauseButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PlayPauseButton,
                                                                 0, 0);
        try {
            playDrawable  = a.getDrawable(R.styleable.PlayPauseButton_playDrawable);
            pauseDrawable = a.getDrawable(R.styleable.PlayPauseButton_pauseDrawable);
            int tintColor = a.getColor(R.styleable.PlayPauseButton_tint, R.color.primary_foreground);
            tint          = new PorterDuffColorFilter(tintColor, PorterDuff.Mode.MULTIPLY);
        } finally {
            a.recycle();
        }

        // Check that the required attributes were set in XML.
        if (playDrawable == null || pauseDrawable == null || tint == null) {
            throw new IllegalArgumentException("PlayPauseButton needs playDrawable, pauseDrawable"
                                               + " and tint attributes!");
        }

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPlaying = !isPlaying;
                update();
                if (listener != null) {
                    listener.onClick(isPlaying);
                }
            }
        });
        update();
    }

    /**
     * Updates the UI which will be reflected in the next onDraw.
     */
    private void update() {
        ColorFilter filter = null;
        if (isEnabled()) {
            filter = tint;
        }
        pauseDrawable.setColorFilter(filter);
        playDrawable.setColorFilter(filter);

        if (isPlaying) {
            setImageDrawable(pauseDrawable);
        } else {
            setImageDrawable(playDrawable);
        }
        invalidate();
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    public void setPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
        update();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        update();
    }

    /**
     * Custom OnClickListener to inform the listener about the button's state.
     */
    public interface OnClickListener {
        public void onClick(boolean playing);
    }
}
