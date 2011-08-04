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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.FeedAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.ObjectPayloadPullRequest;
import com.github.api.v2.schema.Payload;
import com.github.api.v2.schema.Repository;
import com.github.api.v2.schema.UserFeed;
import com.github.api.v2.services.GitHubException;
import com.google.ads.AdRequest;
import com.google.ads.AdView;

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
        AdView adView = (AdView)this.findViewById(R.id.adView);
        AdRequest request = new AdRequest();
//        request.addTestDevice(AdRequest.TEST_EMULATOR);
//        request.addTestDevice("DA870570FFC173C3F71D204CA2F77E67");
        adView.loadAd(request);

        
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mActionBarTitle = data.getString(Constants.ACTIONBAR_TITLE);
        mSubtitle = data.getString(Constants.SUBTITLE);

        setBreadCrumb();

        mFeedAdapter = new FeedAdapter(this, new ArrayList<UserFeed>());
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
    public abstract List<UserFeed> getFeeds();

    /**
     * An asynchronous task that runs on a background thread to load activity
     * list.
     */
    private static class LoadActivityListTask extends AsyncTask<Void, Integer, List<UserFeed>> {

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
        protected List<UserFeed> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getFeeds();
                }
                catch (GitHubException e) {
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
        protected void onPostExecute(List<UserFeed> result) {
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
    protected void fillData(List<UserFeed> feeds) {
        if (feeds != null && feeds.size() > 0) {
            mFeedAdapter.notifyDataSetChanged();
            for (UserFeed feed : feeds) {
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
        UserFeed feed = (UserFeed) adapterView.getAdapter().getItem(position);

        UserFeed.Type eventType = feed.getType();
        Gh4Application context = getApplicationContext();
        /** PushEvent */
        if (UserFeed.Type.PUSH_EVENT.equals(eventType)) {
            if (feed.getRepository() != null) {
                List<String[]> shas = feed.getPayload().getShas();
                // if commit > 1, then show compare activity
                if (shas != null && shas.size() > 1) {
                    Intent intent = new Intent().setClass(context, CompareActivity.class);
                    for (String[] sha : shas) {
                        intent.putExtra("sha" + sha[0], sha);
                    }
                    
                    intent.putExtra(Constants.Repository.REPO_OWNER, feed.getRepository().getOwner());
                    intent.putExtra(Constants.Repository.REPO_NAME, feed.getRepository().getName());
                    intent.putExtra(Constants.Repository.REPO_URL, feed.getUrl());
                    startActivity(intent);
                }
                // only 1 commit, then show the commit details
                else {
                        context.openCommitInfoActivity(this, feed.getRepository().getOwner(), feed
                                .getRepository().getName(), feed.getPayload().getShas().get(0)[0]);
                }
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** IssueEvent */
        else if (UserFeed.Type.ISSUES_EVENT.equals(eventType)) {
            if (feed.getRepository() != null) {
                context.openIssueActivity(this, feed.getRepository().getOwner(), feed.getRepository()
                        .getName(), feed.getPayload().getNumber());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** WatchEvent */
        else if (UserFeed.Type.WATCH_EVENT.equals(eventType)) {
            if (feed.getRepository() != null) {
                context.openRepositoryInfoActivity(this, feed.getRepository());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** CreateEvent */
        else if (UserFeed.Type.CREATE_EVENT.equals(eventType)) {
            if (feed.getRepository() != null) {
                context.openRepositoryInfoActivity(this, feed.getRepository());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** PullRequestEvent */
        else if (UserFeed.Type.PULL_REQUEST_EVENT.equals(eventType)) {
            if (feed.getRepository() != null) {
                Payload payload = feed.getPayload();
                int pullRequestNumber = payload.getNumber();
                if (payload.getPullRequest() instanceof ObjectPayloadPullRequest) {
                    pullRequestNumber = ((ObjectPayloadPullRequest) payload.getPullRequest()).getNumber();
                }
                context.openPullRequestActivity(this, feed.getRepository().getOwner(), feed
                        .getRepository().getName(), pullRequestNumber);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** FollowEvent */
        else if (UserFeed.Type.FOLLOW_EVENT.equals(eventType)) {
            Payload payload = feed.getPayload();
            if (payload.getTarget() != null) {
                context.openUserInfoActivity(this, payload.getTarget().getLogin(), null);
            }
        }

        /** CommitCommentEvent */
        else if (UserFeed.Type.COMMIT_COMMENT_EVENT.equals(eventType)) {
            Repository repository = feed.getRepository();
            if (repository != null) {
                if (!StringUtils.isBlank(feed.getUrl())) {
                    context.openBrowser(this, feed.getUrl());
                }
                else {
                    context.notFoundMessage(this, "URL");
                }
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** DeleteEvent */
        else if (UserFeed.Type.DELETE_EVENT.equals(eventType)) {
            if (feed.getRepository() != null) {
                context.openRepositoryInfoActivity(this, feed.getRepository());
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
        else if (UserFeed.Type.DOWNLOAD_EVENT.equals(eventType)) {
            Repository repository = feed.getRepository();
            if (repository != null) {
                String url = "https://github.com/" + repository.getOwner() + "/"
                        + repository.getName() + "/downloads#download_" + feed.getPayload().getId();
                context.openBrowser(this, url);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** ForkEvent */
        else if (UserFeed.Type.FORK_EVENT.equals(eventType)) {
            if (getToRepoName(feed) != null
                    && getToRepoOwner(feed) != null) {
                context.openRepositoryInfoActivity(this, getToRepoOwner(feed),
                        getToRepoName(feed));
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** ForkEvent */
        else if (UserFeed.Type.FORK_APPLY_EVENT.equals(eventType)) {
            Repository repository = feed.getRepository();
            if (repository != null) {
                context.openRepositoryInfoActivity(this, repository);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }

        /** GollumEvent */
        else if (UserFeed.Type.GOLLUM_EVENT.equals(eventType)) {
            Intent intent = new Intent().setClass(this, WikiListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, feed.getRepository().getOwner());
            intent.putExtra(Constants.Repository.REPO_NAME, feed.getRepository().getName());
            startActivity(intent);
        }

        /** PublicEvent */
        else if (UserFeed.Type.PUBLIC_EVENT.equals(eventType)) {
            Repository repository = feed.getRepository();
            if (repository != null) {
                context.openRepositoryInfoActivity(this, repository);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        
        /** MemberEvent */
        else if (UserFeed.Type.MEMBER_EVENT.equals(eventType)) {
            Repository repository = feed.getRepository();
            if (repository != null) {
                context.openRepositoryInfoActivity(this, repository);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        
        else if (UserFeed.Type.GIST_EVENT.equals(eventType)) {
            Payload payload = feed.getPayload();
            String[] gistPart = payload.getName().split(":");
            if (gistPart.length > 1) {
                String gistId = gistPart[1].trim();
                context.openGistActivity(this, gistId);
            }
        }
        
        /** IssueCommentEvent */
        else if (UserFeed.Type.ISSUE_COMMENT_EVENT.equals(eventType)) {
            //https://github.com/slapperwan/gh4a/issues/32#issuecomment-1531102
            String url = feed.getUrl();
            int idx1 = url.indexOf("/issues/");
            int idx2 = url.indexOf("#issuecomment");
            if (idx2 == -1) {//sometime it return comment only
                idx2 = url.indexOf("#comment");
            }
            String issueNumber = "-1";
            if (idx2 != -1) {
                issueNumber = url.substring(idx1 + 8, idx2);
            }
            if (feed.getRepository() != null) {
                if (!"-1".equals(issueNumber)) {
                    context.openIssueActivity(this, feed.getRepository().getOwner(), feed.getRepository().getName(), Integer.parseInt(issueNumber));
                }
                else {
                    context.openIssueListActivity(this, feed.getRepository().getOwner(), feed.getRepository()
                            .getName(), Constants.Issue.ISSUE_STATE_OPEN);
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
            UserFeed feed = (UserFeed) mFeedAdapter.getItem(info.position);
            UserFeed.Type eventType = feed.getType();
            menu.setHeaderTitle("Go to");

            /** Common menu */
            menu.add("User " + feed.getActor());
            if (feed.getRepository() != null) {
                menu.add("Repo " + feed.getRepository().getOwner() + "/"
                        + feed.getRepository().getName());
            }

            /** PushEvent extra menu for commits */
            if (UserFeed.Type.PUSH_EVENT.equals(eventType)) {
                if (feed.getRepository() != null) {
                    int index = feed.getUrl().lastIndexOf("/");
                    if (index != -1) {
                        menu.add("Compare " + feed.getUrl().substring(index + 1, feed.getUrl().length()));
                    }
                    else {
                        menu.add("Compare");
                    }
                    
                    List<String[]> shas = feed.getPayload().getShas();
                    for (String[] sha : shas) {
                        menu.add("Commit " + sha[0].substring(0, 7) + " " + sha[2]);
                    }
                }
            }

            /** IssueEvent extra menu for commits */
            else if (UserFeed.Type.ISSUES_EVENT.equals(eventType)) {
                menu.add("Issue " + feed.getPayload().getNumber());
            }

            /** FollowEvent */
            else if (UserFeed.Type.FOLLOW_EVENT.equals(eventType)) {
                Payload payload = feed.getPayload();
                if (payload.getTarget() != null) {
                    menu.add("User " + payload.getTarget().getLogin());
                }
            }

            /** CommitCommentEvent */
            else if (UserFeed.Type.COMMIT_COMMENT_EVENT.equals(eventType)) {
                if (feed.getRepository() != null) {
                    menu.add("Commit " + feed.getPayload().getCommit().substring(0, 7));
                    menu.add("Comment in browser");
                }
            }

            /** GistEvent */
            else if (UserFeed.Type.GIST_EVENT.equals(eventType)) {
                menu.add(feed.getPayload().getName() + " in browser");
            }

            /** DownloadEvent */
            else if (UserFeed.Type.DOWNLOAD_EVENT.equals(eventType)) {
                String filename = feed.getPayload().getUrl();
                int index = filename.lastIndexOf("/");
                if (index != -1) {
                    filename = filename.substring(index + 1, filename.length());
                }
                else {
                    filename = "";
                }
                menu.add("File " + filename + " in browser");
            }

            /** ForkEvent */
            else if (UserFeed.Type.FORK_EVENT.equals(eventType)) {
                if (getToRepoName(feed) != null
                        && getToRepoOwner(feed) != null) {
                    menu.add("Forked repo " + getToRepoOwner(feed) + "/" + getToRepoName(feed));
                }
            }

            /** GollumEvent */
            else if (UserFeed.Type.GOLLUM_EVENT.equals(eventType)) {
                menu.add("Wiki in browser");
            }
            
            /** PullRequestEvent */
            else if (UserFeed.Type.PULL_REQUEST_EVENT.equals(eventType)) {
                Payload payload = feed.getPayload();
                int pullRequestNumber = payload.getNumber();
                if (payload.getPullRequest() instanceof ObjectPayloadPullRequest) {
                    pullRequestNumber = ((ObjectPayloadPullRequest) payload.getPullRequest()).getNumber();
                }
                menu.add("Pull request " + pullRequestNumber);
            }
            
            /** IssueCommentEvent */
            else if (UserFeed.Type.ISSUE_COMMENT_EVENT.equals(eventType)) {
                menu.add("Open issues");//TODO: Open issue activity instead issue listing (waiting for github response)
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
        UserFeed feed = (UserFeed) mFeedAdapter.getItem(info.position);

        String title = item.getTitle().toString();
        String value = title.split(" ")[1];

        Gh4Application context = getApplicationContext();

        /** User item */
        if (title.startsWith("User")) {
            context
                    .openUserInfoActivity(this, value, feed.getActorAttributes()
                            .getName());
        }
        /** Repo item */
        else if (title.startsWith("Repo")) {
            context.openRepositoryInfoActivity(this, feed.getRepository());
        }
        /** Commit item */
        else if (title.startsWith("Commit")) {
            if (feed.getRepository() != null) {
                context.openCommitInfoActivity(this, feed.getRepository().getOwner(), feed
                        .getRepository().getName(), value);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        /** Issue comment item */
        else if (title.startsWith("Open issues")) {
            context.openIssueListActivity(this, feed.getRepository().getOwner(), feed.getRepository()
                    .getName(), Constants.Issue.ISSUE_STATE_OPEN);
        }
        /** Issue item */
        else if (title.startsWith("Issue")) {
            context.openIssueActivity(this, feed.getRepository().getOwner(), feed.getRepository()
                    .getName(), feed.getPayload().getNumber());
        }
        /** Commit comment item */
        else if (title.startsWith("Comment in browser")) {
            context.openBrowser(this, feed.getUrl());
        }
        /** Gist item */
        else if (title.startsWith("gist")) {
            context.openBrowser(this, feed.getPayload().getUrl());
        }
        /** Download item */
        else if (title.startsWith("File")) {
            Repository repository = feed.getRepository();
            if (repository != null) {
                String url = "https://github.com/" + repository.getOwner() + "/"
                        + repository.getName() + "/downloads#download_" + feed.getPayload().getId();
                context.openBrowser(this, url);
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        /** Fork item */
        else if (title.startsWith("Forked repo")) {
            if (getToRepoName(feed) != null
                    && getToRepoOwner(feed) != null) {
                context.openRepositoryInfoActivity(this, feed.getActor(), feed.getRepository()
                        .getName());
            }
            else {
                context.notFoundMessage(this, R.plurals.repository);
            }
        }
        /** Wiki item */
        else if (title.startsWith("Wiki in browser")) {
            context.openBrowser(this, feed.getUrl());
        }
        /** Pull Request item */
        else if (title.startsWith("Pull request")) {
            Payload payload = feed.getPayload();
            int pullRequestNumber = payload.getNumber();
            if (payload.getPullRequest() instanceof ObjectPayloadPullRequest) {
                pullRequestNumber = ((ObjectPayloadPullRequest) payload.getPullRequest()).getNumber();
            }
            
            context.openPullRequestActivity(this, feed.getRepository().getOwner(), feed
                    .getRepository().getName(), pullRequestNumber);
        }
        
        else if (title.startsWith("Compare")) {
            if (feed.getRepository() != null) {
                List<String[]> shas = feed.getPayload().getShas();
                Intent intent = new Intent().setClass(context, CompareActivity.class);
                for (String[] sha : shas) {
                    intent.putExtra("sha" + sha[0], sha);
                }
                
                intent.putExtra(Constants.Repository.REPO_OWNER, feed.getRepository().getOwner());
                intent.putExtra(Constants.Repository.REPO_NAME, feed.getRepository().getName());
                intent.putExtra(Constants.Repository.REPO_URL, feed.getUrl());
                startActivity(intent);
            }
        }

        return true;
    }
    
    private static String getToRepoOwner(UserFeed userFeed) {
        String url = userFeed.getUrl();
        if (!StringUtils.isBlank(url)) {
            String[] urlParts = url.split("/");
            if (urlParts.length > 3) {
                return urlParts[3];
            }
            else {
                return null;
            }
        }
        return null;
    }
    
    private static String getToRepoName(UserFeed userFeed) {
        String url = userFeed.getUrl();
        if (!StringUtils.isBlank(url)) {
            String[] urlParts = url.split("/");
            if (urlParts.length > 3) {
                return urlParts[4];
            }
            else {
                return null;
            }
        }
        return null;
    }
}