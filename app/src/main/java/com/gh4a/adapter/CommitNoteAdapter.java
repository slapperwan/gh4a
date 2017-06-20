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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.StyleSpan;
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
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.ReactionBar;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.Reactions;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommitNoteAdapter extends RootAdapter<CommitComment, CommitNoteAdapter.ViewHolder>
        implements ReactionBar.Callback, ReactionBar.ReactionDetailsCache.Listener {
    public interface OnCommentAction<T> {
        void editComment(T comment);
        void deleteComment(T comment);
        void quoteText(CharSequence text);
    }

    private final HttpImageGetter mImageGetter;
    private final OnCommentAction mActionCallback;
    private final String mRepoOwner;
    private final String mRepoName;
    private ReactionBar.ReactionDetailsCache mReactionDetailsCache =
            new ReactionBar.ReactionDetailsCache(this);

    private final ViewHolder.Callback mHolderCallback = new ViewHolder.Callback() {
        @Override
        public boolean onCommentMenuItemClick(CommitComment item, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.edit:
                    mActionCallback.editComment(item);
                    return true;

                case R.id.delete:
                    mActionCallback.deleteComment(item);
                    return true;

                case R.id.share:
                    String subject = mContext.getString(R.string.share_commit_comment_subject,
                            item.getId(), mRepoOwner + "/" + mRepoName);
                    IntentUtils.share(mContext, subject, item.getHtmlUrl());
                    return true;
            }
            return false;
        }

        @Override
        public void quoteText(CharSequence text) {
            mActionCallback.quoteText(text);
        }
    };

    public CommitNoteAdapter(Context context, String repoOwner, String repoName,
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

    public void resume() {
        mImageGetter.resume();
    }

    public void pause() {
        mImageGetter.pause();
    }

    public Set<User> getUsers() {
        final HashSet<User> users = new HashSet<>();
        for (int i = 0; i < getCount(); i++) {
            final User user = getItem(i).getUser();
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    @Override
    public void clear() {
        super.clear();
        mImageGetter.clearHtmlCache();
        mReactionDetailsCache.clear();
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

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_timeline_comment, parent, false);
        ViewHolder holder = new ViewHolder(v, mHolderCallback, this, mReactionDetailsCache);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, CommitComment item) {
        final User user = item.getUser();
        final String login = ApiHelpers.getUserLogin(mContext, user);
        final Date createdAt = item.getCreatedAt();
        final Date updatedAt = item.getUpdatedAt();

        holder.mBoundItem = item;

        AvatarHandler.assignAvatar(holder.ivGravatar, user);
        holder.ivGravatar.setTag(user);

        holder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext, createdAt, true));
        if (createdAt.equals(updatedAt)) {
            holder.tvEditTimestamp.setVisibility(View.GONE);
        } else {
            holder.tvEditTimestamp.setText(StringUtils.formatRelativeTime(mContext, updatedAt, true));
            holder.tvEditTimestamp.setVisibility(View.VISIBLE);
        }

        mImageGetter.bind(holder.tvDesc, item.getBodyHtml(), item.getId());

        SpannableString userName = new SpannableString(login);
        userName.setSpan(new StyleSpan(Typeface.BOLD), 0, userName.length(), 0);
        holder.tvExtra.setText(userName);

        holder.reactions.setReactions(item.getReactions());
        holder.mReactionMenuHelper.update();

        String ourLogin = Gh4Application.get().getAuthLogin();
        boolean canEdit = ApiHelpers.loginEquals(user, ourLogin)
                || ApiHelpers.loginEquals(mRepoOwner, ourLogin);
        MenuItem editMenuItem = holder.mPopupMenu.getMenu().findItem(R.id.edit);
        MenuItem deleteMenuItem = holder.mPopupMenu.getMenu().findItem(R.id.delete);
        MenuItem reactMenuItem = holder.mPopupMenu.getMenu().findItem(R.id.react);

        editMenuItem.setVisible(mActionCallback != null && canEdit);
        deleteMenuItem.setVisible(mActionCallback != null && canEdit);
        reactMenuItem.setVisible(mActionCallback != null && ourLogin != null);
    }

    @Override
    public List<Reaction> loadReactionDetailsInBackground(ReactionBar.Item item) throws IOException {
        CommitComment comment = ((ViewHolder) item).mBoundItem;
        CommitService service = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        return service.getCommentReactions(new RepositoryId(mRepoOwner, mRepoName), comment.getId());
    }

    @Override
    public Reaction addReactionInBackground(ReactionBar.Item item, String content) throws IOException {
        CommitComment comment = ((ViewHolder) item).mBoundItem;
        CommitService service = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        return service.addCommentReaction(new RepositoryId(mRepoOwner, mRepoName), comment.getId(), content);
    }

    @Override
    public void onReactionsUpdated(ReactionBar.Item item, Reactions reactions) {
        ViewHolder holder = (ViewHolder) item;
        holder.mBoundItem.setReactions(reactions);
        holder.reactions.setReactions(reactions);
        if (holder.mReactionMenuHelper != null) {
            holder.mReactionMenuHelper.update();
        }
        notifyItemChanged(holder.getAdapterPosition());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, PopupMenu.OnMenuItemClickListener, ReactionBar.Item {
        private interface Callback {
            boolean onCommentMenuItemClick(CommitComment comment, MenuItem item);
            void quoteText(CharSequence text);
        }

        private ViewHolder(View view, Callback callback,
                ReactionBar.Callback reactionCallback,
                ReactionBar.ReactionDetailsCache reactionDetailsCache) {
            super(view);
            mCallback = callback;

            ivGravatar = (ImageView) view.findViewById(R.id.iv_gravatar);
            tvDesc = (StyleableTextView) view.findViewById(R.id.tv_desc);
            tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
            tvDesc.setCustomSelectionActionModeCallback(new UiUtils.QuoteActionModeCallback(tvDesc) {
                @Override
                public void onTextQuoted(CharSequence text) {
                    mCallback.quoteText(text);
                }
            });

            tvExtra = (StyleableTextView) view.findViewById(R.id.tv_extra);
            tvTimestamp = (TextView) view.findViewById(R.id.tv_timestamp);
            tvEditTimestamp = (TextView) view.findViewById(R.id.tv_edit_timestamp);
            ivMenu = (ImageView) view.findViewById(R.id.iv_menu);
            ivMenu.setOnClickListener(this);
            reactions = (ReactionBar) view.findViewById(R.id.reactions);
            reactions.setCallback(reactionCallback, this);
            reactions.setDetailsCache(reactionDetailsCache);

            mPopupMenu = new PopupMenu(view.getContext(), ivMenu);
            mPopupMenu.getMenuInflater().inflate(R.menu.comment_menu, mPopupMenu.getMenu());
            mPopupMenu.setOnMenuItemClickListener(this);

            MenuItem reactItem = mPopupMenu.getMenu().findItem(R.id.react);
            mPopupMenu.getMenuInflater().inflate(R.menu.reaction_menu, reactItem.getSubMenu());

            mReactionMenuHelper = new ReactionBar.AddReactionMenuHelper(view.getContext(),
                    reactItem.getSubMenu(), reactionCallback, this, reactionDetailsCache);
        }

        private final ImageView ivGravatar;
        private final StyleableTextView tvDesc;
        private final StyleableTextView tvExtra;
        private final TextView tvTimestamp;
        private final TextView tvEditTimestamp;
        private final ImageView ivMenu;
        private final ReactionBar reactions;
        private final PopupMenu mPopupMenu;
        private final Callback mCallback;

        private ReactionBar.AddReactionMenuHelper mReactionMenuHelper;
        protected CommitComment mBoundItem;

        @Override
        public Object getCacheKey() {
            return mBoundItem;
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.iv_menu) {
                if (mReactionMenuHelper != null) {
                    mReactionMenuHelper.startLoadingIfNeeded();
                }
                mPopupMenu.show();
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            if (mReactionMenuHelper != null && mReactionMenuHelper.onItemClick(menuItem)) {
                return true;
            }
            return mCallback.onCommentMenuItemClick(mBoundItem, menuItem);
        }
    }
}
