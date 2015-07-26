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
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.DownloadAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MarkdownLoader;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class ReleaseInfoActivity extends BaseActivity implements View.OnClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private Release mRelease;
    private User mReleaser;

    private HttpImageGetter mImageGetter;

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
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.release);

        Bundle extras = getIntent().getExtras();
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mRelease = (Release) extras.getSerializable(Constants.Release.RELEASE);
        mReleaser = mRelease.getAuthor();
        if (mReleaser == null) {
            mReleaser = (User) extras.getSerializable(Constants.Release.RELEASER);
        }

        mImageGetter = new HttpImageGetter(this);
        fillData();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mRelease.getName());
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mBodyCallback);
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

    private void fillData() {
        ImageView gravatar = (ImageView) findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(gravatar, mRelease.getAuthor());

        StyleableTextView details = (StyleableTextView) findViewById(R.id.tv_releaseinfo);
        String detailsText = getString(R.string.release_details, mReleaser.getLogin(),
                StringUtils.formatRelativeTime(this, mRelease.getCreatedAt(), true));
        StringUtils.applyBoldTagsAndSetText(details, detailsText);

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
            ViewGroup downloadContainer = (ViewGroup) findViewById(R.id.downloads_container);
            DownloadAdapter adapter = new DownloadAdapter(this);
            adapter.addAll(mRelease.getAssets());
            for (int i = 0; i < adapter.getCount(); i++) {
                View item = adapter.getView(i, null, downloadContainer);
                item.setTag(adapter.getItem(i));
                item.setOnClickListener(this);
                downloadContainer.addView(item);
            }
        } else {
            findViewById(R.id.downloads).setVisibility(View.GONE);
        }
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
        if (v.getTag() instanceof Download) {
            Download download = (Download) v.getTag();
            UiUtils.enqueueDownload(this, download.getUrl(), download.getContentType(),
                    download.getName(), download.getDescription(), "application/octet-stream");
        } else {
            switch (v.getId()) {
                case R.id.tv_releasetag:
                    startActivity(IntentUtils.getRepoActivityIntent(this,
                            mRepoOwner, mRepoName, mRelease.getTagName()));
                    break;
            }
        }
    }
}