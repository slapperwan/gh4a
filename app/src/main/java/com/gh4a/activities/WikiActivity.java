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
import androidx.annotation.Nullable;

import com.gh4a.R;
import com.gh4a.model.Feed;
import com.gh4a.utils.IntentUtils;

public class WikiActivity extends WebViewerActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName, Feed feed) {
        Intent intent = new Intent(context, WikiActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName);
        // Avoid TransactionTooLargeExceptions on activity launch when page content is too big
        IntentUtils.putCompressedParcelableExtra(intent, "page_feed", feed, 800_000);
        return intent;
    }

    private String mUserLogin;
    private String mRepoName;
    private Feed mWikiPageFeed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onDataReady();
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return mWikiPageFeed.getTitle();
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
        mWikiPageFeed = IntentUtils.readCompressedParcelableFromBundle(extras, "page_feed");
    }

    @Override
    protected String generateHtml(String cssTheme, boolean addTitleHeader) {
        String title = addTitleHeader ? getDocumentTitle() : null;
        return wrapUnthemedHtml(mWikiPageFeed.getContent(), cssTheme, title);
    }

    @Override
    protected String getDocumentTitle() {
        return getString(R.string.wiki_print_document_title, mWikiPageFeed.getTitle(), mUserLogin, mRepoName);
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
