package se.kodsnack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import se.kodsnack.util.Episode;

/**
 * Subclass of {@link android.widget.ArrayAdapter} that holds a list of
 * Kodsnack's episodes.
 *
 * @author Erik Jansson<erikjansson90@gmail.com>
 */
public class EpisodeListAdapter extends ArrayAdapter<Episode> {
    private LayoutInflater inflater;

    public EpisodeListAdapter(Context context) {
        super(context, R.layout.episode_item);
        inflater = LayoutInflater.from(context);
    }

    /**
     * Replace the data in this adapter.
     *
     * @param data The new list of episodes.
     */
    public void setData(List<Episode> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final LinearLayout view;
        final Episode episode;
        final ViewHolder viewHolder;

        /* Check if we need to inflate a new view of can recycle and old one. */
        if (convertView == null) {
            viewHolder = new ViewHolder();
            view = (LinearLayout) inflater.inflate(R.layout.episode_item, parent, false);

            viewHolder.title = (TextView) view.findViewById(R.id.title);

            view.setTag(viewHolder);
        } else {
            view = (LinearLayout) convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        episode = getItem(position);
        viewHolder.title.setText(episode.name);

        return view;
    }

    /**
     * Private view holder class that caches the findViewById() result since
     * it's expensive to perform view lookups every time a new list element
     * is cycled in.
     */
    private class ViewHolder {
        private TextView title;
    }
}
