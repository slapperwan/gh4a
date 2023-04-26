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

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.adapter.RepositoryIssueAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ActivityResultHelpers;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.search.SearchService;

import io.reactivex.Single;
import retrofit2.Response;

public class IssueListFragment extends PagedDataBaseFragment<Issue> {
    private String mQuery;
    private String mSortMode;
    private String mOrder;
    private int mEmptyTextResId;
    private boolean mShowRepository;
    private String mIssueState;

    private final ActivityResultLauncher<Intent> mIssueLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> super.onRefresh())
    );

    public static IssueListFragment newInstance(String query, String sortMode, String order,
            String state, int emptyTextResId, boolean showRepository) {
        IssueListFragment f = new IssueListFragment();

        Bundle args = new Bundle();
        args.putString("query", query);
        args.putString("sortmode", sortMode);
        args.putString("order", order);
        args.putInt("emptytext", emptyTextResId);
        args.putString("state", state);
        args.putBoolean("withrepo", showRepository);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mQuery = args.getString("query");
        mSortMode = args.getString("sortmode");
        mOrder = args.getString("order");
        mEmptyTextResId = args.getInt("emptytext");
        mIssueState = args.getString("state");
        mShowRepository = args.getBoolean("withrepo");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /** TODO
         * switch (mIssueState) {
         *             case ApiHelpers.IssueState.CLOSED:
         *                 setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
         *                 break;
         *             case ApiHelpers.IssueState.MERGED:
         *                 setHighlightColors(R.attr.colorPullRequestMerged,
         *                         R.attr.colorPullRequestMergedDark);
         *                 break;
         *             default:
         *                 setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
         *                 break;
         *         }
         */
    }

    @Override
    public void onItemClick(Issue issue) {
        Intent intent = issue.pullRequest() != null
                ? PullRequestActivity.makeIntent(getActivity(), issue)
                : IssueActivity.makeIntent(getActivity(), issue);
        mIssueLauncher.launch(intent);
    }

    @Override
    protected RootAdapter<Issue, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return mShowRepository
                ? new RepositoryIssueAdapter(getActivity())
                : new IssueAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return mEmptyTextResId;
    }

    @Override
    protected Single<Response<Page<Issue>>> loadPage(int page, boolean bypassCache) {
        final SearchService service = ServiceFactory.get(SearchService.class, bypassCache);
        return service.searchIssues(mQuery, mSortMode, mOrder, page)
                .compose(RxUtils::searchPageAdapter);
    }

    public static class SortDrawerHelper {
        private String mSortMode = "created";
        private boolean mSortAscending = false;

        private static final SparseArray<String[]> SORT_LOOKUP = new SparseArray<>();
        static {
            SORT_LOOKUP.put(R.id.sort_created_asc, new String[] { "created", "asc" });
            SORT_LOOKUP.put(R.id.sort_created_desc, new String[] { "created", "desc" });
            SORT_LOOKUP.put(R.id.sort_updated_asc, new String[] { "updated", "asc" });
            SORT_LOOKUP.put(R.id.sort_updated_desc, new String[] { "updated", "desc" });
            SORT_LOOKUP.put(R.id.sort_comments_asc, new String[] { "comments", "asc" });
            SORT_LOOKUP.put(R.id.sort_comments_desc, new String[] { "comments", "desc" });
        }

        public static int getMenuResId() {
            return R.menu.issue_list_sort;
        }

        public String getSortMode() {
            return mSortMode;
        }

        public String getSortOrder() {
            return mSortAscending ? "asc" : "desc";
        }

        public void setSortMode(String mode, String order) {
            if (findEntryIndex(mode, order) >= 0) {
                updateSortMode(mode, TextUtils.equals(order, "asc"));
            }
        }

        public void updateMenuCheckState(Menu menu) {
            int index = findEntryIndex(getSortMode(), getSortOrder());
            if (index >= 0) {
                menu.findItem(SORT_LOOKUP.keyAt(index)).setChecked(true);
            }
        }

        public boolean handleItemSelection(MenuItem item) {
            String[] value = SORT_LOOKUP.get(item.getItemId());
            if (value == null) {
                return false;
            }

            updateSortMode(value[0], TextUtils.equals(value[1], "asc"));
            return true;
        }

        protected void updateSortMode(String sortMode, boolean ascending) {
            mSortAscending = ascending;
            mSortMode = sortMode;
        }

        private int findEntryIndex(String mode, String order) {
            for (int i = 0; i < SORT_LOOKUP.size(); i++) {
                String[] value = SORT_LOOKUP.valueAt(i);
                if (TextUtils.equals(mode, value[0]) && TextUtils.equals(order, value[1])) {
                    return i;
                }
            }
            return -1;
        }
    }
}