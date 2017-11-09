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
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.ReactionBar;
import com.gh4a.widget.StyleableTextView;
import com.meisolsson.githubsdk.model.AuthorAssociation;
import com.meisolsson.githubsdk.model.Reaction;
import com.meisolsson.githubsdk.model.Reactions;
import com.meisolsson.githubsdk.model.User;

import java.util.Date;
import java.util.List;

import io.reactivex.Single;

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
        void addText(CharSequence text);
        boolean onMenItemClick(TimelineItem.TimelineComment comment, MenuItem menuItem);
        Single<List<Reaction>> loadReactionDetails(TimelineItem.TimelineComment item);
        Single<Reaction> addReaction(TimelineItem.TimelineComment item, String content);
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
        tvExtra.setOnClickListener(this);
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

        User user = item.getUser();
        Date createdAt = item.getCreatedAt();
        Date updatedAt = item.comment().updatedAt();

        tvExtra.setTag(user);

        AvatarHandler.assignAvatar(ivGravatar, user);
        ivGravatar.setTag(user);

        tvTimestamp.setText(StringUtils.formatRelativeTime(mContext, createdAt, true));
        if (createdAt.equals(updatedAt) || item.getReviewComment() != null) {
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
        mImageGetter.bind(tvDesc, item.comment().bodyHtml(), item.comment().id());

        // Extra view
        String login = ApiHelpers.getUserLogin(mContext, user);
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
        reactions.setReactions(item.comment().reactions());

        String ourLogin = Gh4Application.get().getAuthLogin();
        boolean canEdit = ApiHelpers.loginEquals(user, ourLogin)
                || ApiHelpers.loginEquals(mRepoOwner, ourLogin);

        int position = item.getReviewComment() != null && item.getReviewComment().position() != null
                ? item.getReviewComment().position() : -1;

        Menu menu = mPopupMenu.getMenu();
        menu.findItem(R.id.edit).setVisible(canEdit);
        menu.findItem(R.id.delete).setVisible(canEdit);
        menu.findItem(R.id.view_in_file).setVisible(item.file != null && position != -1);
    }

    @Nullable
    private String getAuthorAssociation(TimelineItem.TimelineComment item) {
        AuthorAssociation authorAssociation = item.comment().authorAssociation();
        if (authorAssociation == null) {
            return null;
        }
        switch (authorAssociation) {
            case Collaborator:
                return mContext.getString(R.string.collaborator);
            case Contributor:
                return mContext.getString(R.string.contributor);
            case FirstTimeContributor:
                return mContext.getString(R.string.first_time_contributor);
            case FirstTimer:
                return mContext.getString(R.string.first_timer);
            case Member:
                return mContext.getString(R.string.member);
            case Owner:
                return mContext.getString(R.string.owner);
            default:
                return null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_menu:
                mReactionMenuHelper.startLoadingIfNeeded();
                mPopupMenu.show();
                break;
            case R.id.iv_gravatar: {
                User user = (User) v.getTag();
                Intent intent = UserActivity.makeIntent(mContext, user);
                if (intent != null) {
                    mContext.startActivity(intent);
                }
                break;
            }
            case R.id.tv_extra: {
                User user = (User) v.getTag();
                mCallback.addText(StringUtils.formatMention(mContext, user));
                break;
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
        return mBoundItem.comment().id();
    }

    public void updateReactions(Reactions reactions) {
        if (mBoundItem != null) {
            mBoundItem.setReactions(reactions);
        }
        this.reactions.setReactions(reactions);
        mReactionMenuHelper.update();
    }


    @Override
    public Single<List<Reaction>> loadReactionDetails(ReactionBar.Item item) {
        return mCallback.loadReactionDetails(mBoundItem);
    }

    @Override
    public Single<Reaction> addReaction(ReactionBar.Item item, String content) {
        return mCallback.addReaction(mBoundItem, content);
    }
}
