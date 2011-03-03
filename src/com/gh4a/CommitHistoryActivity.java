package com.gh4a;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.CommitAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.Commit;
import com.github.api.v2.services.CommitService;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;

public class CommitHistoryActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;
    
    protected String mFilePath;
    
    protected String mObjectSha;
    
    /** The loading. */
    protected LoadingDialog mLoadingDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mFilePath = getIntent().getExtras().getString(Constants.Object.PATH);
        mObjectSha = getIntent().getExtras().getString(Constants.Object.OBJECT_SHA);
        
        setBreadCrumb();
        
        new LoadCommitListTask(this).execute();
    }
    
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[2];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);
        data.put(Constants.Repository.REPO_NAME, mRepoName);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        // Repo
        b = new BreadCrumbHolder();
        b.setLabel(mRepoName);
        b.setTag(Constants.Repository.REPO_NAME);
        b.setData(data);
        breadCrumbHolders[1] = b;

        createBreadcrumb("History - " + mFilePath, breadCrumbHolders);
    }
    
    private static class LoadCommitListTask extends AsyncTask<String, Integer, List<Commit>> {

        /** The target. */
        private WeakReference<CommitHistoryActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /**
         * Instantiates a new load commit list task.
         *
         * @param activity the activity
         */
        public LoadCommitListTask(CommitHistoryActivity activity) {
            mTarget = new WeakReference<CommitHistoryActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Commit> doInBackground(String... params) {
            if (mTarget.get() != null) {
                CommitHistoryActivity activity = mTarget.get();
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                CommitService commitService = factory.createCommitService();
                try {
                    List<Commit> commits = commitService.getCommits(activity.mUserLogin,
                            activity.mRepoName,
                            activity.mObjectSha,
                            activity.mFilePath);
                    return commits;
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
        protected void onPostExecute(List<Commit> result) {
            if (mTarget.get() != null) {
                CommitHistoryActivity activity = mTarget.get();
    
                if (activity.mLoadingDialog != null && activity.mLoadingDialog.isShowing()) {
                    activity.mLoadingDialog.dismiss();
                }
    
                if (mException) {
                    activity.showError();
                }
                else {
                    if (result != null) {
                        activity.fillData(result);
                    }
                }
            }
        }
    }
    
    protected void fillData(List<Commit> commits) {
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);
        
        CommitAdapter commitAdapter = new CommitAdapter(this, new ArrayList<Commit>());
        listView.setAdapter(commitAdapter);
        
        if (commits != null && commits.size() > 0) {
            for (Commit commit : commits) {
                commitAdapter.add(commit);
            }
        }
        commitAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Commit commit = (Commit) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(CommitHistoryActivity.this, CommitActivity.class);
        String[] urlPart = commit.getUrl().split("/");

        intent.putExtra(Constants.Repository.REPO_OWNER, urlPart[1]);
        intent.putExtra(Constants.Repository.REPO_NAME, urlPart[2]);
        intent.putExtra(Constants.Object.OBJECT_SHA, urlPart[4]);
        intent.putExtra(Constants.Object.TREE_SHA, commit.getTree());

        startActivity(intent);
    }
}
