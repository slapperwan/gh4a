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
import org.eclipse.egit.github.core.event.WatchPayload;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarUtils;
import com.gh4a.utils.StringUtils;

public class FeedAdapter extends RootAdapter<Event> {

    private AQuery aq;
    
    public FeedAdapter(Context context, List<Event> objects) {
        super(context, objects);
        aq = new AQuery((BaseSherlockFragmentActivity) context);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.feed_row, null);
            
            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            Typeface regular = app.regular;

            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvTitle.setTypeface(boldCondensed);
            
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(regular);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        Event event = mObjects.get(position);
        final User actor = event.getActor();
        
        if (event != null) {
            
            aq.recycle(convertView);
            aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(actor.getGravatarId()), 
                    true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);

            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (!StringUtils.isBlank(actor.getLogin())) {
                        /** Open user activity */
                        Gh4Application context = (Gh4Application) v.getContext()
                                .getApplicationContext();
                        context.openUserInfoActivity(v.getContext(), actor
                                .getLogin(), actor.getName());
                    }
                }
            });
            SpannableString createdAt = new SpannableString(pt.format(event.getCreatedAt()));
            createdAt.setSpan(new TextAppearanceSpan(v.getContext(), R.style.default_text_small_italic),
                    0, createdAt.length(), 0);
            
            viewHolder.tvTitle.setText(TextUtils.concat(formatTitle(event), " ", createdAt));
            
            String content = formatDescription(event, viewHolder, (RelativeLayout) v);
            if (content != null) {
                viewHolder.tvDesc.setVisibility(View.VISIBLE);
                viewHolder.tvDesc.setText(content);
            }
            else if (View.VISIBLE == viewHolder.tvDesc.getVisibility()) {
                viewHolder.tvDesc.setText(null);
                viewHolder.tvDesc.setVisibility(View.GONE);
            }
            else {
                viewHolder.tvDesc.setText(null);
                viewHolder.tvDesc.setVisibility(View.GONE);
            }
        }
        return v;
    }

    /**
     * Format description.
     *
     * @param feed the feed
     * @param viewHolder the view holder
     * @param baseView the base view
     * @return the string
     */
    private String formatDescription(Event event, ViewHolder viewHolder,
            final RelativeLayout baseView) {
        
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        User actor = event.getActor();
        
        //if payload is a base class, return default eventtype.  Think that it is an old event which not supported
        //by API v3.
        if (event.getPayload().getClass().getSimpleName().equals("EventPayload")) {
            return event.getType();
        }
        
        Resources res = mContext.getResources();
        LinearLayout ll = (LinearLayout) baseView.findViewById(R.id.ll_push_desc);
        ll.removeAllViews();
        TextView generalDesc = (TextView) baseView.findViewById(R.id.tv_desc);
        ll.setVisibility(View.GONE);
        generalDesc.setVisibility(View.VISIBLE);

        /** PushEvent */
        if (Event.TYPE_PUSH.equals(eventType)) {
            generalDesc.setVisibility(View.GONE);
            ll.setVisibility(View.VISIBLE);
            PushPayload payload = (PushPayload) event.getPayload();
            List<Commit> commits = payload.getCommits();
            
            if (commits != null) {
                
                Gh4Application app = (Gh4Application) mContext.getApplicationContext();
                Typeface regular = app.regular;
                Typeface italic = app.italic;
                
                for (int i = 0; i < commits.size(); i++) {
                    Commit commit = commits.get(i);
                    SpannableString spannableSha = new SpannableString(commit.getSha().substring(0, 7));
                    if (eventRepo != null) {
                        spannableSha.setSpan(new TextAppearanceSpan(baseView.getContext(),
                                R.style.default_text_small_url), 0, spannableSha.length(), 0);
                    }
                    else {
                        spannableSha = new SpannableString("(deleted)");
                    }
                    
                    TextView tvCommitMsg = new TextView(baseView.getContext());
                    tvCommitMsg.setText(spannableSha);
                    tvCommitMsg.append(" " + commit.getMessage());
                    tvCommitMsg.setSingleLine(true);
                    tvCommitMsg.setTextAppearance(baseView.getContext(), android.R.style.TextAppearance_Small);
                    tvCommitMsg.setTypeface(regular);
                    ll.addView(tvCommitMsg);
    
                    if (i == 2 && commits.size() > 3) {// show limit 3 lines
                        TextView tvMoreMsg = new TextView(baseView.getContext());
                        String text = res.getString(R.string.event_push_desc, commits.size() - 3);
                        tvMoreMsg.setText(text);
                        tvMoreMsg.setTypeface(italic);
                        ll.addView(tvMoreMsg);
                        break;
                    }
                }
            }
            return null;
        }

        /** CommitCommentEvent */
        else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();

            String text = res.getString(R.string.event_commit_comment_desc) + " ";
            text += payload.getComment().getCommitId().substring(0, 7) 
                    + " - " + payload.getComment().getBody();
            return text;
        }

        /** PullRequestEvent */
        else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            PullRequest pullRequest = payload.getPullRequest();
            
            if (!StringUtils.isBlank(pullRequest.getTitle())) {
                String text = String.format(res.getString(R.string.event_pull_request_desc),
                        pullRequest.getTitle(),
                        pullRequest.getCommits(),
                        pullRequest.getAdditions(),
                        pullRequest.getDeletions());
                return text;
            }
            return null;
        }

        /** FollowEvent */
        else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            User target = payload.getTarget();
            if (target != null) {
                String text = String.format(res.getString(R.string.event_follow_desc),
                        target.getLogin(),
                        target.getPublicRepos(),
                        target.getFollowers());
                return text;
            }
            return null;
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            String text = String.format(res.getString(R.string.event_fork_desc),
                    formatToRepoName(payload.getForkee()));
            return text;
        }

        /** CreateEvent */
        else if (Event.TYPE_CREATE.equals(eventType)) {
            CreatePayload payload = (CreatePayload) event.getPayload();
            if ("repository".equals(payload.getRefType())) {
                String text = String.format(res.getString(R.string.event_create_repo_desc),
                        eventRepo.getName());

                return text;
            }
            else if ("branch".equals(payload.getRefType()) || "tag".equals(payload.getRefType())) {
                String text = String.format(res.getString(R.string.event_create_branch_desc),
                        payload.getRefType(),
                        eventRepo.getName(),
                        payload.getRef());

                return text;
            }
            generalDesc.setVisibility(View.GONE);
            return null;
        }

        /** DownloadEvent */
        else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            return payload.getDownload().getName();
        }

        /** GollumEvent */
        else if (Event.TYPE_GOLLUM.equals(eventType)) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) {
                String text = String.format(res.getString(R.string.event_gollum_desc),
                        pages.get(0).getPageName());
                return text;
            }
            return "";
        }

        /** PublicEvent */
        else if (Event.TYPE_PUBLIC.equals(eventType)) {
            eventRepo.getName();
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
            else {
                return eventType;
            }
        }

        /** PullRequestReviewComment */
        else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload = (PullRequestReviewCommentPayload) event.getPayload();
            
