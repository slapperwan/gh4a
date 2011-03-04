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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.CompareAdapter;
import com.gh4a.holder.BreadCrumbHolder;

/**
 * The Compare activity.
 */
public class CompareActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;
    
    /** The url. */
    protected String mUrl;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
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
    
    /**
     * Fill data into UI components.
     */
    private void fillData() {
        ListView listView = (ListView) findViewById(R.id.list_view);
        
        LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.compare_footer, listView, false);
        listView.addFooterView(ll);
        Button btnAllCommits = (Button) ll.findViewById(R.id.btn_all_commits);
        btnAllCommits.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                getApplicationContext().openBranchListActivity(CompareActivity.this,
                        mUserLogin,
                        mRepoName,
                        R.id.btn_branches);
            }
        });
        
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

        int index = mUrl.lastIndexOf("/");
        if (index != -1) {
            createBreadcrumb("Compare " + mUrl.substring(index + 1, mUrl.length()), breadCrumbHolders);
        }
        else {
            createBreadcrumb("Compare", breadCrumbHolders);
        }
    }
    
    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        HeaderViewListAdapter adapter = (HeaderViewListAdapter) adapterView.getAdapter();
        String[] sha = (String[]) adapter.getItem(position);
        
        getApplicationContext().openCommitInfoActivity(this, mUserLogin, mRepoName, 
                sha[0]);        
    }
}
