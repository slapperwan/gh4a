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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.adapter.ReleaseAssetAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MarkdownLoader;
import com.gh4a.loader.ReleaseLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;
import com.gh4a.widget.SwipeRefreshLayout;

import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.Release;

public class ReleaseInfoActivity extends BaseActivity implements
        View.OnClickListener, SwipeRefreshLayout.ChildScrollDelegate,
        RootAdapter.OnItemClickListener<Download> {
    public static Intent makeIntent(Context context, String repoOwner, String repoName, long id) {
        return new Intent(context, ReleaseInfoActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("id", id);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
                                    Release release) {
        return new Intent(context, ReleaseInfoActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("release", release);
    }

    private String mRepoOwner;
    private String mRepoName;
    private Release mRelease;
    private long mReleaseId;

    private View mRootView;
    private HttpImageGetter mImageGetter;

    private final LoaderCallbacks<Release> mReleaseCallback = new LoaderCallbacks<Release>(this) {
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
    private final LoaderCallbacks<String> mBodyCallback = new LoaderCallbacks<String>(this) {
        @Override
        protected Loader<LoaderResult<String>> onCreateLoader() {
            return new MarkdownLoader(ReleaseInfoActivity.this,
                    mRepoOwner, mRepoName, mRelease.getBody());
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

        if (mRelease != null) {
            handleReleaseReady();
        } else {
            setContentShown(false);
            getSupportLoaderManager().initLoader(0, null, mReleaseCallback);
        }
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.release_title);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mRelease = (Release) extras.getSerializable("release");
        mReleaseId = extras.getLong("id");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.release, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.browser) {
            IntentUtils.launchBrowser(this, Uri.parse(mRelease.getHtmlUrl()));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(mRootView);
    }

    @Override
    public void onRefresh() {
        if (forceLoaderReload(0)) {
            mRelease = null;
            setContentShown(false);
            mImageGetter.clearHtmlCache();
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
        return ReleaseListActivity.makeIntent(this, mRepoOwner, mRepoName);
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
        ImageView gravatar = findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(gravatar, mRelease.getAuthor());
        gravatar.setOnClickListener(this);

        StyleableTextView details = findViewById(R.id.tv_releaseinfo);
        String detailsText = getString(R.string.release_details,
                ApiHelpers.getUserLogin(this, mRelease.getAuthor()),
                StringUtils.formatRelativeTime(this, mRelease.getCreatedAt(), true));
        StringUtils.applyBoldTagsAndSetText(details, detailsText);

        TextView releaseType = findViewById(R.id.tv_releasetype);
        if (mRelease.isDraft()) {
            releaseType.setText(R.string.release_type_draft);
        } else if (mRelease.isPrerelease()) {
            releaseType.setText(R.string.release_type_prerelease);
        } else {
            releaseType.setText(R.string.release_type_final);
        }

        TextView tag = findViewById(R.id.tv_releasetag);
        tag.setText(getString(R.string.release_tag, mRelease.getTagName()));
        tag.setOnClickListener(this);

        if (mRelease.getAssets() != null && !mRelease.getAssets().isEmpty()) {
            RecyclerView downloadsList = findViewById(R.id.download_list);
            ReleaseAssetAdapter adapter = new ReleaseAssetAdapter(this);
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
        TextView body = findViewById(R.id.tv_release_notes);

        if (!StringUtils.isBlank(bodyHtml)) {
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
        Intent intent = null;

        switch (v.getId()) {
            case R.id.tv_releasetag:
                intent = RepositoryActivity.makeIntent(this,
                        mRepoOwner, mRepoName, mRelease.getTagName());
                break;
            case R.id.iv_gravatar:
                intent = UserActivity.makeIntent(this, mRelease.getAuthor());
                break;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }
}
