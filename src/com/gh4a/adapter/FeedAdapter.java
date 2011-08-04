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

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.ObjectPayloadPullRequest;
import com.github.api.v2.schema.Payload;
import com.github.api.v2.schema.PayloadPullRequest;
import com.github.api.v2.schema.PayloadTarget;
import com.github.api.v2.schema.Repository;
import com.github.api.v2.schema.UserFeed;

/**
 * The Feed adapter.
 */
public class FeedAdapter extends RootAdapter<UserFeed> {

    /**
     * Instantiates a new feed adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public FeedAdapter(Context context, List<UserFeed> objects) {
        super(context, objects);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View,
     * android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.feed_row, null);

            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvCreatedAt = (TextView) v.findViewById(R.id.tv_created_at);
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final UserFeed feed = mObjects.get(position);
        if (feed != null) {
            ImageDownloader.getInstance().download(feed.getActorAttributes().getGravatarId(),
                    viewHolder.ivGravatar);
            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    Gh4Application context = (Gh4Application) v.getContext()
                            .getApplicationContext();
                    context.openUserInfoActivity(v.getContext(), feed.getActorAttributes()
                            .getLogin(), feed.getActorAttributes().getName());
                }
            });
            viewHolder.tvTitle.setText(formatTitle(feed));
            
            String content = formatDescription(feed, viewHolder, (RelativeLayout) v);
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

            viewHolder.tvCreatedAt.setText(pt.format(feed.getCreatedAt()));
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
    private String formatDescription(UserFeed feed, ViewHolder viewHolder,
            final RelativeLayout baseView) {
        Payload payload = feed.getPayload();
        Resources res = mContext.getResources();
        LinearLayout ll = (LinearLayout) baseView.findViewById(R.id.ll_push_desc);
        ll.removeAllViews();
        TextView generalDesc = (TextView) baseView.findViewById(R.id.tv_desc);
        ll.setVisibility(View.GONE);
        generalDesc.setVisibility(View.VISIBLE);

        /** PushEvent */
        if (UserFeed.Type.PUSH_EVENT.equals(feed.getType())) {
            generalDesc.setVisibility(View.GONE);
            ll.setVisibility(View.VISIBLE);
            List<String[]> shas = payload.getShas();
            Repository repository = feed.getRepository();
            boolean isRepoExists = repository != null;
            for (int i = 0; i < shas.size(); i++) {
                String[] sha = shas.get(i);
                SpannableString spannableSha = new SpannableString(sha[0].substring(0, 7));
                if (isRepoExists) {
                    spannableSha.setSpan(new TextAppearanceSpan(baseView.getContext(),
                            R.style.default_text_medium_url), 0, spannableSha.length(), 0);
                }
                else {
                    spannableSha = new SpannableString("(deleted)");
                }
                
                TextView tvCommitMsg = new TextView(baseView.getContext());
                tvCommitMsg.setText(spannableSha);
                tvCommitMsg.append(" " + sha[2]);
                tvCommitMsg.setSingleLine(true);
                tvCommitMsg.setTextAppearance(baseView.getContext(), R.style.default_text_medium);
                ll.addView(tvCommitMsg);

                if (i == 2 && shas.size() > 3) {// show limit 3 lines
                    TextView tvMoreMsg = new TextView(baseView.getContext());
                    String text = res.getString(R.string.event_push_desc, shas.size() - 3);
                    tvMoreMsg.setText(text);
                    ll.addView(tvMoreMsg);
                    break;
                }
            }
            return null;
        }

        /** CommitCommentEvent */
        else if (UserFeed.Type.COMMIT_COMMENT_EVENT.equals(feed.getType())) {
            SpannableString spannableSha = new SpannableString(payload.getCommit().substring(0, 7));
            spannableSha.setSpan(new TextAppearanceSpan(baseView.getContext(),
                    R.style.default_text_medium_url), 0, spannableSha.length(), 0);

            generalDesc.setText(R.string.event_commit_comment_desc);
            generalDesc.append(spannableSha);
            return null;
        }

