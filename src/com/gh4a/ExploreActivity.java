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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ExploreActivity extends BaseActivity {
    
    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();

        createBreadcrumb(getResources().getString(R.string.explore), null);
        
        fillData();
    }

    private void fillData() {
        ListView listView = (ListView) findViewById(R.id.list_view);
        String[] exploreItems = getResources().getStringArray(R.array.explore_item);
        listView.setAdapter(new ArrayAdapter<String>(this, R.layout.row_simple, exploreItems));
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position == 0) {//timeline
                    Intent intent = new Intent().setClass(ExploreActivity.this, TimelineActivity.class);
                    intent.putExtra(Constants.User.USER_LOGIN, "");
                    startActivity(intent);
                }
                else if (position == 1) {//trending repos
                    Intent intent = new Intent().setClass(ExploreActivity.this, TrendActivity.class);
                    startActivity(intent);
                }
                else if (position == 2) {//GitHub Blog
                    Intent intent = new Intent().setClass(ExploreActivity.this, BlogListActivity.class);
                    startActivity(intent);
                }
//                else if (position == 3) {//Discussion
//                    Intent intent = new Intent().setClass(ExploreActivity.this, DiscussionCategoryListActivity.class);
//                    startActivity(intent);
//                }
                else if (position == 3) {//GitHub jobs
                    Intent intent = new Intent().setClass(ExploreActivity.this, JobListActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
}
