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

import com.gh4a.adapter.CompareAdapter;

/**
 * The Compare activity.
 */
public class CompareActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;
    
    /** The base. */
    protected String mBase;
    
    /** The head */
    protected String mHead;
    
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
        mBase = getIntent().getExtras().getString(Constants.Repository.BASE);
        mHead = getIntent().getExtras().getString(Constants.Repository.HEAD);
        
        fillData();
    }
    
    /**
     * Fill data into UI components.
     */
    private void fillData() {
        ListView listView = (ListView) findViewById(R.id.list_view);
        
//        LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.compare_footer, listView, false);
//        listView.addFooterView(ll);
//        Button btnAllCommits = (Button) ll.findViewById(R.id.btn_all_commits);
//        btnAllCommits.setOnClickListener(new OnClickListener() {
//            
//            @Override
//            public void onClick(View arg0) {
//                getApplicationContext().openBranchListActivity(CompareActivity.this,
//                        mUserLogin,
//                        mRepoName,
//                        R.id.btn_branches);
//            }
//        });
        
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

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        CompareAdapter adapter = (CompareAdapter) adapterView.getAdapter();
        String[] sha = (String[]) adapter.getItem(position);
        
        getApplicationContext().openCommitInfoActivity(this, mUserLogin, mRepoName, 
                sha[0]);        
    }
}
