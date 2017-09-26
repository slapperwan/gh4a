package com.gh4a.adapter.timeline;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.ReactionBar;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.Reactions;
import org.eclipse.egit.github.core.User;

import java.io.IOException;
import java.util.Date;
import java.util.List;

class CommentViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineComment>
        implements View.OnClickListener, ReactionBar.Item, ReactionBar.Callback,
        PopupMenu.OnMenuItemClickListener {

    private final Context mContext;
    private final HttpImageGetter mImageGetter;
    private final Callback mCallback;
    private final String mRepoOwner;

    private final ImageView ivGravatar;
    private final StyleableTextView tvDesc;
    private final StyleableTextView tvExtra;
    private final TextView tvTimestamp;
    private final TextView tvEditTimestamp;
    private final ImageView ivMenu;
    private final ReactionBar reactions;
    private final PopupMenu mPopupMenu;
    private final ReactionBar.AddReactionMenuHelper mReactionMenuHelper;

    private TimelineItem.TimelineComment mBoundItem;

    private final UiUtils.QuoteActionModeCallback mQuoteActionModeCallback;

    public interface Callback {
        boolean canQuote();
        void quoteText(CharSequence text);
        boolean onMenItemClick(TimelineItem.TimelineComment comment, MenuItem menuItem);
        List<Reaction> loadReactionDetailsInBackground(TimelineItem.TimelineComment item)
                throws IOException;
        Reaction addReactionInBackground(TimelineItem.TimelineComment item, String content)
                throws IOException;
    }

    public CommentViewHolder(View view, HttpImageGetter imageGetter, String repoOwner,
            ReactionBar.ReactionDetailsCache reactionDetailsCache, Callback callback) {
        super(view);

        mContext = view.getContext();
        mImageGetter = imageGetter;
        mCallback = callback;
        mRepoOwner = repoOwner;

        ivGravatar = view.findViewById(R.id.iv_gravatar);
        ivGravatar.setOnClickListener(this);
        tvDesc = view.findViewById(R.id.tv_desc);
        tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        tvExtra = view.findViewById(R.id.tv_extra);
        tvTimestamp = view.findViewById(R.id.tv_timestamp);
        tvEditTimestamp = view.findViewById(R.id.tv_edit_timestamp);
        reactions = view.findViewById(R.id.reactions);
        reactions.setCallback(this, this);
        reactions.setDetailsCache(reactionDetailsCache);
        ivMenu = view.findViewById(R.id.iv_menu);
        ivMenu.setOnClickListener(this);

        mPopupMenu = new PopupMenu(view.getContext(), ivMenu);
        mPopupMenu.getMenuInflater().inflate(R.menu.comment_menu, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(this);

        MenuItem reactItem = mPopupMenu.getMenu().findItem(R.id.react);
        mPopupMenu.getMenuInflater().inflate(R.menu.reaction_menu, reactItem.getSubMenu());

        mReactionMenuHelper = new ReactionBar.AddReactionMenuHelper(view.getContext(),
                reactItem.getSubMenu(), this, this, reactionDetailsCache);
        mQuoteActionModeCallback = new UiUtils.QuoteActionModeCallback(tvDesc) {
            @Override
            public void onTextQuoted(CharSequence text) {
                mCallback.quoteText(text);
            }
        };
    }

    @Override
    public void bind(TimelineItem.TimelineComment item) {
        mBoundItem = item;

        User user = item.comment.getUser();
        Date createdAt = item.comment.getCreatedAt();
        Date updatedAt = item.comment.getUpdatedAt();

        AvatarHandler.assignAvatar(ivGravatar, user);
        ivGravatar.setTag(user);

        tvTimestamp.setText(StringUtils.formatRelativeTime(mContext, createdAt, true));
        if (createdAt.equals(updatedAt) || item.getCommitComment() != null) {
            // Unlike issue comments, the update timestamp for commit comments also changes
            // when e.g. the line number changes due to the diff the comment was made on
            // becoming outdated. As we can't distinguish those updates from comment body
            // updates, hide the edit timestamp for all commit comments.
            tvEditTimestamp.setVisibility(View.GONE);
        } else {
            tvEditTimestamp.setText(StringUtils.formatRelativeTime(mContext, updatedAt, true));
            tvEditTimestamp.setVisibility(View.VISIBLE);
        }

        // Body
        mImageGetter.bind(tvDesc, item.comment.getBodyHtml(), item.comment.getId());

        // Extra view
        String login = ApiHelpers.getUserLogin(mContext, item.comment.getUser());
        SpannableStringBuilder userName = new SpannableStringBuilder(login);
        userName.setSpan(new StyleSpan(Typeface.BOLD), 0, userName.length(), 0);

        String association = getAuthorAssociation(item);
        if (association != null) {
            int start = userName.length();
            userName.append(" (").append(association).append(")");
            userName.setSpan(new RelativeSizeSpan(0.85f), start, userName.length(), 0);
            int color = UiUtils.resolveColor(mContext, android.R.attr.textColorSecondary);
            userName.setSpan(new ForegroundColorSpan(color), start, userName.length(), 0);
        }

        tvExtra.setText(userName);

        if (mCallback.canQuote()) {
            tvDesc.setCustomSelectionActionModeCallback(mQuoteActionModeCallback);
        } else {
            tvDesc.setCustomSelectionActionModeCallback(null);
        }

        ivMenu.setTag(item);

        // Reactions
        reactions.setReactions(item.comment.getReactions());

        String ourLogin = Gh4Application.get().getAuthLogin();
        boolean canEdit = ApiHelpers.loginEquals(user, ourLogin)
                || ApiHelpers.loginEquals(mRepoOwner, ourLogin);

        Menu menu = mPopupMenu.getMenu();
        menu.findItem(R.id.edit).setVisible(canEdit);
        menu.findItem(R.id.delete).setVisible(canEdit);
        menu.findItem(R.id.view_in_file).setVisible(item.file != null
                && item.getCommitComment() != null && item.getCommitComment().getPosition() != -1);
    }

    @Nullable
    private String getAuthorAssociation(TimelineItem.TimelineComment item) {
        String authorAssociation = item.comment.getAuthorAssociation();
        if (authorAssociation == null) {
            return null;
        }
        switch (authorAssociation) {
            case Comment.ASSOCIATION_COLLABORATOR:
                return mContext.getString(R.string.collaborator);
            case Comment.ASSOCIATION_CONTRIBUTOR:
                return mContext.getString(R.string.contributor);
            case Comment.ASSOCIATION_FIRST_TIME_CONTRIBUTOR:
                return mContext.getString(R.string.first_time_contributor);
            case Comment.ASSOCIATION_FIRST_TIMER:
                return mContext.getString(R.string.first_timer);
            case Comment.ASSOCIATION_MEMBER:
                return mContext.getString(R.string.member);
            case Comment.ASSOCIATION_OWNER:
                return mContext.getString(R.string.owner);
            default:
                return null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_menu) {
            mReactionMenuHelper.startLoadingIfNeeded();
            mPopupMenu.show();
        } else if (v.getId() == R.id.iv_gravatar) {
            User user = (User) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, user);
            if (intent != null) {
                mContext.startActivity(intent);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        TimelineItem.TimelineComment comment = (TimelineItem.TimelineComment) ivMenu.getTag();
        if (mReactionMenuHelper.onItemClick(menuItem)) {
            return true;
        }
        return mCallback.onMenItemClick(comment, menuItem);
    }

    @Override
    public Object getCacheKey() {
        return mBoundItem.comment;
    }

    public void updateReactions(Reactions reactions) {
        if (mBoundItem != null) {
            mBoundItem.comment.setReactions(reactions);
        }
        this.reactions.setReactions(reactions);
        mReactionMenuHelper.update();
    }

    @Override
    public List<Reaction> loadReactionDetailsInBackground(ReactionBar.Item item) throws
            IOException {
        return mCallback.loadReactionDetailsInBackground(mBoundItem);
    }

    @Override
    public Reaction addReactionInBackground(ReactionBar.Item item, String content) throws
            IOException {
        return mCallback.addReactionInBackground(mBoundItem, content);
    }
}
