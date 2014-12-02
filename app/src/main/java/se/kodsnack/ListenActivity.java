package se.kodsnack;

import android.app.Activity;
import android.os.Bundle;

public class ListenActivity extends Activity {
    /* Logger tag. */
    private static final String TAG = ListenActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen);
    }

}
