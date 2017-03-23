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
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.ImageView;

import com.gh4a.R;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IntentSpan;
import com.gh4a.widget.IssueLabelSpan;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Rename;
import org.eclipse.egit.github.core.User;

import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueEventAdapter extends CommentAdapterBase<IssueEventHolder> {
    private static final Pattern COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN =
            Pattern.compile(".*github.com\\/repos\\/([^\\/]+)\\/([^\\/]+)\\/commits");

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

    private final int mIssueId;
    private boolean mLocked;

    public IssueEventAdapter(Context context, String repoOwner, String repoName,
            int issueId, OnCommentAction actionCallback) {
        super(context, repoOwner, repoName, actionCallback);
        mIssueId = issueId;
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
        notifyDataSetChanged();
    }

    @Override
    protected User getUser(IssueEventHolder item) {
        return item.getUser();
    }

    @Override
    protected Date getCreatedAt(IssueEventHolder item) {
        return item.getCreatedAt();
    }

    @Override
    protected Date getUpdatedAt(IssueEventHolder item) {
        return item.getUpdatedAt();
    }

    @Override
    protected String getUrl(IssueEventHolder item) {
        return item.comment != null ? item.comment.getHtmlUrl() : null;
    }

    @Override
    protected String getShareSubject(IssueEventHolder item) {
        return mContext.getString(R.string.share_comment_subject,
                item.comment.getId(), mIssueId, mRepoOwner + "/" + mRepoName);
    }

    @Override
    protected void bindBodyView(IssueEventHolder item, StyleableTextView view,
            HttpImageGetter imageGetter) {
        if (item.comment != null) {
            imageGetter.bind(view, item.comment.getBodyHtml(), item.comment.getId());
        } else {
            imageGetter.unbind(view);
            view.setText(formatEvent(item.event, item.getUser(),
                    view.getTypefaceValue(), item.isPullRequestEvent));
        }
    }

    @Override
    protected void bindExtraView(IssueEventHolder item, StyleableTextView view) {
        String login = ApiHelpers.getUserLogin(mContext, item.getUser());
        StringUtils.applyBoldTagsAndSetText(view,
                mContext.getString(R.string.issue_comment_header, login));
    }

    @Override
    protected void bindFileView(IssueEventHolder item, StyleableTextView view) {
        if (item.comment instanceof CommitComment) {
            final CommitComment commitComment = (CommitComment) item.comment;
            SpannableStringBuilder text = StringUtils.applyBoldTags(mContext,
                    mContext.getString(R.string.issue_commit_comment_file),
                    view.getTypefaceValue());

            int pos = text.toString().indexOf("[file]");
            if (pos >= 0) {
                String fileName = FileUtils.getFileName(commitComment.getPath());
                final CommitFile file = item.file;

                text.replace(pos, pos + 6, fileName);
                if (file != null) {
                    text.setSpan(new IntentSpan(mContext) {
                        @Override
                        protected Intent getIntent() {
                            return PullRequestDiffViewerActivity.makeIntent(mContext,
                                    mRepoOwner, mRepoName, mIssueId,
                                    commitComment.getCommitId(), commitComment.getPath(),
                                    file.getPatch(), null, commitComment.getPosition(),
                                    -1, -1, false);
                        }
                    }, pos, pos + fileName.length(), 0);
                }
            }

            view.setVisibility(View.VISIBLE);
            view.setText(text);
            view.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @Override
    protected void bindEventIcon(IssueEventHolder item, ImageView view) {
        Integer eventIconAttr = item.event != null
                ? EVENT_ICONS.get(item.event.getEvent()) : null;
        if (eventIconAttr != null) {
            view.setImageResource(UiUtils.resolveDrawable(mContext, eventIconAttr));
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @Override
    protected boolean hasActionMenu(IssueEventHolder item) {
        return item.comment != null;
    }

    @Override
    protected boolean canQuote(IssueEventHolder item) {
        return item.comment != null && !mLocked;
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
            text.setSpan(new TypefaceSpan("monospace"), pos, pos + 7, 0);
            text.setSpan(new IntentSpan(mContext) {
                @Override
                protected Intent getIntent() {
                    // The commit might be in a different repo. The API doesn't provide
                    // that information directly, so get it indirectly by parsing the URL
                    String repoOwner = mRepoOwner, repoName = mRepoName;
                    String url = event.getCommitUrl();
                    if (url != null) {
                        Matcher matcher = COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN.matcher(url);
                        if (matcher.find()) {
                            repoOwner = matcher.group(1);
                            repoName = matcher.group(2);
                        }
                    }

                    return CommitActivity.makeIntent(mContext,
                            repoOwner, repoName, event.getCommitId());
                }
            }, pos, pos + 7, 0);
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
}
