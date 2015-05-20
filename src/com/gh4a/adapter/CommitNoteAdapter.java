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
import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class CommitNoteAdapter extends RootAdapter<CommitComment> implements View.OnClickListener {
    public interface OnEditComment {
        void editComment(CommitComment comment);
    }

    private String mRepoOwner;
    private HttpImageGetter mImageGetter;
    private OnEditComment mEditCallback;

    public CommitNoteAdapter(Context context, String repoOwner, OnEditComment editCallback) {
        super(context);
        mRepoOwner = repoOwner;
        mEditCallback = editCallback;
        mImageGetter = new HttpImageGetter(context);
    }

    public void destroy() {
        mImageGetter.destroy();
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_gravatar_comment, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);
        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.tvTimestamp = (TextView) v.findViewById(R.id.tv_timestamp);
        viewHolder.ivEdit = (ImageView) v.findViewById(R.id.iv_edit);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, CommitComment comment) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();
        String ourLogin = Gh4Application.get().getAuthLogin();
        User user = comment.getUser();

        AvatarHandler.assignAvatar(viewHolder.ivGravatar, user);

        SpannableString userName = new SpannableString(comment.getUser().getLogin());
        userName.setSpan(new StyleSpan(Typeface.BOLD), 0, userName.length(), 0);

        viewHolder.ivGravatar.setTag(comment);
        viewHolder.tvExtra.setText(userName);
        viewHolder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext,
                comment.getCreatedAt(), true));

        String body = HtmlUtils.format(comment.getBodyHtml()).toString();
        mImageGetter.bind(viewHolder.tvDesc, body, comment.getId());

        if (mEditCallback != null &&
                (user.getLogin().equals(ourLogin) || mRepoOwner.equals(ourLogin))) {
            viewHolder.ivEdit.setVisibility(View.VISIBLE);
            viewHolder.ivEdit.setTag(comment);
            viewHolder.ivEdit.setOnClickListener(this);
        } else {
            viewHolder.ivEdit.setVisibility(View.GONE);
        }
    }

    @Override
    public void clear() {
        super.clear();
        mImageGetter.destroy();
        mImageGetter = new HttpImageGetter(mContext);
    }

    @Override
    public void onClick(View v) {
        CommitComment comment = (CommitComment) v.getTag();
        if (v.getId() == R.id.iv_gravatar) {
            Intent intent = IntentUtils.getUserActivityIntent(mContext, comment.getUser());
            if (intent != null) {
                mContext.startActivity(intent);
            }
        } else if (v.getId() == R.id.iv_edit) {
            mEditCallback.editComment(comment);
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public TextView tvTimestamp;
        public ImageView ivEdit;
    }
}