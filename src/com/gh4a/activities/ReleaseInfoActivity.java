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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.DownloadAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MarkdownLoader;
import com.gh4a.loader.ReleaseLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;
import com.gh4a.widget.SwipeRefreshLayout;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class ReleaseInfoActivity extends BaseActivity implements
        View.OnClickListener, SwipeRefreshLayout.ChildScrollDelegate,
        RootAdapter.OnItemClickListener<Download> {
    private String mRepoOwner;
    private String mRepoName;
    private Release mRelease;
    private long mReleaseId;

    private View mRootView;
    private HttpImageGetter mImageGetter;

    private LoaderCallbacks<Release> mReleaseCallback = new LoaderCallbacks<Release>(this) {
        @Override
        protected Loader<LoaderResult<Release>> onCreateLoader() {
            return new ReleaseLoader(ReleaseInfoActivity.this, mRepoOwner, mRepoName, mReleaseId);
        }

        @Override
        protected void onResultReady(Release result) {
            mRelease = result;
            handleReleaseReady();
            setContentShown(true);
        }
    };
    private LoaderCallbacks<String> mBodyCallback = new LoaderCallbacks<String>(this) {
        @Override
        protected Loader<LoaderResult<String>> onCreateLoader() {
            return new MarkdownLoader(ReleaseInfoActivity.this, mRelease.getBody(), null);
        }

        @Override
        protected void onResultReady(String result) {
            fillNotes(result);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.release);

        mRootView = findViewById(R.id.root);
        mImageGetter = new HttpImageGetter(this);
        setChildScrollDelegate(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.release_title);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (mRelease != null) {
            handleReleaseReady();
        } else {
            setContentShown(false);
            getSupportLoaderManager().initLoader(0, null, mReleaseCallback);
        }
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mRelease = (Release) extras.getSerializable(Constants.Release.RELEASE);
        mReleaseId = extras.getLong(Constants.Release.ID);
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(mRootView);
    }

    @Override
    public void onRefresh() {
        Loader loader = getSupportLoaderManager().getLoader(0);
        if (loader != null) {
            mRelease = null;
            setContentShown(false);
            loader.onContentChanged();
            getSupportLoaderManager().destroyLoader(1);
        }
        super.onRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mImageGetter.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageGetter.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageGetter.destroy();
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mRepoOwner, mRepoName, null);
    }

    private void handleReleaseReady() {
        String name = mRelease.getName();
        if (TextUtils.isEmpty(name)) {
            name = mRelease.getTagName();
        }
        getSupportActionBar().setTitle(name);
        getSupportLoaderManager().initLoader(1, null, mBodyCallback);
        fillData();
    }

    private void fillData() {
        ImageView gravatar = (ImageView) findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(gravatar, mRelease.getAuthor());

        StyleableTextView details = (StyleableTextView) findViewById(R.id.tv_releaseinfo);
        String detailsText = getString(R.string.release_details,
                ApiHelpers.getUserLogin(this, mRelease.getAuthor()),
                StringUtils.formatRelativeTime(this, mRelease.getCreatedAt(), true));
        StringUtils.applyBoldTagsAndSetText(details, detailsText);

        TextView releaseType = (TextView) findViewById(R.id.tv_releasetype);
        if (mRelease.isDraft()) {
            releaseType.setText(R.string.release_type_draft);
        } else if (mRelease.isPrerelease()) {
            releaseType.setText(R.string.release_type_prerelease);
        } else {
            releaseType.setText(R.string.release_type_final);
        }

        TextView tag = (TextView) findViewById(R.id.tv_releasetag);
        tag.setText(getString(R.string.release_tag, mRelease.getTagName()));
        tag.setOnClickListener(this);

        if (mRelease.getAssets() != null && !mRelease.getAssets().isEmpty()) {
            RecyclerView downloadsList = (RecyclerView) findViewById(R.id.download_list);
            DownloadAdapter adapter = new DownloadAdapter(this);
            adapter.addAll(mRelease.getAssets());
            adapter.setOnItemClickListener(this);
            downloadsList.setLayoutManager(new LinearLayoutManager(this));
            downloadsList.setNestedScrollingEnabled(false);
            downloadsList.setAdapter(adapter);
        } else {
            findViewById(R.id.downloads).setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(Download download) {
        UiUtils.enqueueDownloadWithPermissionCheck(this, download.getUrl(),
                download.getContentType(), download.getName(),
                download.getDescription(), "application/octet-stream");
    }

    private void fillNotes(String bodyHtml) {
        TextView body = (TextView) findViewById(R.id.tv_release_notes);

        if (!StringUtils.isBlank(bodyHtml)) {
            bodyHtml = HtmlUtils.format(bodyHtml).toString();
            mImageGetter.bind(body, bodyHtml, mRelease.getId());
            body.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        } else {
            body.setText(R.string.release_no_releasenotes);
        }

        body.setVisibility(View.VISIBLE);
        findViewById(R.id.pb_releasenotes).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_releasetag:
                startActivity(IntentUtils.getRepoActivityIntent(this,
                        mRepoOwner, mRepoName, mRelease.getTagName()));
                break;
        }
    }
}
