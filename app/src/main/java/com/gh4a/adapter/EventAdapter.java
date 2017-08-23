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
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
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
import com.gh4a.widget.CustomTypefaceSpan;
import com.gh4a.widget.EllipsizeLineSpan;
import com.gh4a.widget.StyleableTextView;
import com.vdurmont.emoji.EmojiParser;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GollumPage;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.Team;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.CreatePayload;
import org.eclipse.egit.github.core.event.DeletePayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.event.EventRepository;
import org.eclipse.egit.github.core.event.FollowPayload;
import org.eclipse.egit.github.core.event.ForkPayload;
import org.eclipse.egit.github.core.event.GistPayload;
import org.eclipse.egit.github.core.event.GollumPayload;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.event.IssuesPayload;
import org.eclipse.egit.github.core.event.MemberPayload;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.event.PushPayload;
import org.eclipse.egit.github.core.event.ReleasePayload;
import org.eclipse.egit.github.core.event.TeamAddPayload;

import java.util.List;

public class EventAdapter extends RootAdapter<Event, EventAdapter.EventViewHolder> {
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
    public void onBindViewHolder(EventViewHolder holder, Event event) {
        User actor = event.getActor();

        AvatarHandler.assignAvatar(holder.ivGravatar, actor);
        holder.ivGravatar.setTag(actor);

        holder.tvActor.setText(ApiHelpers.getUserLogin(mContext, actor));

        StringUtils.applyBoldTagsAndSetText(holder.tvTitle, formatTitle(event));
        holder.tvCreatedAt.setText(StringUtils.formatRelativeTime(
                mContext, event.getCreatedAt(), false));

        CharSequence content = formatDescription(event, holder.tvDesc.getTypefaceValue());
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

    private CharSequence formatDescription(Event event, int typefaceValue) {
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();

        if (hasInvalidPayload(event)) {
            return event.getType();
        }

        Resources res = mContext.getResources();

        if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            CommitComment comment = payload.getComment();
            if (comment != null) {
                return EmojiParser.parseToUnicode(comment.getBody());
            }

        } else if (Event.TYPE_CREATE.equals(eventType)) {
            CreatePayload payload = (CreatePayload) event.getPayload();
            String refType = payload.getRefType();
            if (CreatePayload.REF_TYPE_REPO.equals(refType)) {
                return res.getString(R.string.event_create_repo_desc, eventRepo.getName());
            }

        } else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            if (payload.getDownload() != null) {
                return payload.getDownload().getName();
            }

        } else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            User target = payload.getTarget();
            if (target != null) {
                return res.getString(R.string.event_follow_desc,
                        target.getLogin(), target.getPublicRepos(), target.getFollowers());
            }

        } else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            return res.getString(R.string.event_fork_desc, formatToRepoName(payload.getForkee()));

        } else if (Event.TYPE_GOLLUM.equals(eventType)) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) {
                return res.getString(R.string.event_gollum_desc, pages.get(0).getPageName());
            }

        } else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
            if (payload != null && payload.getComment() != null) {
                return EmojiParser.parseToUnicode(payload.getComment().getBody());
            }

        } else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload eventPayload = (IssuesPayload) event.getPayload();
            return eventPayload.getIssue().getTitle();

        } else if (Event.TYPE_PUBLIC.equals(eventType)) {
            return null;

        } else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            PullRequest pullRequest = payload.getPullRequest();

            if (!StringUtils.isBlank(pullRequest.getTitle())) {
                return res.getString(R.string.event_pull_request_desc,
                        pullRequest.getTitle(), pullRequest.getCommits(),
                        pullRequest.getAdditions(), pullRequest.getDeletions());
            }

        } else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload =
                    (PullRequestReviewCommentPayload) event.getPayload();
            CommitComment comment = payload.getComment();
            if (comment != null) {
                return EmojiParser.parseToUnicode(comment.getBody());
            }

        } else if (Event.TYPE_PUSH.equals(eventType)) {
            PushPayload payload = (PushPayload) event.getPayload();
            List<Commit> commits = payload.getCommits();

            if (commits != null && !commits.isEmpty()) {
                SpannableStringBuilder ssb = new SpannableStringBuilder();
                float density = mContext.getResources().getDisplayMetrics().density;
                int bottomMargin = Math.round(2 /* dp */ * density);
                int count = payload.getSize();
                int maxLines =
                        mContext.getResources().getInteger(R.integer.event_description_max_lines);
                int max = Math.min(count > maxLines ? maxLines - 1 : count, commits.size());

                for (int i = 0; i < max; i++) {
                    Commit commit = commits.get(i);
                    if (i != 0) {
                        ssb.append("\n");
                    }

                    int lastLength = ssb.length();
                    String sha = commit.getSha().substring(0, 7);

                    ssb.append(sha);
                    ssb.setSpan(new TextAppearanceSpan(mContext, R.style.small_highlighted_sha),
                            ssb.length() - sha.length(), ssb.length(), 0);

                    ssb.append(" ");
                    ssb.append(getFirstLine(EmojiParser.parseToUnicode(commit.getMessage())));
                    ssb.setSpan(new EllipsizeLineSpan(i == (count - 1) ? 0 : bottomMargin),
                            lastLength, ssb.length(), 0);
                }
                if (count > maxLines) {
                    String text = res.getString(R.string.event_push_desc, count - max);
                    ssb.append("\n");
                    ssb.append(text);
                    ssb.setSpan(new CustomTypefaceSpan(typefaceValue, Typeface.ITALIC),
                            ssb.length() - text.length(), ssb.length(), 0);
                }
                return ssb;
            } else if (eventRepo == null) {
                return mContext.getString(R.string.deleted);
            }

        } else if (Event.TYPE_RELEASE.equals(eventType)) {
            ReleasePayload payload = (ReleasePayload) event.getPayload();
            Release release = payload.getRelease();
            if (release != null) {
                if (!TextUtils.isEmpty(release.getName())) {
                    return release.getName();
                }
                return release.getTagName();
            }

        } else if (Event.TYPE_TEAM_ADD.equals(eventType)) {
            TeamAddPayload payload = (TeamAddPayload) event.getPayload();
            Team team = payload.getTeam();
            if (team != null) {
                return res.getString(R.string.event_team_add_desc, team.getName(),
                        team.getMembersCount(), team.getReposCount());
            }
        }

        return null;
    }

    private String getFirstLine(String input) {
        if (input == null) {
            return null;
        }
        int pos = input.indexOf('\n');
        if (pos < 0) {
            return input;
        }
        return input.substring(0, pos);
    }

    private String formatTitle(Event event) {
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        Resources res = mContext.getResources();

        if (hasInvalidPayload(event)) {
            return event.getType();
        }

        if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            return res.getString(R.string.event_commit_comment_title,
                    payload.getComment().getCommitId().substring(0, 7),
                    formatFromRepoName(eventRepo));

        } else if (Event.TYPE_CREATE.equals(eventType)) {
            CreatePayload payload = (CreatePayload) event.getPayload();
            String type = payload.getRefType();
            if (CreatePayload.REF_TYPE_REPO.equals(type)) {
                return res.getString(R.string.event_create_repo_title);
            } else if (CreatePayload.REF_TYPE_BRANCH.equals(type)
                    || CreatePayload.REF_TYPE_TAG.equals(type)) {
                int resId = CreatePayload.REF_TYPE_BRANCH.equals(type)
                        ? R.string.event_create_branch_title : R.string.event_create_tag_title;
                return res.getString(resId, payload.getRef(), formatFromRepoName(eventRepo));
            }

        } else if (Event.TYPE_DELETE.equals(eventType)) {
            DeletePayload payload = (DeletePayload) event.getPayload();
            String type = payload.getRefType();
            if (DeletePayload.REF_TYPE_BRANCH.equals(type)
                    || DeletePayload.REF_TYPE_TAG.equals(type)) {
                int resId = CreatePayload.REF_TYPE_BRANCH.equals(type)
                        ? R.string.event_delete_branch_title : R.string.event_delete_tag_title;
                return res.getString(resId, payload.getRef(), formatFromRepoName(eventRepo));
            }

        } else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            return res.getString(R.string.event_download_title, formatFromRepoName(eventRepo));

        } else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            return res.getString(R.string.event_follow_title,
                    ApiHelpers.getUserLogin(mContext, payload.getTarget()));

        } else if (Event.TYPE_FORK.equals(event.getType())) {
            return res.getString(R.string.event_fork_title, formatFromRepoName(eventRepo));

        } else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            return res.getString(R.string.event_fork_apply_title, formatFromRepoName(eventRepo));

        } else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            Gist gist = payload.getGist();

            String id = gist != null ? gist.getId() : mContext.getString(R.string.deleted);
            int resId = TextUtils.equals(payload.getAction(), GistPayload.ACTION_UPDATE)
                    ? R.string.event_update_gist_title : R.string.event_create_gist_title;
            return res.getString(resId, id);

        } else if (Event.TYPE_GOLLUM.equals(eventType)) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            int count = pages == null ? 0 : pages.size();
            return res.getString(R.string.event_gollum_title,
                    res.getQuantityString(R.plurals.page, count, count),
                    formatFromRepoName(eventRepo));

        } else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
            Issue issue = payload.getIssue();
            if (issue != null) {
                int formatResId = issue.getPullRequest() != null
                        ? R.string.event_pull_request_comment : R.string.event_issue_comment;
                return res.getString(formatResId, issue.getNumber(), formatFromRepoName(eventRepo));
            }

        } else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            final int resId;
            switch (payload.getAction()) {
                case IssuesPayload.ACTION_OPEN: resId = R.string.event_issues_open_title; break;
                case IssuesPayload.ACTION_CLOSE: resId = R.string.event_issues_close_title; break;
                case IssuesPayload.ACTION_REOPEN: resId = R.string.event_issues_reopen_title; break;
                default: return "";
            }
            return res.getString(resId, payload.getIssue().getNumber(),
                    formatFromRepoName(eventRepo));

        } else if (Event.TYPE_MEMBER.equals(eventType)) {
            MemberPayload payload = (MemberPayload) event.getPayload();
            return res.getString(R.string.event_member_title,
                    ApiHelpers.getUserLogin(mContext, payload.getMember()),
                    formatFromRepoName(eventRepo));

        } else if (Event.TYPE_PUBLIC.equals(eventType)) {
            return res.getString(R.string.event_public_title, formatFromRepoName(eventRepo));

        } else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            PullRequest pr = payload.getPullRequest();
            final int resId;
            switch (payload.getAction()) {
                case PullRequestPayload.ACTION_OPEN:
                    resId = R.string.event_pr_open_title;
                    break;
                case PullRequestPayload.ACTION_CLOSE:
                    resId = pr.isMerged() ? R.string.event_pr_merge_title : R.string.event_pr_close_title;
                    break;
                case PullRequestPayload.ACTION_REOPEN:
                    resId = R.string.event_pr_reopen_title;
                    break;
                case PullRequestPayload.ACTION_SYNCHRONIZE:
                    resId = R.string.event_pr_update_title;
                    break;
                default:
                    return "";
            }
            return res.getString(resId, payload.getNumber(), formatFromRepoName(eventRepo));

        } else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload =
                    (PullRequestReviewCommentPayload) event.getPayload();
            PullRequest pr = payload.getPullRequest();
            CommitComment comment = payload.getComment();
            if (pr != null) {
                return res.getString(R.string.event_pull_request_review_comment_title,
                        pr.getNumber(), formatFromRepoName(eventRepo));
            } else if (comment != null) {
                return res.getString(R.string.event_commit_comment_title,
                        comment.getCommitId().substring(0, 7), formatFromRepoName(eventRepo));
            }

        } else if (Event.TYPE_PUSH.equals(eventType)) {
            PushPayload payload = (PushPayload) event.getPayload();
            String ref = payload.getRef();
            if (ref.startsWith("refs/heads/")) {
                ref = ref.substring(11);
            }
            return res.getString(R.string.event_push_title, ref, formatFromRepoName(eventRepo));

        } else if (Event.TYPE_RELEASE.equals(eventType)) {
            return res.getString(R.string.event_release_title, formatFromRepoName(eventRepo));

        } else if (Event.TYPE_TEAM_ADD.equals(eventType)) {
            TeamAddPayload payload = (TeamAddPayload) event.getPayload();
            Team team = payload.getTeam();
            if (team != null) {
                Repository repo = payload.getRepo();
                if (repo != null) {
                    return res.getString(R.string.event_team_repo_add,
                            formatToRepoName(repo), team.getName());
                } else {
                    return res.getString(R.string.event_team_user_add,
                            ApiHelpers.getUserLogin(mContext, payload.getUser()), team.getName());
                }
            }

        } else if (Event.TYPE_WATCH.equals(eventType)) {
            return res.getString(R.string.event_watch_title, formatFromRepoName(eventRepo));
        }

        return "";
    }

    public static boolean hasInvalidPayload(Event event) {
        EventPayload payload = event.getPayload();
        if (payload == null) {
            return true;
        }
        // we only accept and work with derived classes; if the base class
        // is returned, something went wrong during deserialization
        return EventPayload.class.equals(payload.getClass());
    }

    private String formatFromRepoName(EventRepository repository) {
        if (repository != null) {
            return repository.getName();
        }
        return mContext.getString(R.string.deleted);
    }

    private String formatToRepoName(Repository repository) {
        if (repository != null && repository.getOwner() != null) {
            return repository.getOwner().getLogin() + "/" + repository.getName();
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
        private final StyleableTextView tvTitle;
        private final StyleableTextView tvDesc;
        private final TextView tvCreatedAt;
    }
}