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

import java.util.Iterator;
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
import com.gh4a.R;
import com.gh4a.loader.GistLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;

public class GistActivity extends BaseSherlockFragmentActivity {

    private String mGistId;
    private String mUserLogin;

    private LoaderCallbacks<Gist> mGistCallback = new LoaderCallbacks<Gist>() {
        @Override
        public Loader<LoaderResult<Gist>> onCreateLoader(int id, Bundle args) {
            return new GistLoader(GistActivity.this, mGistId);
        }
        @Override
        public void onResultReady(LoaderResult<Gist> result) {
            hideLoading();
            if (!isLoaderError(result)) {
                fillData(result.getData());
            }
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
        
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(getString(R.string.gist_title, mGistId));
        mActionBar.setSubtitle(mUserLogin);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mGistCallback).forceLoad();
    }
    
    private void fillData(final Gist gist) {
        TextView tvDesc = (TextView) findViewById(R.id.tv_desc);
        if (StringUtils.isBlank(gist.getDescription())) {
            tvDesc.setVisibility(View.GONE);
        }
        else {
            tvDesc.setText(gist.getDescription());
            tvDesc.setVisibility(View.VISIBLE);
        }
        
        TextView tvCreatedAt = (TextView) findViewById(R.id.tv_created_at);
        tvCreatedAt.setText(Gh4Application.pt.format(gist.getCreatedAt()));
        
        LinearLayout llFiles = (LinearLayout) findViewById(R.id.ll_files);

        Map<String, GistFile> files = gist.getFiles();
        if (files != null) {
            Iterator<String> iter = files.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                final GistFile gistFile = files.get(key);
                TextView tvFilename = new TextView(getApplicationContext());
                SpannableString content = new SpannableString(gistFile.getFilename());
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                tvFilename.setText(content);
                tvFilename.setTextColor(getResources().getColor(R.color.highlight));
                tvFilename.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
                tvFilename.setTextAppearance(this, android.R.attr.textAppearanceMedium);
                tvFilename.setPadding(0, 8, 0, 8);
                tvFilename.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent().setClass(GistActivity.this,
                                GistViewerActivity.class);
                        intent.putExtra(Constants.User.USER_LOGIN, gist.getUser().getLogin());
                        intent.putExtra(Constants.Gist.FILENAME, gistFile.getFilename());
                        intent.putExtra(Constants.Gist.ID, gist.getId());
                        startActivity(intent);
                    }
                });
                llFiles.addView(tvFilename);
            }
        }
    }
    
    @Override
    protected void navigateUp() {
        Intent intent = new Intent().setClass(this, GistListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
