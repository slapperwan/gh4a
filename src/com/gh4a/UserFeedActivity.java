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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.GollumPage;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventRepository;
import org.eclipse.egit.github.core.event.FollowPayload;
import org.eclipse.egit.github.core.event.ForkApplyPayload;
import org.eclipse.egit.github.core.event.ForkPayload;
import org.eclipse.egit.github.core.event.GistPayload;
import org.eclipse.egit.github.core.event.GollumPayload;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.event.IssuesPayload;
import org.eclipse.egit.github.core.event.MemberPayload;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.event.PushPayload;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.adapter.FeedAdapter;
import com.gh4a.holder.BreadCrumbHolder;

/**
 * The User activity.
 */
public abstract class UserFeedActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The feed adapter. */
    protected FeedAdapter mFeedAdapter;

    /** The list view feeds. */
    protected ListView mListViewFeeds;

    /** The action bar title. */
    protected String mActionBarTitle;

    /** The subtitle. */
    protected String mSubtitle;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mActionBarTitle = data.getString(Constants.ACTIONBAR_TITLE);
        mSubtitle = data.getString(Constants.SUBTITLE);

        setBreadCrumb();

        mFeedAdapter = new FeedAdapter(this, new ArrayList<Event>());
        mListViewFeeds = (ListView) findViewById(R.id.list_view);
        mListViewFeeds.setAdapter(mFeedAdapter);
        registerForContextMenu(mListViewFeeds);
        mListViewFeeds.setOnItemClickListener(this);

        new LoadActivityListTask(this).execute();
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[1];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        createBreadcrumb(mSubtitle, breadCrumbHolders);
    }

    /**
     * Gets the feeds.
     * 
     * @return the feeds
     */
    public abstract List<Event> getFeeds() throws IOException;

    /**
     * An asynchronous task that runs on a background thread to load activity
     * list.
     */
    private static class LoadActivityListTask extends AsyncTask<Void, Integer, List<Event>> {

        /** The target. */
        private WeakReference<UserFeedActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load activity list task.
         *
         * @param activity the activity
         */
        public LoadActivityListTask(UserFeedActivity activity) {
            mTarget = new WeakReference<UserFeedActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Event> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getFeeds();
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Event> result) {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog.dismiss();
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                }
            }
        }
    }

    /**
     * Fill data into UI components.
     * 
     * @param feeds the feeds
     */
    protected void fillData(List<Event> feeds) {
        if (feeds != null && feeds.size() > 0) {
            mFeedAdapter.notifyDataSetChanged();
            for (Event feed : feeds) {
                mFeedAdapter.add(feed);
            }
        }
        mFeedAdapter.notifyDataSetChanged();
    }

    /*
     * (non-Javadoc)
     * @see
     * android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
     * .AdapterView, android.view.View, int, long)
     */
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
                        commitInfo[1] = commit.getAuthor().getEmail();
                        commitInfo[2] = commit.getMessage();
                        commitInfo[3] = commit.getAuthor().getName();
                        intent.putExtra("commit" + commit.getSha(), commitInfo);
                    }
                    
                    intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                    startActivity(intent);
                }
                // only 1 commit, then show the commit details
                else {
                    context.openCommitInfoActivity(this, repoOwner, repoName,
                            payload.getCommits().get(0).getSha());
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
                context.openRepositoryInfoActivity(this, repoOwner, repoName);
                //context.openRepositoryInfoActivity(this, feed.getRepository());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** CreateEvent */
        else if (Event.TYPE_CREATE.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(this, repoOwner, repoName);
                //context.openRepositoryInfoActivity(this, feed.getRepository());
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
                        payload.getComment().getCommitId());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** DeleteEvent */
        else if (Event.TYPE_DELETE.equals(eventType)) {
            if (eventRepo != null) {
                context.openRepositoryInfoActivity(this, repoOwner, repoName);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** GistEvent */
//        else if (UserFeed.Type.GIST_EVENT.equals(eventType)) {
//            context.openBrowser(this, feed.getPayload().getUrl());
//        }

        /** DownloadEvent */
        else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            if (eventRepo != null) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
//                String url = "https://github.com/" + repoOwner + "/"
//                        + repoName + "/downloads#download_" + feed.getPayload().getId();
                context.openBrowser(this, payload.getDownload().getUrl());
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
                context.openRepositoryInfoActivity(this, repoOwner, repoName);
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
                context.openRepositoryInfoActivity(this, repoOwner, repoName);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        
        /** MemberEvent */
        else if (Event.TYPE_MEMBER.equals(eventType)) {
            if (eventRepo != null) {
                MemberPayload payload = (MemberPayload) event.getPayload();
                context.openRepositoryInfoActivity(this, repoOwner, repoName);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        
        /** Gist Event **/
        else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            context.openGistActivity(this, payload.getGist().getId());
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
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
     * android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.list_view) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Event event = (Event) mFeedAdapter.getItem(info.position);
            
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
            String repoUrl = eventRepo.getUrl();
            
            menu.setHeaderTitle("Go to");

            /** Common menu */
            menu.add("User " + event.getActor().getLogin());
            if (repoOwner != null) {
                menu.add("Repo " + repoOwner + "/" + repoName);
            }

            /** PushEvent extra menu for commits */
            if (Event.TYPE_PUSH.equals(eventType)) {
                if (repoOwner != null) {
                    PushPayload payload = (PushPayload) event.getPayload();
                    menu.add("Compare " + payload.getHead());
                    
                    List<Commit> commits = payload.getCommits();
                    for (Commit commit : commits) {
                        menu.add("Commit " + commit.getSha());
                    }
                }
            }

            /** IssueEvent extra menu for commits */
            else if (Event.TYPE_ISSUES.equals(eventType)) {
                IssuesPayload payload = (IssuesPayload) event.getPayload();
                menu.add("Issue " + payload.getIssue().getNumber());
            }

            /** FollowEvent */
            else if (Event.TYPE_FOLLOW.equals(eventType)) {
                FollowPayload payload = (FollowPayload) event.getPayload();
                if (payload.getTarget() != null) {
                    menu.add("User " + payload.getTarget().getLogin());
                }
            }

            /** CommitCommentEvent */
            else if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
                if (repoOwner != null) {
                    CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
                    menu.add("Commit " + payload.getComment().getCommitId().substring(0, 7));
                    //menu.add("Comment in browser");
                }
            }

            /** GistEvent */
            else if (Event.TYPE_GIST.equals(eventType)) {
                GistPayload payload = (GistPayload) event.getPayload();
                menu.add(payload.getGist().getId() + " in browser");
            }

            /** DownloadEvent */
            else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                menu.add("File " + payload.getDownload().getName() + " in browser");
            }

            /** ForkEvent */
            else if (Event.TYPE_FORK.equals(eventType)) {
                ForkPayload payload = (ForkPayload) event.getPayload();
                Repository forkee = payload.getForkee();
                if (forkee != null) {
                    menu.add("Forked repo " + forkee.getOwner().getLogin() + "/" + forkee.getName());
                }
            }

            /** GollumEvent */
            else if (Event.TYPE_GOLLUM.equals(eventType)) {
                menu.add("Wiki in browser");
            }
            
            /** PullRequestEvent */
            else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
                PullRequestPayload payload = (PullRequestPayload) event.getPayload();
                menu.add("Pull request " + payload.getNumber());
            }
            
            /** IssueCommentEvent */
            else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
                menu.add("Open issues");
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Event event = (Event) mFeedAdapter.getItem(info.position);
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String[] repoNamePart = eventRepo.getName().split("/");
        String repoOwner = null;
        String repoName = null;
        if (repoNamePart.length == 2) {
            repoOwner = repoNamePart[0];
            repoName = repoNamePart[1];
        }
        String repoUrl = eventRepo.getUrl();
        
        String title = item.getTitle().toString();
        String value = title.split(" ")[1];

        Gh4Application context = getApplicationContext();

        /** User item */
        if (title.startsWith("User")) {
            context
                    .openUserInfoActivity(this, value, event.getActor().getLogin());
        }
        /** Repo item */
        else if (title.startsWith("Repo")) {
            context.openRepositoryInfoActivity(this, repoOwner, repoName);
        }
        /** Commit item */
        else if (title.startsWith("Commit")) {
            if (repoOwner != null) {
                context.openCommitInfoActivity(this, repoOwner, repoName, value);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        /** Issue comment item */
        else if (title.startsWith("Open issues")) {
            context.openIssueListActivity(this, repoOwner, repoName, Constants.Issue.ISSUE_STATE_OPEN);
        }
        /** Issue item */
        else if (title.startsWith("Issue")) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            context.openIssueActivity(this, repoOwner, repoName, payload.getIssue().getNumber());
        }
        /** Commit comment item */
        else if (title.startsWith("Comment in browser")) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            context.openBrowser(this, payload.getComment().getUrl());
        }
        /** Gist item */
        else if (title.startsWith("gist")) {
            GistPayload payload = (GistPayload) event.getPayload();
            context.openBrowser(this, payload.getGist().getUrl());
        }
        /** Download item */
        else if (title.startsWith("File")) {
            if (repoOwner != null) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                context.openBrowser(this, payload.getDownload().getUrl());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        /** Fork item */
        else if (title.startsWith("Forked repo")) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                context.openRepositoryInfoActivity(this, forkee);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        /** Wiki item */
        else if (title.startsWith("Wiki in browser")) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) {//TODO: now just open the first page
                context.openBrowser(this, pages.get(0).getHtmlUrl());                
            }
        }
        /** Pull Request item */
        else if (title.startsWith("Pull request")) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            context.openPullRequestActivity(this, repoOwner, repoName, payload.getNumber());
        }
        
        else if (title.startsWith("Compare")) {
            if (repoOwner != null) {
                PushPayload payload = (PushPayload) event.getPayload();
                
                List<Commit> commits = payload.getCommits();
                Intent intent = new Intent().setClass(context, CompareActivity.class);
                for (Commit commit : commits) {
                    intent.putExtra("sha" + commit.getSha(), commit.getSha());
                }
                
                intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                intent.putExtra(Constants.Repository.REPO_URL, event.getRepo().getUrl());
                startActivity(intent);
            }
        }

        return true;
    }
    
//    private static String getToRepoOwner(Event event) {
//        String url = userFeed.getUrl();
//        if (!StringUtils.isBlank(url)) {
//            String[] urlParts = url.split("/");
//            if (urlParts.length > 3) {
//                return urlParts[3];
//            }
//            else {
//                return null;
//            }
//        }
//        return null;
//    }
//    
//    private static String getToRepoName(UserFeed userFeed) {
//        String url = userFeed.getUrl();
//        if (!StringUtils.isBlank(url)) {
//            String[] urlParts = url.split("/");
//            if (urlParts.length > 3) {
//                return urlParts[4];
//            }
//            else {
//                return null;
//            }
//        }
//        return null;
//    }
}