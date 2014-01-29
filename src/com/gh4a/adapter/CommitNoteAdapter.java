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

import org.eclipse.egit.github.core.CommitComment;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.StringUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class CommitNoteAdapter extends RootAdapter<CommitComment> implements OnClickListener {
    private HttpImageGetter mImageGetter;
    
    public CommitNoteAdapter(Context context) {
        super(context);
        mImageGetter = new HttpImageGetter(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_gravatar_comment, null);
        ViewHolder viewHolder = new ViewHolder();
        
        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);
        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setMovementMethod(LinkMovementMethod.getInstance());
        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.ivEdit = (ImageView) v.findViewById(R.id.iv_edit);
        viewHolder.ivEdit.setVisibility(View.GONE);
        
        v.setTag(viewHolder);
        return v;
    }
    
    @Override
    protected void bindView(View v, CommitComment comment) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        GravatarHandler.assignGravatar(viewHolder.ivGravatar, comment.getUser());

        viewHolder.ivGravatar.setTag(comment);
        viewHolder.tvExtra.setText(comment.getUser().getLogin() + "\n"
                + StringUtils.formatRelativeTime(mContext, comment.getCreatedAt(), true));

        String body = HtmlUtils.format(comment.getBodyHtml()).toString();
        mImageGetter.bind(viewHolder.tvDesc, body, comment.getId());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            CommitComment comment = (CommitComment) v.getTag();
            /** Open user activity */
            Gh4Application.get(mContext).openUserInfoActivity(mContext,
                    comment.getUser().getLogin(), null);
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }
    
    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public ImageView ivEdit;
    }
}