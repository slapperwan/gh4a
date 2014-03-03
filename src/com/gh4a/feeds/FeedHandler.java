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
import android.text.TextUtils;

import com.gh4a.holder.Feed;

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
        mFeeds = new ArrayList<Feed>();
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
                String gravatarUrl = attributes.getValue("url");
                if (gravatarUrl != null) {
                    mFeed.setGravatar(extractGravatarId(gravatarUrl), gravatarUrl);
                }
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
                }
                catch (ParseException e) {
                }
            } else if (localName.equalsIgnoreCase("entry")) {
                mFeeds.add(mFeed);
                mFeed = null;
            }
        }
        mBuilder.setLength(0);
    }

    public List<Feed> getFeeds() {
        return mFeeds;
    }

    private static String extractGravatarId(String url) {
        Uri uri = Uri.parse(url);
        String idParam = uri.getQueryParameter("gravatar_id");
        if (idParam != null) {
            return idParam;
        }
        // Construct fake IDs for github's own avatars, they're only used
        // for identification purposes in GravatarHandler
        if (TextUtils.equals(uri.getHost(), "avatars.githubusercontent.com")) {
            if (uri.getPathSegments().size() == 2) {
                return "github_" + uri.getLastPathSegment();
            }
        }
        return null;
    }
}
