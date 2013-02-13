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
package com.gh4a;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventRepository;
import org.eclipse.egit.github.core.event.FollowPayload;
import org.eclipse.egit.github.core.event.ForkApplyPayload;
import org.eclipse.egit.github.core.event.ForkPayload;
import org.eclipse.egit.github.core.event.GistPayload;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.event.IssuesPayload;
import org.eclipse.egit.github.core.event.MemberPayload;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.event.PushPayload;
import org.eclipse.egit.github.core.service.EventService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.adapter.FeedAdapter;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.CommitUtils;



public class TimelineActivity extends BaseSherlockFragmentActivity
    implements OnItemClickListener, LoaderManager.LoaderCallbacks<List<Event>> {

    private FeedAdapter mFeedAdapter;
    private ListView mListView;
    private PageIterator<Event> mDataIterator;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.pub_timeline);
        actionBar.setSubtitle(R.string.explore);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mFeedAdapter = new FeedAdapter(this, new ArrayList<Event>());
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setAdapter(mFeedAdapter);
        mListView.setOnItemClickListener(this);

        loadData();
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
    }

    public void loadData() {
        EventService eventService = new EventService();
        mDataIterator = eventService.pagePublicEvents();    
    }

    private void fillData(List<Event> events) {
        invalidateOptionsMenu();
        mFeedAdapter.clear();
        mFeedAdapter.addAll(events);
        mFeedAdapter.notifyDataSetChanged();
    }
    
    @Override
    public Loader<List<Event>> onCreateLoader(int arg0, Bundle arg1) {
        return new PageIteratorLoader<Event>(this, mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<Event>> loader, List<Event> events) {
        hideLoading();
        fillData(events);
    }

    @Override
    public void onLoaderReset(Loader<List<Event>> arg0) {
        // TODO Auto-generated method stub
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.explore_menu, menu);
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!isAuthorized()) {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
                else {
                    getApplicationContext().openUserInfoActivity(this, getAuthLogin(), 
                            null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    return true;
                }
            case R.id.pub_timeline:
                Intent intent = new Intent().setClass(this, TimelineActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.trend:
                intent = new Intent().setClass(this, TrendingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.blog:
                intent = new Intent().setClass(this, BlogListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.refresh:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                loadData();
                getSupportLoaderManager().restartLoader(0, null, this);
                getSupportLoaderManager().getLoader(0).forceLoad();
                return true;
            default:
                return true;
        }
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Event event = (Event) adapterView.getAdapter().getItem(position);
        Gh4Application context = getApplicationContext();
        
        //if payload is a base class, return void.  Think that it is an old event which not supported
        //by API v3.
        if (event.getPayload().getClass().getSimpleName().equals("EventPayload")) {
            return;
        }
        
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String[] repoNamePart = eventRepo.getName().split("/");
        String repoOwner = "";
        String repoName = "";
        if (repoNamePart.length == 2) {
            repoOwner = repoNamePart[0];
            repoName = repoNamePart[1];
        }
        String repoUrl = eventRepo.getUrl();
        
        /** PushEvent */
        if (Event.TYPE_PUSH.equals(eventType)) {
            
            if (eventRepo != null) {
                PushPayload payload = (PushPayload) event.getPayload();
                
                List<Commit> commits = payload.getCommits();
                // if commit > 1, then show compare activity
                
                if (commits != null && commits.size() > 1) {
                    Intent intent = new Intent().setClass(context, CompareActivity.class);
                    for (Commit commit : commits) {
                        String[] commitInfo = new String[4];
                        commitInfo[0] = commit.getSha();
                        commitInfo[1] = CommitUtils.getAuthorEmail(commit);
                        commitInfo[2] = commit.getMessage();
                        commitInfo[3] = CommitUtils.getAuthorName(commit);
                        intent.putExtra("commit" + commit.getSha(), commitInfo);
                    }
                    
                    intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                    intent.putExtra(Constants.Repository.HEAD, payload.getHead());
                    intent.putExtra(Constants.Repository.BASE, payload.getRef());
                    startActivity(intent);
                }
                // only 1 commit, then show the commit details
                else {
                    context.openCommitInfoActivity(this, repoOwner, repoName,
                            payload.getCommits().get(0).getSha(), 0);
                }
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** IssueEvent */
        else if (Event.TYPE_ISSUES.equals(eventType)) {
            if (eventRepo != null) {
                IssuesPayload payload = (IssuesPayload) event.getPayload();
                context.openIssueActivity(this, repoOwner, repoName, payload.getIssue().getNumber());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** WatchEvent */
        else if (Event.TYPE_WATCH.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(this, repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** CreateEvent */
        else if (Event.TYPE_CREATE.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(this, repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** PullRequestEvent */
        else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            if (eventRepo != null) {
                PullRequestPayload payload = (PullRequestPayload) event.getPayload();
                int pullRequestNumber = payload.getNumber();
                context.openPullRequestActivity(this, repoOwner, repoName, pullRequestNumber);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** FollowEvent */
        else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            if (payload.getTarget() != null) {
                context.openUserInfoActivity(this, payload.getTarget().getLogin(), null);
            }
        }

        /** CommitCommentEvent */
        else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            if (eventRepo != null) {
                CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
                context.openCommitInfoActivity(this, repoOwner, repoName, 
                        payload.getComment().getCommitId(), 0);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** DeleteEvent */
        else if (Event.TYPE_DELETE.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(this, repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** DownloadEvent */
        else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            if (eventRepo != null) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                context.openBrowser(this, payload.getDownload().getHtmlUrl());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                context.openRepositoryInfoActivity(this, forkee);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            if (eventRepo != null) {
                ForkApplyPayload payload = (ForkApplyPayload) event.getPayload();
                context.openRepositoryInfoActivity(this, repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** GollumEvent */
        else if (Event.TYPE_GOLLUM.equals(eventType)) {
            Intent intent = new Intent().setClass(this, WikiListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, repoName);
            startActivity(intent);
        }

        /** PublicEvent */
        else if (Event.TYPE_PUBLIC.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(this, repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        
        /** MemberEvent */
        else if (Event.TYPE_MEMBER.equals(eventType)) {
            if (eventRepo != null) {
                MemberPayload payload = (MemberPayload) event.getPayload();
                context.openRepositoryInfoActivity(this, repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        
        /** Gist Event **/
        else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            context.openGistActivity(this, payload.getGist().getUser().getLogin(),
                    payload.getGist().getId(), 0);
        }
        
        /** IssueCommentEvent */
        else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            if (eventRepo != null) {
                IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
                String type = payload.getIssue().getPullRequest().getDiffUrl() != null ? "pullrequest" : "issue";
                if ("pullrequest".equals(type)) {
                    context.openPullRequestActivity(this, repoOwner, repoName, payload.getIssue().getNumber());   
                }
                else {
                    context.openIssueActivity(this, repoOwner, repoName, payload.getIssue().getNumber(),
                            payload.getIssue().getState()); 
                }
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        
        /** PullRequestReviewComment */
        else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload = (PullRequestReviewCommentPayload) event.getPayload();
            context.openCommitInfoActivity(this, repoOwner, repoName, 
                    payload.getComment().getCommitId(), 0);
        }
    }
}
