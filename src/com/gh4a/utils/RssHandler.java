package com.gh4a.utils;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (mYourActionFeed != null) {
            if (localName.equalsIgnoreCase("id")) {
                mYourActionFeed.setId(builder.toString());
            }
            else if (localName.equalsIgnoreCase("title")){
                mYourActionFeed.setTitle(builder.toString());
            }
            else if (localName.equalsIgnoreCase("content")){
                mYourActionFeed.setContent(builder.toString());
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
