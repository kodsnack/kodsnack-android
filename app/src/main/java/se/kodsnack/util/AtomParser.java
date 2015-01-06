package se.kodsnack.util;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Atom parser specifically for parsing Kodsnack's RSS feed.
 *
 * @author Erik Jansson<erikjansson90@gmail.com>
 */
public class AtomParser {

    private static final String TAG_FEED = "rss";
    private static final String TAG_CHANNEL = "channel";
    private static final String TAG_ITEM = "item";
    private static final String TAG_TITLE = "title";
    private static final String TAG_SUBTITLE = "itunes:subtitle";
    private static final String TAG_DURATION = "itunes:duration";
    private static final String TAG_PUBLISHED = "pubDate";
    private static final String TAG_ENCLOSURE = "enclosure";

    // We don't use XML namespaces
    private static final String ns = null;

    /**
     * Parse the Atom feed at a URL, returns a list of {@link Episode}s.
     *
     * @param url The URL to fetch the feed from.
     * @return List of {@link se.kodsnack.util.Episode} objects.
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources") // Ignore try-with-resources warn since it
                                                         // requires too high API level.
    public static List<Episode> parse(String url)
            throws XmlPullParserException, IOException, ParseException {
        final AtomParser  atomParser = new AtomParser();
        final InputStream in         = new URL(url).openStream();

        try {
            final XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return atomParser.readFeed(parser);
        } finally {
            in.close();
        }
    }

    /**
     * Decode the feed and return a list of the episodes in the feeds.
     *
     * @param parser Incoming XMl
     * @return List of {@link se.kodsnack.util.Episode} objects.
     */
    private List<Episode> readFeed(XmlPullParser parser)
            throws XmlPullParserException, IOException, ParseException {
        List<Episode> entries = new ArrayList<>();

        // Search for <feed> tags.
        parser.require(XmlPullParser.START_TAG, ns, TAG_FEED);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            // Look for the <channel> tag.
            if (parser.getName().equals(TAG_CHANNEL)) {
                entries = readChannel(parser);
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    /**
     * Parse the <channel></channel> parts of the Atom feed and return the
     * episodes.
     *
     * @param parser The parser to parse from.
     * @return A list of the episodes in the Atom.
     */
    private List<Episode> readChannel(XmlPullParser parser)
            throws XmlPullParserException, IOException, ParseException {
        List<Episode> episodes = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, TAG_CHANNEL);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            //Log.d("AtomParser", "Name: " + name);
            // Look for the <item> tag.
            if (parser.getName().equals(TAG_ITEM)) {
                episodes.add(readItem(parser));
            } else {
                skip(parser);
            }
        }

        return episodes;
    }

    /**
     * Parses the contents of an item.
     *
     * @param parser The parser.
     * @return A new Episode parsed from the feed.
     */
    private Episode readItem(XmlPullParser parser)
            throws XmlPullParserException, IOException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_ITEM);
        String title = null;
        String subtitle = null;
        String duration = null;
        String link = null;
        long publishedOn = 0;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case TAG_TITLE:
                    title = readBasicTag(parser, TAG_TITLE);
                    break;
                case TAG_SUBTITLE:
                    subtitle = readBasicTag(parser, TAG_SUBTITLE);
                    break;
                case TAG_ENCLOSURE:
                    String tempLink = readEnclosure(parser);
                    if (tempLink != null) {
                        link = tempLink;
                    }
                    break;
                case TAG_PUBLISHED:
                    // TODO: Does this date parsing work?
                    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
                    Date date = format.parse(readBasicTag(parser, TAG_PUBLISHED));
                    publishedOn = date.getTime();
                    break;
                case TAG_DURATION:
                    duration = readBasicTag(parser, TAG_DURATION);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return new Episode(title, link, subtitle, duration, publishedOn);
    }

    /**
     * Reads the body of a basic XML tag, which is guaranteed not to contain any nested elements.
     *
     * @param parser Parser object.
     * @param tag XML element tag name to parse.
     * @return Body text of the specified tag
     */
    private String readBasicTag(XmlPullParser parser, String tag)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        final String result = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tag);
        return result;
    }

    /**
     * Processes enclosure tags in the feed.
     */
    private String readEnclosure(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_ENCLOSURE);
        final String link = parser.getAttributeValue(null, "url");
        if (link == null) {
            throw new XmlPullParserException("Failed to parse <enclosure>");
        }
        parser.nextTag();
        return link;
    }

    /**
     * For the tags title and summary, extracts their text values.
     */
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = null;
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /**
     * Skips tags until the corresponding END_TAG.
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
