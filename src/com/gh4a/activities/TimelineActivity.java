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
package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.PublicTimelineFragment;

public class TimelineActivity extends BaseSherlockFragmentActivity {
    private PublicTimelineFragment mFragment;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            mFragment = PublicTimelineFragment.newInstance();
            fm.beginTransaction().add(android.R.id.content, mFragment).commit();
        } else {
            mFragment = (PublicTimelineFragment) fm.findFragmentById(android.R.id.content);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.explore_menu, menu);
        menu.findItem(R.id.pub_timeline).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        if (!Gh4Application.get(this).isAuthorized()) {
            Intent intent = new Intent(this, Github4AndroidActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            Gh4Application app = Gh4Application.get(this);
            app.openUserInfoActivity(this, app.getAuthLogin(), null,
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.trend:
                Intent intent = new Intent(this, TrendingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.blog:
                intent = new Intent(this, BlogListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.refresh:
                mFragment.refresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
