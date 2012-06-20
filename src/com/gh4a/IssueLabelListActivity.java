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

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.LabelService;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.adapter.SimpleStringAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;

/**
 * The IssueLabelList activity.
 */
public class IssueLabelListActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    private String mUserLogin;

    /** The repo name. */
    private String mRepoName;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    /** The list view. */
    protected ListView mListView;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);

        setBreadCrumb();
        
        new LoadIssueLabelsTask(this).execute();
    }
    
    /**
     * Sets the bread crumb.
     */
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
    
    /**
     * An asynchronous task that runs on a background thread
     * to load issue labels.
     */
    private static class LoadIssueLabelsTask extends AsyncTask<Void, Integer, List<Label>> {

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
        protected List<Label> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    LabelService labelService = new LabelService(client);
                    return labelService.getLabels(mTarget.get().mUserLogin,
                            mTarget.get().mRepoName);
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
        protected void onPostExecute(List<Label> result) {
            if (mTarget.get() != null) {
                IssueLabelListActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillData(result);
                }
            }
        }
    }
    
    /**
     * Fill data into UI components.
     *
     * @param result the result
     */
    private void fillData(List<Label> result) {
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setOnItemClickListener(this);
        SimpleStringAdapter adapter = new SimpleStringAdapter(this, new ArrayList<String>());
        mListView.setAdapter(adapter);
        registerForContextMenu(mListView);
        
        if (result != null && result.size() > 0) {
            for (Label label : result) {
                adapter.add(label.getName());
            }
        }
        else {
            getApplicationContext().notFoundMessage(this, "Labels");
        }
        adapter.notifyDataSetChanged();
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
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
    
    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAuthorized()) {
            menu.clear();
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.labels_menu, menu);
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see com.gh4a.BaseActivity#setMenuOptionItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_label:
                if (isAuthorized()) {
                    showCreateLabelForm();
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            default:
                return true;
        }
    }
    
    /**
     * Show create label form.
     */
    private void showCreateLabelForm() {
        final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.issue_create_label);
        dialog.setTitle("Create Label");
        dialog.setCancelable(true);
        
        final TextView tvLabel = (TextView) dialog.findViewById(R.id.et_title);
        Button btnCreate = (Button) dialog.findViewById(R.id.btn_create);
        btnCreate.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                String label = tvLabel.getText().toString();
                if (!StringUtils.isBlank(label)) {
                    dialog.dismiss();
                    new AddIssueLabelsTask(IssueLabelListActivity.this).execute(label);
                }
                else {
                    showMessage(getResources().getString(R.string.issue_error_label), false);
                }
            }
        });
        
        dialog.show();
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (isAuthorized()) {
            if (v.getId() == R.id.list_view) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                String label = (String) mListView.getItemAtPosition(info.position);
                
                menu.setHeaderTitle(label);
                menu.add(0, Menu.FIRST + 1, 0, "Delete");
                menu.add(0, Menu.FIRST + 2, 0, "View Issues");
            }
        }
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case Menu.FIRST + 1:
            //try {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure?")
                       .setCancelable(false)
                       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.dismiss();
                               new DeleteIssueLabelsTask(IssueLabelListActivity.this).execute(info.position);
                           }
                       })
                       .setNegativeButton("No", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                           }
                       });
                AlertDialog alert = builder.create();
                alert.show();
//            }
//            catch (IOException e) {
//                Log.e(Constants.LOG_TAG, e.getMessage(), e);
//                showMessage(getResources().getString(R.string.issue_error_delete_label), false);
//            }
            break;
        case Menu.FIRST + 2:
            String label = (String) mListView.getItemAtPosition(info.position);
            Intent intent = new Intent().setClass(this, IssueListByLabelActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Issue.ISSUE_LABEL, label);
            startActivity(intent);
            break;
        default:
            break;
        }
        
        return true;
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to delete issue labels.
     */
    private static class DeleteIssueLabelsTask extends AsyncTask<Integer, Void, Void> {

        /** The target. */
        private WeakReference<IssueLabelListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         */
        public DeleteIssueLabelsTask(IssueLabelListActivity activity) {
            mTarget = new WeakReference<IssueLabelListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Integer... params) {
            if (mTarget.get() != null) {
                try {
                    IssueLabelListActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    LabelService labelService = new LabelService(client);
                    
                    String label = (String) activity.mListView.getItemAtPosition(params[0]);
                    label = StringUtils.encodeUrl(label);
                    
                    labelService.deleteLabel(activity.mUserLogin, activity.mRepoName, label);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueLabelListActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    new LoadIssueLabelsTask(activity).execute();
                }
            }
        }
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to add issue labels.
     */
    private static class AddIssueLabelsTask extends AsyncTask<String, Void, Void> {

        /** The target. */
        private WeakReference<IssueLabelListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         */
        public AddIssueLabelsTask(IssueLabelListActivity activity) {
            mTarget = new WeakReference<IssueLabelListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    IssueLabelListActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    LabelService labelService = new LabelService(client);
                    
                    String labelName = params[0];
                    //labelName = StringUtils.encodeUrl(labelName);
                    
                    Label label = new Label();
                    label.setName(labelName);
                    
                    labelService.createLabel(activity.mUserLogin, activity.mRepoName, label);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueLabelListActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_create_label), false);
                }
                else {
                    new LoadIssueLabelsTask(activity).execute();
                }
            }
        }
    }
}