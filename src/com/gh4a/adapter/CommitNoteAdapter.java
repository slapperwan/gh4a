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

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.User;

public class CommitNoteAdapter extends RootAdapter<CommitComment, CommitNoteAdapter.ViewHolder>
        implements View.OnClickListener {
    public interface OnCommentAction {
        void editComment(CommitComment comment);
        void quoteText(CharSequence text);
    }

    private final String mRepoOwner;
    private final HttpImageGetter mImageGetter;
    private final OnCommentAction mActionCallback;

    public CommitNoteAdapter(Context context, String repoOwner, OnCommentAction actionCallback) {
        super(context);
        mRepoOwner = repoOwner;
        mActionCallback = actionCallback;
        mImageGetter = new HttpImageGetter(context);
    }

    public void destroy() {
        mImageGetter.destroy();
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_gravatar_comment, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, CommitComment comment) {
        User user = comment.getUser();

        AvatarHandler.assignAvatar(holder.ivGravatar, user);

        SpannableString userName = new SpannableString(ApiHelpers.getUserLogin(mContext, user));
        userName.setSpan(new StyleSpan(Typeface.BOLD), 0, userName.length(), 0);

        holder.ivGravatar.setTag(user);
        holder.tvExtra.setText(userName);
        holder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext,
                comment.getCreatedAt(), true));

        String body = HtmlUtils.format(comment.getBodyHtml()).toString();
        mImageGetter.bind(holder.tvDesc, body, comment.getId());

        holder.tvDesc.setCustomSelectionActionModeCallback(
                new UiUtils.QuoteActionModeCallback(holder.tvDesc) {
            @Override
            public void onTextQuoted(CharSequence text) {
                mActionCallback.quoteText(text);
            }
        });

        String ourLogin = Gh4Application.get().getAuthLogin();
        boolean canEdit = ApiHelpers.loginEquals(user, ourLogin)
                || ApiHelpers.loginEquals(mRepoOwner, ourLogin);

        if (mActionCallback != null && canEdit) {
            holder.ivEdit.setVisibility(View.VISIBLE);
            holder.ivEdit.setTag(comment);
            holder.ivEdit.setOnClickListener(this);
        } else {
            holder.ivEdit.setVisibility(View.GONE);
        }
    }

    public void resume() {
        mImageGetter.resume();
    }

    public void pause() {
        mImageGetter.pause();
    }

    @Override
    public void clear() {
        super.clear();
        mImageGetter.clearHtmlCache();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            User user = (User) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, user);
            if (intent != null) {
                mContext.startActivity(intent);
            }
        } else if (v.getId() == R.id.iv_edit) {
            CommitComment comment = (CommitComment) v.getTag();
            mActionCallback.editComment(comment);
        } else {
            super.onClick(v);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            ivGravatar = (ImageView) view.findViewById(R.id.iv_gravatar);
            tvDesc = (TextView) view.findViewById(R.id.tv_desc);
            tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
            tvExtra = (TextView) view.findViewById(R.id.tv_extra);
            tvTimestamp = (TextView) view.findViewById(R.id.tv_timestamp);
            ivEdit = (ImageView) view.findViewById(R.id.iv_edit);
        }

        private final ImageView ivGravatar;
        private final TextView tvDesc;
        private final TextView tvExtra;
        private final TextView tvTimestamp;
        private final ImageView ivEdit;
    }
}