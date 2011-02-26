package com.gh4a;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.SimpleStringAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.IssueService;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;

public class IssueLabelListActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    private String mUserLogin;

    /** The repo name. */
    private String mRepoName;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    protected ListView mListView;
    
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
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setOnItemClickListener(this);
        SimpleStringAdapter adapter = new SimpleStringAdapter(this, new ArrayList<String>());
        mListView.setAdapter(adapter);
        registerForContextMenu(mListView);
        
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
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu.size() == 1) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.labels_menu, menu);
            
        }
        return true;
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_label:
                if (isAuthenticated()) {
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
                    try {
                        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                        IssueService issueService = factory.createIssueService();
                        Authentication auth = new LoginPasswordAuthentication(getAuthUsername(), getAuthPassword());
                        issueService.setAuthentication(auth);
                        label = label.replaceAll(" ", "%20");
                        issueService.addLabelWithoutIssue(mUserLogin, mRepoName, label);
                        new LoadIssueLabelsTask(IssueLabelListActivity.this).execute();
                    }
                    catch (GitHubException e) {
                        Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        showMessage(getResources().getString(R.string.issue_error_create_label), false);
                    }
                    finally {
                        dialog.dismiss();
                    }
                }
                else {
                    showMessage(getResources().getString(R.string.issue_error_label), false);
                }
            }
        });
        
        dialog.show();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (isAuthenticated()) {
            if (v.getId() == R.id.list_view) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                String label = (String) mListView.getItemAtPosition(info.position);
                
                menu.setHeaderTitle(label);
                menu.add(0, Menu.FIRST + 1, 0, "Delete");
                menu.add(0, Menu.FIRST + 2, 0, "View Issues");
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case Menu.FIRST + 1:
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure?")
                       .setCancelable(false)
                       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                               IssueService issueService = factory.createIssueService();
                               Authentication auth = new LoginPasswordAuthentication(getAuthUsername(), getAuthPassword());
                               issueService.setAuthentication(auth);
                               String label = (String) mListView.getItemAtPosition(info.position);
                               label = label.replaceAll(" ", "%20");
                               issueService.removeLabelWithoutIssue(mUserLogin, mRepoName, label);
                               new LoadIssueLabelsTask(IssueLabelListActivity.this).execute();
                           }
                       })
                       .setNegativeButton("No", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                           }
                       });
                AlertDialog alert = builder.create();
                alert.show();
            }
            catch (GitHubException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                showMessage(getResources().getString(R.string.issue_error_delete_label), false);
            }
            break;
        case Menu.FIRST + 2:
            String label = (String) mListView.getItemAtPosition(info.position);
            label = label.replaceAll(" ", "%20");
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
}