package com.gh4a;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.CompareAdapter;
import com.gh4a.holder.BreadCrumbHolder;

public class CompareActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;
    
    protected String mUrl;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mUrl = getIntent().getExtras().getString(Constants.Repository.REPO_URL);
        
        setBreadCrumb();
        
        fillData();
    }
    
    private void fillData() {
        ListView listView = (ListView) findViewById(R.id.list_view);
        CompareAdapter compareAdapter = new CompareAdapter(this, new ArrayList<String[]>());
        listView.setAdapter(compareAdapter);
        listView.setOnItemClickListener(this);
        
        Bundle extra = getIntent().getExtras();
        Iterator<String> iter = extra.keySet().iterator();
        
        List<String[]> shas = new ArrayList<String[]>();
        while (iter.hasNext()) {
            String key = iter.next();
            if (key.startsWith("sha")) {
                String[] sha = extra.getStringArray(key);
                shas.add(sha);    
            }
            
        }
        
        if (shas != null && shas.size() > 0) {
            for (String[] sha : shas) {
                compareAdapter.add(sha);
            }
        }
        compareAdapter.notifyDataSetChanged();
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

        int index = mUrl.lastIndexOf("/");
        if (index != -1) {
            createBreadcrumb("Compare " + mUrl.substring(index + 1, mUrl.length()), breadCrumbHolders);
        }
        else {
            createBreadcrumb("Compare", breadCrumbHolders);
        }
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        CompareAdapter adapter = (CompareAdapter) adapterView.getAdapter();
        String[] sha = (String[]) adapter.getItem(position);
        
        getApplicationContext().openCommitInfoActivity(this, mUserLogin, mRepoName, 
                sha[0]);        
    }
}
