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
import android.content.res.Resources;
import android.graphics.Typeface;

import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.widget.EllipsizeLineSpan;
import com.meisolsson.githubsdk.model.Gist;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.GitHubEventType;
import com.meisolsson.githubsdk.model.GitHubWikiPage;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.model.ReferenceType;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.Team;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.model.git.GitCommit;
import com.meisolsson.githubsdk.model.payload.CommitCommentPayload;
import com.meisolsson.githubsdk.model.payload.CreatePayload;
import com.meisolsson.githubsdk.model.payload.DeletePayload;
import com.meisolsson.githubsdk.model.payload.DownloadPayload;
import com.meisolsson.githubsdk.model.payload.FollowPayload;
import com.meisolsson.githubsdk.model.payload.ForkPayload;
import com.meisolsson.githubsdk.model.payload.GistPayload;
import com.meisolsson.githubsdk.model.payload.GitHubPayload;
import com.meisolsson.githubsdk.model.payload.GollumPayload;
import com.meisolsson.githubsdk.model.payload.IssueCommentPayload;
import com.meisolsson.githubsdk.model.payload.IssuesPayload;
import com.meisolsson.githubsdk.model.payload.MemberPayload;
import com.meisolsson.githubsdk.model.payload.PullRequestPayload;
import com.meisolsson.githubsdk.model.payload.PullRequestReviewCommentPayload;
import com.meisolsson.githubsdk.model.payload.PullRequestReviewPayload;
import com.meisolsson.githubsdk.model.payload.PushPayload;
import com.meisolsson.githubsdk.model.payload.ReleasePayload;
import com.meisolsson.githubsdk.model.payload.TeamAddPayload;
import com.vdurmont.emoji.EmojiParser;

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RootAdapter<GitHubEvent, EventAdapter.EventViewHolder> {
    public EventAdapter(Context context) {
        super(context);
    }

    @Override
    public EventViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent,
            int viewType) {
        View v = inflater.inflate(R.layout.row_event, parent, false);
        EventViewHolder holder = new EventViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(EventViewHolder holder, GitHubEvent event) {
        User actor = event.actor();

        AvatarHandler.assignAvatar(holder.ivGravatar, actor);
        holder.ivGravatar.setTag(actor);

        holder.tvActor.setText(ApiHelpers.getUserLoginWithType(mContext, actor));

        SpannableStringBuilder title = StringUtils.applyBoldTags(formatTitle(event));
        Label labelInTitle = extractLabelForTitle(event);
        StringUtils.replaceLabelPlaceholder(mContext, title, labelInTitle);
        holder.tvTitle.setText(title);

        holder.tvCreatedAt.setText(StringUtils.formatRelativeTime(
                mContext, event.createdAt(), false));

        CharSequence content = formatDescription(event);
        holder.tvDesc.setText(content);
        holder.tvDesc.setVisibility(content != null ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            User actor = (User) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, actor);
            if (intent != null) {
                mContext.startActivity(intent);
            }
        } else {
            super.onClick(v);
        }
    }

    @Override
    public boolean isCardStyle() {
        return true;
    }

    private CharSequence formatDescription(GitHubEvent event) {
        GitHubEventType eventType = event.type();
        GitHubEvent.RepoIdentifier eventRepo = event.repo();

        if (hasInvalidPayload(event)) {
            return eventType.toString();
        }

        Resources res = mContext.getResources();

        switch (eventType) {
            case CommitCommentEvent: {
                CommitCommentPayload payload = (CommitCommentPayload) event.payload();
                GitComment comment = payload.comment();
                if (comment != null) {
                    return EmojiParser.parseToUnicode(comment.body());
                }
                break;
            }

            case CreateEvent: {
                CreatePayload payload = (CreatePayload) event.payload();
                if (payload.refType() == ReferenceType.Repository) {
                    return res.getString(R.string.event_create_repo_desc, eventRepo.repoWithUserName());
                }
                break;
            }

            case DownloadEvent: {
                DownloadPayload payload = (DownloadPayload) event.payload();
                if (payload.download() != null) {
                    return payload.download().name();
                }
                break;
            }

            case FollowEvent: {
                FollowPayload payload = (FollowPayload) event.payload();
                User target = payload.target();
                if (target != null) {
                    return res.getString(R.string.event_follow_desc,
                            target.login(), target.publicRepos(), target.followers());
                }
                break;
            }

            case ForkEvent: {
                ForkPayload payload = (ForkPayload) event.payload();
                return res.getString(R.string.event_fork_desc, ApiHelpers.formatRepoName(mContext, payload.forkee()));
            }

            case GollumEvent: {
                GollumPayload payload = (GollumPayload) event.payload();
                List<GitHubWikiPage> pages = payload.pages();
                if (pages != null && !pages.isEmpty()) {
                    return res.getString(R.string.event_gollum_desc, pages.get(0).pageName());
                }
                break;
            }

            case IssueCommentEvent: {
                IssueCommentPayload payload = (IssueCommentPayload) event.payload();
                if (payload != null && payload.comment() != null) {
                    return EmojiParser.parseToUnicode(payload.comment().body());
                }
                break;
            }

            case IssuesEvent: {
                IssuesPayload payload = (IssuesPayload) event.payload();
                String desc = payload.issue().title();
                if (payload.action() == IssuesPayload.Action.Assigned ||
                        payload.action() == IssuesPayload.Action.Unassigned) {
                    return formatAssignmentDesc(res, desc, payload.assignees());
                } else {
                    return desc;
                }
            }

            case PublicEvent:
                return null;

            case PullRequestEvent: {
                PullRequestPayload payload = (PullRequestPayload) event.payload();
                PullRequest pullRequest = payload.pullRequest();
                String title = pullRequest.title();

                if (payload.action() == PullRequestPayload.Action.Assigned ||
                        payload.action() == PullRequestPayload.Action.Unassigned) {
                    return formatAssignmentDesc(res, title, payload.assignees());
                } else if (!StringUtils.isBlank(title)) {
                    return res.getString(R.string.event_pull_request_desc,
                            title, pullRequest.commits(),
                            pullRequest.additions(), pullRequest.deletions());
                }
                break;
            }

            case PullRequestReviewEvent: {
                PullRequestReviewPayload payload = (PullRequestReviewPayload) event.payload();
                Review review = payload.review();
                String body = review.body();
                if (body != null) {
                    return EmojiParser.parseToUnicode(review.body());
                }
                break;
            }
            case PullRequestReviewCommentEvent: {
                PullRequestReviewCommentPayload payload =
                        (PullRequestReviewCommentPayload) event.payload();
                ReviewComment comment = payload.comment();
                if (comment != null) {
                    return EmojiParser.parseToUnicode(comment.body());
                }
                break;
            }

            case PushEvent: {
                PushPayload payload = (PushPayload) event.payload();
                List<GitCommit> commits = payload.commits();

                if (commits != null && !commits.isEmpty()) {
                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    float density = mContext.getResources().getDisplayMetrics().density;
                    int bottomMargin = Math.round(2 /* dp */ * density);
                    int count = payload.size();
                    int maxLines =
                            mContext.getResources()
                                    .getInteger(R.integer.event_description_max_lines);
                    int max = Math.min(count > maxLines ? maxLines - 1 : count, commits.size());

                    for (int i = 0; i < max; i++) {
                        GitCommit commit = commits.get(i);
                        if (i != 0) {
                            ssb.append("\n");
                        }

                        int lastLength = ssb.length();
                        String sha = commit.sha().substring(0, 7);

                        ssb.append(sha);
                        ssb.setSpan(new TextAppearanceSpan(mContext, R.style.TextAppearance_SmallUrl_Sha),
                                ssb.length() - sha.length(), ssb.length(), 0);

                        ssb.append(" ");
                        ssb.append(StringUtils.getFirstLine(EmojiParser.parseToUnicode(commit.message())));
                        ssb.setSpan(new EllipsizeLineSpan(i == (count - 1) ? 0 : bottomMargin),
                                lastLength, ssb.length(), 0);
                    }
                    if (count > maxLines) {
                        String text = res.getString(R.string.event_push_desc, count - max);
                        ssb.append("\n");
                        ssb.append(text);
                        ssb.setSpan(new StyleSpan(Typeface.ITALIC),
                                ssb.length() - text.length(), ssb.length(), 0);
                    }
                    return ssb;
                } else if (eventRepo == null) {
                    return mContext.getString(R.string.deleted);
                }
                break;
            }

            case ReleaseEvent: {
                ReleasePayload payload = (ReleasePayload) event.payload();
                Release release = payload.release();
                if (release != null) {
                    if (!TextUtils.isEmpty(release.name())) {
                        return release.name();
                    }
                    return release.tagName();
                }
                break;
            }

            case TeamAddEvent: {
                TeamAddPayload payload = (TeamAddPayload) event.payload();
                Team team = payload.team();
                if (team != null) {
                    return res.getString(R.string.event_team_add_desc, team.name(),
                            team.membersCount(), team.reposCount());
                }
                break;
            }
        }

        return null;
    }

    private CharSequence formatAssignmentDesc(Resources res, String desc, List<User> assignees) {
        List<String> assigneeNames = new ArrayList<>();
        if (assignees != null) {
            for (User assignee : assignees) {
                if (assignee.login() != null) {
                    assigneeNames.add(assignee.login());
                }
            }
        }
        String assigneeText = assigneeNames.isEmpty()
                ? res.getString(R.string.event_issue_unassigned_desc)
                : res.getString(R.string.event_issue_assigned_desc, TextUtils.join(", ", assigneeNames));
        final String finalDesc;
        if (TextUtils.isEmpty(desc)) {
            finalDesc = assigneeText;
        } else {
            finalDesc = desc + "\n" + assigneeText;
        }
        return StringUtils.applyBoldTags(finalDesc);
    }

    private String formatTitle(GitHubEvent event) {
        GitHubEvent.RepoIdentifier eventRepo = event.repo();
        Resources res = mContext.getResources();

        if (hasInvalidPayload(event)) {
            return event.type().toString();
        }

        switch (event.type()) {
            case CommitCommentEvent: {
                CommitCommentPayload payload = (CommitCommentPayload) event.payload();
                return res.getString(R.string.event_commit_comment_title,
                        payload.comment().commitId().substring(0, 7),
                        formatFromRepoIdentifier(eventRepo));
            }

            case CreateEvent: {
                CreatePayload payload = (CreatePayload) event.payload();
                switch (payload.refType()) {
                    case Repository:
                        return res.getString(R.string.event_create_repo_title);
                    case Branch:
                        return res.getString(R.string.event_create_branch_title, payload.ref(),
                                formatFromRepoIdentifier(eventRepo));
                    case Tag:
                        return res.getString(R.string.event_create_tag_title, payload.ref(),
                                formatFromRepoIdentifier(eventRepo));
                }
                break;
            }

            case DeleteEvent: {
                DeletePayload payload = (DeletePayload) event.payload();
                switch (payload.refType()) {
                    case Branch:
                        return res.getString(R.string.event_delete_branch_title, payload.ref(),
                                formatFromRepoIdentifier(eventRepo));
                    case Tag:
                        return res.getString(R.string.event_delete_tag_title, payload.ref(),
                                formatFromRepoIdentifier(eventRepo));
                }
                break;
            }

            case DownloadEvent:
                return res.getString(R.string.event_download_title,
                        formatFromRepoIdentifier(eventRepo));

            case FollowEvent: {
                FollowPayload payload = (FollowPayload) event.payload();
                return res.getString(R.string.event_follow_title,
                        ApiHelpers.getUserLogin(mContext, payload.target()));
            }

            case ForkEvent:
                return res.getString(R.string.event_fork_title,
                        formatFromRepoIdentifier(eventRepo));

            case ForkApplyEvent:
                return res.getString(R.string.event_fork_apply_title,
                        formatFromRepoIdentifier(eventRepo));

            case GistEvent: {
                GistPayload payload = (GistPayload) event.payload();
                Gist gist = payload.gist();

                String id = gist != null ? gist.id() : mContext.getString(R.string.deleted);
                int resId = payload.action() == GistPayload.Action.Updated
                        ? R.string.event_update_gist_title : R.string.event_create_gist_title;
                return res.getString(resId, id);
            }

            case GollumEvent: {
                GollumPayload payload = (GollumPayload) event.payload();
                List<GitHubWikiPage> pages = payload.pages();
                int count = pages == null ? 0 : pages.size();
                return res.getString(R.string.event_gollum_title,
                        res.getQuantityString(R.plurals.page, count, count),
                        formatFromRepoIdentifier(eventRepo));
            }

            case IssueCommentEvent: {
                IssueCommentPayload payload = (IssueCommentPayload) event.payload();
                Issue issue = payload.issue();
                if (issue != null) {
                    int formatResId = issue.pullRequest() != null
                            ? R.string.event_pull_request_comment : R.string.event_issue_comment;
                    return res.getString(formatResId, issue.number(),
                            formatFromRepoIdentifier(eventRepo));
                }
                break;
            }

            case IssuesEvent: {
                IssuesPayload payload = (IssuesPayload) event.payload();
                final int resId;
                switch (payload.action()) {
                    case Opened: resId = R.string.event_issues_open_title; break;
                    case Closed: resId = R.string.event_issues_close_title; break;
                    case Reopened: resId = R.string.event_issues_reopen_title; break;
                    case Labeled: resId = R.string.event_issues_label_title; break;
                    case Unlabeled: resId = R.string.event_issues_unlabel_title; break;
                    case Assigned: resId = R.string.event_issues_assign_title; break;
                    case Unassigned: resId = R.string.event_issues_unassign_title; break;
                    default: return "";
                }
                return res.getString(resId, payload.issue().number(),
                        formatFromRepoIdentifier(eventRepo));
            }

            case MemberEvent: {
                MemberPayload payload = (MemberPayload) event.payload();
                return res.getString(R.string.event_member_title,
                        ApiHelpers.getUserLogin(mContext, payload.member()),
                        formatFromRepoIdentifier(eventRepo));
            }

            case PublicEvent:
                return res.getString(R.string.event_public_title,
                        formatFromRepoIdentifier(eventRepo));

            case PullRequestEvent: {
                PullRequestPayload payload = (PullRequestPayload) event.payload();
                PullRequest pr = payload.pullRequest();
                final int resId;
                switch (payload.action()) {
                    case Opened:
                        resId = R.string.event_pr_open_title;
                        break;
                    case Closed:
                        resId = R.string.event_pr_close_title;
                        break;
                    case Labeled:
                        resId = R.string.event_pr_label_title;
                        break;
                    case Unlabeled:
                        resId = R.string.event_pr_unlabel_title;
                        break;
                    case Assigned:
                        resId = R.string.event_pr_assign_title;
                        break;
                    case Unassigned:
                        resId = R.string.event_pr_unassign_title;
                        break;
                    case Reopened:
                        resId = R.string.event_pr_reopen_title;
                        break;
                    case Synchronized:
                        resId = R.string.event_pr_update_title;
                        break;
                    case Merged:
                        resId = R.string.event_pr_merge_title;
                        break;
                    default:
                        return "";
                }
                return res.getString(resId, payload.number(), formatFromRepoIdentifier(eventRepo));
            }

            case PullRequestReviewEvent: {
                PullRequestReviewPayload payload = (PullRequestReviewPayload) event.payload();
                PullRequest pr = payload.pullRequest();

                // assuming action is 'created' for now
                return res.getString(R.string.event_review_title, pr.number(),
                        formatFromRepoIdentifier(eventRepo));
            }

            case PullRequestReviewCommentEvent: {
                PullRequestReviewCommentPayload payload =
                        (PullRequestReviewCommentPayload) event.payload();
                PullRequest pr = payload.pullRequest();
                ReviewComment comment = payload.comment();
                if (pr != null) {
                    return res.getString(R.string.event_pull_request_review_comment_title,
                            pr.number(), formatFromRepoIdentifier(eventRepo));
                } else if (comment != null) {
                    return res.getString(R.string.event_commit_comment_title,
                            comment.commitId().substring(0, 7),
                            formatFromRepoIdentifier(eventRepo));
                }
                break;
            }

            case PushEvent: {
                PushPayload payload = (PushPayload) event.payload();
                String ref = payload.ref();
                if (ref.startsWith("refs/heads/")) {
                    ref = ref.substring(11);
                }
                return res.getString(R.string.event_push_title, ref,
                        formatFromRepoIdentifier(eventRepo));
            }

            case ReleaseEvent:
                return res.getString(R.string.event_release_title,
                        formatFromRepoIdentifier(eventRepo));

            case TeamAddEvent: {
                TeamAddPayload payload = (TeamAddPayload) event.payload();
                Team team = payload.team();
                if (team != null) {
                    Repository repo = payload.repository();
                    if (repo != null) {
                        return res.getString(R.string.event_team_repo_add,
                                ApiHelpers.formatRepoName(mContext, repo), team.name());
                    }
                }
                break;
            }

            case WatchEvent:
                return res.getString(R.string.event_watch_title,
                        formatFromRepoIdentifier(eventRepo));
        }

        return "";
    }

    private static Label extractLabelForTitle(GitHubEvent event) {
        switch (event.type()) {
            case IssuesEvent:
                return ((IssuesPayload) event.payload()).label();
            case PullRequestEvent:
                return ((PullRequestPayload) event.payload()).label();
        }
        return null;
    }

    public static boolean hasInvalidPayload(GitHubEvent event) {
        GitHubPayload payload = event.payload();
        if (payload == null) {
            return true;
        }
        // we only accept and work with derived classes; if the base class
        // is returned, something went wrong during deserialization
        return GitHubPayload.class.equals(payload.getClass());
    }

    private String formatFromRepoIdentifier(GitHubEvent.RepoIdentifier repository) {
        if (repository != null) {
            return repository.repoWithUserName();
        }
        return mContext.getString(R.string.deleted);
    }

    /**
     * The Class ViewHolder.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        private EventViewHolder(View view) {
            super(view);
            ivGravatar = view.findViewById(R.id.iv_gravatar);
            tvActor = view.findViewById(R.id.tv_actor);
            tvTitle = view.findViewById(R.id.tv_title);
            tvDesc = view.findViewById(R.id.tv_desc);
            tvCreatedAt = view.findViewById(R.id.tv_created_at);
        }

        private final ImageView ivGravatar;
        private final TextView tvActor;
        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvCreatedAt;
    }
}