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
import android.text.style.UnderlineSpan;

import com.gh4a.R;
import com.gh4a.widget.LinkSpan;

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
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import static android.graphics.Paint.Style.FILL;

public class HtmlUtils {
    public static void writeScriptInclude(StringBuilder builder, String scriptName) {
        builder.append("<script src='file:///android_asset/");
        builder.append(scriptName);
        builder.append(".js' type='text/javascript'></script>");
    }

    public static void writeCssInclude(StringBuilder builder, String cssType, String cssTheme) {
        builder.append("<link href='file:///android_asset/");
        builder.append(cssType);
        builder.append("-");
        builder.append(cssTheme);
        builder.append(".css' rel='stylesheet' type='text/css'/>");
    }

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

    private static class NumberedItemSpan implements LeadingMarginSpan {
        private final int mNumber;
        private final float mTextScaling;

        public NumberedItemSpan(int number, float textScaling) {
            mNumber = number;
            mTextScaling = textScaling;
        }

        private String getItemText() {
            return mNumber + ". ";
        }

        @Override
        public int getLeadingMargin(boolean first) {
            // Since we don't have access to the Paint object to measure text width here, we have to
            // compute the width in a more approximate way by taking into account the current text scaling
            return 4 + Math.round(getItemText().length() * 6 * mTextScaling);
        }

        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom,
                CharSequence text, int start, int end, boolean first, Layout l) {
            if (((Spanned) text).getSpanStart(this) == start) {
                Paint.Style previousStyle = p.getStyle();

                p.setStyle(Paint.Style.FILL);
                c.drawText(getItemText(), x + dir, bottom - p.descent(), p);

                p.setStyle(previousStyle);
            }
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
     */
    public static String rewriteRelativeUrls(final String html, final String repoUser,
            final String repoName, final String ref, final String folderPath) {
        final String baseUrl = "https://github.com/" + repoUser + "/" + repoName + "/blob/" + ref + "/" + folderPath;
        String rewrittenHtml = rewriteUrlsInAttribute("href", html, baseUrl);

        final String baseUrlForImages = "https://raw.github.com/" + repoUser + "/" + repoName + "/" + ref + "/" + folderPath;
        return rewriteUrlsInAttribute("src", rewrittenHtml, baseUrlForImages);
    }

    private static String rewriteUrlsInAttribute(String attribute, String html, String baseUrl) {
        final Matcher matcher = Pattern.compile("(" + attribute + ")=\"(\\S+)\"").matcher(html);
        StringBuffer sb = null; // lazy initialized only if there's any match
        while (matcher.find()) {
            String url = matcher.group(2);
            boolean isAbsoluteUrl = url.contains(":");
            boolean isAnchorUrl = url.startsWith("#");
            if (!isAbsoluteUrl && !isAnchorUrl) {
                if (url.startsWith("/")) {
                    url = baseUrl + url;
                } else {
                    url = baseUrl + "/" + url;
                }
            }
            if (sb == null) {
                sb = new StringBuffer(html.length());
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + "=\"" + url + "\""));
        }
        if (sb == null) {
            // No match was found
            return html;
        }
        matcher.appendTail(sb);
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
            1.75f, 1.5f, 1.25f, 1.1f, 1f, 0.9f
        };
        private static final float SMALL_TEXT_SIZE = 0.8f;

        private static final int REPLY_MARKER_COLOR = 0xffdddddd;

        private final float mDividerHeight;
        private final float mDisplayTextScaling;
        private final int mBulletMargin;
        private final int mReplyMargin;
        private final int mReplyMarkerSize;
        private final int mCodeBlockBackgroundColor;

