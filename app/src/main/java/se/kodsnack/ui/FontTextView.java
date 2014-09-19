package se.kodsnack.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Subclass of TextView to use a custom font.
 *
 * @author Erik Jansson<erikjansson90@gmail.com>
 */
public class FontTextView extends TextView {

    public FontTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode())
            init();
    }

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode())
            init();
    }

    public FontTextView(Context context) {
        super(context);
        if (!isInEditMode())
            init();
    }

    private void init() {
        int style = 0;
        if (getTypeface() != null) {
            style = getTypeface().getStyle();
        }
        setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/vt323.ttf"), style);
    }
}
