package se.kodsnack.util;


/**
 * Class representing an episode in Kodsnack's feed.
 *
 * @author Erik Jansson<erikjansson90@gmail.com>
 */
public class Episode {
    /**
     * Name of this episode.
     */
    public final String name;

    /**
     * The URL to the media file of this episode.
     */
    public final String url;

    /**
     * A short description of this episode.
     */
    public final String desc;

    /**
     * The duration of this episode (in hh:mm).
     */
    public final String duration;

    /**
     * The date which this episode was published on.
     */
    public final long publishedDate;

    public Episode(String name, String url, String desc, String duration, long published) {
        this.name          = name;
        this.url           = url;
        this.desc          = desc;
        this.duration      = duration;
        this.publishedDate = published;
    }

    public String toString() {
        return name + " (" + url + ")";
    }
}
