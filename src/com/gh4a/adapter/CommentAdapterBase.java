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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.User;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

abstract class CommentAdapterBase<T> extends RootAdapter<T, CommentAdapterBase.ViewHolder> {
    public interface OnCommentAction<T> {
        void editComment(T comment);
        void quoteText(CharSequence text);
    }

    private final ViewHolder.OnCommentMenuItemClick<T> mCommentMenuItemClickCallback =
            new ViewHolder.OnCommentMenuItemClick<T>() {
        @Override
        public boolean onCommentMenuItemClick(T item, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.edit:
                    mActionCallback.editComment(item);
                    return true;

                case R.id.share:
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject(item));
                    shareIntent.putExtra(Intent.EXTRA_TEXT, getUrl(item));
                    shareIntent = Intent.createChooser(shareIntent,
                            mContext.getString(R.string.share_title));
                    mContext.startActivity(shareIntent);
                    return true;
            }
            return false;
        }
    };

    private final HttpImageGetter mImageGetter;
    private final OnCommentAction mActionCallback;
    protected final String mRepoOwner;
    protected final String mRepoName;

    protected CommentAdapterBase(Context context, String repoOwner, String repoName,
            OnCommentAction actionCallback) {
        super(context);
        mImageGetter = new HttpImageGetter(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mActionCallback = actionCallback;
    }

    public void destroy() {
        mImageGetter.destroy();
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_gravatar_comment, parent, false);
        ViewHolder holder = new ViewHolder(v, mCommentMenuItemClickCallback);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, T item) {
        User user = getUser(item);
        Date createdAt = getCreatedAt(item);
        Date updatedAt = getUpdatedAt(item);

        AvatarHandler.assignAvatar(holder.ivGravatar, user);

        holder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext, createdAt, true));
        if (createdAt.equals(updatedAt)) {
            holder.tvEditTimestamp.setVisibility(View.GONE);
        } else {
            holder.tvEditTimestamp.setText(StringUtils.formatRelativeTime(mContext, updatedAt, true));
            holder.tvEditTimestamp.setVisibility(View.VISIBLE);
        }

        bindBodyView(item, holder.tvDesc, mImageGetter);
        bindExtraView(item, holder.tvExtra);
        bindFileView(item, holder.tvFile);
        bindEventIcon(item, holder.ivEventIcon);

        if (canQuote(item)) {
            holder.tvDesc.setCustomSelectionActionModeCallback(
                    new UiUtils.QuoteActionModeCallback(holder.tvDesc) {
                @Override
                public void onTextQuoted(CharSequence text) {
                    mActionCallback.quoteText(text);
                }
            });
        } else {
            holder.tvDesc.setCustomSelectionActionModeCallback(null);
        }

        if (hasActionMenu(item)) {
            holder.ivMenu.setTag(item);
            holder.ivMenu.setVisibility(View.VISIBLE);
        } else {
            holder.ivMenu.setVisibility(View.GONE);
        }

        String ourLogin = Gh4Application.get().getAuthLogin();
        boolean canEdit = ApiHelpers.loginEquals(user, ourLogin)
                || ApiHelpers.loginEquals(mRepoOwner, ourLogin);
        MenuItem editMenuItem = holder.mPopupMenu.getMenu().findItem(R.id.edit);

        editMenuItem.setVisible(mActionCallback != null && canEdit);
    }

    public void resume() {
        mImageGetter.resume();
    }

    public void pause() {
        mImageGetter.pause();
    }

    public Set<User> getUsers() {
        final HashSet<User> users = new HashSet<>();
        for (int i = 0; i < getCount(); i++) {
            final T item = getItem(i);
            users.add(getUser(item));
        }
        return users;
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
        } else {
            super.onClick(v);
        }
    }

    protected abstract User getUser(T item);
    protected abstract Date getCreatedAt(T item);
    protected abstract Date getUpdatedAt(T item);
    protected abstract String getUrl(T item);
    protected abstract String getShareSubject(T item);
    protected abstract void bindBodyView(T item, StyleableTextView view, HttpImageGetter imageGetter);
    protected abstract void bindExtraView(T item, StyleableTextView view);
    protected abstract void bindFileView(T item, StyleableTextView view);
    protected abstract void bindEventIcon(T item, ImageView view);
    protected abstract boolean hasActionMenu(T item);
    protected abstract boolean canQuote(T item);

    public static class ViewHolder<T> extends RecyclerView.ViewHolder
            implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        private interface OnCommentMenuItemClick<T> {
            boolean onCommentMenuItemClick(T comment, MenuItem item);
        }

        private ViewHolder(View view, OnCommentMenuItemClick commentMenuItemClickCallback) {
            super(view);
            mCommentMenuItemClickCallback = commentMenuItemClickCallback;

            ivGravatar = (ImageView) view.findViewById(R.id.iv_gravatar);
            ivEventIcon = (ImageView) view.findViewById(R.id.iv_event_icon);
            tvDesc = (StyleableTextView) view.findViewById(R.id.tv_desc);
            tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
            tvExtra = (StyleableTextView) view.findViewById(R.id.tv_extra);
            tvTimestamp = (TextView) view.findViewById(R.id.tv_timestamp);
            tvEditTimestamp = (TextView) view.findViewById(R.id.tv_edit_timestamp);
            tvFile = (StyleableTextView) view.findViewById(R.id.tv_file);
            ivMenu = (ImageView) view.findViewById(R.id.iv_menu);
            ivMenu.setOnClickListener(this);

            mPopupMenu = new PopupMenu(view.getContext(), ivMenu);
            mPopupMenu.getMenuInflater().inflate(R.menu.comment_menu, mPopupMenu.getMenu());
            mPopupMenu.setOnMenuItemClickListener(this);
        }

        private final ImageView ivGravatar;
        private final ImageView ivEventIcon;
        private final StyleableTextView tvDesc;
        private final StyleableTextView tvExtra;
        private final TextView tvTimestamp;
        private final TextView tvEditTimestamp;
        private final StyleableTextView tvFile;
        private final ImageView ivMenu;
        private final PopupMenu mPopupMenu;
        private final OnCommentMenuItemClick mCommentMenuItemClickCallback;

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.iv_menu) {
                mPopupMenu.show();
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            T item = (T) ivMenu.getTag();
            return mCommentMenuItemClickCallback.onCommentMenuItemClick(item, menuItem);
        }
    }
}
