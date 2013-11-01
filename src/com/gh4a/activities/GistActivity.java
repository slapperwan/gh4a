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
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.loader.GistLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;

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
            if (!result.handleError(GistActivity.this)) {
                fillData(result.getData());
            } else {
                setContentEmpty(true);
            }
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
        tvCreatedAt.setText(Gh4Application.pt.format(gist.getCreatedAt()));
        
        LinearLayout llFiles = (LinearLayout) findViewById(R.id.ll_files);
        Map<String, GistFile> files = gist.getFiles();
        if (files != null) {
            for (GistFile gistFile : files.values()) {
                TextView tvFilename = new TextView(this, null, R.style.SelectableLabel);
                SpannableString content = new SpannableString(gistFile.getFilename());
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                tvFilename.setText(content);
                tvFilename.setTextColor(getResources().getColor(R.color.highlight));
                tvFilename.setOnClickListener(this);
                tvFilename.setTag(gistFile);
                llFiles.addView(tvFilename);
            }
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
    
    @Override
    protected void navigateUp() {
        Intent intent = new Intent().setClass(this, GistListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}