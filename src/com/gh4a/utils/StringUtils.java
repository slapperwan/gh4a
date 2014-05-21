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
package com.gh4a.utils;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * The Class StringUtils.
 */
public class StringUtils {
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+");

    /**
     * Checks if is blank.
     *
     * @param val the val
     * @return true, if is blank
     */
    public static boolean isBlank(String val) {
        return val == null || val.trim().isEmpty();
    }

    /**
     * Do teaser.
     *
     * @param text the text
     * @return the string
     */
    public static String doTeaser(String text) {
        if (isBlank(text)) {
            return "";
        }

        int indexNewLine = text.indexOf("\n");
        int indexDot = text.indexOf(". ");

        if (indexDot != -1 && indexNewLine != -1) {
            if (indexDot > indexNewLine) {
                text = text.substring(0, indexNewLine);
            } else {
                text = text.substring(0, indexDot + 1);
            }
        } else if (indexDot != -1) {
            text = text.substring(0, indexDot + 1);
        } else if (indexNewLine != -1) {
            text = text.substring(0, indexNewLine);
        }

        return text;
    }

    /**
     * Md5 hex.
     *
     * @param s the s
     * @return the string
     */
    public static String md5Hex(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(s.getBytes());
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // this won't happen, we know Java has MD5!
        }
        return null;
    }

    /**
     * To hex.
     *
     * @param dataBytes
     * @return the hex string representation of dataBytes
     */
    public static String toHex(byte[] dataBytes) {
        StringBuilder sb = new StringBuilder(dataBytes.length * 2);
        for (byte dataByte : dataBytes) {
            sb.append(Character.forDigit((dataByte & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(dataByte & 0x0f, 16));
        }
        return sb.toString();
    }

    /**
     * Format name.
     *
     * @param userLogin the user login
     * @param name the name
     * @return the string
     */
    public static String formatName(String userLogin, String name) {
        if (StringUtils.isBlank(userLogin)) {
            return name;
        }

        return userLogin + (!StringUtils.isBlank(name) ? " - " + name : "");
    }

    public static Pair<String,Boolean> highlightSyntax(String data, boolean highlight, String fileName,
            String repoOwner, String repoName, String ref) {
        String ext = FileUtils.getFileExtension(fileName);
        boolean highlighted = false;
        boolean themed = false;

        StringBuilder content = new StringBuilder();
        content.append("<html><head><title></title>");
        if (highlight) {
            if (Constants.MARKDOWN_EXT.contains(ext)) {
                content.append("<script src='file:///android_asset/showdown.js' type='text/javascript'></script>");
                content.append("<link href='file:///android_asset/markdown.css' rel='stylesheet' type='text/css'/>");
                content.append("</head>");
                content.append("<body>");
                content.append("<div id='content'>");
                highlighted = true;
            } else if (!Constants.SKIP_PRETTIFY_EXT.contains(ext)) {
                data = TextUtils.htmlEncode(data).replace("\r\n", "<br>").replace("\n", "<br>");

                content.append("<link href='file:///android_asset/prettify-");
                content.append(ThemeUtils.getCssTheme(Gh4Application.THEME));
                content.append(".css' rel='stylesheet' type='text/css'/>");
                content.append("<script src='file:///android_asset/prettify.js' type='text/javascript'></script>");
                content.append("</head>");
                content.append("<body onload='prettyPrint()'>");
                content.append("<pre class='prettyprint linenums'>");
                highlighted = true;
                themed = true;
            }
        }
        if (!highlighted) {
            data = TextUtils.htmlEncode(data).replace("\r\n", "<br>").replace("\n", "<br>");
            content.append("</head>");
            content.append("<body>");
            content.append("<pre>");
        }

        content.append(data);

        if (Constants.MARKDOWN_EXT.contains(ext)) {
            content.append("</div>");

            content.append("<script>");
            if (repoOwner != null && repoName != null) {
                content.append("var GitHub = new Object();");
                content.append("GitHub.nameWithOwner = \"").append(repoOwner).append("/").append(repoName).append("\";");
                if (ref != null) {
                    content.append("GitHub.branch = \"").append(ref).append("\";");
                }
            }
            content.append("var text = document.getElementById('content').innerHTML;");
            content.append("var converter = new Showdown.converter();");
            content.append("var html = converter.makeHtml(text);");
            //content.append("html = html.replace(/>/g, '>\n').replace(/</g, '\n<').replace(/\n{2,}/g, '\n\n')");
            content.append("document.getElementById('content').innerHTML = html;");
            content.append("</script>");
        } else {
            content.append("</pre>");
        }

        content.append("</body></html>");

        return new Pair<String, Boolean>(content.toString(), themed);
    }

    public static String highlightImage(String imageUrl) {
        return "<html><body style=\"background-color:#dddddd;margin:auto\">" + "<span class=\"border:solid 1px #333333;\">" + "<img src=\"" + imageUrl + "\" style=\"\"/>" + "</span>" + "</body></html>";
    }

    public static boolean checkEmail(String email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    public static CharSequence formatRelativeTime(Context context, Date date, boolean showDateIfLongAgo) {
        long now = System.currentTimeMillis();
        long time = date.getTime();
        long duration = Math.abs(now - time);

        if (showDateIfLongAgo && duration >= DateUtils.WEEK_IN_MILLIS) {
            return DateUtils.getRelativeTimeSpanString(context, time, true);
        }
        return Gh4Application.get(context).getPrettyTimeInstance().format(date);
    }
}