package com.gh4a.utils;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.holder.YourActionFeed;

public class RssHandler extends DefaultHandler {

    private List<YourActionFeed> mYourActionFeeds;
    private YourActionFeed mYourActionFeed;
    private StringBuilder builder;
    
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        builder.append(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        mYourActionFeeds = new ArrayList<YourActionFeed>();
        builder = new StringBuilder();
    }

    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        
        if (localName.equalsIgnoreCase("entry")) {
            mYourActionFeed = new YourActionFeed();
        }
        
        if (mYourActionFeed != null) {
            if (localName.equalsIgnoreCase("thumbnail")){
                String gravatarUrl = attributes.getValue(2);
                String[] gravatarUrlPart = gravatarUrl.split("/");
                String gravatarId = gravatarUrl.substring(gravatarUrl.indexOf(gravatarUrlPart[4]), gravatarUrl.indexOf("?"));
                mYourActionFeed.setGravatarId(gravatarId);
            }
            else if (localName.equalsIgnoreCase("link")){
                String url = attributes.getValue(2);
                String[] urlPart = url.split("/");
                String owner;
                String repoName;
                if (urlPart.length > 3) {
                    owner = urlPart[3];
                    repoName = urlPart[4];
                }
                else {
                    owner = url.substring(url.indexOf(urlPart[3]), url.indexOf(urlPart[4]) - 1);
                    repoName = url.substring(url.indexOf(urlPart[4]), url.length() - 1);
                }
                mYourActionFeed.setRepoOWner(owner);
                mYourActionFeed.setRepoName(repoName);
                if (urlPart.length > 3) {
                    mYourActionFeed.setActionPath(url.substring(url.indexOf(urlPart[4]), url.length()));
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (mYourActionFeed != null) {
            if (localName.equalsIgnoreCase("id")) {
                mYourActionFeed.setId(builder.toString());
            }
            else if (localName.equalsIgnoreCase("title")){
                mYourActionFeed.setTitle(builder.toString().trim());
            }
            else if (localName.equalsIgnoreCase("content")){
                mYourActionFeed.setContent(builder.toString().trim());
            }
            else if (localName.equalsIgnoreCase("published")){
                mYourActionFeed.setPublished(builder.toString().trim());
            }
            else if (localName.equalsIgnoreCase("media")){
                mYourActionFeed.setPublished(builder.toString().trim());
            }
            else if (localName.equalsIgnoreCase("entry")){
                mYourActionFeeds.add(mYourActionFeed);
            }
        }
        builder.setLength(0);
    }
    
    public List<YourActionFeed> getFeeds() {
        return mYourActionFeeds;
    }
}