        private final Context mContext;
        private final String mSource;
        private final XMLReader mReader;
        private final SpannableStringBuilder mSpannableStringBuilder;
        private final ImageGetter mImageGetter;

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
                ImageGetter imageGetter, Parser parser) {
            final Resources res = context.getResources();
            mDividerHeight = res.getDimension(R.dimen.divider_span_height);
            mDisplayTextScaling = res.getDisplayMetrics().scaledDensity;
            mBulletMargin = res.getDimensionPixelSize(R.dimen.bullet_span_margin);
            mReplyMargin = res.getDimensionPixelSize(R.dimen.reply_span_margin);
            mReplyMarkerSize = res.getDimensionPixelSize(R.dimen.reply_span_size);
            mCodeBlockBackgroundColor = UiUtils.resolveColor(context, R.attr.colorCodeBackground);

            mContext = context;
            mSource = source;
            mSpannableStringBuilder = new SpannableStringBuilder();
            mImageGetter = imageGetter;
            mReader = parser;
        }

        public Spanned convert() {
            mReader.setContentHandler(this);
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
                mSpannableStringBuilder.setSpan(span.actualSpan(), start, end,
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
                startBlockElement(attributes);
                startCssStyle(attributes);
            } else if (tag.equalsIgnoreCase("ul")) {
                startBlockElement(attributes);
                start(new List());
            } else if (tag.equalsIgnoreCase("ol")) {
                startBlockElement(attributes);
                start(new List(parseIntAttribute(attributes, "start", 1)));
            } else if (tag.equalsIgnoreCase("li")) {
                startLi(attributes);
            } else if (tag.equalsIgnoreCase("input")) {
                if ("checkbox".equalsIgnoreCase(attributes.getValue("", "type"))) {
                    boolean checked = attributes.getIndex("", "checked") >= 0;
                    Drawable d = ContextCompat.getDrawable(mContext, checked
                            ? R.drawable.checkbox_checked_small
                            : R.drawable.checkbox_unchecked_small);
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);

                    mSpannableStringBuilder.append("  ");
                    mSpannableStringBuilder.setSpan(span, mSpannableStringBuilder.length() - 2,
                            mSpannableStringBuilder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (tag.equalsIgnoreCase("div") || tag.equalsIgnoreCase("details")) {
                startBlockElement(attributes);
            } else if (tag.equalsIgnoreCase("summary")) {
                startBlockElement(attributes, 1);
            } else if (tag.equalsIgnoreCase("span")) {
                startCssStyle(attributes);
            } else if (tag.equalsIgnoreCase("hr")) {
                HorizontalLineSpan span = new HorizontalLineSpan(mDividerHeight, 0x60aaaaaa);
                appendNewlines(1);
                mSpannableStringBuilder.append(' '); // enforce the following newline to be written
                appendNewlines(1);
                int len = mSpannableStringBuilder.length();
                mSpannableStringBuilder.setSpan(span, len - 2, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            } else if (tag.equalsIgnoreCase("strong")) {
                start(new Bold());
            } else if (tag.equalsIgnoreCase("b")) {
                start(new Bold());
            } else if (tag.equalsIgnoreCase("em")) {
                start(new Italic());
            } else if (tag.equalsIgnoreCase("cite")) {
                start(new Italic());
            } else if (tag.equalsIgnoreCase("dfn")) {
                start(new Italic());
            } else if (tag.equalsIgnoreCase("i")) {
                start(new Italic());
            } else if (tag.equalsIgnoreCase("big")) {
                start(new Big());
            } else if (tag.equalsIgnoreCase("small")) {
                start(new Small());
            } else if (tag.equalsIgnoreCase("font")) {
                startFont(attributes);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                startBlockquote(attributes);
            } else if (tag.equalsIgnoreCase("samp")) {
                start(new Monospace());
            } else if (tag.equalsIgnoreCase("pre")) {
                startBlockElement(attributes, 1);
                start(new Pre());
            } else if (tag.equalsIgnoreCase("a")) {
                startA(attributes);
            } else if (tag.equalsIgnoreCase("u")) {
                start(new Underline());
            } else if (tag.equalsIgnoreCase("del")) {
                start(new Strikethrough());
            } else if (tag.equalsIgnoreCase("s")) {
                start(new Strikethrough());
            } else if (tag.equalsIgnoreCase("strike")) {
                start(new Strikethrough());
            } else if (tag.equalsIgnoreCase("sup")) {
                start(new Super());
            } else if (tag.equalsIgnoreCase("sub")) {
                start(new Sub());
            } else if (tag.equalsIgnoreCase("code") || tag.equalsIgnoreCase("tt")) {
                boolean inPre = getLast(Pre.class) != null;
                // GitHub code blocks have <code> tags inside a <pre> tag. In these cases, we ignore
                // <code> tags since the code block formatting is already applied by handling the <pre> tag.
                if (!inPre) {
                    start(new Code());
                }
            } else if (tag.length() == 2 &&
                    Character.toLowerCase(tag.charAt(0)) == 'h' &&
                    tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                startHeading(attributes, tag.charAt(1) - '1');
            } else if (tag.equalsIgnoreCase("img")) {
                startImg(attributes, mImageGetter);
            } else if (tag.equalsIgnoreCase("video")) {
                appendVideoLink(attributes.getValue("src"));
            } else if (tag.equalsIgnoreCase("table")) {
                appendNewlines(2);
            } else if (tag.equalsIgnoreCase("th")) {
                start(new Bold());
            } else if (tag.equalsIgnoreCase("td")) {
                String cssClass = attributes.getValue("class");
                if (cssClass != null) {
                    startCodeSnippetLineIfAppropriate(cssClass);
                }
            }
        }

        private void handleEndTag(String tag) {
            if (tag.equalsIgnoreCase("br")) {
                handleBr();
            } else if (tag.equalsIgnoreCase("p")) {
                endCssStyle();
                endBlockElement();
            } else if (tag.equalsIgnoreCase("ul")) {
                endBlockElement();
                end(List.class);
            } else if (tag.equalsIgnoreCase("ol")) {
                endBlockElement();
                end(List.class);
            } else if (tag.equalsIgnoreCase("li")) {
                endLi();
            } else if (tag.equalsIgnoreCase("div") ||
                       tag.equalsIgnoreCase("details") ||
                       tag.equalsIgnoreCase("summary")) {
                endBlockElement();
            } else if (tag.equalsIgnoreCase("span")) {
                endCssStyle();
            } else if (tag.equalsIgnoreCase("strong")) {
                end(Bold.class, new StyleSpan(Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("b")) {
                end(Bold.class, new StyleSpan(Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("em")) {
                end(Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("cite")) {
                end(Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("dfn")) {
                end(Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("i")) {
                end(Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("big")) {
                end(Big.class, new RelativeSizeSpan(1.25f));
            } else if (tag.equalsIgnoreCase("small")) {
                end(Small.class, new RelativeSizeSpan(SMALL_TEXT_SIZE));
            } else if (tag.equalsIgnoreCase("font")) {
                endFont();
            } else if (tag.equalsIgnoreCase("blockquote")) {
                endBlockquote();
            } else if (tag.equalsIgnoreCase("samp")) {
                end(Monospace.class, new TypefaceSpan("monospace"));
            } else if (tag.equalsIgnoreCase("pre")) {
                endBlockElement();
                end(Pre.class, new TypefaceSpan("monospace"), new CodeBlockSpan(mCodeBlockBackgroundColor));
            } else if (tag.equalsIgnoreCase("a")) {
                endA();
            } else if (tag.equalsIgnoreCase("u")) {
                end(Underline.class, new UnderlineSpan());
            } else if (tag.equalsIgnoreCase("del")) {
                end(Strikethrough.class, new StrikethroughSpan());
            } else if (tag.equalsIgnoreCase("s")) {
                end(Strikethrough.class, new StrikethroughSpan());
            } else if (tag.equalsIgnoreCase("strike")) {
                end(Strikethrough.class, new StrikethroughSpan());
            } else if (tag.equalsIgnoreCase("sup")) {
                end(Super.class, new SuperscriptSpan(), new RelativeSizeSpan(SMALL_TEXT_SIZE));
            } else if (tag.equalsIgnoreCase("sub")) {
                end(Sub.class, new SubscriptSpan(), new RelativeSizeSpan(SMALL_TEXT_SIZE));
            } else if (tag.equalsIgnoreCase("code") || tag.equalsIgnoreCase("tt")) {
                end(Code.class, new TypefaceSpan("monospace"), new BackgroundColorSpan(mCodeBlockBackgroundColor));
            } else if (tag.length() == 2 &&
                    Character.toLowerCase(tag.charAt(0)) == 'h' &&
                    tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                endHeading();
            } else if (tag.equalsIgnoreCase("table")) {
                appendNewlines(2);
            } else if (tag.equalsIgnoreCase("tr")) {
                mSpannableStringBuilder.append('\n');
                Code codeMark = getLast(Code.class);
                if (codeMark != null) {
                    endCodeSnippetLine(codeMark);
                }
            } else if (tag.equalsIgnoreCase("td")) {
                mSpannableStringBuilder.append('\u2003');
            } else if (tag.equalsIgnoreCase("th")) {
                end(Bold.class, new StyleSpan(Typeface.BOLD));
                mSpannableStringBuilder.append('\u2003');
            }
        }

        private void appendNewlines(int minNewline) {
            final int len = mSpannableStringBuilder.length();

            if (len == 0) {
                return;
            }

            int existingNewlines = 0;
            for (int i = len - 1; i >= 0 && mSpannableStringBuilder.charAt(i) == '\n'; i--) {
                existingNewlines++;
            }

            for (int j = existingNewlines; j < minNewline; j++) {
                mSpannableStringBuilder.append("\n");
            }
        }

        private void appendVideoLink(String videoUrl) {
            mSpannableStringBuilder.append("\uD83C\uDFA5 "); // movie camera emoji
            String videoLinkText = mContext.getString(R.string.view_video);
            mSpannableStringBuilder.append(videoLinkText);
            mSpannableStringBuilder.setSpan(new LinkSpan(videoUrl),
                    mSpannableStringBuilder.length() - videoLinkText.length(),
                    mSpannableStringBuilder.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        private void startBlockElement(Attributes attributes) {
            startBlockElement(attributes, 2);
        }

        private void startBlockElement(Attributes attributes, int newlines) {
            appendNewlines(newlines);
            start(new BlockElement(newlines, getBlockElementTypeFor(attributes)));

            String style = attributes.getValue("", "style");
            if (style != null) {
                Matcher m = getTextAlignPattern().matcher(style);
                if (m.find()) {
                    String alignment = m.group(1);
                    if (alignment.equalsIgnoreCase("start")) {
                        start(new Alignment(Layout.Alignment.ALIGN_NORMAL));
                    } else if (alignment.equalsIgnoreCase("center")) {
                        start(new Alignment(Layout.Alignment.ALIGN_CENTER));
                    } else if (alignment.equalsIgnoreCase("end")) {
                        start(new Alignment(Layout.Alignment.ALIGN_OPPOSITE));
                    }
                }
            }
        }

        private void endBlockElement() {
            BlockElement block = getLast(BlockElement.class);
            if (block != null) {
                appendNewlines(block.numNewlines());
                end(BlockElement.class, getSpansForBlockElementType(block.type()));
            }

            Alignment a = getLast(Alignment.class);
            if (a != null) {
                setSpanFromMark(a, new AlignmentSpan.Standard(a.alignment()));
            }
        }

        @NonNull
        private BlockElement.Type getBlockElementTypeFor(Attributes attributes) {
            String classAttribute = Optional.ofNullable(attributes.getValue("class")).orElse("");

            /*
             * GitHub's Markdown alerts are rendered as <div>s with ad hoc styling, as in the following example:
             * <div class="markdown-alert markdown-alert-note" dir="auto">
             *   <p class="markdown-alert-title" dir="auto"> [title] </p>
             *   <p dir="auto"> [content] </p>
             * </div>
             */
            var cssClasses = Arrays.asList(classAttribute.split(" "));
            if (cssClasses.contains("markdown-alert")) {
                return BlockElement.Type.Alert;
            } else if (cssClasses.contains("markdown-alert-title")) {
                return BlockElement.Type.AlertTitle;
            } else {
                return BlockElement.Type.Generic;
            }
        }

        private Object[] getSpansForBlockElementType(@NonNull BlockElement.Type type) {
            return switch (type) {
                case Alert -> new Object[] { new ReplySpan(mReplyMargin, mReplyMarkerSize, REPLY_MARKER_COLOR) };
                case AlertTitle -> new Object[] { new StyleSpan(Typeface.BOLD_ITALIC) };
                case Generic -> new Object[0];
            };
        }

        private void handleBr() {
            mSpannableStringBuilder.append('\n');
        }

        private void startLi(Attributes attributes) {
            ListItem item = new ListItem(getLast(List.class), attributes);
            startBlockElement(attributes, 1);
            start(item);
            startCssStyle(attributes);
        }

        private void endLi() {
            endCssStyle();
            endBlockElement();
            ListItem item = getLast(ListItem.class);
            if (item != null) {
                if (item.mOrdered) {
                    setSpanFromMark(item, new NumberedItemSpan(item.mPosition, mDisplayTextScaling));
                } else {
                    setSpanFromMark(item, new BulletSpan(mBulletMargin));
                }
            }
        }

        private void startBlockquote(Attributes attributes) {
            startBlockElement(attributes);
            start(new Blockquote());
        }

        private void endBlockquote() {
            endBlockElement();
            end(Blockquote.class, new ReplySpan(mReplyMargin, mReplyMarkerSize, REPLY_MARKER_COLOR));
        }

        private void startHeading(Attributes attributes, int level) {
            startBlockElement(attributes);
            start(new Heading(level));
        }

        private void endHeading() {
            // RelativeSizeSpan and StyleSpan are CharacterStyles
            // Their ranges should not include the newlines at the end
            Heading h = getLast(Heading.class);
            if (h != null) {
                setSpanFromMark(h, new RelativeSizeSpan(HEADING_SIZES[h.level()]),
                        new StyleSpan(Typeface.BOLD));
            }

            endBlockElement();
        }

        /*
         * Embedded code snippets in comments (which include suggested changes) are rendered in an HTML table
         * in which every <tr> maps to a line of code, as you can see in the following example
         * taken from a suggested change snippet:
         * <tbody>
         *   <tr class="border-0">
         *     <td class="blob-num blob-num-deletion [...]" data-line-number=""></td>
         *     <td class="blob-code-deletion js-blob-code-deletion [...]"> [line of code] </td>
         *   </tr>
         *   <tr class="border-0">
         *     <td class="blob-num blob-num-addition [...]" data-line-number=""></td>
         *     <td class="blob-code-addition js-blob-code-addition [...]"> [line of code] </td>
         *   </tr>
         * </tbody>
         */
        private void startCodeSnippetLineIfAppropriate(String tdCssClass) {
            if (tdCssClass.contains("blob-num-addition")) {
                start(new Pre());
                int color = UiUtils.resolveColor(mContext, R.attr.colorDiffAddBackground);
                start(new Code(color));
                mSpannableStringBuilder.append('+');
            } else if (tdCssClass.contains("blob-num-deletion")) {
                start(new Pre());
                int color = UiUtils.resolveColor(mContext, R.attr.colorDiffRemoveBackground);
                start(new Code(color));
                mSpannableStringBuilder.append('-');
            } else if (tdCssClass.contains("blob-num")) {
                start(new Pre());
                start(new Code(mCodeBlockBackgroundColor));
            }
        }

        private void endCodeSnippetLine(Code codeMark) {
            mSpannableStringBuilder.removeSpan(getLast(Pre.class));
            setSpanFromMark(codeMark, new TypefaceSpan("monospace"), new CodeBlockSpan(codeMark.color()));
        }

        private <T> T getLast(Class<T> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
            T[] objs = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length(), kind);

            if (objs.length == 0) {
                return null;
            } else {
                return objs[objs.length - 1];
            }
        }

        private void setSpanFromMark(Object mark, Object... spans) {
            int where = mSpannableStringBuilder.getSpanStart(mark);
            mSpannableStringBuilder.removeSpan(mark);
            int len = mSpannableStringBuilder.length();
            if (where != len) {
                for (Object span : spans) {
                    if (span instanceof LeadingMarginSpan) {
                        span = new NeedsReversingSpan(span);
                    }
                    mSpannableStringBuilder.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private void start(Object mark) {
            int len = mSpannableStringBuilder.length();
            mSpannableStringBuilder.setSpan(mark, len, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        private void end(Class<?> kind, Object... spans) {
            Object obj = getLast(kind);
            if (obj != null) {
                setSpanFromMark(obj, spans);
            }
        }

        private void startCssStyle(Attributes attributes) {
            String style = attributes.getValue("", "style");
            if (style != null) {
                Matcher m = getForegroundColorPattern().matcher(style);
                if (m.find()) {
                    Integer c = parseColor(m.group(1));
                    if (c != null) {
                        start(new Foreground(c));
                    }
                }

                m = getBackgroundColorPattern().matcher(style);
                if (m.find()) {
                    Integer c = parseColor(m.group(1));
                    if (c != null) {
                        start(new Background(c));
                    }
                }

                m = getTextDecorationPattern().matcher(style);
                if (m.find()) {
                    String textDecoration = m.group(1);
                    if (textDecoration.equalsIgnoreCase("line-through")) {
                        start(new Strikethrough());
                    }
                }
            }
        }

        private void endCssStyle() {
            Strikethrough s = getLast(Strikethrough.class);
            if (s != null) {
                setSpanFromMark(s, new StrikethroughSpan());
            }

            Background b = getLast(Background.class);
            if (b != null) {
                setSpanFromMark(b, new BackgroundColorSpan(b.backgroundColor()));
            }

            Foreground f = getLast(Foreground.class);
            if (f != null) {
                setSpanFromMark(f, new ForegroundColorSpan(f.foregroundColor()));
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

        private void startImg(Attributes attributes, ImageGetter img) {
            String src = attributes.getValue("", "src");
            Drawable d = img.getDrawable(src);

            int len = mSpannableStringBuilder.length();
            mSpannableStringBuilder.append("\uFFFC");

            mSpannableStringBuilder.setSpan(new ImageSpan(d, src), len, mSpannableStringBuilder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private void startFont(Attributes attributes) {
            String color = attributes.getValue("", "color");
            String face = attributes.getValue("", "face");

            if (!TextUtils.isEmpty(color)) {
                Integer c = parseColor(color);
                if (c != null) {
                    start(new Foreground(c));
                }
            }

            if (!TextUtils.isEmpty(face)) {
                start(new Font(face));
            }
        }

        private void endFont() {
            Font font = getLast(Font.class);
            if (font != null) {
                setSpanFromMark(font, new TypefaceSpan(font.face()));
            }

            Foreground foreground = getLast(Foreground.class);
            if (foreground != null) {
                setSpanFromMark(foreground, new ForegroundColorSpan(foreground.foregroundColor()));
            }
        }

        private void startA(Attributes attributes) {
            String href = attributes.getValue("", "href");
            start(new Href(href));
        }

        private void endA() {
            Href h = getLast(Href.class);
            if (h != null) {
                if (h.href() != null) {
                    setSpanFromMark(h, new LinkSpan(h.href()));
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

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (getLast(Pre.class) != null) {
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

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
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
        private record Font(String face) { }
        private record Href(String href) { }
        private record Foreground(int foregroundColor) { }
        private record Background(int backgroundColor) { }
        private record Heading(int level) { }
        private record Alignment(Layout.Alignment alignment) { }
        private record NeedsReversingSpan(Object actualSpan) { }

        private record Code(int color) {
            public Code() {
                this(0);
            }
        }

        private record BlockElement(int numNewlines, @NonNull BlockElement.Type type) {
            enum Type {
                Alert,
                AlertTitle,
                Generic
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
    }
}

