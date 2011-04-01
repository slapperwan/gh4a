/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.feeds;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.gh4a.holder.Trend;
import com.gh4a.holder.YourActionFeed;

/**
 * Github trending repos RSS provided by http://github-trends.oscardelben.com/ 
 * (https://github.com/oscardelben/github-trends)
 */
public class TrendHandler extends DefaultHandler {

    private List<Trend> mTrends;
    private Trend mTrend;
    private StringBuilder mBuilder;
    
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        mBuilder.append(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        mTrends = new ArrayList<Trend>();
        mBuilder = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        
        if (localName.equalsIgnoreCase("item")) {
            mTrend = new Trend();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (mTrend != null) {
            if (localName.equalsIgnoreCase("title")) {
                String title = mBuilder.toString().trim();
                mTrend.setTitle(title);
            }
            else if (localName.equalsIgnoreCase("link")){
                mTrend.setLink(mBuilder.toString().trim());
            }
            else if (localName.equalsIgnoreCase("description")){
                mTrend.setDescription(mBuilder.toString().trim());
            }
            else if (localName.equalsIgnoreCase("item")){
                mTrends.add(mTrend);
            }
        }
        mBuilder.setLength(0);
    }
    
    public List<Trend> getTrends() {
        return mTrends;
    }
}
