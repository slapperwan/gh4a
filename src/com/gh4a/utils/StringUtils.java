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
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;

import com.gh4a.Gh4Application;
import com.gh4a.widget.CustomTypefaceSpan;
import com.gh4a.widget.StyleableTextView;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * The Class StringUtils.
 */
public class StringUtils {
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\._%\\-\\+]{1,256}" +
            "@" +
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
        return Gh4Application.get().getPrettyTimeInstance().format(date);
    }

    public static void applyBoldTagsAndSetText(StyleableTextView view, String input) {
        SpannableStringBuilder text = applyBoldTags(view.getContext(),
                input, view.getTypefaceValue());
        view.setText(text);
    }

    public static SpannableStringBuilder applyBoldTags(Context context,
            String input, int baseTypefaceValue) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int pos = 0;

        while (pos >= 0) {
            int start = input.indexOf("[b]", pos);
            int end = input.indexOf("[/b]", pos);
            if (start >= 0 && end >= 0) {
                int tokenLength = end - start - 3 /* length of [b] */;
                ssb.append(input.substring(pos, start));
                ssb.append(input.substring(start + 3, end));

                Object span = new CustomTypefaceSpan(context, baseTypefaceValue, Typeface.BOLD);
                ssb.setSpan(span, ssb.length() - tokenLength, ssb.length(), 0);

                pos = end + 4;
            } else {
                ssb.append(input.substring(pos, input.length()));
                pos = -1;
            }
        }
        return ssb;
    }
}