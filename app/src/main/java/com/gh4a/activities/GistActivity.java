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
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.BackgroundTask;
import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.loader.GistLoader;
import com.gh4a.loader.GistStarLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.service.GistService;

import java.io.IOException;
import java.util.Map;

public class GistActivity extends BaseActivity implements View.OnClickListener {
    public static Intent makeIntent(Context context, String gistId) {
        return new Intent(context, GistActivity.class)
                .putExtra("id", gistId);
    }

    private String mGistId;
    private Gist mGist;
    private Boolean mIsStarred;

    private final LoaderCallbacks<Gist> mGistCallback = new LoaderCallbacks<Gist>(this) {
        @Override
        protected Loader<LoaderResult<Gist>> onCreateLoader() {
            return new GistLoader(GistActivity.this, mGistId);
        }
        @Override
        protected void onResultReady(Gist result) {
            fillData(result);
            setContentShown(true);
            supportInvalidateOptionsMenu();
        }
    };

    private final LoaderCallbacks<Boolean> mStarCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new GistStarLoader(GistActivity.this, mGistId);
        }
        @Override
        protected void onResultReady(Boolean result) {
            mIsStarred = result;
            supportInvalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gist);
        setContentShown(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.gist_title, mGistId));
        actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mGistCallback);
        getSupportLoaderManager().initLoader(1, null, mStarCallback);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mGistId = extras.getString("id");
    }

    @Override
    public void onRefresh() {
        forceLoaderReload(0, 1);
        mGist = null;
        mIsStarred = null;
        setContentShown(false);
        super.onRefresh();
    }

    private void fillData(final Gist gist) {
        mGist = gist;

        if (gist.getOwner() != null) {
            getSupportActionBar().setSubtitle(gist.getOwner().getLogin());
        }

        TextView tvDesc = findViewById(R.id.tv_desc);
        tvDesc.setText(TextUtils.isEmpty(gist.getDescription())
                ? getString(R.string.gist_no_description) : gist.getDescription());

        TextView tvCreatedAt = findViewById(R.id.tv_created_at);
        tvCreatedAt.setText(StringUtils.formatRelativeTime(this, gist.getCreatedAt(), true));

        Map<String, GistFile> files = gist.getFiles();
        if (files != null && !files.isEmpty()) {
            ViewGroup container = findViewById(R.id.file_container);
            LayoutInflater inflater = getLayoutInflater();

            container.removeAllViews();
            for (GistFile gistFile : files.values()) {
                TextView rowView = (TextView) inflater.inflate(R.layout.selectable_label,
                        container, false);

                rowView.setText(gistFile.getFilename());
                rowView.setTextColor(UiUtils.resolveColor(this, android.R.attr.textColorPrimary));
                rowView.setOnClickListener(this);
                rowView.setTag(gistFile);
                container.addView(rowView);
            }
        } else {
            findViewById(R.id.file_card).setVisibility(View.GONE);
        }

        findViewById(R.id.tv_private).setVisibility(gist.isPublic() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        GistFile file = (GistFile) view.getTag();
        startActivity(GistViewerActivity.makeIntent(this, mGistId, file.getFilename()));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem starItem = menu.add(0, R.id.star, 0, R.string.repo_star_action)
                .setIcon(R.drawable.star);
        MenuItemCompat.setShowAsAction(starItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        if (mGist != null) {
            MenuItem shareItem = menu.add(0, R.id.share, 0, R.string.share)
                    .setIcon(R.drawable.social_share);
            MenuItemCompat.setShowAsAction(shareItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean authorized = Gh4Application.get().isAuthorized();

        MenuItem starAction = menu.findItem(R.id.star);
        starAction.setVisible(authorized);
        if (authorized) {
            if (mIsStarred == null) {
                MenuItemCompat.setActionView(starAction, R.layout.ab_loading);
                MenuItemCompat.expandActionView(starAction);
            } else if (mIsStarred) {
                starAction.setTitle(R.string.repo_unstar_action);
                starAction.setIcon(R.drawable.unstar);
            } else {
                starAction.setTitle(R.string.repo_star_action);
                starAction.setIcon(R.drawable.star);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                String login = ApiHelpers.getUserLogin(this, mGist.getOwner());
                IntentUtils.share(this, getString(R.string.share_gist_subject, mGistId, login),
                        mGist.getHtmlUrl());
                return true;
            case R.id.star:
                MenuItemCompat.setActionView(item, R.layout.ab_loading);
                MenuItemCompat.expandActionView(item);
                new UpdateStarTask().schedule();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Intent navigateUp() {
        String login = mGist != null && mGist.getOwner() != null
                ? mGist.getOwner().getLogin() : null;
        return login != null ? GistListActivity.makeIntent(this, login) : null;
    }

    private class UpdateStarTask extends BackgroundTask<Void> {
        public UpdateStarTask() {
            super(GistActivity.this);
        }

        @Override
        protected Void run() throws IOException {
            GistService gistService = (GistService)
                    Gh4Application.get().getService(Gh4Application.GIST_SERVICE);
            if (mIsStarred) {
                gistService.unstarGist(mGistId);
            } else {
                gistService.starGist(mGistId);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mIsStarred = !mIsStarred;
            supportInvalidateOptionsMenu();
        }
    }
}