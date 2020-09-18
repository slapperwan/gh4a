package com.gh4a.adapter.timeline;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.StringRes;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.ImageView;

import com.gh4a.R;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.widget.IntentSpan;
import com.gh4a.widget.IssueLabelSpan;
import com.gh4a.widget.StyleableTextView;
import com.gh4a.widget.TimestampToastSpan;

import com.meisolsson.githubsdk.model.IssueEvent;
import com.meisolsson.githubsdk.model.IssueEventType;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.Rename;
import com.meisolsson.githubsdk.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EventViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineEvent>
        implements View.OnClickListener {

    private static final Pattern COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN =
            Pattern.compile(".*github\\.com/repos/([^/]+)/([^/]+)/commits");
    private static final HashMap<IssueEventType, Integer> EVENT_ICONS = new HashMap<>();

    static {
        EVENT_ICONS.put(IssueEventType.Closed, R.drawable.issue_event_closed);
        EVENT_ICONS.put(IssueEventType.Reopened, R.drawable.issue_event_reopened);
        EVENT_ICONS.put(IssueEventType.Merged, R.drawable.issue_event_merged);
        EVENT_ICONS.put(IssueEventType.Referenced, R.drawable.issue_event_referenced);
        EVENT_ICONS.put(IssueEventType.Assigned, R.drawable.issue_event_person);
        EVENT_ICONS.put(IssueEventType.Unassigned, R.drawable.issue_event_person);
        EVENT_ICONS.put(IssueEventType.Labeled, R.drawable.issue_event_label);
        EVENT_ICONS.put(IssueEventType.Unlabeled, R.drawable.issue_event_label);
        EVENT_ICONS.put(IssueEventType.Locked, R.drawable.issue_event_locked);
        EVENT_ICONS.put(IssueEventType.Unlocked, R.drawable.issue_event_unlocked);
        EVENT_ICONS.put(IssueEventType.Milestoned, R.drawable.issue_event_milestone);
        EVENT_ICONS.put(IssueEventType.Demilestoned, R.drawable.issue_event_milestone);
        EVENT_ICONS.put(IssueEventType.Renamed, R.drawable.issue_event_renamed);
        EVENT_ICONS.put(IssueEventType.HeadRefDeleted, R.drawable.timeline_event_branch);
        EVENT_ICONS.put(IssueEventType.HeadRefRestored, R.drawable.timeline_event_branch);
        EVENT_ICONS.put(IssueEventType.ReviewRequested, R.drawable.timeline_event_review_requested);
        EVENT_ICONS.put(IssueEventType.ReviewRequestRemoved, R.drawable.timeline_event_review_request_removed);
    }

    private final Context mContext;
    private final String mRepoOwner;
    private final String mRepoName;
    private final boolean mIsPullRequest;

    private final ImageView mAvatarView;
    private final ImageView mEventIconView;
    private final StyleableTextView mMessageView;
    private final View mAvatarContainer;

    public EventViewHolder(View itemView, String repoOwner, String repoName,
            boolean isPullRequest) {
        super(itemView);

        mContext = itemView.getContext();
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIsPullRequest = isPullRequest;

        mAvatarView = itemView.findViewById(R.id.iv_gravatar);
        mEventIconView = itemView.findViewById(R.id.iv_event_icon);
        mMessageView = itemView.findViewById(R.id.tv_message);
        mAvatarContainer = itemView.findViewById(R.id.avatar_container);
        mAvatarContainer.setOnClickListener(this);
    }

    @Override
    public void bind(TimelineItem.TimelineEvent item) {
        User user = item.event.assigner() != null
                ? item.event.assigner() : item.event.actor();
        AvatarHandler.assignAvatar(mAvatarView, user);
        mAvatarContainer.setTag(user);

        Integer eventIconResId = EVENT_ICONS.get(item.event.event());
        if (eventIconResId != null) {
            mEventIconView.setImageResource(eventIconResId);
            mEventIconView.setVisibility(View.VISIBLE);
        } else {
            mEventIconView.setVisibility(View.GONE);
        }

        mMessageView.setText(formatEvent(item.event, user,
                mMessageView.getTypefaceValue(), mIsPullRequest));
    }

    private CharSequence formatEvent(final IssueEvent event, final User user, int typefaceValue,
            boolean isPullRequestEvent) {
        String textBase = null;
        int textResId = 0;

        switch (event.event()) {
            case Closed:
                if (isPullRequestEvent) {
                    textResId = event.commitId() != null
                            ? R.string.pull_request_event_closed_with_commit
                            : R.string.pull_request_event_closed;
                } else {
                    textResId = event.commitId() != null
                            ? R.string.issue_event_closed_with_commit
                            : R.string.issue_event_closed;
                }
                break;
            case Reopened:
                textResId = isPullRequestEvent
                        ? R.string.pull_request_event_reopened
                        : R.string.issue_event_reopened;
                break;
            case Merged:
                textResId = event.commitId() != null
                        ? R.string.pull_request_event_merged_with_commit
                        : R.string.pull_request_event_merged;
                break;
            case Referenced:
                if (isPullRequestEvent) {
                    textResId = event.commitId() != null
                            ? R.string.pull_request_event_referenced_with_commit
                            : R.string.pull_request_event_referenced;
                } else {
                    textResId = event.commitId() != null
                            ? R.string.issue_event_referenced_with_commit
                            : R.string.issue_event_referenced;
                }
                break;
            case Assigned:
            case Unassigned: {
                boolean isAssign = event.event() == IssueEventType.Assigned;
                String actorLogin = user != null ? user.login() : null;
                String assigneeLogin = event.assignee() != null ? event.assignee().login() : null;
                if (assigneeLogin != null && assigneeLogin.equals(actorLogin)) {
                    if (isAssign) {
                        textResId = isPullRequestEvent
                                ? R.string.pull_request_event_assigned_self
                                : R.string.issue_event_assigned_self;
                    } else {
                        textResId = R.string.issue_event_unassigned_self;
                    }
                } else {
                    textResId = isAssign
                            ? R.string.issue_event_assigned
                            : R.string.issue_event_unassigned;
                    textBase = mContext.getString(textResId,
                            getUserLoginWithBotSuffix(user),
                            getUserLoginWithBotSuffix(event.assignee()));
                }
                break;
            }
            case Labeled:
                textResId = R.string.issue_event_labeled;
                break;
            case Unlabeled:
                textResId = R.string.issue_event_unlabeled;
                break;
            case Locked:
                textResId = R.string.issue_event_locked;
                break;
            case Unlocked:
                textResId = R.string.issue_event_unlocked;
                break;
            case Milestoned:
            case Demilestoned:
                textResId = event.event() == IssueEventType.Milestoned
                        ? R.string.issue_event_milestoned
                        : R.string.issue_event_demilestoned;
                textBase = mContext.getString(textResId,
                        getUserLoginWithBotSuffix(user), event.milestone().title());
                break;
            case Renamed: {
                Rename rename = event.rename();
                textBase = mContext.getString(R.string.issue_event_renamed,
                        getUserLoginWithBotSuffix(user), rename.from(), rename.to());
                break;
            }
            case ReviewRequested:
            case ReviewRequestRemoved: {
                if (event.requestedTeam() != null) {
                    @StringRes int stringResId = event.event() == IssueEventType.ReviewRequested
                            ? R.string.pull_request_event_team_review_requested
                            : R.string.pull_request_event_team_review_request_removed;
                    textBase = mContext.getString(stringResId,
                            getUserLoginWithBotSuffix(event.reviewRequester()),
                            mRepoOwner + "/" + event.requestedTeam().name());
                } else {
                    final String reviewerNames;
                    if (event.requestedReviewers() != null) {
                        ArrayList<String> reviewers = new ArrayList<>();
                        for (User reviewer : event.requestedReviewers()) {
                            reviewers.add(ApiHelpers.getUserLogin(mContext, reviewer));
                        }
                        reviewerNames = TextUtils.join(", ", reviewers);
                    } else {
                        reviewerNames = ApiHelpers.getUserLogin(mContext, event.requestedReviewer());
                    }
                    @StringRes int stringResId = event.event() == IssueEventType.ReviewRequested
                            ? R.string.pull_request_event_review_requested
                            : R.string.pull_request_event_review_request_removed;
                    textBase = mContext.getString(stringResId,
                            getUserLoginWithBotSuffix(event.reviewRequester()), reviewerNames);
                }
                break;
            }
            case HeadRefDeleted:
                textResId = R.string.pull_request_event_ref_deleted;
                break;
            case HeadRefRestored:
                textResId = R.string.pull_request_event_ref_restored;
                break;
            default:
                return null;
        }

        if (textBase == null) {
            textBase = mContext.getString(textResId, getUserLoginWithBotSuffix(user));
        }
        SpannableStringBuilder text = StringUtils.applyBoldTags(textBase, typefaceValue);

        int pos = text.toString().indexOf("[commit]");
        if (event.commitId() != null && pos >= 0) {
            text.replace(pos, pos + 8, event.commitId().substring(0, 7));
            text.setSpan(new IntentSpan(mContext, context -> {
                // The commit might be in a different repo. The API doesn't provide
                // that information directly, so get it indirectly by parsing the URL
                String repoOwner = mRepoOwner;
                String repoName = mRepoName;
                String url = event.commitUrl();
                if (url != null) {
                    Matcher matcher = COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN.matcher(url);
                    if (matcher.find()) {
                        repoOwner = matcher.group(1);
                        repoName = matcher.group(2);
                    }
                }

                return CommitActivity.makeIntent(context, repoOwner, repoName, event.commitId());
            }), pos, pos + 7, 0);
            text.setSpan(new TypefaceSpan("monospace"), pos, pos + 7, 0);
        }

        pos = text.toString().indexOf("[label]");
        Label label = event.label();
        if (label != null && pos >= 0) {
            int length = label.name().length();
            text.replace(pos, pos + 7, label.name());
            text.setSpan(new IssueLabelSpan(mContext, label, false), pos, pos + length, 0);
        }

        pos = text.toString().indexOf("[bot]");
        if (pos >= 0) {
            text.delete(pos, pos + 5);
            StringUtils.addUserTypeSpan(mContext, text, pos, mContext.getString(R.string.user_type_bot));
        }

        CharSequence time = event.createdAt() != null
                ? StringUtils.formatRelativeTime(mContext, event.createdAt(), true) : "";

        pos = text.toString().indexOf("[time]");
        if (pos >= 0) {
            text.replace(pos, pos + 6, time);
            if (event.createdAt() != null) {
                text.setSpan(new TimestampToastSpan(event.createdAt()), pos,
                        pos + time.length(), 0);
            }
        }

        return text;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.avatar_container) {
            User user = (User) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, user);
            if (intent != null) {
                mContext.startActivity(intent);
            }
        }
    }

    private String getUserLoginWithBotSuffix(User user) {
        if (user != null && user.login() != null) {
            return user.login();
        }
        return mContext.getString(R.string.deleted);
    }
}
