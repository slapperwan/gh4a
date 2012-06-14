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
package com.gh4a.adapter;

import java.util.List;

import org.eclipse.egit.github.core.Comment;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;

/**
 * The Comment adapter.
 */
public class CommentAdapter extends RootAdapter<Comment> {

    /**
     * Instantiates a new comment adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public CommentAdapter(Context context, List<Comment> objects) {
        super(context, objects);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View,
     * android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_gravatar_comment, null);
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);

            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Comment comment = mObjects.get(position);
        if (comment != null) {
            ImageDownloader.getInstance().download(comment.getUser().getGravatarId(), viewHolder.ivGravatar);
            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    Gh4Application context = (Gh4Application) v.getContext()
                            .getApplicationContext();
                    context.openUserInfoActivity(v.getContext(), comment.getUser().getLogin(), null);
                }
            });

            Resources res = v.getResources();
            String extraData = String.format(res.getString(R.string.more_data), comment.getUser().getLogin(),
                    pt.format(comment.getCreatedAt()));

            viewHolder.tvExtra.setText(extraData);
            
            String body = comment.getBody();
            body = body.replaceAll("\n", "<br/>");
            viewHolder.tvDesc.setText(Html.fromHtml(body));
        }
        return v;
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {
        
        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;
    }

    /**
     * The Class InternalWebViewClient.
     */
    private class InternalWebViewClient extends WebViewClient {

        /*
         * (non-Javadoc)
         * @see
         * android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit
         * .WebView, java.lang.String)
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return false;
        }
    }

    /**
     * Sets the span between tokens.
     * 
     * @param text the text
     * @param token the token
     * @param cs the cs
     * @return the char sequence
     */
    public static CharSequence setSpanBetweenTokens(CharSequence text, String token,
            CharacterStyle... cs) {
        // Start and end refer to the points where the span will apply
        int tokenLen = token.length();
        int start = text.toString().indexOf(token) + tokenLen;
        int end = text.toString().indexOf(token, start);

        if (start > -1 && end > -1) {
            // Copy the spannable string to a mutable spannable string
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            for (CharacterStyle c : cs)
                ssb.setSpan(c, start, end, 0);

            // Delete the tokens before and after the span
            ssb.delete(end, end + tokenLen);
            ssb.delete(start - tokenLen, start);

            text = ssb;
        }

        return text;
    }

    /**
     * The Class CommentTagHandler.
     */
    private class CommentTagHandler implements TagHandler {

        /*
         * (non-Javadoc)
         * @see android.text.Html.TagHandler#handleTag(boolean,
         * java.lang.String, android.text.Editable, org.xml.sax.XMLReader)
         */
        @Override
        public void handleTag(boolean arg0, String arg1, Editable arg2, XMLReader reader) {
            Log.v(Constants.LOG_TAG, arg0 + " +++ " + arg1 + " +++ " + arg2 + " +++ " + reader);
        }

    }

}