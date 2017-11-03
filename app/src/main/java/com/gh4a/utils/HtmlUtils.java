/*
 * Copyright 2012 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Html.ImageGetter;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.ParagraphStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

import com.gh4a.R;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.graphics.Paint.Style.FILL;

public class HtmlUtils {
    private static class ReplySpan implements LeadingMarginSpan {
        private final int mColor;
        private final int mMargin;
        private final int mSize;

        public ReplySpan(int margin, int size, int color) {
            mColor = color;
            mMargin = margin;
            mSize = size;
        }

        @Override
        public int getLeadingMargin(boolean first) {
            return mMargin;
        }

        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
                int top, int baseline, int bottom, CharSequence text,
                int start, int end, boolean first, Layout layout) {
            final Style style = p.getStyle();
            final int color = p.getColor();

            p.setStyle(FILL);
            p.setColor(mColor);

            c.drawRect(x, top, x + dir * mSize, bottom, p);

            p.setStyle(style);
            p.setColor(color);
        }
    }

    private static class CodeBlockSpan implements LineBackgroundSpan {
        private final int mColor;

        public CodeBlockSpan(int color) {
            mColor = color;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline,
                int bottom, CharSequence text, int start, int end, int lnum) {
            final int paintColor = p.getColor();
            p.setColor(mColor);
            c.drawRect(left, top, right, bottom, p);
            p.setColor(paintColor);
        }
    }

    private static class HorizontalLineSpan implements LineBackgroundSpan {
        private final int mColor;
        private final float mHeight;

        public HorizontalLineSpan(float height, int color) {
            mColor = color;
            mHeight = height;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline,
                                   int bottom, CharSequence text, int start, int end, int lnum) {
            final int paintColor = p.getColor();
            final float centerY = (top + bottom) / 2;
            p.setColor(mColor);
            c.drawRect(left, centerY - mHeight / 2, right, centerY + mHeight / 2, p);
            p.setColor(paintColor);
        }
    }

    /**
     * Rewrite relative URLs in HTML fetched e.g. from markdown files.
     *
     * @param html
     * @param repoUser
     * @param repoName
     * @param branch
     * @return
     */
    public static String rewriteRelativeUrls(final String html, final String repoUser,
            final String repoName, final String branch) {
        final String baseUrl = "https://raw.github.com/" + repoUser + "/" + repoName + "/" + branch;
        final StringBuffer sb = new StringBuffer();
        final Pattern p = Pattern.compile("(href|src)=\"(\\S+)\"");
        final Matcher m = p.matcher(html);

        while (m.find()) {
            String url = m.group(2);
            if (!url.contains("://") && !url.startsWith("#")) {
                if (url.startsWith("/")) {
                    url = baseUrl + url;
                } else {
                    url = baseUrl + "/" + url;
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "=\"" + url + "\""));
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /**
     * Encode HTML
     *
     * @param html
     * @param imageGetter
     * @return html
     */
    public static CharSequence encode(final Context context, final String html,
                                      final ImageGetter imageGetter) {
        if (TextUtils.isEmpty(html))
            return "";

        return Html.fromHtml(context, html, imageGetter);
    }

    /* a copy of the framework's HTML class, stripped down and extended for our use cases */
    private static class Html {
        private Html() { }

        /**
         * Lazy initialization holder for HTML parser.
         */
        private static class HtmlParser {
            private static final HTMLSchema schema = new HTMLSchema();
        }

        public static Spanned fromHtml(Context context,
                String source, android.text.Html.ImageGetter imageGetter) {
            Parser parser = new Parser();
            try {
                parser.setProperty(Parser.schemaProperty, HtmlParser.schema);
            } catch (org.xml.sax.SAXNotRecognizedException | org.xml.sax.SAXNotSupportedException e) {
                // Should not happen.
                throw new RuntimeException(e);
            }

            HtmlToSpannedConverter converter =
                    new HtmlToSpannedConverter(context, source, imageGetter, parser);
            return converter.convert();
        }
    }

    private static class HtmlToSpannedConverter implements ContentHandler {
        private static final float[] HEADING_SIZES = {
            1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
        };

        private final float mDividerHeight;
        private final int mBulletMargin;
        private final int mReplyMargin;
        private final int mReplyMarkerSize;

        private final Context mContext;
        private final String mSource;
        private final XMLReader mReader;
        private final SpannableStringBuilder mSpannableStringBuilder;
        private final android.text.Html.ImageGetter mImageGetter;

        private static Pattern sTextAlignPattern;
        private static Pattern sForegroundColorPattern;
        private static Pattern sBackgroundColorPattern;
        private static Pattern sTextDecorationPattern;

        private static Pattern getTextAlignPattern() {
            if (sTextAlignPattern == null) {
                sTextAlignPattern = Pattern.compile("(?:\\s+|\\A)text-align\\s*:\\s*(\\S*)\\b");
            }
            return sTextAlignPattern;
        }

        private static Pattern getForegroundColorPattern() {
            if (sForegroundColorPattern == null) {
                sForegroundColorPattern = Pattern.compile(
                        "(?:\\s+|\\A)color\\s*:\\s*(\\S*)\\b");
            }
            return sForegroundColorPattern;
        }

        private static Pattern getBackgroundColorPattern() {
            if (sBackgroundColorPattern == null) {
                sBackgroundColorPattern = Pattern.compile(
                        "(?:\\s+|\\A)background(?:-color)?\\s*:\\s*(\\S*)\\b");
            }
            return sBackgroundColorPattern;
        }

        private static Pattern getTextDecorationPattern() {
            if (sTextDecorationPattern == null) {
                sTextDecorationPattern = Pattern.compile(
                        "(?:\\s+|\\A)text-decoration\\s*:\\s*(\\S*)\\b");
            }
            return sTextDecorationPattern;
        }

        public HtmlToSpannedConverter(Context context, String source,
                android.text.Html.ImageGetter imageGetter, Parser parser) {
            final Resources res = context.getResources();
            mDividerHeight = res.getDimension(R.dimen.divider_span_height);
            mBulletMargin = res.getDimensionPixelSize(R.dimen.bullet_span_margin);
            mReplyMargin = res.getDimensionPixelSize(R.dimen.reply_span_margin);
            mReplyMarkerSize = res.getDimensionPixelSize(R.dimen.reply_span_size);

            mContext = context;
            mSource = source;
            mSpannableStringBuilder = new SpannableStringBuilder();
            mImageGetter = imageGetter;
            mReader = parser;
        }

        public Spanned convert() {
            mReader.setContentHandler(this);
            //noinspection TryWithIdenticalCatches
            try {
                mReader.parse(new InputSource(new StringReader(mSource)));
            } catch (IOException e) {
                // We are reading from a string. There should not be IO problems.
                throw new RuntimeException(e);
            } catch (SAXException e) {
                // TagSoup doesn't throw parse exceptions.
                throw new RuntimeException(e);
            }

            // Replace the placeholders for leading margin spans in reverse order, so the leading
            // margins are drawn in order of tag start
            Object[] obj = mSpannableStringBuilder.getSpans(0,
                    mSpannableStringBuilder.length(), NeedsReversingSpan.class);
            for (int i = obj.length - 1; i >= 0; i--) {
                NeedsReversingSpan span = (NeedsReversingSpan) obj[i];
                int start = mSpannableStringBuilder.getSpanStart(span);
                int end = mSpannableStringBuilder.getSpanEnd(span);

                mSpannableStringBuilder.removeSpan(span);
                mSpannableStringBuilder.setSpan(span.mActualSpan, start, end,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }

            // Fix flags and range for paragraph-type markup.
            obj = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length(), ParagraphStyle.class);
            for (Object span : obj) {
                int start = mSpannableStringBuilder.getSpanStart(span);
                int end = mSpannableStringBuilder.getSpanEnd(span);

                // If the last line of the range is blank, back off by one.
                if (end - 2 >= 0 && (end - start) >= 2) {
                    if (mSpannableStringBuilder.charAt(end - 1) == '\n' &&
                            mSpannableStringBuilder.charAt(end - 2) == '\n') {
                        end--;
                    }
                }

                if (end == start) {
                    mSpannableStringBuilder.removeSpan(span);
                } else {
                    mSpannableStringBuilder.setSpan(span, start, end, Spannable.SPAN_PARAGRAPH);
                }
            }

            // Remove leading newlines
            while (mSpannableStringBuilder.length() > 0
                    && mSpannableStringBuilder.charAt(0) == '\n') {
                mSpannableStringBuilder.delete(0, 1);
            }

            // Remove trailing newlines
            int last = mSpannableStringBuilder.length() - 1;
            while (last >= 0 && mSpannableStringBuilder.charAt(last) == '\n') {
                mSpannableStringBuilder.delete(last, last + 1);
                last = mSpannableStringBuilder.length() - 1;
            }

            return mSpannableStringBuilder;
        }

        private void handleStartTag(String tag, Attributes attributes) {
            //noinspection StatementWithEmptyBody
            if (tag.equalsIgnoreCase("br")) {
                // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
                // so we can safely emit the linebreaks when we handle the close tag.
            } else if (tag.equalsIgnoreCase("p")) {
                startBlockElement(mSpannableStringBuilder, attributes);
                startCssStyle(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("ul")) {
                startBlockElement(mSpannableStringBuilder, attributes);
                start(mSpannableStringBuilder, new List());
            } else if (tag.equalsIgnoreCase("ol")) {
                startBlockElement(mSpannableStringBuilder, attributes);
                start(mSpannableStringBuilder, new List(parseIntAttribute(attributes, "start", 1)));
            } else if (tag.equalsIgnoreCase("li")) {
                startLi(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("input")) {
                if ("checkbox".equalsIgnoreCase(attributes.getValue("", "type"))) {
                    @AttrRes int drawableAttrResId = attributes.getIndex("", "checked") >= 0
                            ? R.attr.checkboxCheckedSmallIcon
                            : R.attr.checkboxUncheckedSmallIcon;
                    Drawable d = ContextCompat.getDrawable(mContext,
                            UiUtils.resolveDrawable(mContext, drawableAttrResId));
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);

                    mSpannableStringBuilder.append("  ");
                    mSpannableStringBuilder.setSpan(span, mSpannableStringBuilder.length() - 2,
                            mSpannableStringBuilder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (tag.equalsIgnoreCase("div")) {
                startBlockElement(mSpannableStringBuilder, attributes);
                String cssClass = attributes.getValue("", "class");
                if (cssClass != null && cssClass.indexOf("highlight") == 0) {
                    start(mSpannableStringBuilder, new CodeDiv());
                }
                CodeDiv code = getLast(mSpannableStringBuilder, CodeDiv.class);
                if (code != null) {
                    code.mLevel++;
                }
            } else if (tag.equalsIgnoreCase("span")) {
                startCssStyle(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("hr")) {
                HorizontalLineSpan span = new HorizontalLineSpan(mDividerHeight, 0x60aaaaaa);
                // enforce the following newlines to be written
                mSpannableStringBuilder.append(' ');
                appendNewlines(mSpannableStringBuilder, 2);
                int len = mSpannableStringBuilder.length();
                mSpannableStringBuilder.setSpan(span, len - 1, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            } else if (tag.equalsIgnoreCase("strong")) {
                start(mSpannableStringBuilder, new Bold());
            } else if (tag.equalsIgnoreCase("b")) {
                start(mSpannableStringBuilder, new Bold());
            } else if (tag.equalsIgnoreCase("em")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("cite")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("dfn")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("i")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("big")) {
                start(mSpannableStringBuilder, new Big());
            } else if (tag.equalsIgnoreCase("small")) {
                start(mSpannableStringBuilder, new Small());
            } else if (tag.equalsIgnoreCase("font")) {
                startFont(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                startBlockquote(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("tt")) {
                start(mSpannableStringBuilder, new Monospace());
            } else if (tag.equalsIgnoreCase("pre")) {
                start(mSpannableStringBuilder, new Pre());
                CodeDiv div = getLast(mSpannableStringBuilder, CodeDiv.class);
                if (div != null) {
                    div.mHasPre = true;
                }
            } else if (tag.equalsIgnoreCase("a")) {
                startA(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("u")) {
                start(mSpannableStringBuilder, new Underline());
            } else if (tag.equalsIgnoreCase("del")) {
                start(mSpannableStringBuilder, new Strikethrough());
            } else if (tag.equalsIgnoreCase("s")) {
                start(mSpannableStringBuilder, new Strikethrough());
            } else if (tag.equalsIgnoreCase("strike")) {
                start(mSpannableStringBuilder, new Strikethrough());
            } else if (tag.equalsIgnoreCase("sup")) {
                start(mSpannableStringBuilder, new Super());
            } else if (tag.equalsIgnoreCase("sub")) {
                start(mSpannableStringBuilder, new Sub());
            } else if (tag.equalsIgnoreCase("code")) {
                boolean inPre = getLast(mSpannableStringBuilder, Pre.class) != null;
                if (inPre) {
                    appendNewlines(mSpannableStringBuilder, 1);
                }
                start(mSpannableStringBuilder, new Code(inPre));
            } else if (tag.length() == 2 &&
                    Character.toLowerCase(tag.charAt(0)) == 'h' &&
                    tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                startHeading(mSpannableStringBuilder, attributes, tag.charAt(1) - '1');
            } else if (tag.equalsIgnoreCase("img")) {
                startImg(mSpannableStringBuilder, attributes, mImageGetter);
            }
        }

        private void handleEndTag(String tag) {
            if (tag.equalsIgnoreCase("br")) {
                handleBr(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("p")) {
                endCssStyle(mSpannableStringBuilder);
                endBlockElement(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("ul")) {
                endBlockElement(mSpannableStringBuilder);
                end(mSpannableStringBuilder, List.class, null);
            } else if (tag.equalsIgnoreCase("ol")) {
                endBlockElement(mSpannableStringBuilder);
                end(mSpannableStringBuilder, List.class, null);
            } else if (tag.equalsIgnoreCase("li")) {
                endLi(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("div")) {
                endBlockElement(mSpannableStringBuilder);
                CodeDiv code = getLast(mSpannableStringBuilder, CodeDiv.class);
                if (code != null && --code.mLevel == 0) {
                    if (code.mHasPre) {
                        setSpanFromMark(mSpannableStringBuilder, code, new CodeBlockSpan(0x30aaaaaa));
                    } else {
                        mSpannableStringBuilder.removeSpan(code);
                    }
                }
            } else if (tag.equalsIgnoreCase("span")) {
                endCssStyle(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("strong")) {
                end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("b")) {
                end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("em")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("cite")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("dfn")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("i")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("big")) {
                end(mSpannableStringBuilder, Big.class, new RelativeSizeSpan(1.25f));
            } else if (tag.equalsIgnoreCase("small")) {
                end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
            } else if (tag.equalsIgnoreCase("font")) {
                endFont(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                endBlockquote(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("tt")) {
                end(mSpannableStringBuilder, Monospace.class, new TypefaceSpan("monospace"));
            } else if (tag.equalsIgnoreCase("pre")) {
                end(mSpannableStringBuilder, Pre.class, new TypefaceSpan("monospace"));
            } else if (tag.equalsIgnoreCase("a")) {
                endA(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("u")) {
                end(mSpannableStringBuilder, Underline.class, new UnderlineSpan());
            } else if (tag.equalsIgnoreCase("del")) {
                end(mSpannableStringBuilder, Strikethrough.class, new StrikethroughSpan());
            } else if (tag.equalsIgnoreCase("s")) {
                end(mSpannableStringBuilder, Strikethrough.class, new StrikethroughSpan());
            } else if (tag.equalsIgnoreCase("strike")) {
                end(mSpannableStringBuilder, Strikethrough.class, new StrikethroughSpan());
            } else if (tag.equalsIgnoreCase("sup")) {
                end(mSpannableStringBuilder, Super.class, new SuperscriptSpan());
            } else if (tag.equalsIgnoreCase("sub")) {
                end(mSpannableStringBuilder, Sub.class, new SubscriptSpan());
            } else if (tag.equalsIgnoreCase("code")) {
                Code code = getLast(mSpannableStringBuilder, Code.class);
                if (code != null) {
                    Object backgroundSpan = code.mInPre
                            ? new CodeBlockSpan(0x30aaaaaa) : new BackgroundColorSpan(0x30aaaaaa);
                    if (code.mInPre) {
                        appendNewlines(mSpannableStringBuilder, 1);
                    }
                    setSpanFromMark(mSpannableStringBuilder, code,
                            new TypefaceSpan("monospace"), backgroundSpan);
                }
            } else if (tag.length() == 2 &&
                    Character.toLowerCase(tag.charAt(0)) == 'h' &&
                    tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                endHeading(mSpannableStringBuilder);
            }
        }

        private static void appendNewlines(Editable text, int minNewline) {
            final int len = text.length();

            if (len == 0) {
                return;
            }

            int existingNewlines = 0;
            for (int i = len - 1; i >= 0 && text.charAt(i) == '\n'; i--) {
                existingNewlines++;
            }

            for (int j = existingNewlines; j < minNewline; j++) {
                text.append("\n");
            }
        }

        private static void startBlockElement(Editable text, Attributes attributes) {
            startBlockElement(text, attributes, 2);
        }

        private static void startBlockElement(Editable text, Attributes attributes, int newlines) {
            appendNewlines(text, newlines);
            start(text, new Newline(newlines));

            String style = attributes.getValue("", "style");
            if (style != null) {
                Matcher m = getTextAlignPattern().matcher(style);
                if (m.find()) {
                    String alignment = m.group(1);
                    if (alignment.equalsIgnoreCase("start")) {
                        start(text, new Alignment(Layout.Alignment.ALIGN_NORMAL));
                    } else if (alignment.equalsIgnoreCase("center")) {
                        start(text, new Alignment(Layout.Alignment.ALIGN_CENTER));
                    } else if (alignment.equalsIgnoreCase("end")) {
                        start(text, new Alignment(Layout.Alignment.ALIGN_OPPOSITE));
                    }
                }
            }
        }

        private static void endBlockElement(Editable text) {
            Newline n = getLast(text, Newline.class);
            if (n != null) {
                appendNewlines(text, n.mNumNewlines);
                text.removeSpan(n);
            }

            Alignment a = getLast(text, Alignment.class);
            if (a != null) {
                setSpanFromMark(text, a, new AlignmentSpan.Standard(a.mAlignment));
            }
        }

        private static void handleBr(Editable text) {
            text.append('\n');
        }

        private void startLi(Editable text, Attributes attributes) {
            ListItem item = new ListItem(getLast(text, List.class), attributes);
            startBlockElement(text, attributes, 1);
            start(text, item);
            if (item.mOrdered) {
                text.insert(text.length(), "" + item.mPosition + ". ");
            }
            startCssStyle(text, attributes);
        }

        private void endLi(Editable text) {
            endCssStyle(text);
            endBlockElement(text);
            ListItem item = getLast(text, ListItem.class);
            if (item != null) {
                if (item.mOrdered) {
                    text.removeSpan(item);
                } else {
                    setSpanFromMark(text, item, new BulletSpan(mBulletMargin));
                }
            }
        }

        private void startBlockquote(Editable text, Attributes attributes) {
            startBlockElement(text, attributes);
            start(text, new Blockquote());
        }

        private void endBlockquote(Editable text) {
            endBlockElement(text);
            end(text, Blockquote.class, new ReplySpan(mReplyMargin, mReplyMarkerSize, 0xffdddddd));
        }

        private void startHeading(Editable text, Attributes attributes, int level) {
            startBlockElement(text, attributes);
            start(text, new Heading(level));
        }

        private static void endHeading(Editable text) {
            // RelativeSizeSpan and StyleSpan are CharacterStyles
            // Their ranges should not include the newlines at the end
            Heading h = getLast(text, Heading.class);
            if (h != null) {
                setSpanFromMark(text, h, new RelativeSizeSpan(HEADING_SIZES[h.mLevel]),
                        new StyleSpan(Typeface.BOLD));
            }

            endBlockElement(text);
        }

        private static <T> T getLast(Spanned text, Class<T> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
            T[] objs = text.getSpans(0, text.length(), kind);

            if (objs.length == 0) {
                return null;
            } else {
                return objs[objs.length - 1];
            }
        }

        private static void setSpanFromMark(Spannable text, Object mark, Object... spans) {
            int where = text.getSpanStart(mark);
            text.removeSpan(mark);
            int len = text.length();
            if (where != len) {
                for (Object span : spans) {
                    if (span instanceof LeadingMarginSpan) {
                        span = new NeedsReversingSpan(span);
                    }
                    text.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private static void start(Editable text, Object mark) {
            int len = text.length();
            text.setSpan(mark, len, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        private static void end(Editable text, Class<?> kind, Object repl) {
            Object obj = getLast(text, kind);
            if (obj != null) {
                setSpanFromMark(text, obj, repl);
            }
        }

        private void startCssStyle(Editable text, Attributes attributes) {
            String style = attributes.getValue("", "style");
            if (style != null) {
                Matcher m = getForegroundColorPattern().matcher(style);
                if (m.find()) {
                    Integer c = parseColor(m.group(1));
                    if (c != null) {
                        start(text, new Foreground(c));
                    }
                }

                m = getBackgroundColorPattern().matcher(style);
                if (m.find()) {
                    Integer c = parseColor(m.group(1));
                    if (c != null) {
                        start(text, new Background(c));
                    }
                }

                m = getTextDecorationPattern().matcher(style);
                if (m.find()) {
                    String textDecoration = m.group(1);
                    if (textDecoration.equalsIgnoreCase("line-through")) {
                        start(text, new Strikethrough());
                    }
                }
            }
        }

        private static void endCssStyle(Editable text) {
            Strikethrough s = getLast(text, Strikethrough.class);
            if (s != null) {
                setSpanFromMark(text, s, new StrikethroughSpan());
            }

            Background b = getLast(text, Background.class);
            if (b != null) {
                setSpanFromMark(text, b, new BackgroundColorSpan(b.mBackgroundColor));
            }

            Foreground f = getLast(text, Foreground.class);
            if (f != null) {
                setSpanFromMark(text, f, new ForegroundColorSpan(f.mForegroundColor));
            }
        }

        private static Integer parseColor(String colorString) {
            if (colorString == null) {
                return null;
            }
            try {
                int color = Color.parseColor(colorString);
                return color | 0xff000000;
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static void startImg(Editable text, Attributes attributes,
                android.text.Html.ImageGetter img) {
            String src = attributes.getValue("", "src");
            Drawable d = img.getDrawable(src);

            int len = text.length();
            text.append("\uFFFC");

            text.setSpan(new ImageSpan(d, src), len, text.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private void startFont(Editable text, Attributes attributes) {
            String color = attributes.getValue("", "color");
            String face = attributes.getValue("", "face");

            if (!TextUtils.isEmpty(color)) {
                Integer c = parseColor(color);
                if (c != null) {
                    start(text, new Foreground(c));
                }
            }

            if (!TextUtils.isEmpty(face)) {
                start(text, new Font(face));
            }
        }

        private static void endFont(Editable text) {
            Font font = getLast(text, Font.class);
            if (font != null) {
                setSpanFromMark(text, font, new TypefaceSpan(font.mFace));
            }

            Foreground foreground = getLast(text, Foreground.class);
            if (foreground != null) {
                setSpanFromMark(text, foreground,
                        new ForegroundColorSpan(foreground.mForegroundColor));
            }
        }

        private static void startA(Editable text, Attributes attributes) {
            String href = attributes.getValue("", "href");
            start(text, new Href(href));
        }

        private static void endA(Editable text) {
            Href h = getLast(text, Href.class);
            if (h != null) {
                if (h.mHref != null) {
                    setSpanFromMark(text, h, new URLSpan((h.mHref)));
                }
            }
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            handleStartTag(localName, attributes);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            handleEndTag(localName);
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (getLast(mSpannableStringBuilder, Pre.class) != null) {
                /* We're in a pre block, so keep whitespace intact. */
                for (int i = 0; i < length; i++) {
                    mSpannableStringBuilder.append(ch[i + start]);
                }
                return;
            }

            StringBuilder sb = new StringBuilder();
            /*
             * Ignore whitespace that immediately follows other whitespace;
             * newlines count as spaces.
             */
            for (int i = 0; i < length; i++) {
                char c = ch[i + start];

                if (c == ' ' || c == '\n') {
                    char pred;
                    int len = sb.length();

                    if (len == 0) {
                        len = mSpannableStringBuilder.length();

                        if (len == 0) {
                            pred = '\n';
                        } else {
                            pred = mSpannableStringBuilder.charAt(len - 1);
                        }
                    } else {
                        pred = sb.charAt(len - 1);
                    }

                    if (pred != ' ' && pred != '\n') {
                        sb.append(' ');
                    }
                } else {
                    sb.append(c);
                }
            }

            mSpannableStringBuilder.append(sb);
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        private static int parseIntAttribute(Attributes attributes, String name, int defaultValue) {
            String value = attributes.getValue("", name);
            if (value != null) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // fall through
                }
            }
            return defaultValue;
        }

        private static class Bold { }
        private static class Italic { }
        private static class Underline { }
        private static class Strikethrough { }
        private static class Big { }
        private static class Small { }
        private static class Monospace { }
        private static class Blockquote { }
        private static class Super { }
        private static class Sub { }
        private static class Pre { }

        private static class CodeDiv {
            public boolean mHasPre;
            public int mLevel;
        }

        private static class NeedsReversingSpan {
            public final Object mActualSpan;
            public NeedsReversingSpan(Object actualSpan) {
                mActualSpan = actualSpan;
            }
        }

        private static class Code {
            public final boolean mInPre;

            public Code(boolean inPre) {
                mInPre = inPre;
            }
        }

        private static class List {
            public final boolean mOrdered;
            public int mPosition = 0;

            public List() {
                mOrdered = false;
            }
            public List(int position) {
                mOrdered = true;
                mPosition = position;
            }
        }

        private static class ListItem {
            public final boolean mOrdered;
            public final int mPosition;

            public ListItem(List list, Attributes attrs) {
                mOrdered = list != null && list.mOrdered;
                int position = list != null ? list.mPosition : -1;
                if (mOrdered) {
                    position = parseIntAttribute(attrs, "value", position);
                }
                mPosition = position;
                if (list != null) {
                    list.mPosition = position + 1;
                }
            }
        }

        private static class Font {
            public final String mFace;

            public Font(String face) {
                mFace = face;
            }
        }

        private static class Href {
            public final String mHref;

            public Href(String href) {
                mHref = href;
            }
        }

        private static class Foreground {
            private final int mForegroundColor;

            public Foreground(int foregroundColor) {
                mForegroundColor = foregroundColor;
            }
        }

        private static class Background {
            private final int mBackgroundColor;

            public Background(int backgroundColor) {
                mBackgroundColor = backgroundColor;
            }
        }

        private static class Heading {
            private final int mLevel;

            public Heading(int level) {
                mLevel = level;
            }
        }

        private static class Newline {
            private final int mNumNewlines;

            public Newline(int numNewlines) {
                mNumNewlines = numNewlines;
            }
        }

        private static class Alignment {
            private final Layout.Alignment mAlignment;

            public Alignment(Layout.Alignment alignment) {
                mAlignment = alignment;
            }
        }
    }
}

