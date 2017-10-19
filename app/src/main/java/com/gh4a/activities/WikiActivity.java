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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gh4a.R;
import com.gh4a.model.Feed;

public class WikiActivity extends WebViewerActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName, Feed feed) {
        return new Intent(context, WikiActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("title", feed.getTitle())
                .putExtra("content", feed.getContent());
    }

    private String mUserLogin;
    private String mRepoName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onDataReady();
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getIntent().getStringExtra("title");
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mUserLogin + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mUserLogin = extras.getString("owner");
        mRepoName = extras.getString("repo");
    }

    @Override
    protected String generateHtml(String cssTheme, boolean addTitleHeader) {
        String title = addTitleHeader ? getDocumentTitle() : null;
        return wrapUnthemedHtml(getIntent().getStringExtra("content"), cssTheme, title);
    }

    @Override
    protected String getDocumentTitle() {
        return getString(R.string.wiki_print_document_title,
                getIntent().getStringExtra("title"), mUserLogin, mRepoName);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // content is passed in intent extras
        return false;
    }

    @Override
    protected Intent navigateUp() {
        return WikiListActivity.makeIntent(this, mUserLogin, mRepoName, null);
    }
}
