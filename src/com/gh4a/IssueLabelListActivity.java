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

import com.gh4a.adapter.SimpleStringAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.IssueService;

public class IssueLabelListActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    private String mUserLogin;

    /** The repo name. */
    private String mRepoName;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);

        setBreadCrumb();
        
        new LoadIssueLabelsTask(this).execute();
    }
    
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[3];

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

        // Issues
        b = new BreadCrumbHolder();
        b.setLabel("Issues");
        b.setTag(Constants.Issue.ISSUES);
        b.setData(data);
        breadCrumbHolders[2] = b;
        
        createBreadcrumb("Labels", breadCrumbHolders);
    }
    
    private static class LoadIssueLabelsTask extends AsyncTask<Void, Integer, List<String>> {

        /** The target. */
        private WeakReference<IssueLabelListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load issue task.
         *
         * @param activity the activity
         */
        public LoadIssueLabelsTask(IssueLabelListActivity activity) {
            mTarget = new WeakReference<IssueLabelListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    IssueService issueService = factory.createIssueService();
                    return issueService.getIssueLabels(mTarget.get().mUserLogin,
                            mTarget.get().mRepoName);
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
        protected void onPostExecute(List<String> result) {
            if (mTarget.get() != null) {
                IssueLabelListActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    if (result != null && !result.isEmpty()) {
                        activity.fillData(result);
                    }
                    else {
                        activity.getApplicationContext().notFoundMessage(activity, "Labels");
                    }
                }
            }
        }
    }
    
    private void fillData(List<String> result) {
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);
        SimpleStringAdapter adapter = new SimpleStringAdapter(this, new ArrayList<String>());
        listView.setAdapter(adapter);
        
        if (result != null && result.size() > 0) {
            for (String label : result) {
                adapter.add(label);
            }
            adapter.notifyDataSetChanged();
        }
        else {
            getApplicationContext().notFoundMessage(this, "Labels");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SimpleStringAdapter adapter = (SimpleStringAdapter) adapterView.getAdapter();
        String label = (String) adapter.getItem(position);
        
        Intent intent = new Intent().setClass(this, IssueListByLabelActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Issue.ISSUE_LABEL, label);
        startActivity(intent);
    }
    
}