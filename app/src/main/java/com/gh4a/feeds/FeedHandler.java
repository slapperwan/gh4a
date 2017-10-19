package com.gh4a.feeds;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;

import com.gh4a.model.Feed;
import com.gh4a.utils.StringUtils;

public class FeedHandler extends DefaultHandler {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    private List<Feed> mFeeds;
    private Feed mFeed;
    private StringBuilder mBuilder;
    private boolean mAuthor;

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        mBuilder.append(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        mFeeds = new ArrayList<>();
        mBuilder = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {

        if (localName.equalsIgnoreCase("entry")) {
            mFeed = new Feed();
        }

        if (mFeed != null) {
            if (localName.equalsIgnoreCase("author")) {
                mAuthor = true;
            } else if (localName.equalsIgnoreCase("thumbnail")) {
                mFeed.setAvatarUrl(attributes.getValue("url"));
            } else if (localName.equalsIgnoreCase("link")) {
                String url = attributes.getValue("href");
                mFeed.setLink(url);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (mFeed != null) {
            if (localName.equalsIgnoreCase("id")) {
                String id = mBuilder.toString().trim();
                int pos = id.lastIndexOf('/');
                if (pos > 0) {
                    mFeed.setId(id.substring(pos + 1));
                }
            }
            if (localName.equalsIgnoreCase("title")) {
                String title = mBuilder.toString().trim();
                mFeed.setTitle(title);
            } else if (localName.equalsIgnoreCase("content")) {
                mFeed.setContent(mBuilder.toString().trim());
            } else if (localName.equalsIgnoreCase("name") && mAuthor) {
                mFeed.setAuthor(mBuilder.toString().trim());
                mAuthor = false;
            } else if (localName.equalsIgnoreCase("published")) {
                try {
                    mFeed.setPublished(sdf.parse(mBuilder.toString().trim()));
                } catch (ParseException e) {
                    // ignored
                }
            } else if (localName.equalsIgnoreCase("entry")) {
                if (StringUtils.isBlank(mFeed.getTitle())) {
                    mFeed.setTitle(getTitleFromUrl(mFeed.getLink()));
                }
                mFeed.setUserId(determineUserId(mFeed.getAvatarUrl(), mFeed.getAuthor()));
                mFeeds.add(mFeed);
                mFeed = null;
            }
        }
        mBuilder.setLength(0);
    }

    public List<Feed> getFeeds() {
        return mFeeds;
    }

    private int determineUserId(String url, String userName) {
        if (url == null) {
            return -1;
        }

        Uri uri = Uri.parse(url);
        String host = uri.getHost();

        if (host.startsWith("avatars") && host.contains("githubusercontent.com")) {
            if (uri.getPathSegments().size() == 2) {
                return Integer.valueOf(uri.getLastPathSegment());
            }
        }

        // We couldn't parse the user ID from the avatar, so construct a fake
        // user ID for identification purposes in GravatarHandler
        if (userName != null) {
            return userName.hashCode();
        }
        return -1;
    }

    private String getTitleFromUrl(String wikiUrl) {
        if (wikiUrl == null) {
            return null;
        }
        return wikiUrl.substring(wikiUrl.lastIndexOf("/") + 1, wikiUrl.length()).replaceAll("-", " ");
    }
}