        /** PullRequestEvent */
        else if (UserFeed.Type.PULL_REQUEST_EVENT.equals(feed.getType())) {
            PayloadPullRequest pullRequest = payload.getPullRequest();
            
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
        else if (UserFeed.Type.FOLLOW_EVENT.equals(feed.getType())) {
            PayloadTarget target = payload.getTarget();
            if (target != null) {
                String text = String.format(res.getString(R.string.event_follow_desc),
                        target.getLogin(),
                        target.getRepos(),
                        target.getFollowers());
                return text;
            }
            return null;
        }

        /** WatchEvent */
        else if (UserFeed.Type.WATCH_EVENT.equals(feed.getType())) {
            Repository repository = feed.getRepository();
            StringBuilder sb = new StringBuilder();
            if (repository != null) {
                sb.append(StringUtils.doTeaser(repository.getDescription()));
            }
            return sb.toString();
        }

        /** ForkEvent */
        else if (UserFeed.Type.FORK_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_fork_desc),
                    formatToRepoName(feed));
            return text;
        }

        /** CreateEvent */
        else if (UserFeed.Type.CREATE_EVENT.equals(feed.getType())) {
            if ("repository".equals(payload.getObject())) {
                String text = String.format(res.getString(R.string.event_create_repo_desc),
                        feed.getActor(),
                        feed.getPayload().getName());

                return text;
            }
            else if ("branch".equals(payload.getObject()) || "tag".equals(payload.getObject())) {
                String text = String.format(res.getString(R.string.event_create_branch_desc),
                        payload.getObject(),
                        feed.getActor(),
                        feed.getPayload().getName(),
                        feed.getPayload().getObjectName());

                return text;
            }
            generalDesc.setVisibility(View.GONE);
            return null;
        }

        /** DownloadEvent */
        else if (UserFeed.Type.DOWNLOAD_EVENT.equals(feed.getType())) {
            String filename = payload.getUrl();
            int index = filename.lastIndexOf("/");
            if (index != -1) {
                filename = filename.substring(index + 1, filename.length());
            }
            return filename;
        }

        /** GollumEvent */
        else if (UserFeed.Type.GOLLUM_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_gollum_desc),
                    payload.getTitle());
            return text;
        }

        /** PublicEvent */
        else if (UserFeed.Type.PUBLIC_EVENT.equals(feed.getType())) {
            Repository repository = feed.getRepository();
            if (repository != null) {
                return StringUtils.doTeaser(repository.getDescription());
            }
            else {
                return payload.getRepo();
            }
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
    private String formatTitle(UserFeed feed) {
        Payload payload = feed.getPayload();
        Resources res = mContext.getResources();

        /** PushEvent */
        if (UserFeed.Type.PUSH_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_push_title),
                    feed.getActor(),
                    payload.getRef().split("/")[2],
                    formatFromRepoName(feed));
            return text;
        }

        /** IssuesEvent */
        else if (UserFeed.Type.ISSUES_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_issues_title),
                    feed.getActor(),
                    payload.getAction(),
                    payload.getNumber(),
                    formatFromRepoName(feed)); 
            return text;
        }

        /** CommitCommentEvent */
        else if (UserFeed.Type.COMMIT_COMMENT_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_commit_comment_title),
                    feed.getActor(),
                    formatFromRepoName(feed));
            return text;
        }

        /** PullRequestEvent */
        else if (UserFeed.Type.PULL_REQUEST_EVENT.equals(feed.getType())) {
            int pullRequestNumber = payload.getNumber();
            if (payload.getPullRequest() instanceof ObjectPayloadPullRequest) {
                pullRequestNumber = ((ObjectPayloadPullRequest) payload.getPullRequest()).getNumber();
            }
            String text = String.format(res.getString(R.string.event_pull_request_title),
                    feed.getActor(),
                    "closed".equals(payload.getAction()) ? "merged" : payload.getAction(),
                    pullRequestNumber,
                    formatFromRepoName(feed));
            return text;
        }

        /** WatchEvent */
        else if (UserFeed.Type.WATCH_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_watch_title),
                    feed.getActor(), payload.getAction(),
                    formatFromRepoName(feed));
            return text;
        }

        /** GistEvent */
        else if (UserFeed.Type.GIST_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_gist_title),
                    feed.getActor(),
                    payload.getAction(), payload.getName());
            return text;
        }

        /** ForkEvent */
        else if (UserFeed.Type.FORK_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_fork_title), 
                    feed.getActor(),
                    formatFromRepoName(feed));
            return text;
        }

        /** ForkApplyEvent */
        else if (UserFeed.Type.FORK_APPLY_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_fork_apply_title),
                    feed.getActor(),
                    formatFromRepoName(feed));
            return text;
        }

        /** FollowEvent */
        else if (UserFeed.Type.FOLLOW_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_follow_title),
                    feed.getActor(),
                    payload.getTarget() != null ? payload.getTarget().getLogin() : payload
                            .getTargetId());
            return text;
        }

        /** CreateEvent */
        else if (UserFeed.Type.CREATE_EVENT.equals(feed.getType())) {
            if ("repository".equals(payload.getRefType())) {
                String text = String.format(res.getString(R.string.event_create_repo_title),
                        feed.getActor(), formatFromRepoName(feed));
                return text;
            }
            else if ("branch".equals(payload.getRefType()) || "tag".equals(payload.getRefType())) {
                String text = String.format(res.getString(R.string.event_create_branch_title),
                        feed.getActor(), payload.getRefType(), payload.getRef(),
                        formatFromRepoName(feed));
                return text;
            }
            else {
                return feed.getActor();
            }
        }

        /** DeleteEvent */
        else if (UserFeed.Type.DELETE_EVENT.equals(feed.getType())) {
            if ("repository".equals(payload.getObject())) {
                String text = String.format(res.getString(R.string.event_delete_repo_title),
                        feed.getActor(), payload.getName());
                return text;
            }
            else {
                String text = String.format(res.getString(R.string.event_delete_branch_title),
                        feed.getActor(), payload.getRefType(), payload.getRef(),
                        formatFromRepoName(feed));
                return text;
            }
        }

        /** WikiEvent */
        else if (UserFeed.Type.WIKI_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_wiki_title), 
                    feed.getActor(),
                    payload.getAction(),
                    formatFromRepoName(feed));
            return text;
        }

        /** MemberEvent */
        else if (UserFeed.Type.MEMBER_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_member_title),
                    feed.getActor(), payload.getMember().getLogin(),
                    formatFromRepoName(feed));
            return text;
        }

        /** DownloadEvent */
        else if (UserFeed.Type.DOWNLOAD_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_download_title),
                    feed.getActor(),
                    formatFromRepoName(feed));
            return text;
        }

        /** GollumEvent */
        else if (UserFeed.Type.GOLLUM_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_gollum_title),
                    feed.getActor(), payload.getAction(), payload.getTitle(),
                    formatFromRepoName(feed));
            return text;
        }

        /** PublicEvent */
        else if (UserFeed.Type.PUBLIC_EVENT.equals(feed.getType())) {
            String text = String.format(res.getString(R.string.event_public_title),
                    feed.getActor(), formatFromRepoName(feed));
            return text;
        }
        
        /** IssueCommentEvent */
        else if (UserFeed.Type.ISSUE_COMMENT_EVENT.equals(feed.getType())) {
            String url = feed.getUrl();
            int idx1 = url.indexOf("/issues/");
            int idx2 = url.indexOf("#issuecomment");
            if (idx2 == -1) {//sometime it return comment only
                idx2 = url.indexOf("#comment");
            }
            String issueId = "";
            if (idx2 != -1) {
                issueId = url.substring(idx1 + 8, idx2);
            }
            String text = String.format(res.getString(R.string.event_issue_comment),
                    feed.getActor(), issueId, formatFromRepoName(feed));
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
    
    private static String formatFromRepoName(UserFeed userFeed) {
        Repository repository = userFeed.getRepository();
        if (repository != null) {
            return repository.getOwner() + "/" + repository.getName();
        }
        return "(deleted)";
    }

    private static String formatToRepoName(UserFeed userFeed) {
        String url = userFeed.getUrl();
        if (!StringUtils.isBlank(url)) {
            String[] urlParts = url.split("/");
            if (urlParts.length > 3) {
                return urlParts[3] + "/" + urlParts[4];
            }
            else {
                return "(deleted)";
            }
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