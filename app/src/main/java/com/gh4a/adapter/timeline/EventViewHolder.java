package com.gh4a.adapter.timeline;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.ImageView;

import com.gh4a.R;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IntentSpan;
import com.gh4a.widget.IssueLabelSpan;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Rename;
import org.eclipse.egit.github.core.User;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EventViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineEvent>
        implements View.OnClickListener {

    private static final Pattern COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN =
            Pattern.compile(".*github\\.com/repos/([^/]+)/([^/]+)/commits");
    private static final HashMap<String, Integer> EVENT_ICONS = new HashMap<>();

    static {
        EVENT_ICONS.put(IssueEvent.TYPE_CLOSED, R.attr.issueEventClosedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_REOPENED, R.attr.issueEventReopenedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_MERGED, R.attr.issueEventMergedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_REFERENCED, R.attr.issueEventReferencedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_ASSIGNED, R.attr.issueEventAssignedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_UNASSIGNED, R.attr.issueEventUnassignedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_LABELED, R.attr.issueEventLabeledIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_UNLABELED, R.attr.issueEventUnlabeledIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_LOCKED, R.attr.issueEventLockedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_UNLOCKED, R.attr.issueEventUnlockedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_MILESTONED, R.attr.issueEventMilestonedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_DEMILESTONED, R.attr.issueEventDemilestonedIcon);
        EVENT_ICONS.put(IssueEvent.TYPE_RENAMED, R.attr.issueEventRenamedIcon);
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

        mAvatarView = (ImageView) itemView.findViewById(R.id.iv_gravatar);
        mEventIconView = (ImageView) itemView.findViewById(R.id.iv_event_icon);
        mMessageView = (StyleableTextView) itemView.findViewById(R.id.tv_message);
        mMessageView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        mAvatarContainer = itemView.findViewById(R.id.avatar_container);
        mAvatarContainer.setOnClickListener(this);
    }

    @Override
    public void bind(TimelineItem.TimelineEvent item) {
        User user = item.event.getAssigner() != null
                ? item.event.getAssigner() : item.event.getActor();
        AvatarHandler.assignAvatar(mAvatarView, user);
        mAvatarContainer.setTag(user);

        Integer eventIconAttr = EVENT_ICONS.get(item.event.getEvent());
        if (eventIconAttr != null) {
            mEventIconView.setImageResource(UiUtils.resolveDrawable(mContext, eventIconAttr));
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

        switch (event.getEvent()) {
            case IssueEvent.TYPE_CLOSED:
                if (isPullRequestEvent) {
                    textResId = event.getCommitId() != null
                            ? R.string.pull_request_event_closed_with_commit
                            : R.string.pull_request_event_closed;
                } else {
                    textResId = event.getCommitId() != null
                            ? R.string.issue_event_closed_with_commit
                            : R.string.issue_event_closed;
                }
                break;
            case IssueEvent.TYPE_REOPENED:
                textResId = isPullRequestEvent
                        ? R.string.pull_request_event_reopened
                        : R.string.issue_event_reopened;
                break;
            case IssueEvent.TYPE_MERGED:
                textResId = event.getCommitId() != null
                        ? R.string.pull_request_event_merged_with_commit
                        : R.string.pull_request_event_merged;
                break;
            case IssueEvent.TYPE_REFERENCED:
                if (isPullRequestEvent) {
                    textResId = event.getCommitId() != null
                            ? R.string.pull_request_event_referenced_with_commit
                            : R.string.pull_request_event_referenced;
                } else {
                    textResId = event.getCommitId() != null
                            ? R.string.issue_event_referenced_with_commit
                            : R.string.issue_event_referenced;
                }
                break;
            case IssueEvent.TYPE_ASSIGNED:
            case IssueEvent.TYPE_UNASSIGNED: {
                boolean isAssign = TextUtils.equals(event.getEvent(), IssueEvent.TYPE_ASSIGNED);
                String actorLogin = user != null ? user.getLogin() : null;
                String assigneeLogin = event.getAssignee() != null
                        ? event.getAssignee().getLogin() : null;
                if (assigneeLogin != null && assigneeLogin.equals(actorLogin)) {
                    if (isPullRequestEvent) {
                        textResId = isAssign
                                ? R.string.pull_request_event_assigned_self
                                : R.string.pull_request_event_unassigned_self;
                    } else {
                        textResId = isAssign
                                ? R.string.issue_event_assigned_self
                                : R.string.issue_event_unassigned_self;
                    }
                } else {
                    if (isPullRequestEvent) {
                        textResId = isAssign
                                ? R.string.pull_request_event_assigned
                                : R.string.pull_request_event_unassigned;
                    } else {
                        textResId = isAssign
                                ? R.string.issue_event_assigned
                                : R.string.issue_event_unassigned;
                    }
                    textBase = mContext.getString(textResId,
                            ApiHelpers.getUserLogin(mContext, user),
                            ApiHelpers.getUserLogin(mContext, event.getAssignee()));
                }
                break;
            }
            case IssueEvent.TYPE_LABELED:
                textResId = R.string.issue_event_labeled;
                break;
            case IssueEvent.TYPE_UNLABELED:
                textResId = R.string.issue_event_unlabeled;
                break;
            case IssueEvent.TYPE_LOCKED:
                textResId = isPullRequestEvent
                        ? R.string.pull_request_event_locked
                        : R.string.issue_event_locked;
                break;
            case IssueEvent.TYPE_UNLOCKED:
                textResId = isPullRequestEvent
                        ? R.string.pull_request_event_unlocked
                        : R.string.issue_event_unlocked;
                break;
            case IssueEvent.TYPE_MILESTONED:
            case IssueEvent.TYPE_DEMILESTONED:
                textResId = TextUtils.equals(event.getEvent(), IssueEvent.TYPE_MILESTONED)
                        ? R.string.issue_event_milestoned : R.string.issue_event_demilestoned;
                textBase = mContext.getString(textResId, event.getMilestone().getTitle(),
                        ApiHelpers.getUserLogin(mContext, user));
                break;
            case IssueEvent.TYPE_RENAMED: {
                Rename rename = event.getRename();
                textBase = mContext.getString(R.string.issue_event_renamed,
                        rename.getFrom(), rename.getTo(), ApiHelpers.getUserLogin(mContext, user));
                break;
            }
            default:
                return null;
        }

        if (textBase == null) {
            textBase = mContext.getString(textResId, ApiHelpers.getUserLogin(mContext, user));
        }
        SpannableStringBuilder text = StringUtils.applyBoldTags(mContext, textBase, typefaceValue);

        int pos = text.toString().indexOf("[commit]");
        if (event.getCommitId() != null && pos >= 0) {
            text.replace(pos, pos + 8, event.getCommitId().substring(0, 7));
            text.setSpan(new IntentSpan(mContext) {
                @Override
                protected Intent getIntent() {
                    // The commit might be in a different repo. The API doesn't provide
                    // that information directly, so get it indirectly by parsing the URL
                    String repoOwner = mRepoOwner;
                    String repoName = mRepoName;
                    String url = event.getCommitUrl();
                    if (url != null) {
                        Matcher matcher = COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN.matcher(url);
                        if (matcher.find()) {
                            repoOwner = matcher.group(1);
                            repoName = matcher.group(2);
                        }
                    }

                    return CommitActivity.makeIntent(mContext, repoOwner, repoName,
                            event.getCommitId());
                }
            }, pos, pos + 7, 0);
            text.setSpan(new TypefaceSpan("monospace"), pos, pos + 7, 0);
        }

        pos = text.toString().indexOf("[label]");
        Label label = event.getLabel();
        if (label != null && pos >= 0) {
            int length = label.getName().length();
            text.replace(pos, pos + 7, label.getName());
            text.setSpan(new IssueLabelSpan(mContext, label, false), pos, pos + length, 0);
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
}
