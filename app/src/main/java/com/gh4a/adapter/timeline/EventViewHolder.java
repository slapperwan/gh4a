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
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.widget.IntentSpan;
import com.gh4a.widget.IssueLabelSpan;
import com.gh4a.widget.LinkHandlingTextView;
import com.gh4a.widget.TimestampToastSpan;

import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueEvent;
import com.meisolsson.githubsdk.model.IssueEventType;
import com.meisolsson.githubsdk.model.IssueStateReason;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.Rename;
import com.meisolsson.githubsdk.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EventViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineEvent>
        implements View.OnClickListener {

    private static final Pattern COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN =
            Pattern.compile(".*github\\.com/repos/([^/]+)/([^/]+)/commits");

    private final Context mContext;
    private final String mRepoOwner;
    private final String mRepoName;
    private final boolean mIsPullRequest;

    private final ImageView mAvatarView;
    private final ImageView mEventIconView;
    private final LinkHandlingTextView mMessageView;
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

        Integer eventIconResId = getEventIcon(item.event);
        if (eventIconResId != null) {
            mEventIconView.setImageResource(eventIconResId);
            mEventIconView.setVisibility(View.VISIBLE);
        } else {
            mEventIconView.setVisibility(View.GONE);
        }

        mMessageView.setText(formatEvent(item.event, user));
    }

    private Integer getEventIcon(IssueEvent event) {
        switch (event.event()) {
            case Closed:
                return mIsPullRequest || event.stateReason() == IssueStateReason.NotPlanned
                        ? R.drawable.issue_event_closed
                        : R.drawable.issue_event_closed_completed;
            case Reopened: return R.drawable.issue_event_reopened;
            case Merged: return R.drawable.issue_event_merged;
            case Referenced: return R.drawable.issue_event_referenced;
            case Assigned:
            case Unassigned:
                return R.drawable.issue_event_person;
            case Labeled:
            case Unlabeled:
                return R.drawable.issue_event_label;
            case Locked: return R.drawable.issue_event_locked;
            case Unlocked: return R.drawable.issue_event_unlocked;
            case Milestoned:
            case Demilestoned:
                return R.drawable.issue_event_milestone;
            case Renamed: return R.drawable.issue_event_renamed;
            case CommentDeleted:
            case ReviewDismissed:
                return R.drawable.timeline_event_dismissed_deleted;
            case HeadRefDeleted:
            case HeadRefRestored:
            case HeadRefForcePushed:
            case ConvertToDraft:
                return R.drawable.timeline_event_branch;
            case ReviewRequested:
            case ReadyForReview:
                return R.drawable.timeline_event_review;
            case ReviewRequestRemoved: return R.drawable.timeline_event_review_request_removed;
            case CrossReferenced: return R.drawable.timeline_event_cross_referenced;
        }
        return null;
    }

    private CharSequence formatEvent(final IssueEvent event, final User user) {
        String textBase = null;
        int textResId = 0;
        String commitId = event.commitId();
        String commitUrl = event.commitUrl();

        switch (event.event()) {
            case Closed:
                if (mIsPullRequest) {
                    textResId = commitId != null
                            ? R.string.pull_request_event_closed_with_commit
                            : R.string.pull_request_event_closed;
                } else {
                    if (event.stateReason() == IssueStateReason.NotPlanned) {
                        textResId = commitId != null
                                ? R.string.issue_event_closed_not_planned_with_commit
                                : R.string.issue_event_closed_not_planned;
                    } else {
                        textResId = commitId != null
                                ? R.string.issue_event_closed_completed_with_commit
                                : R.string.issue_event_closed_completed;
                    }
                }
                break;
            case Reopened:
                textResId = mIsPullRequest
                        ? R.string.pull_request_event_reopened
                        : R.string.issue_event_reopened;
                break;
            case Merged:
                textResId = commitId != null
                        ? R.string.pull_request_event_merged_with_commit
                        : R.string.pull_request_event_merged;
                break;
            case Referenced:
                if (mIsPullRequest) {
                    textResId = commitId != null
                            ? R.string.pull_request_event_referenced_with_commit
                            : R.string.pull_request_event_referenced;
                } else {
                    textResId = commitId != null
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
                        textResId = mIsPullRequest
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
                if (event.lockReason() == null) {
                    textResId = R.string.issue_event_locked;
                } else {
                    textBase = mContext.getString(R.string.issue_event_locked_with_reason,
                        getUserLoginWithBotSuffix(user), event.lockReason());
                }
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
            case ReviewDismissed:
                String dismissalCommitId = event.dismissedReview().dismissalCommitId();
                if (dismissalCommitId != null) {
                    commitId = dismissalCommitId;
                    commitUrl = null;
                    textResId = R.string.pull_request_event_review_dismissed_via_commit;
                } else {
                    textResId = R.string.pull_request_event_review_dismissed;
                }
                break;
            case HeadRefDeleted:
                textResId = R.string.pull_request_event_ref_deleted;
                break;
            case HeadRefRestored:
                textResId = R.string.pull_request_event_ref_restored;
                break;
            case HeadRefForcePushed:
                textResId = R.string.pull_request_event_ref_force_pushed;
                break;
            case CommentDeleted:
                textResId = R.string.pull_request_event_comment_deleted;
                break;
            case ConvertToDraft:
                textResId = R.string.pull_request_event_convert_to_draft;
                break;
            case ReadyForReview:
                textResId = R.string.pull_request_event_ready_for_review;
                break;
            case CrossReferenced:
                textResId = mIsPullRequest
                        ? R.string.pull_request_event_mentioned
                        : R.string.issue_event_mentioned;
                break;
            default:
                return null;
        }

        if (textBase == null) {
            textBase = mContext.getString(textResId, getUserLoginWithBotSuffix(user));
        }

        SpannableStringBuilder text = StringUtils.applyBoldTags(textBase);
        replaceCommitPlaceholder(text, commitId, commitUrl);
        replaceLabelPlaceholder(text, event.label());
        replaceBotPlaceholder(text);
        replaceSourcePlaceholder(text, event.source());
        replaceTimePlaceholder(text, event.createdAt());
        return text;
    }

    private void replaceCommitPlaceholder(SpannableStringBuilder text, String commitId, String commitUrl) {
        int pos = text.toString().indexOf("[commit]");
        if (commitId == null || pos < 0) {
            return;
        }
        // The commit might be in a different repo. The API doesn't provide
        // that information directly, so get it indirectly by parsing the URL
        String commitRepoOwner = mRepoOwner;
        String commitRepoName = mRepoName;
        if (commitUrl != null) {
            Matcher matcher = COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN.matcher(commitUrl);
            if (matcher.find()) {
                commitRepoOwner = matcher.group(1);
                commitRepoName = matcher.group(2);
            }
        }
        boolean isCommitInDifferentRepo = !mRepoOwner.equals(commitRepoOwner) || !mRepoName.equals(commitRepoName);
        String shortCommitSha = commitId.substring(0, 7);
        String commitText = isCommitInDifferentRepo
                ? commitRepoOwner + "/" + commitRepoName + "@" + shortCommitSha
                : shortCommitSha;
        text.replace(pos, pos + 8, commitText);

        String finalRepoOwner = commitRepoOwner;
        String finalRepoName = commitRepoName;
        text.setSpan(new IntentSpan(mContext, context ->
                CommitActivity.makeIntent(context, finalRepoOwner, finalRepoName, commitId)), pos, pos + commitText.length(), 0);
        text.setSpan(new TypefaceSpan("monospace"), pos, pos + commitText.length(), 0);
    }

    private void replaceLabelPlaceholder(SpannableStringBuilder text, Label label) {
        int pos = text.toString().indexOf("[label]");
        if (label != null && pos >= 0) {
            int length = label.name().length();
            text.replace(pos, pos + 7, label.name());
            text.setSpan(new IssueLabelSpan(mContext, label, false), pos, pos + length, 0);
        }
    }

    private void replaceBotPlaceholder(SpannableStringBuilder text) {
        int pos = text.toString().indexOf("[bot]");
        if (pos >= 0) {
            text.delete(pos, pos + 5);
            StringUtils.addUserTypeSpan(mContext, text, pos, mContext.getString(R.string.user_type_bot));
        }
    }

    private void replaceSourcePlaceholder(SpannableStringBuilder text, IssueEvent.CrossReferenceSource referenceSource) {
        int pos = text.toString().indexOf("[source]");
        if (pos < 0) {
            return;
        }
        final Issue source = referenceSource.issue();
        var sourceRepoOwnerAndName = ApiHelpers.extractRepoOwnerAndNameFromIssue(source);
        String sourceRepoOwner = sourceRepoOwnerAndName.first;
        String sourceRepoName = sourceRepoOwnerAndName.second;
        boolean isSourceInDifferentRepo = !mRepoOwner.equals(sourceRepoOwner) || !mRepoName.equals(sourceRepoName);
        String sourceLabel = isSourceInDifferentRepo
                ? sourceRepoOwner + "/" + sourceRepoName + "#" + source.number()
                : "#" + source.number();
        text.replace(pos, pos + 8, sourceLabel);
        text.setSpan(new IntentSpan(mContext, context ->
                source.pullRequest() != null
                        ? PullRequestActivity.makeIntent(context, sourceRepoOwner, sourceRepoName, source.number())
                        : IssueActivity.makeIntent(context, sourceRepoOwner, sourceRepoName, source.number())
        ), pos, pos + sourceLabel.length(), 0);
    }

    private void replaceTimePlaceholder(SpannableStringBuilder text, Date time) {
        int pos = text.toString().indexOf("[time]");
        if (pos < 0) {
            return;
        }
        CharSequence formattedTime = time != null ? StringUtils.formatRelativeTime(mContext, time, true) : "";
        text.replace(pos, pos + 6, formattedTime);
        if (time != null) {
            text.setSpan(new TimestampToastSpan(time), pos, pos + formattedTime.length(), 0);
        }
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
