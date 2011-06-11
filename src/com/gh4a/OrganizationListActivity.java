package com.gh4a;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.SimpleStringAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.Organization;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.OrganizationService;
import com.github.api.v2.services.UserService;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;

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

        createBreadcrumb(getResources().getString(R.string.user_organizations), breadCrumbHolders);
        
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
    
    private static class LoadOrganizationsTask extends AsyncTask<Void, Void, List<Organization>> {

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
        protected List<Organization> doInBackground(Void... arg0) {
            if (mTarget.get() != null) {
                try {
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    if (mTarget.get().mUserLogin.equals(mTarget.get().getAuthUsername())) {
                        OrganizationService orgService = factory.createOrganizationService();
                        Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                                mTarget.get().getAuthPassword());
                        orgService.setAuthentication(auth);
                        return orgService.getUserOrganizations();    
                    }
                    else {
                        UserService userService = factory.createUserService();
                        Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                                mTarget.get().getAuthPassword());
                        userService.setAuthentication(auth);
                        return userService.getUserOrganizations(mTarget.get().mUserLogin);    
                    }
                }
                catch (GitHubException e) {
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
        protected void onPostExecute(List<Organization> result) {
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
    
    private void fillData(List<Organization> organizations) {
        if (organizations != null && organizations.size() > 0) {
            for (Organization org : organizations) {
                mAdapter.add(org.getLogin());
            }
        }
        mAdapter.notifyDataSetChanged();
    }
}
