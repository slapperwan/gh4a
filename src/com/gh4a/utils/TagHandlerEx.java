package com.gh4a.utils;

import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html.TagHandler;

public class TagHandlerEx implements TagHandler {

    boolean first = true;
    String parent = null;
    int listIndex = 1;

    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        if ("ul".equalsIgnoreCase(tag)) {
            parent = "ul";
        }
        
        if ("ol".equalsIgnoreCase(tag)) {
            parent = "ol";
        }
        
        if ("li".equalsIgnoreCase(tag)) {
            if(parent.equals("ul")){
                char lastChar = 0;
                if (output.length() > 0)
                    lastChar = output.charAt(output.length() - 1);
                if (first) {
                    if (lastChar == '\n')
                        output.append("\t\u2022 ");
                    else
                        output.append("\n\t\u2022 ");
                    first = false;
                } else {
                    first = true;
                }
            }
            else {
                char lastChar = 0;
                if (output.length() > 0)
                    lastChar = output.charAt(output.length() - 1);
                if (first) {
                    if (lastChar == '\n')
                        output.append("\t" + listIndex + ". ");
                    else
                        output.append("\n\t" + listIndex + ". ");
                    first = false;
                    listIndex++;
                } else {
                    first = true;
                }
            }
        }
    }
}
