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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.adapter.ReleaseAssetAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.DownloadUtils;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.ReactionBar;
import com.gh4a.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import com.meisolsson.githubsdk.model.Reaction;
import com.meisolsson.githubsdk.model.Reactions;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.model.ReleaseAsset;
import com.meisolsson.githubsdk.model.request.ReactionRequest;
import com.meisolsson.githubsdk.service.reactions.ReactionService;
import com.meisolsson.githubsdk.service.repositories.RepositoryReleaseService;

import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Single;
import retrofit2.Response;

public class ReleaseInfoActivity extends BaseActivity implements
        View.OnClickListener, SwipeRefreshLayout.ChildScrollDelegate, ReactionBar.Callback,
        RootAdapter.OnItemClickListener<ReleaseAsset>,
        RootAdapter.OnItemLongClickListener<ReleaseAsset> {
    public static Intent makeIntent(Context context, String repoOwner, String repoName, long id) {
        return new Intent(context, ReleaseInfoActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("id", id);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName, String tagName) {
        return new Intent(context, ReleaseInfoActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("tag", tagName);
    }

    private static final int ID_LOADER_RELEASE = 0;

    private Release mRelease;
    private String mRepoOwner;
    private String mRepoName;
    private long mReleaseId;
    private String mTagName;

    private View mRootView;
    private HttpImageGetter mImageGetter;
    private ReactionBar.AddReactionMenuHelper mReactionMenuHelper;
    private final ReactionBar.ReactionDetailsCache mReactionDetailsCache =
            new ReactionBar.ReactionDetailsCache(this::onReactionsUpdated);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.release);

        mRootView = findViewById(R.id.root);
        mImageGetter = new HttpImageGetter(this);
        setChildScrollDelegate(this);

        setContentShown(false);
        loadRelease(false);
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
        mTagName = extras.getString("tag");
        mReleaseId = extras.getLong("id");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.release, menu);
        MenuItem reactItem = menu.findItem(R.id.react);
        getMenuInflater().inflate(R.menu.release_reaction_menu, reactItem.getSubMenu());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (canAddReaction()) {
            MenuItem reactItem = menu.findItem(R.id.react);
            reactItem.setVisible(true);
            mReactionMenuHelper = new ReactionBar.AddReactionMenuHelper(this,
                    reactItem.getSubMenu(), this, () -> mRelease.id(), mReactionDetailsCache);
            mReactionMenuHelper.startLoadingIfNeeded();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mReactionMenuHelper != null && mReactionMenuHelper.onItemClick(item)) {
            return true;
        }
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
    public void onRefresh() {
        mRelease = null;
        setContentShown(false);
        mImageGetter.clearHtmlCache();
        mReactionDetailsCache.clear();
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
        mReactionDetailsCache.destroy();
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
        invalidateOptionsMenu();
        fillData();
    }

    private void fillData() {
        ImageView gravatar = findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(gravatar, mRelease.author());
        gravatar.setOnClickListener(this);

        TextView details = findViewById(R.id.tv_releaseinfo);
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
        ReactionBar reactionBar = findViewById(R.id.reactions);
        reactionBar.setReactions(mRelease.reactions());
        reactionBar.setDetailsCache(mReactionDetailsCache);
        reactionBar.setAddReactionPopupMenu(R.menu.release_reaction_menu);
        reactionBar.setCallback(this, () -> mRelease.id());

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
    public boolean canAddReaction() {
        return Gh4Application.get().isAuthorized() && mRelease != null && !mRelease.draft();
    }

    @Override
    public Single<List<Reaction>> loadReactionDetails(ReactionBar.Item item, boolean bypassCache) {
        var service = ServiceFactory.getForFullPagedLists(ReactionService.class, bypassCache);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getReleaseReactions(mRepoOwner, mRepoName, mRelease.id(), page));
    }

    @Override
    public Single<Reaction> addReaction(ReactionBar.Item item, String content) {
        ReactionService service = ServiceFactory.get(ReactionService.class, false);
        ReactionRequest request = ReactionRequest.builder().content(content).build();
        return service.createReleaseReaction(mRepoOwner, mRepoName, mRelease.id(), request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapWithRetrySnackbar(this, R.string.add_reaction_error));
    }

    @Override
    public Single<Boolean> deleteReaction(ReactionBar.Item item, long reactionId) {
        ReactionService service = ServiceFactory.get(ReactionService.class, false);
        return service.deleteReleaseReaction(mRepoOwner, mRepoName, mRelease.id(), reactionId)
                .map(ApiHelpers::mapToTrueOnSuccess)
                .compose(RxUtils.wrapWithRetrySnackbar(this, R.string.remove_reaction_error));
    }

    private void onReactionsUpdated(ReactionBar.Item item, Reactions reactions) {
        ReactionBar reactionBar = findViewById(R.id.reactions);
        reactionBar.setReactions(reactions);
    }

    @Override
    public void onItemClick(ReleaseAsset item) {
        DownloadUtils.enqueueDownloadWithPermissionCheck(this, item);
    }

    @Override
    public boolean onItemLongClick(ReleaseAsset item) {
        String label = "Release asset " + item.name();
        IntentUtils.copyToClipboard(this, label, item.browserDownloadUrl());
        Snackbar.make(getRootLayout(), R.string.link_copied, Snackbar.LENGTH_SHORT).show();

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

        Single<Response<Release>> releaseSingle;
        if (mTagName != null) {
            releaseSingle = service.getRelaseByTagName(mRepoOwner, mRepoName, mTagName);
        } else {
            releaseSingle = service.getRelease(mRepoOwner, mRepoName, mReleaseId);
        }
        releaseSingle
                .map(ApiHelpers::throwOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_RELEASE, force))
                .subscribe(result -> {
                    mRelease = result;
                    handleReleaseReady();
                    setContentShown(true);
                }, this::handleLoadFailure);
    }
}
