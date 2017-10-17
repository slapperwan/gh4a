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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.GistFile;
import com.meisolsson.githubsdk.service.gists.GistService;

public class GistViewerActivity extends WebViewerActivity {
    public static Intent makeIntent(Context context, String id, String fileName) {
        return new Intent(context, GistViewerActivity.class)
                .putExtra("id", id)
                .putExtra("file", fileName);
    }

    private static final int ID_LOADER_GIST = 0;

    private String mFileName;
    private String mGistId;
    private GistFile mGistFile;
    private String mGistOwner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadGist(false);
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return mFileName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mFileName = extras.getString("file");
        mGistId = extras.getString("id");
    }

    @Override
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void onRefresh() {
        setContentShown(false);
        mGistFile = null;
        loadGist(true);
        super.onRefresh();
    }

    @Override
    protected String generateHtml(String cssTheme, boolean addTitleHeader) {
        if (FileUtils.isMarkdown(mGistFile.filename())) {
            String base64Data = StringUtils.toBase64(mGistFile.content());
            return generateMarkdownHtml(base64Data, null, null, null, cssTheme, addTitleHeader);
        } else {
            return generateCodeHtml(mGistFile.content(), mFileName,
                    -1, -1, cssTheme, addTitleHeader);
        }
    }

    @Override
    protected String getDocumentTitle() {
        return getString(R.string.gist_print_document_title, mFileName, mGistId, mGistOwner);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.file_viewer_menu, menu);

        menu.removeItem(R.id.share);
        if (mGistFile == null || FileUtils.isMarkdown(mGistFile.filename())) {
            menu.removeItem(R.id.wrap);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected Intent navigateUp() {
        return GistActivity.makeIntent(this, mGistId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(mGistFile.rawUrl()));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadGist(boolean force) {
        GistService service = Gh4Application.get().getGitHubService(GistService.class);
        service.getGist(mGistId)
                .map(ApiHelpers::throwOnFailure)
                .toObservable()
                .compose(makeLoaderObservable(ID_LOADER_GIST, force))
                .subscribe(result -> {
                    mGistOwner = ApiHelpers.getUserLogin(GistViewerActivity.this, result.owner());
                    mGistFile = result.files().get(mFileName);
                    onDataReady();
                }, error -> {});

    }
}
