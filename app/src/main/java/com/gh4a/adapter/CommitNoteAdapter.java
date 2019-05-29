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
import android.net.Uri;
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
import com.gh4a.ServiceFactory;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.ReactionBar;
import com.gh4a.widget.StyleableTextView;
import com.meisolsson.githubsdk.model.Reaction;
import com.meisolsson.githubsdk.model.Reactions;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.model.request.ReactionRequest;
import com.meisolsson.githubsdk.service.reactions.ReactionService;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;

public class CommitNoteAdapter extends RootAdapter<GitComment, CommitNoteAdapter.ViewHolder>
        implements ReactionBar.Callback, ReactionBar.ReactionDetailsCache.Listener {
    public interface OnCommentAction<T> {
        void editComment(T comment);
        void deleteComment(T comment);
        void quoteText(CharSequence text);
        void addText(CharSequence text);
    }

    private final HttpImageGetter mImageGetter;
    private final OnCommentAction mActionCallback;
    private final String mRepoOwner;
    private final String mRepoName;
    private final ReactionBar.ReactionDetailsCache mReactionDetailsCache =
            new ReactionBar.ReactionDetailsCache(this);

    private final ViewHolder.Callback mHolderCallback = new ViewHolder.Callback() {
        @Override
        public boolean onCommentMenuItemClick(GitComment item, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.edit:
                    mActionCallback.editComment(item);
                    return true;

                case R.id.delete:
                    mActionCallback.deleteComment(item);
                    return true;

                case R.id.share:
                    String subject = mContext.getString(R.string.share_commit_comment_subject,
                            item.id(), mRepoOwner + "/" + mRepoName);
                    IntentUtils.share(mContext, subject, Uri.parse(item.htmlUrl()));
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
        mReactionDetailsCache.destroy();
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
            final User user = getItem(i).user();
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
        } else if (v.getId() == R.id.tv_extra) {
            User user = (User) v.getTag();
            mActionCallback.addText(StringUtils.formatMention(mContext, user));
        } else {
            super.onClick(v);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_timeline_comment, parent, false);
        ViewHolder holder = new ViewHolder(v, mHolderCallback, this, mReactionDetailsCache);
        holder.ivGravatar.setOnClickListener(this);
        holder.tvExtra.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, GitComment item) {
        final User user = item.user();
        final String login = ApiHelpers.getUserLogin(mContext, user);
        final Date createdAt = item.createdAt();
        final Date updatedAt = item.updatedAt();

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

        mImageGetter.bind(holder.tvDesc, item.bodyHtml(), item.id());

        SpannableString userName = new SpannableString(login);
        userName.setSpan(new StyleSpan(Typeface.BOLD), 0, userName.length(), 0);
        holder.tvExtra.setText(userName);
        holder.tvExtra.setTag(user);

        holder.reactions.setReactions(item.reactions());
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
    public Single<List<Reaction>> loadReactionDetails(ReactionBar.Item item, boolean bypassCache) {
        final GitComment comment = ((ViewHolder) item).mBoundItem;
        final ReactionService service = ServiceFactory.get(ReactionService.class, bypassCache);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getCommitCommentReactions(mRepoOwner, mRepoName, comment.id(), page));
    }

    @Override
    public Single<Reaction> addReaction(ReactionBar.Item item, String content) {
        GitComment comment = ((ViewHolder) item).mBoundItem;
        ReactionService service = ServiceFactory.get(ReactionService.class, false);
        ReactionRequest request = ReactionRequest.builder().content(content).build();
        return service.createCommitCommentReaction(mRepoOwner, mRepoName, comment.id(), request)
                .map(ApiHelpers::throwOnFailure);
    }

    @Override
    public void onReactionsUpdated(ReactionBar.Item item, Reactions reactions) {
        ViewHolder holder = (ViewHolder) item;
        holder.mBoundItem = holder.mBoundItem.toBuilder().reactions(reactions).build();
        holder.reactions.setReactions(reactions);
        if (holder.mReactionMenuHelper != null) {
            holder.mReactionMenuHelper.update();
        }
        notifyItemChanged(holder.getAdapterPosition());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, PopupMenu.OnMenuItemClickListener, ReactionBar.Item {
        private interface Callback {
            boolean onCommentMenuItemClick(GitComment comment, MenuItem item);
            void quoteText(CharSequence text);
        }

        private ViewHolder(View view, Callback callback,
                ReactionBar.Callback reactionCallback,
                ReactionBar.ReactionDetailsCache reactionDetailsCache) {
            super(view);
            mCallback = callback;

            ivGravatar = view.findViewById(R.id.iv_gravatar);
            tvDesc = view.findViewById(R.id.tv_desc);
            tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
            tvDesc.setCustomSelectionActionModeCallback(new UiUtils.QuoteActionModeCallback(tvDesc) {
                @Override
                public void onTextQuoted(CharSequence text) {
                    mCallback.quoteText(text);
                }
            });

            tvExtra = view.findViewById(R.id.tv_extra);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvEditTimestamp = view.findViewById(R.id.tv_edit_timestamp);
            ivMenu = view.findViewById(R.id.iv_menu);
            ivMenu.setOnClickListener(this);
            reactions = view.findViewById(R.id.reactions);
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

        private final ReactionBar.AddReactionMenuHelper mReactionMenuHelper;
        protected GitComment mBoundItem;

        @Override
        public Object getCacheKey() {
            return mBoundItem.id();
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
