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

import com.gh4a.adapter.FileAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.Tree;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.ObjectService;

/**
 * The FileManager activity.
 */
public class FileManagerActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The object sha. */
    protected String mObjectSha;

    /** The tree sha. */
    private String mTreeSha;

    /** The branch name. */
    private String mBranchName;

    /** The path. */
    protected String mPath;

    /** The from btn id. */
    protected int mFromBtnId;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

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

        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mObjectSha = getIntent().getExtras().getString(Constants.Object.OBJECT_SHA);
        mTreeSha = getIntent().getExtras().getString(Constants.Object.TREE_SHA);
        mBranchName = getIntent().getExtras().getString(Constants.Repository.REPO_BRANCH);
        mPath = getIntent().getExtras().getString(Constants.Object.PATH);
        mFromBtnId = getIntent().getExtras().getInt(Constants.VIEW_ID);

        setBreadCrumb();

        new LoadTreeListTask(this).execute();
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders;
        breadCrumbHolders = new BreadCrumbHolder[4];

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

        // branches/tags
        b = new BreadCrumbHolder();
        if (R.id.btn_tags == mFromBtnId) {
            b.setLabel("tag");
            b.setTag(Constants.Object.TAGS);
        }
        else {
            b.setTag(Constants.Object.BRANCHES);
            b.setLabel("branch");
        }

        b.setData(data);
        breadCrumbHolders[2] = b;

        // branch name
        b = new BreadCrumbHolder();
        b.setLabel(mBranchName);
        b.setTag(Constants.Repository.REPO_BRANCH);
        data.put(Constants.Object.TREE_SHA, mTreeSha);
        data.put(Constants.Repository.REPO_BRANCH, mBranchName);
        data.put(Constants.Object.PATH, mPath);
        data.put(Constants.VIEW_ID, String.valueOf(mFromBtnId));
        b.setData(data);
        breadCrumbHolders[3] = b;

        if (!mPath.equals("Tree")) {
            mPath = mPath.replaceFirst("Tree/", "");
        }
        createBreadcrumb(mPath, breadCrumbHolders);
    }

    /**
     * An asynchronous task that runs on a background thread to load tree list.
     */
    private static class LoadTreeListTask extends AsyncTask<Void, Integer, List<Tree>> {

        /** The target. */
        private WeakReference<FileManagerActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadTreeListTask(FileManagerActivity activity) {
            mTarget = new WeakReference<FileManagerActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Tree> doInBackground(Void... params) {
            try {
                FileManagerActivity activity = mTarget.get();
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                ObjectService objectService = factory.createObjectService();
                return objectService.getTree(activity.mUserLogin, activity.mRepoName,
                        activity.mObjectSha);
            }
            catch (GitHubException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                mException = true;
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Tree> result) {
            if (mException) {
                mTarget.get().showError();
            }
            else {
                mTarget.get().fillData(result);
                mTarget.get().mLoadingDialog.dismiss();
            }
        }
    }

    /**
     * Fill data into UI components.
     * 
     * @param trees the trees
     */
    protected void fillData(List<Tree> trees) {
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);

        FileAdapter fileAdapter = new FileAdapter(this, trees);
        listView.setAdapter(fileAdapter);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        FileAdapter adapter = (FileAdapter) adapterView.getAdapter();
        Tree tree = (Tree) adapter.getItem(position);
        if (Tree.Type.TREE.equals(tree.getType())) {
            Intent intent = new Intent().setClass(this, FileManagerActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Object.OBJECT_SHA, tree.getSha());
            intent.putExtra(Constants.Object.TREE_SHA, mTreeSha);
            intent.putExtra(Constants.Repository.REPO_BRANCH, mBranchName);
            intent.putExtra(Constants.Object.PATH, mPath + "/" + tree.getName());
            intent.putExtra(Constants.VIEW_ID, mFromBtnId);
            startActivity(intent);
        }
        else {
            Intent intent = new Intent().setClass(this, FileViewerActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Repository.REPO_BRANCH, mBranchName);
            intent.putExtra(Constants.Object.OBJECT_SHA, tree.getSha());
            intent.putExtra(Constants.Object.TREE_SHA, mTreeSha);
            intent.putExtra(Constants.Object.NAME, tree.getName());
            intent.putExtra(Constants.Object.MIME_TYPE, tree.getMimeType());
            intent.putExtra(Constants.Object.PATH, mPath + "/" + tree.getName());
            intent.putExtra(Constants.VIEW_ID, mFromBtnId);
            startActivity(intent);
        }
    }
}
