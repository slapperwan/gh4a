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
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.adapter.FileAdapter;

public class FileManagerActivity extends BaseActivity implements OnItemClickListener {

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();

        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mObjectSha = getIntent().getExtras().getString(Constants.Object.OBJECT_SHA);

        new LoadTreeListTask(this).execute();
    }

    private static class LoadTreeListTask extends AsyncTask<Void, Integer, Tree> {

        private WeakReference<FileManagerActivity> mTarget;
        private boolean mException;

        public LoadTreeListTask(FileManagerActivity activity) {
            mTarget = new WeakReference<FileManagerActivity>(activity);
        }

        @Override
        protected Tree doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    FileManagerActivity activity = mTarget.get();
                    
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    
                    if (activity.mObjectSha == null) {
                        RepositoryService repoService = new RepositoryService(client);
                        Repository repo = repoService.getRepository(activity.mRepoOwner, 
                                activity.mRepoName);
                        String masterBranch = repo.getMasterBranch();
                        
                        List<RepositoryBranch> branches = repoService.getBranches(
                                new RepositoryId(activity.mRepoOwner, 
                                        activity.mRepoName));
                        
                        for (RepositoryBranch repositoryBranch : branches) {
                            if (repositoryBranch.getName().equals(masterBranch)) {
                                activity.mObjectSha = repositoryBranch.getCommit().getSha();
                                break;
                            }
                        }
                    }
                    
                    DataService dataService = new DataService(client);
                    return dataService.getTree(new RepositoryId(activity.mRepoOwner, activity.mRepoName),
                            activity.mObjectSha);
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

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Tree result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                }
            }
        }
    }

    protected void fillData(Tree tree) {
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);

        FileAdapter fileAdapter = new FileAdapter(this, tree.getTree());
        listView.setAdapter(fileAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        FileAdapter adapter = (FileAdapter) adapterView.getAdapter();
        TreeEntry tree = (TreeEntry) adapter.getItem(position);
        
        if ("tree".equals(tree.getType())) {
            Intent intent = new Intent().setClass(this, FileManagerActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Object.OBJECT_SHA, tree.getSha());
            startActivity(intent);
        }
        else {
            Intent intent = new Intent().setClass(this, FileViewerActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Repository.REPO_BRANCH, "master");
            intent.putExtra(Constants.Object.OBJECT_SHA, tree.getSha());
            intent.putExtra(Constants.Object.NAME, tree.getPath());
            startActivity(intent);
        }
    }
}
