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
import java.util.Iterator;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.gh4a.adapter.BranchTagAdapter;
import com.gh4a.holder.BranchTag;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.RepositoryService;

/**
 * The TagList activity.
 */
public class TagListActivity extends BaseActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

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

        setContentView(R.layout.branch_tag_list);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);

        setBreadCrumb();

        new LoadBranchTagListTask(this).execute();
    }

    /**
     * Sets the bread crumb.
     */
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

        createBreadcrumb("Tags", breadCrumbHolders);
    }

    /**
     * An asynchronous task that runs on a background thread
     * to load branch tag list.
     */
    private static class LoadBranchTagListTask extends
            AsyncTask<Void, Integer, HashMap<String, HashMap<String, String>>> {

        /** The target. */
        private WeakReference<TagListActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadBranchTagListTask(TagListActivity activity) {
            mTarget = new WeakReference<TagListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected HashMap<String, HashMap<String, String>> doInBackground(Void... params) {
            try {
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                RepositoryService service = factory.createRepositoryService();
                HashMap<String, HashMap<String, String>> branchTagMap = new HashMap<String, HashMap<String, String>>();
                TagListActivity activity = mTarget.get();

                HashMap<String, String> map = (HashMap<String, String>) service.getTags(
                        activity.mUserLogin, activity.mRepoName);
                branchTagMap.put("tags", map);

                return branchTagMap;
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
        protected void onPostExecute(HashMap<String, HashMap<String, String>> result) {
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
     * @param map the map
     */
    protected void fillData(HashMap<String, HashMap<String, String>> map) {
        ListView branchListView = (ListView) findViewById(R.id.list_view);

        ArrayList<BranchTag> tagList = new ArrayList<BranchTag>();
        HashMap<String, String> tagMap = map.get("tags");
        if (tagMap.size() == 0) {
            getApplicationContext().notFoundMessage(this, "Tags");
            return;
        }
        Iterator<String> it = tagMap.keySet().iterator();

        BranchTag branchTag;
        while (it.hasNext()) {
            String name = it.next();
            branchTag = new BranchTag();
            branchTag.setName(name);
            branchTag.setSha(tagMap.get(name));

            tagList.add(branchTag);
        }

        BranchTagAdapter branchAdapter = new BranchTagAdapter(this, tagList, mUserLogin, mRepoName);
        branchListView.setAdapter(branchAdapter);
    }

}