//            String text = String.format(res.getString(R.string.event_pull_request_review_comment_desc),
//                    payload.getComment().getPath(), payload.getComment().getId());
            
            String text = res.getString(R.string.event_commit_comment_desc) + " ";
            text += payload.getComment().getCommitId().substring(0, 7) 
                    + " - " + payload.getComment().getBody();
            
            return text;
        }
        
        else {
            generalDesc.setVisibility(View.GONE);
            return null;
        }
    }

    /**
     * Format title.
     * 
     * @param feed the feed
     * @return the string
     */
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
            String text = String.format(res.getString(R.string.event_push_title),
                    actor.getLogin(),
                    refPart.length == 3 ? refPart[2] : payload.getRef(),
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** IssuesEvent */
        else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload eventPayload = (IssuesPayload) event.getPayload();
            String text = String.format(res.getString(R.string.event_issues_title),
                    actor.getLogin(),
                    eventPayload.getAction(),
                    eventPayload.getIssue().getNumber(),
                    formatFromRepoName(eventRepo)); 
            return text;
        }

        /** CommitCommentEvent */
        else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            String text = String.format(res.getString(R.string.event_commit_comment_title),
                    actor.getLogin(),
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** PullRequestEvent */
        else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            int pullRequestNumber = payload.getNumber();
            String text = String.format(res.getString(R.string.event_pull_request_title),
                    actor.getLogin(),
                    payload.getAction(),
                    pullRequestNumber,
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** WatchEvent */
        else if (Event.TYPE_WATCH.equals(eventType)) {
            WatchPayload payload = (WatchPayload) event.getPayload();
//            String text = String.format(res.getString(R.string.event_watch_title),
//                    actor.getLogin(), payload.getAction(),
//                    formatFromRepoName(eventRepo));
            String text = String.format(res.getString(R.string.event_watch_title),
                    actor.getLogin(),
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** GistEvent */
        else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            String login = actor.getLogin();
            if (StringUtils.isBlank(login) && payload.getGist() != null
                    && payload.getGist().getUser() != null) {
                login = payload.getGist().getUser().getLogin(); 
            }
            
            String text = String.format(res.getString(R.string.event_gist_title),
                    !StringUtils.isBlank(login) ? login : "Unknown",
                    payload.getAction(), 
                    payload.getGist() != null ? payload.getGist().getId() : "(deleted)");
            return text;
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK.equals(event.getType())) {
            String text = String.format(res.getString(R.string.event_fork_title), 
                    actor.getLogin(),
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** ForkApplyEvent */
        else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            String text = String.format(res.getString(R.string.event_fork_apply_title),
                    actor.getLogin(),
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** FollowEvent */
        else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            String text = String.format(res.getString(R.string.event_follow_title),
                    actor.getLogin(),
                    payload.getTarget().getLogin());
            return text;
        }

        /** CreateEvent */
        else if (Event.TYPE_CREATE.equals(eventType)) {
            CreatePayload payload = (CreatePayload) event.getPayload();
            if ("repository".equals(payload.getRefType())) {
                String text = String.format(res.getString(R.string.event_create_repo_title),
                        actor.getLogin(), formatFromRepoName(eventRepo));
                return text;
            }
            else if ("branch".equals(payload.getRefType()) || "tag".equals(payload.getRefType())) {
                String text = String.format(res.getString(R.string.event_create_branch_title),
                        actor.getLogin(), payload.getRefType(), payload.getRef(),
                        formatFromRepoName(eventRepo));
                return text;
            }
            else {
                return actor.getLogin();
            }
        }

        /** DeleteEvent */
        else if (Event.TYPE_DELETE.equals(eventType)) {
            DeletePayload payload = (DeletePayload) event.getPayload();
            if ("repository".equals(payload.getRefType())) {
                String text = String.format(res.getString(R.string.event_delete_repo_title),
                        actor.getLogin(), payload.getRef());
                return text;
            }
            else {
                String text = String.format(res.getString(R.string.event_delete_branch_title),
                        actor.getLogin(), payload.getRefType(), payload.getRef(),
                        formatFromRepoName(eventRepo));
                return text;
            }
        }

        /** WikiEvent */
//        else if (Event.TYPE_.WIKI_EVENT.equals(feed.getType())) {
//            String text = String.format(res.getString(R.string.event_wiki_title), 
//                    feed.getActor(),
//                    payload.getAction(),
//                    formatFromRepoName(feed));
//            return text;
//        }

        /** MemberEvent */
        else if (Event.TYPE_MEMBER.equals(eventType)) {
            MemberPayload payload = (MemberPayload) event.getPayload();
            String text = String.format(res.getString(R.string.event_member_title),
                    actor.getLogin(), payload.getMember() != null ? payload.getMember().getLogin() : "",
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** DownloadEvent */
        else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            String text = String.format(res.getString(R.string.event_download_title),
                    actor.getLogin(),
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** GollumEvent */
        else if (Event.TYPE_GOLLUM.equals(eventType)) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            String text = String.format(res.getString(R.string.event_gollum_title),
                    actor.getLogin(), "edited", "page",
                    formatFromRepoName(eventRepo));
            return text;
        }

        /** PublicEvent */
        else if (Event.TYPE_PUBLIC.equals(eventType)) {
            String text = String.format(res.getString(R.string.event_public_title),
                    actor.getLogin(), formatFromRepoName(eventRepo));
            return text;
        }
        
        /** IssueCommentEvent */
        else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
            String type = mContext.getResources().getString(R.string.issue).toLowerCase();
            if (payload.getIssue() != null) {
                if (payload.getIssue().getPullRequest() != null
                        && payload.getIssue().getPullRequest().getHtmlUrl() != null) {
                    type = mContext.getResources().getString(R.string.pull_request_title).toLowerCase();
                }
                
                String text = String.format(res.getString(R.string.event_issue_comment),
                        actor.getLogin(), type, payload.getIssue().getNumber(), formatFromRepoName(eventRepo));
                return text;
            }
            else {
                return "";
            }
        }

        /** PullRequestReviewComment */
        else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload = (PullRequestReviewCommentPayload) event.getPayload();
            String text = String.format(res.getString(R.string.event_commit_comment_title),
                    actor.getLogin(),
                    formatFromRepoName(eventRepo));
            return text;
        }
        
        else {
            return "";
        }
    }

    /**
     * The Class InternalURLSpan.
     */
    static class InternalURLSpan extends ClickableSpan {

        /** The listener. */
        OnClickListener mListener;

        /**
         * Instantiates a new internal url span.
         * 
         * @param listener the listener
         */
        public InternalURLSpan(OnClickListener listener) {
            mListener = listener;
        }

        /*
         * (non-Javadoc)
         * @see android.text.style.ClickableSpan#onClick(android.view.View)
         */
        @Override
        public void onClick(View widget) {
            mListener.onClick(widget);
        }
    }
    
    private static String formatFromRepoName(EventRepository repository) {
        if (repository != null) {
            return repository.getName();
        }
        return "(deleted)";
    }

    private static String formatToRepoName(Repository repository) {
        if (repository != null && repository.getOwner() != null) {
            return repository.getOwner().getLogin() + "/" + repository.getName();
        }
        return "(deleted)";
    }
    
    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {
        
        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv title. */
        public TextView tvTitle;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv created at. */
        public TextView tvCreatedAt;
    }
}