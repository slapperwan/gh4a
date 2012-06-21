package com.gh4a;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.OrganizationService;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.gh4a.adapter.SimpleStringAdapter;

public class OrganizationListActivity extends BaseActivity {

    private String mUserLogin;
    private LoadingDialog mLoadingDialog;
    private SimpleStringAdapter mAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);

        setUpActionBar();
        mUserLogin = getIntent().getStringExtra(Constants.User.USER_LOGIN);
        
        ListView mListView = (ListView) findViewById(R.id.list_view);
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String organizationLogin = (String) adapterView.getAdapter().getItem(position);
                getApplicationContext().openUserInfoActivity(OrganizationListActivity.this, organizationLogin, null);   
            }
            
        });
        mAdapter = new SimpleStringAdapter(this, new ArrayList<String>());
        mListView.setAdapter(mAdapter);
        
        new LoadOrganizationsTask(this).execute();
    }
    
    private static class LoadOrganizationsTask extends AsyncTask<Void, Void, List<User>> {

        /** The target. */
        private WeakReference<OrganizationListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        private boolean isAuthError;

        /**
         * Instantiates a new load user info task.
         *
         * @param activity the activity
         */
        public LoadOrganizationsTask(OrganizationListActivity activity) {
            mTarget = new WeakReference<OrganizationListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<User> doInBackground(Void... arg0) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    OrganizationService orgService = new OrganizationService(client);
                    return orgService.getOrganizations(mTarget.get().mUserLogin);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    if (e.getCause() != null
                            && e.getCause().getMessage().equalsIgnoreCase(
                                    "Received authentication challenge is null")) {
                        isAuthError = true;
                    }
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<User> result) {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog.dismiss();
                if (mException && isAuthError) {
                    Toast.makeText(mTarget.get(),
                            "Your API token maybe invalid.",
                            Toast.LENGTH_LONG).show();
                }
                else if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                }
            }
        }
    }
    
    private void fillData(List<User> organizations) {
        if (organizations != null && organizations.size() > 0) {
            for (User org : organizations) {
                mAdapter.add(org.getLogin());
            }
        }
        mAdapter.notifyDataSetChanged();
    }
}
