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
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.adapter.ReleaseAssetAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.DownloadUtils;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;
import com.gh4a.widget.SwipeRefreshLayout;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.model.ReleaseAsset;
import com.meisolsson.githubsdk.service.repositories.RepositoryReleaseService;

import java.util.Date;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ReleaseInfoActivity extends BaseActivity implements
        View.OnClickListener, SwipeRefreshLayout.ChildScrollDelegate,
        RootAdapter.OnItemClickListener<ReleaseAsset>,
        RootAdapter.OnItemLongClickListener<ReleaseAsset> {
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

    private static final int ID_LOADER_RELEASE = 0;

    private String mRepoOwner;
    private String mRepoName;
    private Release mRelease;
    private long mReleaseId;

    private View mRootView;
    private HttpImageGetter mImageGetter;

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
            loadRelease(false);
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
        mRelease = extras.getParcelable("release");
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
            IntentUtils.launchBrowser(this, Uri.parse(mRelease.htmlUrl()));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(mRootView);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // Allow refresh only when the release is not passed via intent extras
        return mReleaseId != 0;
    }

    @Override
    public void onRefresh() {
        mRelease = null;
        setContentShown(false);
        mImageGetter.clearHtmlCache();
        loadRelease(true);
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
        String name = mRelease.name();
        if (TextUtils.isEmpty(name)) {
            name = mRelease.tagName();
        }
        getSupportActionBar().setTitle(name);
        fillData();
    }

    private void fillData() {
        ImageView gravatar = findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(gravatar, mRelease.author());
        gravatar.setOnClickListener(this);

        StyleableTextView details = findViewById(R.id.tv_releaseinfo);
        Date releaseDateToShow = mRelease.publishedAt() != null ? mRelease.publishedAt() : mRelease.createdAt();
        String detailsText = getString(R.string.release_details,
                ApiHelpers.getUserLogin(this, mRelease.author()),
                StringUtils.formatRelativeTime(this, releaseDateToShow, true));
        StringUtils.applyBoldTagsAndSetText(details, detailsText);

        TextView releaseType = findViewById(R.id.tv_releasetype);
        if (mRelease.draft()) {
            releaseType.setText(R.string.release_type_draft);
        } else if (mRelease.prerelease()) {
            releaseType.setText(R.string.release_type_prerelease);
        } else {
            releaseType.setText(R.string.release_type_final);
        }

        TextView tag = findViewById(R.id.tv_releasetag);
        tag.setText(getString(R.string.release_tag, mRelease.tagName()));
        tag.setOnClickListener(this);

        TextView body = findViewById(R.id.tv_release_notes);
        if (!TextUtils.isEmpty(mRelease.bodyHtml())) {
            mImageGetter.bind(body, mRelease.bodyHtml(), mRelease.id());
        } else {
            body.setText(R.string.release_no_releasenotes);
        }

        if (mRelease.assets() != null && !mRelease.assets().isEmpty()) {
            RecyclerView downloadsList = findViewById(R.id.download_list);
            ReleaseAssetAdapter adapter = new ReleaseAssetAdapter(this);
            adapter.addAll(mRelease.assets());
            adapter.setOnItemClickListener(this);
            adapter.setOnItemLongClickListener(this);
            downloadsList.setLayoutManager(new LinearLayoutManager(this));
            downloadsList.setNestedScrollingEnabled(false);
            downloadsList.setAdapter(adapter);
        } else {
            findViewById(R.id.downloads).setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(ReleaseAsset item) {
        DownloadUtils.enqueueDownloadWithPermissionCheck(this, item);
    }

    @Override
    public boolean onItemLongClick(ReleaseAsset item) {
        String label = "Release asset " + item.name();
        IntentUtils.copyToClipboard(this, label, item.browserDownloadUrl());
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show();

        return true;
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;

        switch (v.getId()) {
            case R.id.tv_releasetag:
                intent = RepositoryActivity.makeIntent(this,
                        mRepoOwner, mRepoName, mRelease.tagName());
                break;
            case R.id.iv_gravatar:
                intent = UserActivity.makeIntent(this, mRelease.author());
                break;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    private void loadRelease(boolean force) {
        RepositoryReleaseService service = ServiceFactory.get(RepositoryReleaseService.class, force);

        service.getRelease(mRepoOwner, mRepoName, mReleaseId)
                .map(ApiHelpers::throwOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_RELEASE, force))
                .subscribe(result -> {
                    mRelease = result;
                    handleReleaseReady();
                    setContentShown(true);
                }, this::handleLoadFailure);
    }
}
