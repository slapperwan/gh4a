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
package com.gh4a.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.CommitActivity;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.StringUtils;
import com.gh4a.widget.ContextMenuAwareRecyclerView;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

import java.net.HttpURLConnection;

import io.reactivex.Single;
import retrofit2.Response;

public class CommitListFragment extends PagedDataBaseFragment<Commit> {
    public interface ContextSelectionCallback {
        boolean baseSelectionAllowed();
        void onCommitSelectedAsBase(Commit commit);
    }

    private static final int REQUEST_COMMIT = 2000;
    private static final int MENU_SELECT_AS_BASE = Menu.FIRST + 2;
    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_REF = "ref";
    private static final String EXTRA_PATH = "path";

    private String mRepoOwner;
    private String mRepoName;
    private String mRef;
    private String mFilePath;

    private CommitAdapter mAdapter;
    private ContextSelectionCallback mCallback;

    public static CommitListFragment newInstance(Repository repo, String ref) {
        return newInstance(repo.owner().login(), repo.name(),
                StringUtils.isBlank(ref) ? repo.defaultBranch() : ref, null);
    }

    public static CommitListFragment newInstance(String repoOwner, String repoName,
            String ref, String filePath) {
        CommitListFragment f = new CommitListFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_OWNER, repoOwner);
        args.putString(EXTRA_REPO, repoName);
        args.putString(EXTRA_REF, ref);
        args.putString(EXTRA_PATH, filePath);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(EXTRA_OWNER);
        mRepoName = getArguments().getString(EXTRA_REPO);
        mRef = getArguments().getString(EXTRA_REF);
        mFilePath = getArguments().getString(EXTRA_PATH);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = context instanceof ContextSelectionCallback
                ? (ContextSelectionCallback) context : null;
    }

    @Override
    protected RootAdapter<Commit, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new CommitAdapter(getActivity());
        mAdapter.setContextMenuSupported(mCallback != null && mCallback.baseSelectionAllowed());
        return mAdapter;
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        registerForContextMenu(view);
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_commits_found;
    }

    @Override
    public void onItemClick(Commit commit) {
        String[] urlPart = commit.url().split("/");
        Intent intent = CommitActivity.makeIntent(getActivity(),
                urlPart[4], urlPart[5], commit.sha());
        startActivityForResult(intent, REQUEST_COMMIT);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, MENU_SELECT_AS_BASE, Menu.NONE, R.string.commit_use_as_ref);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuAwareRecyclerView.RecyclerContextMenuInfo info =
                (ContextMenuAwareRecyclerView.RecyclerContextMenuInfo) item.getMenuInfo();
        if (info.position >= mAdapter.getItemCount()) {
            return false;
        }

        if (item.getItemId() == MENU_SELECT_AS_BASE) {
            Commit commit = mAdapter.getItemFromAdapterPosition(info.position);
            mCallback.onCommitSelectedAsBase(commit);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COMMIT) {
            if (resultCode == Activity.RESULT_OK) {
                // comments were updated
                onRefresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected Single<Response<Page<Commit>>> loadPage(int page, boolean bypassCache) {
        final RepositoryCommitService service =
                ServiceFactory.get(RepositoryCommitService.class, bypassCache);
        return service.getCommits(mRepoOwner, mRepoName, mRef, mFilePath, page)
                .map(response -> {
                    // 409 is returned for empty repos
                    if (response.code() == HttpURLConnection.HTTP_CONFLICT) {
                        return Response.success(new ApiHelpers.DummyPage<>());
                    }
                    return response;
                });
    }
}