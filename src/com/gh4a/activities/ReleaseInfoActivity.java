/*
 * Copyright 2014 Danny Baumann
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

import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.User;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.adapter.DownloadAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MarkdownLoader;
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class ReleaseInfoActivity extends LoadingFragmentActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private Release mRelease;
    private User mReleaser;

    private LoaderCallbacks<String> mBodyCallback = new LoaderCallbacks<String>() {
        @Override
        public Loader<LoaderResult<String>> onCreateLoader(int id, Bundle args) {
            return new MarkdownLoader(ReleaseInfoActivity.this, mRelease.getBody(), null);
        }

        @Override
        public void onResultReady(LoaderResult<String> result) {
            fillNotes(result.getData());
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.release);

        Bundle extras = getIntent().getExtras();
        mRepoOwner = extras.getString(Constants.Repository.REPO_OWNER);
        mRepoName = extras.getString(Constants.Repository.REPO_NAME);
        mRelease = (Release) extras.getSerializable(Constants.Release.RELEASE);
        mReleaser = mRelease.getAuthor();
        if (mReleaser == null) {
            mReleaser = (User) extras.getSerializable(Constants.Release.RELEASER);
        }

        fillData();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mRelease.getName());
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mBodyCallback);
    }

    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openRepositoryInfoActivity(this,
                mRepoOwner, mRepoName, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private void fillData() {
        UiUtils.assignTypeface(this, Gh4Application.get(this).boldCondensed, new int[] {
            R.id.release_notes_title, R.id.downloads_title
        });

        ImageView gravatar = (ImageView) findViewById(R.id.iv_gravatar);
        GravatarHandler.assignGravatar(gravatar, mRelease.getAuthor());

        TextView details = (TextView) findViewById(R.id.tv_releaseinfo);
        details.setText(getString(R.string.release_details, mReleaser.getLogin(),
                Gh4Application.pt.format(mRelease.getCreatedAt())));

        TextView releaseType = (TextView) findViewById(R.id.tv_releasetype);
        if (mRelease.isDraft()) {
            releaseType.setText(R.string.release_type_draft);
        } else if (mRelease.isPreRelease()) {
            releaseType.setText(R.string.release_type_prerelease);
        } else {
            releaseType.setText(R.string.release_type_final);
        }

        TextView tag = (TextView) findViewById(R.id.tv_releasetag);
        tag.setText(getString(R.string.release_tag, mRelease.getTagName()));
        tag.setOnClickListener(this);

        if (mRelease.getAssets() != null && !mRelease.getAssets().isEmpty()) {
            ListView downloadsList = (ListView) findViewById(R.id.download_list);
            DownloadAdapter adapter = new DownloadAdapter(this);
            adapter.addAll(mRelease.getAssets());
            downloadsList.setAdapter(adapter);
            downloadsList.setOnItemClickListener(this);
        } else {
            findViewById(R.id.downloads).setVisibility(View.GONE);
        }
    }

    private void fillNotes(String bodyHtml) {
        TextView body = (TextView) findViewById(R.id.tv_release_notes);

        if (!StringUtils.isBlank(bodyHtml)) {
            HttpImageGetter imageGetter = new HttpImageGetter(this);

            bodyHtml = HtmlUtils.format(bodyHtml).toString();
            imageGetter.bind(body, bodyHtml, mRelease.getId());
            body.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            body.setText(R.string.release_no_releasenotes);
        }

        body.setVisibility(View.VISIBLE);
        findViewById(R.id.pb_releasenotes).setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DownloadAdapter adapter = (DownloadAdapter) parent.getAdapter();
        final Download download = adapter.getItem(position);

        UiUtils.enqueueDownload(this, download.getUrl(), download.getContentType(),
                download.getName(), download.getDescription());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_releasetag:
                Gh4Application.get(this).openRepositoryInfoActivity(this,
                        mRepoOwner, mRepoName, mRelease.getTagName(), 0);
                break;
        }
    }
}