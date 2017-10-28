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
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.util.Base64;

import com.gh4a.Gh4Application;
import com.gh4a.widget.CustomTypefaceSpan;
import com.gh4a.widget.StyleableTextView;
import com.meisolsson.githubsdk.model.User;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class StringUtils.
 */
public class StringUtils {
    private static final Pattern HUNK_START_PATTERN =
            Pattern.compile("@@ -(\\d+),\\d+ \\+(\\d+),\\d+.*");

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

    public static CharSequence formatRelativeTime(Context context, Date date, boolean showDateIfLongAgo) {
        long now = System.currentTimeMillis();
        long time = date.getTime();
        long duration = Math.abs(now - time);

        if (showDateIfLongAgo && duration >= DateUtils.WEEK_IN_MILLIS) {
            return DateUtils.getRelativeTimeSpanString(context, time, true);
        }
        return Gh4Application.get().getPrettyTimeInstance().format(date);
    }

    public static CharSequence formatExactTime(Context context, Date date) {
        return DateUtils.formatDateTime(context, date.getTime(), DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_SHOW_YEAR);
    }

    public static void applyBoldTagsAndSetText(StyleableTextView view, String input) {
        SpannableStringBuilder text = applyBoldTags(input, view.getTypefaceValue());
        view.setText(text);
    }

    public static SpannableStringBuilder applyBoldTags(String input, int baseTypefaceValue) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int pos = 0;

        while (pos >= 0) {
            int start = input.indexOf("[b]", pos);
            int end = input.indexOf("[/b]", pos);
            if (start >= 0 && end >= 0) {
                int tokenLength = end - start - 3 /* length of [b] */;
                ssb.append(input.substring(pos, start));
                ssb.append(input.substring(start + 3, end));

                Object span = new CustomTypefaceSpan(baseTypefaceValue, Typeface.BOLD);
                ssb.setSpan(span, ssb.length() - tokenLength, ssb.length(), 0);

                pos = end + 4;
            } else {
                ssb.append(input.substring(pos, input.length()));
                pos = -1;
            }
        }
        return ssb;
    }

    @Nullable
    public static int[] findMatchingLines(String input, String match) {
        int pos = input.indexOf(match);
        if (pos < 0) {
            return null;
        }

        int start = input.substring(0, pos).split("\n").length;
        int end = start + match.split("\n").length - 1;
        return new int[] { start, end };
    }

    public static int[] extractDiffHunkLineNumbers(@NonNull String diffHunk) {
        if (!diffHunk.startsWith("@@")) {
            return null;
        }

        Matcher matcher = HUNK_START_PATTERN.matcher(diffHunk);
        if (matcher.matches()) {
            int leftLine = Integer.parseInt(matcher.group(1)) - 1;
            int rightLine = Integer.parseInt(matcher.group(2)) - 1;
            return new int[] { leftLine, rightLine };
        }

        return null;
    }

    public static CharSequence formatMention(Context context, User user) {
        String userLogin = ApiHelpers.getUserLogin(context, user);
        return "@" + userLogin + " ";
    }

    public static String toBase64(String data) {
        return Base64.encodeToString(data.getBytes(), Base64.NO_WRAP);
    }

    public static String fromBase64(String encoded) {
        return new String(Base64.decode(encoded, Base64.DEFAULT));
    }
}
