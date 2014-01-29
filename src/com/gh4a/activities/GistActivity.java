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

import java.util.Map;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.loader.GistLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

public class GistActivity extends LoadingFragmentActivity implements OnClickListener {
    private String mGistId;
    private String mUserLogin;
    private Gist mGist;

    private LoaderCallbacks<Gist> mGistCallback = new LoaderCallbacks<Gist>() {
        @Override
        public Loader<LoaderResult<Gist>> onCreateLoader(int id, Bundle args) {
            return new GistLoader(GistActivity.this, mGistId);
        }
        @Override
        public void onResultReady(LoaderResult<Gist> result) {
            boolean success = !result.handleError(GistActivity.this);
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

        mGistId = getIntent().getExtras().getString(Constants.Gist.ID);
        mUserLogin = getIntent().getExtras().getString(Constants.User.USER_LOGIN);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.gist);
        setContentShown(false);

        UiUtils.assignTypeface(this, Gh4Application.get(this).boldCondensed, new int[] {
            R.id.tv_desc, R.id.files_title
        });
        
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(getString(R.string.gist_title, mGistId));
        mActionBar.setSubtitle(mUserLogin);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mGistCallback);
    }
    
    private void fillData(final Gist gist) {
        mGist = gist;

        TextView tvDesc = (TextView) findViewById(R.id.tv_desc);
        tvDesc.setText(gist.getDescription());
        tvDesc.setVisibility(StringUtils.isBlank(gist.getDescription()) ? View.GONE : View.VISIBLE);
        
        TextView tvCreatedAt = (TextView) findViewById(R.id.tv_created_at);
        tvCreatedAt.setText(StringUtils.formatRelativeTime(this, gist.getCreatedAt(), true));
        
        LinearLayout llFiles = (LinearLayout) findViewById(R.id.ll_files);
        Map<String, GistFile> files = gist.getFiles();
        if (files != null && !files.isEmpty()) {
            for (GistFile gistFile : files.values()) {
                View rowView = getLayoutInflater().inflate(R.layout.selectable_label, null);
                TextView tvTitle = (TextView) rowView.findViewById(R.id.tv_title);

                tvTitle.setText(gistFile.getFilename());
                tvTitle.setTextColor(getResources().getColor(R.color.highlight));
                tvTitle.setOnClickListener(this);
                tvTitle.setTag(gistFile);
                llFiles.addView(tvTitle);
            }
        } else {
            llFiles.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, GistViewerActivity.class);
        GistFile gist = (GistFile) view.getTag();
        intent.putExtra(Constants.User.USER_LOGIN, mGist.getUser().getLogin());
        intent.putExtra(Constants.Gist.FILENAME, gist.getFilename());
        intent.putExtra(Constants.Gist.ID, mGist.getId());
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.id.share, 0, getString(R.string.share))
            .setIcon(UiUtils.resolveDrawable(this, R.attr.shareIcon))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.share_gist_subject, mGistId, mUserLogin));
                shareIntent.putExtra(Intent.EXTRA_TEXT,  mGist.getHtmlUrl());
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void navigateUp() {
        Intent intent = new Intent(this, GistListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}