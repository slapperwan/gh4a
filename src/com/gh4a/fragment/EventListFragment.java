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
package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.GollumPage;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventRepository;
import org.eclipse.egit.github.core.event.FollowPayload;
import org.eclipse.egit.github.core.event.ForkPayload;
import org.eclipse.egit.github.core.event.GistPayload;
import org.eclipse.egit.github.core.event.GollumPayload;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.event.IssuesPayload;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.event.PushPayload;
import org.eclipse.egit.github.core.service.EventService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CompareActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.adapter.FeedAdapter;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.StringUtils;

public abstract class EventListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<Event>>, OnItemClickListener, OnScrollListener {

    static final int MENU_USER = 1;
    static final int MENU_REPO = 2;
    static final int MENU_OPEN_ISSUES = 4;
    static final int MENU_ISSUE = 5;
    static final int MENU_COMMENT_IN_BROWSER = 6;
    static final int MENU_GIST = 7;
    static final int MENU_FILE = 8;
    static final int MENU_FORKED_REPO = 9;
    static final int MENU_WIKI_IN_BROWSER = 10;
    static final int MENU_PULL_REQ = 11;
    static final int MENU_COMPARE = 12;
    static final int MENU_COMMENT_COMMIT = 99;
    static final int MENU_PUSH_COMMIT_START = 100;
    static final int MENU_PUSH_COMMIT_END = 300;
    
    private String mLogin;
    private boolean mIsPrivate;
    private ListView mListView;
    private FeedAdapter mAdapter;
    protected PageIterator<Event> mDataIterator;
    private boolean isLoadMore;
    private boolean isLoadCompleted;
    private TextView mLoadingView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mIsPrivate = getArguments().getBoolean(Constants.Event.IS_PRIVATE);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        LayoutInflater vi = getSherlockActivity().getLayoutInflater();
        mLoadingView = (TextView) vi.inflate(R.layout.row_simple, null);
        mLoadingView.setText(R.string.loading_msg);
        mLoadingView.setTextColor(getResources().getColor(R.color.highlight));
        
