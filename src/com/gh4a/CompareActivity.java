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
import java.util.Iterator;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.adapter.CompareAdapter;

public class CompareActivity extends BaseActivity implements OnItemClickListener {

    private String mRepoOwner;
    private String mRepoName;
    private String mBase;
    private String mHead;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mBase = getIntent().getExtras().getString(Constants.Repository.BASE);
        mHead = getIntent().getExtras().getString(Constants.Repository.HEAD);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.commit_compare);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        hideLoading();
        fillData();
    }
    
    private void fillData() {
        ListView listView = (ListView) findViewById(R.id.list_view);
        
        CompareAdapter compareAdapter = new CompareAdapter(this, new ArrayList<String[]>());
        listView.setAdapter(compareAdapter);
        listView.setOnItemClickListener(this);
        
        Bundle extra = getIntent().getExtras();
        Iterator<String> iter = extra.keySet().iterator();

        List<String[]> commits = new ArrayList<String[]>();
        while (iter.hasNext()) {
            String key = iter.next();
            if (key.startsWith("commit")) {
                String[] commitInfo = extra.getStringArray(key);
                commits.add(commitInfo);    
            }
            
        }
        
        if (commits != null && commits.size() > 0) {
            for (String[] commitInfo : commits) {
                compareAdapter.add(commitInfo);
            }
        }
        compareAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        CompareAdapter adapter = (CompareAdapter) adapterView.getAdapter();
        String[] sha = (String[]) adapter.getItem(position);
        
        getApplicationContext().openCommitInfoActivity(this, mRepoOwner, mRepoName, 
                sha[0]);        
    }
}
