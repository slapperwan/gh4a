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

import android.content.Context;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;

public class CommentAdapter extends RootAdapter<Comment> {

    public CommentAdapter(Context context, List<Comment> objects) {
        super(context, objects);
    }

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
            viewHolder.tvDesc.setMovementMethod(LinkMovementMethod.getInstance());
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
                    Gh4Application context = (Gh4Application) v.getContext()
                            .getApplicationContext();
                    context.openUserInfoActivity(v.getContext(), comment.getUser().getLogin(), null);
                }
            });

            viewHolder.tvExtra.setText(comment.getUser().getLogin() + "\n" + pt.format(comment.getCreatedAt()));
            
            String body = comment.getBodyHtml();
            Spanned htmlSpan = Html.fromHtml(body);
            StringUtils.removeLastNewline((SpannableStringBuilder) htmlSpan);
            viewHolder.tvDesc.setText(htmlSpan);
        }
        return v;
    }
    
    private static class ViewHolder {
        
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        
    }
}