        mAdapter = new FeedAdapter(getSherlockActivity());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
        registerForContextMenu(mListView);
        
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (!isLoadCompleted) {
            refresh();
        }
    }
    
    public void loadData() {
        EventService eventService = (EventService)
                Gh4Application.get(getActivity()).getService(Gh4Application.EVENT_SERVICE);
        if (mIsPrivate) {
            mDataIterator = eventService.pageUserReceivedEvents(mLogin, true);
        }
        else {
            mDataIterator = eventService.pageUserEvents(mLogin, false);
        }
    }
    
    public void refresh() {
        isLoadMore = false;
        loadData();
        if (getLoaderManager().getLoader(0) == null) {
            getLoaderManager().initLoader(0, null, this);
        }
        else {
            getLoaderManager().restartLoader(0, null, this);
        }
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData(List<Event> events) {
        SherlockFragmentActivity activity = getSherlockActivity();
        activity.invalidateOptionsMenu();
        if (events != null && !events.isEmpty()) {
            if (mListView.getFooterViewsCount() == 0) {
                mListView.addFooterView(mLoadingView);
                mListView.setAdapter(mAdapter);
            }
            if (isLoadMore) {
                mAdapter.addAll(mAdapter.getCount(), events);
                mAdapter.notifyDataSetChanged();
            }
            else {
                mAdapter.clear();
                mAdapter.addAll(events);
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(0);
            }
        }
        else {
            mListView.removeFooterView(mLoadingView);
        }
    }
    
    @Override
    public Loader<List<Event>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<Event>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<Event>> loader, List<Event> events) {
        isLoadCompleted = true;
        hideLoading();
        fillData(events);
    }

    @Override
    public void onLoaderReset(Loader<List<Event>> arg0) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {

        boolean loadMore = firstVisible + visibleCount >= totalCount;

        if(loadMore) {
            if (getLoaderManager().getLoader(0) != null
                    && isLoadCompleted) {
                isLoadMore = true;
                isLoadCompleted = false;
                getLoaderManager().getLoader(0).forceLoad();
            }
        }
    }
    
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Event event = (Event) adapterView.getAdapter().getItem(position);
        Gh4Application context = Gh4Application.get(getActivity());
        
        if (event == null || event.getPayload() == null) {
            return;
        }
        
        //if payload is a base class, return void.  Think that it is an old event which not supported
        //by API v3.
        if (event.getPayload().getClass().getSimpleName().equals("EventPayload")) {
            return;
        }
        
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String repoOwner = "";
        String repoName = "";

        if (eventRepo != null) {
            String[] repoNamePart = eventRepo.getName().split("/");
            if (repoNamePart.length == 2) {
                repoOwner = repoNamePart[0];
                repoName = repoNamePart[1];
            }
        }
        
        /** PushEvent */
        if (Event.TYPE_PUSH.equals(eventType)) {
            
            if (eventRepo != null) {
                PushPayload payload = (PushPayload) event.getPayload();
                
                List<Commit> commits = payload.getCommits();
                // if commit > 1, then show compare activity
                
                if (commits != null) {
                    if (commits.size() > 1) {
                        Intent intent = new Intent().setClass(context, CompareActivity.class);
                        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                        intent.putExtra(Constants.Repository.HEAD, payload.getHead());
                        intent.putExtra(Constants.Repository.BASE, payload.getBefore());
                        startActivity(intent);
                    }
                    // only 1 commit, then show the commit details
                    else if (commits.size() == 1) {
                        context.openCommitInfoActivity(getSherlockActivity(), repoOwner, repoName,
                                payload.getCommits().get(0).getSha(), 0);
                    }
                    else {
                        context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName, 0);
                    }
                }
                else {
                    context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName, 0);
                }
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** IssueEvent */
        else if (Event.TYPE_ISSUES.equals(eventType)) {
            if (eventRepo != null) {
                IssuesPayload payload = (IssuesPayload) event.getPayload();
                context.openIssueActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber());
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** WatchEvent */
        else if (Event.TYPE_WATCH.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** CreateEvent */
        else if (Event.TYPE_CREATE.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(this.getSherlockActivity(), repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** PullRequestEvent */
        else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            if (eventRepo != null) {
                PullRequestPayload payload = (PullRequestPayload) event.getPayload();
                int pullRequestNumber = payload.getNumber();
                context.openPullRequestActivity(getSherlockActivity(), repoOwner, repoName, pullRequestNumber);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** FollowEvent */
        else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            if (payload.getTarget() != null) {
                context.openUserInfoActivity(getSherlockActivity(), payload.getTarget().getLogin(), null);
            }
        }

        /** CommitCommentEvent */
        else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            if (eventRepo != null) {
                CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
                context.openCommitInfoActivity(getSherlockActivity(), repoOwner, repoName, 
                        payload.getComment().getCommitId(), 0);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** DeleteEvent */
        else if (Event.TYPE_DELETE.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** DownloadEvent */
        else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            if (eventRepo != null) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                context.openBrowser(getSherlockActivity(), payload.getDownload().getHtmlUrl());
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), forkee);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** ForkEvent */
        else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }

        /** GollumEvent */
        else if (Event.TYPE_GOLLUM.equals(eventType)) {
            Intent intent = new Intent().setClass(getSherlockActivity(), WikiListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, repoName);
            GollumPayload payload = (GollumPayload) event.getPayload();
            if (!payload.getPages().isEmpty()) {
                intent.putExtra(Constants.Object.OBJECT_SHA, payload.getPages().get(0).getSha());
            }
            startActivity(intent);
        }

        /** PublicEvent */
        else if (Event.TYPE_PUBLIC.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        
        /** MemberEvent */
        else if (Event.TYPE_MEMBER.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName, 0);
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        
        /** Gist Event **/
        else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            String login = event.getActor().getLogin();
            if (StringUtils.isBlank(login) && payload.getGist() != null
                    && payload.getGist().getUser() != null) {
                login = payload.getGist().getUser().getLogin(); 
            }
            if (!StringUtils.isBlank(login)) {
                context.openGistActivity(getSherlockActivity(), login,
                        payload.getGist().getId(), 0);
            }
        }
        
        /** IssueCommentEvent */
        else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            if (eventRepo != null) {
                IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
                if (payload.getIssue() != null) {
                    if (payload.getIssue().getPullRequest() != null) {
                        if (payload.getIssue().getPullRequest().getHtmlUrl() != null) {
                            context.openPullRequestActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber());
                        }
                        else {
                            context.openIssueActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber(),
                                    payload.getIssue().getState()); 
                        }
                    }
                    else {
                        context.openIssueActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber(),
                                payload.getIssue().getState()); 
                    }
                }
            }
            else {
                context.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        
        /** PullRequestReviewComment */
        else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload = (PullRequestReviewCommentPayload) event.getPayload();
            context.openCommitInfoActivity(getSherlockActivity(), repoOwner, repoName, 
                    payload.getComment().getCommitId(), 0);
        }
    }
    
    public abstract int getMenuGroupId();
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        if (v.getId() == R.id.list_view) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Event event = (Event) mAdapter.getItem(info.position);
            int groupId = getMenuGroupId();

            //if payload is a base class, return void.  Think that it is an old event which not supported
            //by API v3.
            if (event.getPayload().getClass().getSimpleName().equals("EventPayload")) {
                return;
            }
            
            String eventType = event.getType();
            EventRepository eventRepo = event.getRepo();
            String[] repoNamePart = eventRepo.getName().split("/");
            String repoOwner = null;
            String repoName = null;
            if (repoNamePart.length == 2) {
                repoOwner = repoNamePart[0];
                repoName = repoNamePart[1];
            }

            menu.setHeaderTitle(R.string.go_to);

            /** Common menu */
            menu.add(groupId, MENU_USER, Menu.NONE, getString(R.string.menu_user, event.getActor().getLogin()));
            if (repoOwner != null) {
                menu.add(groupId, MENU_REPO, Menu.NONE, getString(R.string.menu_repo, repoOwner + "/" + repoName));
            }

            /** PushEvent extra menu for commits */
            if (Event.TYPE_PUSH.equals(eventType)) {
                if (repoOwner != null) {
                    PushPayload payload = (PushPayload) event.getPayload();
                    menu.add(groupId, MENU_COMPARE, Menu.NONE,
                            getString(R.string.menu_compare, payload.getHead()));
                    
                    List<Commit> commits = payload.getCommits();
                    for (int i = 0; i < commits.size(); i++) {
                        menu.add(groupId, MENU_PUSH_COMMIT_START + i, Menu.NONE,
                                getString(R.string.menu_commit, commits.get(i).getSha()));
                    }
                }
            }

            /** IssueEvent extra menu for commits */
            else if (Event.TYPE_ISSUES.equals(eventType)) {
                IssuesPayload payload = (IssuesPayload) event.getPayload();
                menu.add(groupId, MENU_ISSUE, Menu.NONE,
                        getString(R.string.menu_issue, payload.getIssue().getNumber()));
            }

            /** FollowEvent */
            else if (Event.TYPE_FOLLOW.equals(eventType)) {
                FollowPayload payload = (FollowPayload) event.getPayload();
                if (payload.getTarget() != null) {
                    menu.add(groupId, MENU_USER, Menu.NONE,
                            getString(R.string.menu_user, payload.getTarget().getLogin()));
                }
            }

            /** CommitCommentEvent */
            else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
                if (repoOwner != null) {
                    CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
                    menu.add(groupId, MENU_COMMENT_COMMIT, Menu.NONE,
                            getString(R.string.menu_commit, payload.getComment().getCommitId().substring(0, 7)));
                }
            }

            /** GistEvent */
            else if (Event.TYPE_GIST.equals(eventType)) {
                GistPayload payload = (GistPayload) event.getPayload();
                menu.add(groupId, MENU_GIST, Menu.NONE,
                        getString(R.string.menu_gist, payload.getGist().getId()));
            }

            /** DownloadEvent */
            else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                menu.add(groupId, MENU_FILE, Menu.NONE,
                        getString(R.string.menu_file, payload.getDownload().getName()));
            }

            /** ForkEvent */
            else if (Event.TYPE_FORK.equals(eventType)) {
                ForkPayload payload = (ForkPayload) event.getPayload();
                Repository forkee = payload.getForkee();
                if (forkee != null) {
                    menu.add(groupId, MENU_FORKED_REPO, Menu.NONE,
                            getString(R.string.menu_fork, forkee.getOwner().getLogin() + "/" + forkee.getName()));
                }
            }

            /** GollumEvent */
            else if (Event.TYPE_GOLLUM.equals(eventType)) {
                menu.add(groupId, MENU_WIKI_IN_BROWSER, Menu.NONE, getString(R.string.menu_wiki));
            }
            
            /** PullRequestEvent */
            else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
                PullRequestPayload payload = (PullRequestPayload) event.getPayload();
                menu.add(groupId, MENU_PULL_REQ, Menu.NONE, getString(R.string.menu_pull, payload.getNumber()));
            }
            
            /** IssueCommentEvent */
            else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
                menu.add(groupId, MENU_OPEN_ISSUES, Menu.NONE, getString(R.string.menu_issues));
            }
        }
    }
    
    public boolean open(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        
        Event event = (Event) mAdapter.getItem(info.position);
        EventRepository eventRepo = event.getRepo();
        String[] repoNamePart = eventRepo.getName().split("/");
        String repoOwner = null;
        String repoName = null;
        if (repoNamePart.length == 2) {
            repoOwner = repoNamePart[0];
            repoName = repoNamePart[1];
        }
        
        Gh4Application app = Gh4Application.get(getActivity());
        int id = item.getItemId();

        /** User item */
        if (id == MENU_USER) {
            app.openUserInfoActivity(getSherlockActivity(), repoOwner, null);
        }
        /** Repo item */
        else if (id == MENU_REPO) {
            app.openRepositoryInfoActivity(getSherlockActivity(), repoOwner, repoName, 0);
        }
        /** Commit item */
        else if (id >= MENU_PUSH_COMMIT_START && id <= MENU_PUSH_COMMIT_END || id == MENU_COMMENT_COMMIT) {
            if (repoOwner != null) {
                String sha = null;
                if (Event.TYPE_PUSH.equals(event.getType())) {
                    int offset = id - MENU_PUSH_COMMIT_START;
                    sha = ((PushPayload) event.getPayload()).getCommits().get(offset).getSha();
                } else if (Event.TYPE_COMMIT_COMMENT.equals(event.getType())) {
                    sha = ((CommitCommentPayload) event.getPayload()).getComment().getCommitId();
                }
                if (sha != null) {
                    app.openCommitInfoActivity(getSherlockActivity(), repoOwner, repoName, sha, 0);
                }
            }
            else {
                app.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        /** Open issues item */
        else if (id == MENU_OPEN_ISSUES) {
            app.openIssueListActivity(getSherlockActivity(), repoOwner, repoName,
                    Constants.Issue.ISSUE_STATE_OPEN);
        }
        /** Issue item */
        else if (id == MENU_ISSUE) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            app.openIssueActivity(getSherlockActivity(), repoOwner, repoName, payload.getIssue().getNumber());
        }
        /** Gist item */
        else if (id == MENU_GIST) {
            GistPayload payload = (GistPayload) event.getPayload();
            app.openGistActivity(getSherlockActivity(), payload.getGist().getUser().getLogin(),
                    payload.getGist().getId(), 0);
        }
        /** Download item */
        else if (id == MENU_FILE) {
            if (repoOwner != null) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                app.openBrowser(getSherlockActivity(), payload.getDownload().getHtmlUrl());
            }
            else {
                app.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        /** Fork item */
        else if (id == MENU_FORKED_REPO) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                app.openRepositoryInfoActivity(getSherlockActivity(), forkee);
            }
            else {
                app.notFoundMessage(getSherlockActivity(), R.plurals.repository);
            }
        }
        /** Wiki item */
        else if (id == MENU_WIKI_IN_BROWSER) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) {//TODO: now just open the first page
                app.openBrowser(getSherlockActivity(), pages.get(0).getHtmlUrl());                
            }
        }
        /** Pull Request item */
        else if (id == MENU_PULL_REQ) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            app.openPullRequestActivity(getSherlockActivity(), repoOwner, repoName, payload.getNumber());
        }
        
        else if (id == MENU_COMPARE) {
            if (repoOwner != null) {
                PushPayload payload = (PushPayload) event.getPayload();
                
                Intent intent = new Intent().setClass(app, CompareActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                intent.putExtra(Constants.Repository.HEAD, payload.getHead());
                intent.putExtra(Constants.Repository.BASE, payload.getBefore());
                startActivity(intent);
            }
        }
        return true;
    }
    
}
