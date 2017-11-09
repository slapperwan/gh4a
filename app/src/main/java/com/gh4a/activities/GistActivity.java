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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.Gist;
import com.meisolsson.githubsdk.model.GistFile;
import com.meisolsson.githubsdk.service.gists.GistService;

import java.util.Map;

import io.reactivex.Single;
import retrofit2.Response;

public class GistActivity extends BaseActivity implements View.OnClickListener {
    public static Intent makeIntent(Context context, String gistId) {
        return new Intent(context, GistActivity.class)
                .putExtra("id", gistId);
    }

    private static final int ID_LOADER_GIST = 0;
    private static final int ID_LOADER_STARRED = 1;

    private String mGistId;
    private Gist mGist;
    private Boolean mIsStarred;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gist);
        setContentShown(false);

        loadGist(false);
        loadStarredState(false);
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.gist_title, mGistId);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mGistId = extras.getString("id");
    }

    @Override
    public void onRefresh() {
        mGist = null;
        mIsStarred = null;
        setContentShown(false);
        loadGist(true);
        loadStarredState(true);
        super.onRefresh();
    }

    private void fillData(final Gist gist) {
        mGist = gist;

        if (gist.owner() != null) {
            getSupportActionBar().setSubtitle(gist.owner().login());
        }

        TextView tvDesc = findViewById(R.id.tv_desc);
        tvDesc.setText(TextUtils.isEmpty(gist.description())
                ? getString(R.string.gist_no_description) : gist.description());

        TextView tvCreatedAt = findViewById(R.id.tv_created_at);
        tvCreatedAt.setText(StringUtils.formatRelativeTime(this, gist.createdAt(), true));

        Map<String, GistFile> files = gist.files();
        if (files != null && !files.isEmpty()) {
            ViewGroup container = findViewById(R.id.file_container);
            LayoutInflater inflater = getLayoutInflater();

            container.removeAllViews();
            for (GistFile gistFile : files.values()) {
                TextView rowView = (TextView) inflater.inflate(R.layout.selectable_label,
                        container, false);

                rowView.setText(gistFile.filename());
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
        startActivity(GistViewerActivity.makeIntent(this, mGistId, file.filename()));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.id.star, 0, R.string.repo_star_action)
                .setIcon(R.drawable.star)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (mGist != null) {
            menu.add(0, R.id.share, 0, R.string.share)
                    .setIcon(R.drawable.social_share)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
                starAction.setActionView(R.layout.ab_loading);
                starAction.expandActionView();
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
                String login = ApiHelpers.getUserLogin(this, mGist.owner());
                IntentUtils.share(this, getString(R.string.share_gist_subject, mGistId, login),
                        mGist.htmlUrl());
                return true;
            case R.id.star:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                updateStarringState();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Intent navigateUp() {
        String login = mGist != null && mGist.owner() != null
                ? mGist.owner().login() : null;
        return login != null ? GistListActivity.makeIntent(this, login) : null;
    }

    private void updateStarringState() {
        GistService service = ServiceFactory.get(GistService.class);
        Single<Response<Void>> responseSingle = mIsStarred
                ? service.unstarGist(mGistId) : service.starGist(mGistId);
        responseSingle.map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    mIsStarred = !mIsStarred;
                    supportInvalidateOptionsMenu();
                }, error -> supportInvalidateOptionsMenu());
    }

    private void loadGist(boolean force) {
        GistService service = ServiceFactory.get(GistService.class);
        service.getGist(mGistId)
                .map(ApiHelpers::throwOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_GIST, force))
                .subscribe(result -> {
                    fillData(result);
                    setContentShown(true);
                    supportInvalidateOptionsMenu();
                }, error -> {});
    }

    private void loadStarredState(boolean force) {
        GistService service = ServiceFactory.get(GistService.class);
        service.checkIfGistIsStarred(mGistId)
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_STARRED, force))
                .subscribe(result -> {
                    mIsStarred = result;
                    supportInvalidateOptionsMenu();
                }, error -> {});
    }
}