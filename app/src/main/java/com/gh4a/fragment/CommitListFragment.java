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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.CommitActivity;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ActivityResultHelpers;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.StringUtils;
import com.gh4a.widget.ContextMenuAwareRecyclerView;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.ContentType;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;

import io.reactivex.Maybe;
import io.reactivex.Single;
import retrofit2.Response;

public class CommitListFragment extends PagedDataBaseFragment<Commit> implements MenuProvider {
    public interface ContextSelectionCallback {
        boolean baseSelectionAllowed();
        void onCommitSelectedAsBase(Commit commit);
    }

    private String mRepoOwner;
    private String mRepoName;
    private String mRef;
    private String mFilePath;
    private ContentType mFileType;
    private boolean mFollowRenames;
    private FileRenameFollowData mMostRecentRenameFollowData;
    private Commit mOldestLoadedCommit;

    private CommitAdapter mAdapter;
    private ContextSelectionCallback mCallback;

    private final ActivityResultLauncher<Intent> mCommitLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> onRefresh())
    );

    public static CommitListFragment newInstance(Repository repo, String ref) {
        return newInstance(repo.owner().login(), repo.name(),
                StringUtils.isBlank(ref) ? repo.defaultBranch() : ref, null, null);
    }

    public static CommitListFragment newInstance(String repoOwner, String repoName,
            String ref, String filePath, ContentType type) {
        CommitListFragment f = new CommitListFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putString("ref", ref);
        args.putString("path", filePath);
        args.putSerializable("type", type);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
        mRef = getArguments().getString("ref");
        mFilePath = getArguments().getString("path");
        mFileType = (ContentType) getArguments().getSerializable("type");
        if (savedInstanceState != null) {
            mFollowRenames = savedInstanceState.getBoolean("follow_renames");
            mMostRecentRenameFollowData = savedInstanceState.getParcelable("rename_follow_data");
            mOldestLoadedCommit = savedInstanceState.getParcelable("oldest_commit");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("follow_renames", mFollowRenames);
        outState.putParcelable("rename_follow_data", mMostRecentRenameFollowData);
        outState.putParcelable("oldest_commit", mOldestLoadedCommit);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (mFileType == ContentType.File || mFileType == ContentType.Submodule) {
            inflater.inflate(R.menu.commit_history_menu, menu);
        }
    }

    @Override
    public void onPrepareMenu(Menu menu) {
        MenuItem followItem = menu.findItem(R.id.follow_renames);
        if (followItem != null) {
            followItem.setChecked(mFollowRenames);
        }
    }

    @Override
    public boolean onMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.follow_renames) {
            mFollowRenames = !mFollowRenames;
            item.setChecked(mFollowRenames);
            onRefresh();
            return true;
        }

        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = context instanceof ContextSelectionCallback
                ? (ContextSelectionCallback) context : null;
        requireActivity().addMenuProvider(this);
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
        mCommitLauncher.launch(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, R.id.select_as_branch_ref, Menu.NONE, R.string.commit_use_as_ref);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuAwareRecyclerView.RecyclerContextMenuInfo info =
                (ContextMenuAwareRecyclerView.RecyclerContextMenuInfo) item.getMenuInfo();
        if (info.position >= mAdapter.getItemCount()) {
            return false;
        }

        if (item.getItemId() == R.id.select_as_branch_ref) {
            Commit commit = mAdapter.getItemFromAdapterPosition(info.position);
            mCallback.onCommitSelectedAsBase(commit);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected Single<Response<Page<Commit>>> loadPage(int page, boolean bypassCache) {
        final RepositoryCommitService service =
                ServiceFactory.get(RepositoryCommitService.class, bypassCache);
        final String ref = mMostRecentRenameFollowData != null ? mMostRecentRenameFollowData.commitSha : mRef;
        final String filePath = mMostRecentRenameFollowData != null ? mMostRecentRenameFollowData.fileName : mFilePath;

        return service.getCommits(mRepoOwner, mRepoName, ref, filePath, page)
                .flatMap(response -> {
                    Page<Commit> commits = response.isSuccessful() ? response.body() : null;
                    if (commits != null) {
                        final List<Commit> items = commits.items();
                        // The last page may be an empty one, so memorize the oldest commit for
                        // tracking renames in that case
                        if (!items.isEmpty()) {
                            mOldestLoadedCommit = items.get(items.size() - 1);
                        }
                        if (commits.next() == null && mFollowRenames && mOldestLoadedCommit != null) {
                            return determineFollowDataIfRenamedInCommit(mOldestLoadedCommit, filePath, service)
                                    .map(data -> {
                                        mMostRecentRenameFollowData = data;
                                        return Response.success(Page.<Commit>builder()
                                                .items(items)
                                                .next(1)
                                                .prev(commits.prev())
                                                .build());
                                    })
                                    .defaultIfEmpty(response)
                                    .toSingle();
                        }
                    }
                    return Single.just(response);
                })
                .map(response -> {
                    // 409 is returned for empty repos
                    if (response.code() == HttpURLConnection.HTTP_CONFLICT) {
                        return Response.success(new ApiHelpers.DummyPage<>());
                    }
                    return response;
                });
    }

    @Override
    protected void resetSubject() {
        super.resetSubject();
        mMostRecentRenameFollowData = null;
        mOldestLoadedCommit = null;
    }

    private Maybe<FileRenameFollowData> determineFollowDataIfRenamedInCommit(
            Commit commit, String currentFileName, RepositoryCommitService service) {
        return service.getCommit(mRepoOwner, mRepoName, commit.sha())
                .filter(Response::isSuccessful)
                .map(Response::body)
                .map(actualCommit -> {
                    List<GitHubFile> files = actualCommit != null ? actualCommit.files() : null;
                    List<Commit> parents = actualCommit != null ? actualCommit.parents() : null;
                    if (files == null || parents == null || parents.isEmpty()) {
                        return Optional.<FileRenameFollowData>empty();
                    }
                    return files.stream()
                            .filter(f -> currentFileName.equals(f.filename()) && f.previousFilename() != null)
                            .findFirst()
                            .map(f -> new FileRenameFollowData(parents.get(0).sha(), f.previousFilename()));
                })
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private static class FileRenameFollowData implements Parcelable {
        final String commitSha;
        final String fileName;

        public FileRenameFollowData(String sha, String fileName) {
            this.commitSha = sha;
            this.fileName = fileName;
        }

        private FileRenameFollowData(Parcel in) {
            commitSha = in.readString();
            fileName = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(commitSha);
            parcel.writeString(fileName);
        }

        public static Parcelable.Creator<FileRenameFollowData> CREATOR = new Parcelable.Creator<>() {
            @Override
            public FileRenameFollowData createFromParcel(Parcel parcel) {
                return new FileRenameFollowData(parcel);
            }

            @Override
            public FileRenameFollowData[] newArray(int size) {
                return new FileRenameFollowData[size];
            }
        };
    }
}