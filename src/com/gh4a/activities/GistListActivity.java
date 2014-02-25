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

import java.util.List;

import org.eclipse.egit.github.core.Gist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.adapter.GistAdapter;
import com.gh4a.loader.GistListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.ToastUtils;

public class GistListActivity extends LoadingFragmentActivity implements OnItemClickListener {
    private String mUserLogin;

    private LoaderCallbacks<List<Gist>> mGistsCallback = new LoaderCallbacks<List<Gist>>() {
        @Override
        public Loader<LoaderResult<List<Gist>>> onCreateLoader(int id, Bundle args) {
            return new GistListLoader(GistListActivity.this, mUserLogin);
        }

        @Override
        public void onResultReady(LoaderResult<List<Gist>> result) {
            boolean success = !result.handleError(GistListActivity.this);
            if (success) {
                fillData(result.getData());
            }
            setContentEmpty(!success);
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mUserLogin = getIntent().getExtras().getString(Constants.User.LOGIN);

        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.generic_list);
        setContentShown(false);

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(getResources().getQuantityString(R.plurals.gist, 0));
        mActionBar.setSubtitle(mUserLogin);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mGistsCallback);
    }

    private void fillData(List<Gist> gists) {
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);

        GistAdapter adapter = new GistAdapter(this);
        if (gists != null && !gists.isEmpty()) {
            adapter.addAll(gists);
            listView.setAdapter(adapter);
        } else {
            ToastUtils.notFoundMessage(this, R.plurals.gist);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Gist gist = (Gist) adapterView.getAdapter().getItem(position);
        IntentUtils.openGistActivity(this, mUserLogin, gist.getId(), 0);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openUserInfoActivity(this, mUserLogin, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}