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

        if (mYourActionFeed != null) {
            if (localName.equalsIgnoreCase("thumbnail")) {
                String gravatarUrl = attributes.getValue(2);
                String[] gravatarUrlPart = gravatarUrl.split("/");
                String gravatarId = gravatarUrl.substring(gravatarUrl.indexOf(gravatarUrlPart[4]),
                        gravatarUrl.indexOf("?"));
                mYourActionFeed.setGravatarId(gravatarId);
            } else if (localName.equalsIgnoreCase("link")) {
                String url = attributes.getValue(2);
                String[] urlPart = url.split("/");
                String owner = null;
                String repoName = null;

                if (urlPart.length > 4) {
                    owner = urlPart[3];
                    repoName = urlPart[4];
                } else if (urlPart.length > 3) {
                    owner = urlPart[3];
                }

                mYourActionFeed.setLink(url);
                mYourActionFeed.setRepoOWner(owner);
                mYourActionFeed.setRepoName(repoName);
                if (urlPart.length > 4) {
                    mYourActionFeed.setActionPath(url.substring(url.indexOf(urlPart[4]), url.length()));
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (mYourActionFeed != null) {
            if (localName.equalsIgnoreCase("id")) {
                String id = builder.toString();
                mYourActionFeed.setId(id);

                int index1 = id.lastIndexOf(":");
                int index2 = id.lastIndexOf("/");

                mYourActionFeed.setEvent(id.substring(index1 + 1, index2));
            } else if (localName.equalsIgnoreCase("title")) {
                mYourActionFeed.setTitle(builder.toString().trim());
            } else if (localName.equalsIgnoreCase("content")) {
                mYourActionFeed.setContent(formatContent(builder.toString().trim()));
            } else if (localName.equalsIgnoreCase("published")) {
                mYourActionFeed.setPublished(builder.toString().trim());
            } else if (localName.equalsIgnoreCase("media")) {
                mYourActionFeed.setPublished(builder.toString().trim());
            } else if (localName.equalsIgnoreCase("email")) {
                mYourActionFeed.setEmail(builder.toString().trim());
            } else if (localName.equalsIgnoreCase("name")) {
                mYourActionFeed.setAuthor(builder.toString().trim());
            } else if (localName.equalsIgnoreCase("entry")) {
                mYourActionFeeds.add(mYourActionFeed);
            }
        }
        builder.setLength(0);
    }

    private String formatContent(String content) {
        content = content.replaceAll("\\<.*?>","");
        content = content.replaceAll("[\\n]{2,}", "")
                .replaceAll("[\r\n]{2,}", "")
                .replaceAll("[\\s]{2,}", "\n").trim()
                .replaceAll("[^\n][\\s]+$", "")
                .replaceAll("&#47;", "/")
                .replaceAll("&raquo;", "");

//        if (UserFeed.Type.FOLLOW_EVENT.value().equals(event)) {
//            content = content.replaceAll("\n", " ");
//        }
//        if (UserFeed.Type.PULL_REQUEST_EVENT.value().equals(event)) {
//            content = content.replaceAll("\n", " ");
//        } else if (UserFeed.Type.PUSH_EVENT.value().equals(event)) {
//            StringBuilder sb = new StringBuilder();
//
//            String[] commitDesc = content.split("\n");
//            for (String str : commitDesc) {
//                String[] committedWord = str.split(" ");
//                if (committedWord.length > 1) {
//                    if (committedWord[1].equals("committed")) {
//                        sb.append(committedWord[2]).append(" ");
//                    } else {
//                        sb.append(str).append("\n");
//                    }
//                } else {
//                    sb.append(str).append("\n");
//                }
//            }
//            content = sb.toString();
//        } else if (UserFeed.Type.WATCH_EVENT.value().equals(event)) {
//            int index = content.indexOf("\n");
//            if (index != -1) {
//                content = content.replaceAll(content.substring(0, index + 1), "");
//            }
//        }
        return content;
    }
    public List<YourActionFeed> getFeeds() {
        return mYourActionFeeds;
    }
}
