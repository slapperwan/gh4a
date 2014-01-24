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

import java.util.List;
import java.util.Locale;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.GollumPage;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.CreatePayload;
import org.eclipse.egit.github.core.event.DeletePayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.Event;
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.StringUtils;

public class FeedAdapter extends RootAdapter<Event> implements OnClickListener {
    public FeedAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.feed_row, null);
        ViewHolder viewHolder = new ViewHolder();

        Gh4Application app = (Gh4Application) mContext.getApplicationContext();
        Typeface boldCondensed = app.boldCondensed;
        Typeface regular = app.regular;

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvTitle.setTypeface(boldCondensed);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setTypeface(regular);

        viewHolder.llPushDesc = (ViewGroup) v.findViewById(R.id.ll_push_desc);

        v.setTag(viewHolder);
        return v;
    }
    
    @Override
    protected void bindView(View v, Event event) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();
        User actor = event.getActor();

        GravatarHandler.assignGravatar(viewHolder.ivGravatar, actor);
        viewHolder.ivGravatar.setTag(actor);

        SpannableString createdAt = new SpannableString(pt.format(event.getCreatedAt()));
        createdAt.setSpan(new TextAppearanceSpan(v.getContext(), R.style.default_text_small_italic),
                0, createdAt.length(), 0);

        viewHolder.tvTitle.setText(TextUtils.concat(formatTitle(event), " ", createdAt));

        String content = formatDescription(event, viewHolder);
        viewHolder.tvDesc.setText(content);
        viewHolder.tvDesc.setVisibility(content != null ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            User actor = (User) v.getTag();
            if (!StringUtils.isBlank(actor.getLogin())) {
                /** Open user activity */
                Gh4Application.get(mContext).openUserInfoActivity(mContext,
                        actor.getLogin(), actor.getName());
            }
        }
    }

    private String formatDescription(Event event, ViewHolder viewHolder) {
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        
        //if payload is a base class, return default eventtype.  Think that it is an old event which not supported
        //by API v3.
        if (event.getPayload().getClass().getSimpleName().equals("EventPayload")) {
            return event.getType();
        }
        
        Resources res = mContext.getResources();
        
        viewHolder.llPushDesc.setVisibility(View.GONE);

        /** PushEvent */
        if (Event.TYPE_PUSH.equals(eventType)) {
            viewHolder.llPushDesc.setVisibility(View.VISIBLE);
            viewHolder.llPushDesc.removeAllViews();

            PushPayload payload = (PushPayload) event.getPayload();
            List<Commit> commits = payload.getCommits();
            
            if (commits != null) {
                Gh4Application app = (Gh4Application) mContext.getApplicationContext();
                Typeface regular = app.regular;
                Typeface italic = app.italic;
                
                for (int i = 0; i < commits.size() && i < 3; i++) {
                    Commit commit = commits.get(i);
                    SpannableString spannableSha = new SpannableString(commit.getSha().substring(0, 7));

                    if (eventRepo != null) {
                        spannableSha.setSpan(new TextAppearanceSpan(mContext,
                                R.style.default_text_small_url), 0, spannableSha.length(), 0);
                    }
                    else {
                        spannableSha = new SpannableString(mContext.getString(R.string.deleted));
                    }
                    
                    TextView tvCommitMsg = new TextView(mContext);
                    tvCommitMsg.setText(spannableSha);
                    tvCommitMsg.append(" " + commit.getMessage());
                    tvCommitMsg.setSingleLine(true);
                    tvCommitMsg.setEllipsize(TruncateAt.END);
                    tvCommitMsg.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
                    tvCommitMsg.setTypeface(regular);
                    viewHolder.llPushDesc.addView(tvCommitMsg);
                }
                if (commits.size() > 3) {
                    TextView tvMoreMsg = new TextView(mContext);
                    String text = res.getString(R.string.event_push_desc, commits.size() - 3);
                    tvMoreMsg.setText(text);
                    tvMoreMsg.setTypeface(italic);
                    viewHolder.llPushDesc.addView(tvMoreMsg);
                }
            }
            return null;
        }

        /** CommitCommentEvent */
        else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();

            return res.getString(R.string.event_commit_comment_desc,
                    payload.getComment().getCommitId().substring(0, 7), 
                    payload.getComment().getBody());
        }

        /** PullRequestEvent */
        else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            PullRequest pullRequest = payload.getPullRequest();
            
            if (!StringUtils.isBlank(pullRequest.getTitle())) {
                return res.getString(R.string.event_pull_request_desc,
                        pullRequest.getTitle(), pullRequest.getCommits(),
                        pullRequest.getAdditions(), pullRequest.getDeletions());
            }
            return null;
        }

        /** FollowEvent */
        else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            User target = payload.getTarget();
            if (target != null) {
                return res.getString(R.string.event_follow_desc,
                        target.getLogin(), target.getPublicRepos(), target.getFollowers());
            }
            return null;
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            return res.getString(R.string.event_fork_desc, formatToRepoName(payload.getForkee()));
        }

        /** CreateEvent */
        else if (Event.TYPE_CREATE.equals(eventType)) {
            CreatePayload payload = (CreatePayload) event.getPayload();
            if ("repository".equals(payload.getRefType())) {
                return res.getString(R.string.event_create_repo_desc, eventRepo.getName());
            }
            else if ("branch".equals(payload.getRefType()) || "tag".equals(payload.getRefType())) {
                return res.getString(R.string.event_create_branch_desc,
                        payload.getRefType(), eventRepo.getName(), payload.getRef());
            }
            return null;
        }

        /** DownloadEvent */
        else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            if (payload.getDownload() != null) {
                return payload.getDownload().getName();
            }
            return null;
        }

        /** GollumEvent */
        else if (Event.TYPE_GOLLUM.equals(eventType)) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) {
                return res.getString(R.string.event_gollum_desc, pages.get(0).getPageName());
            }
            return null;
        }

        /** PublicEvent */
        else if (Event.TYPE_PUBLIC.equals(eventType)) {
            return null;
        }
        
        /** IssuesEvent */
        else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload eventPayload = (IssuesPayload) event.getPayload();
            return eventPayload.getIssue().getTitle();
        }
        
        /** IssueCommentEvent */
        else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
            if (payload != null && payload.getComment() != null) {
                return payload.getComment().getBody();
            }
            return null;
        }

        /** PullRequestReviewComment */
        else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload = (PullRequestReviewCommentPayload) event.getPayload();
            return res.getString(R.string.event_commit_comment_desc,
                    payload.getComment().getCommitId().substring(0, 7), 
                    payload.getComment().getBody());
        }

        /** ReleaseEvent */
        else if (Event.TYPE_RELEASE.equals(eventType)) {
            ReleasePayload payload = (ReleasePayload) event.getPayload();
            if (payload.getRelease() != null) {
                return payload.getRelease().getName();
            }
        }

        return null;
    }

    private String formatTitle(Event event) {
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        User actor = event.getActor();
        Resources res = mContext.getResources();
        
        //if payload is a base class, return default eventtype.  Think that it is an old event which not supported
        //by API v3.
        if (event.getPayload().getClass().getSimpleName().equals("EventPayload")) {
            return event.getType();
        }

        /** PushEvent */
        if (Event.TYPE_PUSH.equals(eventType)) {
            PushPayload payload = (PushPayload) event.getPayload();
            String[] refPart = payload.getRef().split("/"); 
            return res.getString(R.string.event_push_title,
                    actor.getLogin(), refPart.length == 3 ? refPart[2] : payload.getRef(),
                    formatFromRepoName(eventRepo));
        }

        /** IssuesEvent */
        else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload eventPayload = (IssuesPayload) event.getPayload();
            return res.getString(R.string.event_issues_title,
                    actor.getLogin(), eventPayload.getAction(),
                    eventPayload.getIssue().getNumber(), formatFromRepoName(eventRepo)); 
        }

        /** CommitCommentEvent */
        else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            return res.getString(R.string.event_commit_comment_title,
                    actor.getLogin(), formatFromRepoName(eventRepo));
        }

        /** PullRequestEvent */
        else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            return res.getString(R.string.event_pull_request_title,
                    actor.getLogin(), payload.getAction(),
                    payload.getNumber(), formatFromRepoName(eventRepo));
        }

        /** WatchEvent */
        else if (Event.TYPE_WATCH.equals(eventType)) {
            return res.getString(R.string.event_watch_title,
                    actor.getLogin(), formatFromRepoName(eventRepo));
        }

        /** GistEvent */
        else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            String login = actor.getLogin();
            if (StringUtils.isBlank(login) && payload.getGist() != null
                    && payload.getGist().getUser() != null) {
                login = payload.getGist().getUser().getLogin(); 
            }
            
            String id = payload.getGist() != null
                    ? payload.getGist().getId() : mContext.getString(R.string.deleted);
            return res.getString(R.string.event_gist_title,
                    !StringUtils.isBlank(login) ? login : mContext.getString(R.string.unknown),
                    payload.getAction(), id);
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK.equals(event.getType())) {
            return res.getString(R.string.event_fork_title, 
                    actor.getLogin(), formatFromRepoName(eventRepo));
        }

        /** ForkApplyEvent */
        else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            return res.getString(R.string.event_fork_apply_title,
                    actor.getLogin(), formatFromRepoName(eventRepo));
        }

        /** FollowEvent */
        else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            return res.getString(R.string.event_follow_title,
                    actor.getLogin(), payload.getTarget().getLogin());
        }

        /** CreateEvent */
        else if (Event.TYPE_CREATE.equals(eventType)) {
            CreatePayload payload = (CreatePayload) event.getPayload();
            if ("repository".equals(payload.getRefType())) {
                return res.getString(R.string.event_create_repo_title,
                        actor.getLogin(), formatFromRepoName(eventRepo));
            }
            else if ("branch".equals(payload.getRefType()) || "tag".equals(payload.getRefType())) {
                return res.getString(R.string.event_create_branch_title,
                        actor.getLogin(), payload.getRefType(), payload.getRef(),
                        formatFromRepoName(eventRepo));
            }
            else {
                return actor.getLogin();
            }
        }

        /** DeleteEvent */
        else if (Event.TYPE_DELETE.equals(eventType)) {
            DeletePayload payload = (DeletePayload) event.getPayload();
            if ("repository".equals(payload.getRefType())) {
                return res.getString(R.string.event_delete_repo_title,
                        actor.getLogin(), payload.getRef());
            }
            else {
                return res.getString(R.string.event_delete_branch_title,
                        actor.getLogin(), payload.getRefType(), payload.getRef(),
                        formatFromRepoName(eventRepo));
            }
        }

        /** MemberEvent */
        else if (Event.TYPE_MEMBER.equals(eventType)) {
            MemberPayload payload = (MemberPayload) event.getPayload();
            return res.getString(R.string.event_member_title,
                    actor.getLogin(), payload.getMember() != null ? payload.getMember().getLogin() : "",
                    formatFromRepoName(eventRepo));
        }

        /** DownloadEvent */
        else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            return res.getString(R.string.event_download_title,
                    actor.getLogin(), formatFromRepoName(eventRepo));
        }

        /** GollumEvent */
        else if (Event.TYPE_GOLLUM.equals(eventType)) {
            return res.getString(R.string.event_gollum_title,
                    actor.getLogin(), formatFromRepoName(eventRepo));
        }

        /** PublicEvent */
        else if (Event.TYPE_PUBLIC.equals(eventType)) {
            return res.getString(R.string.event_public_title,
                    actor.getLogin(), formatFromRepoName(eventRepo));
        }
        
        /** IssueCommentEvent */
        else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
            String type = mContext.getResources().getString(R.string.issue).toLowerCase(Locale.getDefault());
            if (payload.getIssue() != null) {
                if (payload.getIssue().getPullRequest() != null
                        && payload.getIssue().getPullRequest().getHtmlUrl() != null) {
                    type = mContext.getResources().getString(
                            R.string.pull_request_title).toLowerCase(Locale.getDefault());
                }
                
                return res.getString(R.string.event_issue_comment,
                        actor.getLogin(), type, payload.getIssue().getNumber(), formatFromRepoName(eventRepo));
            }
        }

        /** PullRequestReviewComment */
        else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            return res.getString(R.string.event_commit_comment_title,
                    actor.getLogin(), formatFromRepoName(eventRepo));
        }

        /** ReleaseEvent */
        else if (Event.TYPE_RELEASE.equals(eventType)) {
            return res.getString(R.string.event_release_title, actor.getLogin(),
                    formatFromRepoName(eventRepo));
        }
        return "";
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
    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvTitle;
        public TextView tvDesc;
        public ViewGroup llPushDesc;
    }
}