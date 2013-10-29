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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.FollowerFollowingListActivity;
import com.gh4a.activities.GistListActivity;
import com.gh4a.activities.OrganizationMemberListActivity;
import com.gh4a.activities.RepositoryListActivity;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.OrganizationListLoader;
import com.gh4a.loader.RepositoryListLoader;
import com.gh4a.loader.UserLoader;
import com.gh4a.utils.GravatarUtils;
import com.gh4a.utils.StringUtils;

public class UserFragment extends BaseFragment implements  OnClickListener {
    private String mUserLogin;
    private String mUserName;
    private User mUser;
    private List<Repository> mTopRepos;
    private List<User> mOrgs;

    private LoaderCallbacks<User> mUserCallback = new LoaderCallbacks<User>() {
        @Override
        public Loader<LoaderResult<User>> onCreateLoader(int id, Bundle args) {
            return new UserLoader(getSherlockActivity(), mUserLogin);
        }
        @Override
        public void onResultReady(LoaderResult<User> result) {
            if (!checkForError(result)) {
                hideLoading();
                mUser = (User) result.getData();
                fillData();
            }
        }
    };

    private LoaderCallbacks<List<Repository>> mRepoListCallback = new LoaderCallbacks<List<Repository>>() {
        @Override
        public Loader<LoaderResult<List<Repository>>> onCreateLoader(int id, Bundle args) {
            Map<String, String> filterData = new HashMap<String, String>();
            filterData.put("sort", "pushed");
            return new RepositoryListLoader(getSherlockActivity(), mUserLogin,
                    mUser.getType(), filterData, 5);
        }
        @Override
        public void onResultReady(LoaderResult<List<Repository>> result) {
            if (!checkForError(result)) {
                hideLoading(R.id.pb_top_repos, 0);
                mTopRepos = (List<Repository>) result.getData();
                fillTopRepos();
            }
        }
    };

    private LoaderCallbacks<List<User>> mOrganizationCallback = new LoaderCallbacks<List<User>>() {
        @Override
        public Loader<LoaderResult<List<User>>> onCreateLoader(int id, Bundle args) {
            return new OrganizationListLoader(getSherlockActivity(), mUserLogin);
        }
        @Override
        public void onResultReady(LoaderResult<List<User>> result) {
            mOrgs = result.getData();
            fillOrganizations();
        }
    };

    public static UserFragment newInstance(String login, String name) {
        UserFragment f = new UserFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putString(Constants.User.USER_NAME, name);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mUserName = getArguments().getString(Constants.User.USER_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.user, container, false);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        showLoading();
        getLoaderManager().initLoader(0, null, mUserCallback);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void refresh() {
        getLoaderManager().restartLoader(0, null, mUserCallback);
    }

    private void fillData() {
        View v = getView();
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        Typeface boldCondensed = app.boldCondensed;
        Typeface regular = app.regular;
        
        AQuery aq = new AQuery(getSherlockActivity());
        aq.id(R.id.iv_gravatar).image(GravatarUtils.getGravatarUrl(mUser.getGravatarId()), 
                true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), AQuery.FADE_IN);

        TextView tvName = (TextView) v.findViewById(R.id.tv_name);
        tvName.setTypeface(boldCondensed);
        
        TextView tvCreated = (TextView) v.findViewById(R.id.tv_created_at);
        tvCreated.setTypeface(regular);
        
        TextView tvFollowersCount = (TextView) v.findViewById(R.id.tv_followers_count);
        tvFollowersCount.setTypeface(boldCondensed);
        tvFollowersCount.setText(String.valueOf(mUser.getFollowers()));
        
        TableLayout tlOrgMembers = (TableLayout) v.findViewById(R.id.cell_org_members);
        TableLayout tlFollowers = (TableLayout) v.findViewById(R.id.cell_followers);
        
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            tlFollowers.setOnClickListener(this);
            
            TextView tvFollowers = (TextView) v.findViewById(R.id.tv_followers_label);
            tvFollowers.setText(R.string.user_followers);
            tlFollowers.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            
            tlOrgMembers.setVisibility(View.GONE);
        } else {
            TextView tvMemberCount = (TextView) v.findViewById(R.id.tv_members_count);
            tvMemberCount.setTypeface(boldCondensed);

            tlOrgMembers.setOnClickListener(this);
            tlOrgMembers.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            
            tlFollowers.setVisibility(View.GONE);
        }
        
        TableLayout tlRepos = (TableLayout) v.findViewById(R.id.cell_repos);
        tlRepos.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        tlRepos.setOnClickListener(this);
        
        TextView tvReposCount = (TextView) v.findViewById(R.id.tv_repos_count);
        tvReposCount.setTypeface(boldCondensed);
        
        if (mUserLogin.equals(Gh4Application.get(getActivity()).getAuthLogin())) {
            tvReposCount.setText(String.valueOf(mUser.getTotalPrivateRepos() + mUser.getPublicRepos()));    
        } else {
            tvReposCount.setText(String.valueOf(mUser.getPublicRepos()));
        }
        
        //hide gists repos if organization
        fillCountIfUser(v, R.id.cell_gists, R.id.tv_gists_count, mUser.getPublicGists(), app);
        //hide following if organization
        fillCountIfUser(v, R.id.cell_following, R.id.tv_following_count, mUser.getFollowing(), app);
        
        tvName.setText(StringUtils.isBlank(mUser.getName()) ? mUser.getLogin() : mUser.getName());
        if (Constants.User.USER_TYPE_ORG.equals(mUser.getType())) {
            tvName.append(" (");
            tvName.append(Constants.User.USER_TYPE_ORG);
            tvName.append(")");
        }

        if (mUser.getCreatedAt() != null) {
            tvCreated.setText(getString(R.string.user_created_at,
                    DateFormat.getMediumDateFormat(getActivity()).format(mUser.getCreatedAt())));
            tvCreated.setVisibility(View.VISIBLE);
        } else {
            tvCreated.setVisibility(View.GONE);
        }

        fillTextView(v, R.id.tv_email, mUser.getEmail(), app);
        fillTextView(v, R.id.tv_website, mUser.getBlog(), app);
        fillTextView(v, R.id.tv_company, mUser.getCompany(), app);
        fillTextView(v, R.id.tv_location, mUser.getLocation(), app);
        
        TextView tvPubRepo = (TextView) v.findViewById(R.id.tv_pub_repos_label);
        tvPubRepo.setTypeface(boldCondensed);
        tvPubRepo.setTextColor(getResources().getColor(R.color.highlight));
        
        TextView tvOrgs = (TextView) v.findViewById(R.id.tv_orgs);
        tvOrgs.setTypeface(boldCondensed);
        tvOrgs.setTextColor(getResources().getColor(R.color.highlight));
        
        getLoaderManager().initLoader(1, null, mRepoListCallback);
        getLoaderManager().initLoader(2, null, mOrganizationCallback);
        getSherlockActivity().invalidateOptionsMenu();
    }

    private void fillCountIfUser(View parent, int layoutId, int countId, int count, Gh4Application app) {
        TableLayout layout = (TableLayout) parent.findViewById(layoutId);
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            TextView countView = (TextView) parent.findViewById(countId);
            countView.setTypeface(app.boldCondensed);
            countView.setText(String.valueOf(count));
        
            layout.setOnClickListener(this);
            layout.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void fillTextView(View parent, int id, String text, Gh4Application app) {
        TextView view = (TextView) parent.findViewById(id);
        if (!StringUtils.isBlank(text)) {
            view.setTypeface(app.regular);
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View view) {
        int id = view.getId();
        Intent intent = null;

        if (id == R.id.cell_followers) {
            if (Constants.User.USER_TYPE_ORG.equals(mUser.getType())) {
                intent = new Intent(getActivity(), OrganizationMemberListActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            } else {
                intent = new Intent(getActivity(), FollowerFollowingListActivity.class);
                intent.putExtra("FIND_FOLLOWERS", true);
                startActivity(intent);
            }
        } else if (id == R.id.cell_following) {
            intent = new Intent(getActivity(), FollowerFollowingListActivity.class);
            intent.putExtra("FIND_FOLLOWERS", false);
        } else if (id == R.id.cell_repos || id == R.id.btn_repos) {
            intent = new Intent(getActivity(), RepositoryListActivity.class);
        } else if (id == R.id.cell_gists) {
            intent = new Intent(getActivity(), GistListActivity.class);
        } else if (id == R.id.cell_org_members) {
            intent = new Intent(getActivity(), OrganizationMemberListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        } else if (view.getTag() instanceof Repository) {
            Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
            app.openRepositoryInfoActivity(getSherlockActivity(), (Repository) view.getTag());
        } else if (view.getTag() instanceof User) {
            Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
            User user = (User) view.getTag();
            app.openUserInfoActivity(getSherlockActivity(), user.getLogin(), null);
        }
        if (intent != null) {
            intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
            intent.putExtra(Constants.User.USER_NAME, mUserName);
            intent.putExtra(Constants.User.USER_TYPE, mUser.getType());
            startActivity(intent);
        }
    }

    public void fillTopRepos() {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        Typeface boldCondensed = app.boldCondensed;
        
        View v = getView();
        LinearLayout ll = (LinearLayout) v.findViewById(R.id.ll_top_repos);
        ll.removeAllViews();
        
        int count = mTopRepos != null ? mTopRepos.size() : 0;
        LayoutInflater inflater = getLayoutInflater(null);
        int padding = getResources().getDimensionPixelSize(R.dimen.org_member_list_padding);

        for (int i = 0; i < count; i++) {
            final Repository repo = mTopRepos.get(i); 
            View rowView = inflater.inflate(R.layout.row_simple_3, null);
            rowView.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            rowView.setPadding(0, padding, 0, padding);
            rowView.setOnClickListener(this);
            rowView.setTag(repo);

            TextView tvTitle = (TextView) rowView.findViewById(R.id.tv_title);
            tvTitle.setTypeface(boldCondensed);
            tvTitle.setText(repo.getOwner().getLogin() + "/" + repo.getName());

            TextView tvDesc = (TextView) rowView.findViewById(R.id.tv_desc);
            tvDesc.setSingleLine(true);
            if (!StringUtils.isBlank(repo.getDescription())) {
                tvDesc.setVisibility(View.VISIBLE);
                tvDesc.setText(repo.getDescription());
            } else {
                tvDesc.setVisibility(View.GONE);
            }

            TextView tvExtra = (TextView) rowView.findViewById(R.id.tv_extra);
            String language = repo.getLanguage() != null
                    ? repo.getLanguage() : getString(R.string.unknown);
            tvExtra.setText(getString(R.string.repo_search_extradata, language,
                    StringUtils.toHumanReadbleFormat(repo.getSize()),
                    repo.getForks(), repo.getWatchers()));

            ll.addView(rowView);
        }

        Button btnMore = (Button) getView().findViewById(R.id.btn_repos);
        if (count > 0) {
            btnMore.setOnClickListener(this);
        } else {
            btnMore.setVisibility(View.GONE);
            TextView noRepos = new TextView(getSherlockActivity());
            noRepos.setText(R.string.no_repos_found);
            ll.addView(noRepos);
        }
    }
    
    public void fillOrganizations() {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        Typeface boldCondensed = app.boldCondensed;
        
        View v = getView();
        LinearLayout llOrgs = (LinearLayout) v.findViewById(R.id.ll_orgs);
        LinearLayout llOrg = (LinearLayout) v.findViewById(R.id.ll_org);
        int count = mOrgs != null ? mOrgs.size() : 0;
        LayoutInflater inflater = getLayoutInflater(null);
        int padding = getResources().getDimensionPixelSize(R.dimen.top_repo_list_padding);

        llOrg.removeAllViews();
        llOrgs.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < count; i++) {
            User org = mOrgs.get(i);
            View rowView = inflater.inflate(R.layout.row_simple, null);
            
            rowView.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            rowView.setPadding(0, padding, 0, padding);
            rowView.setOnClickListener(this);
            rowView.setTag(org);
                
            TextView tvTitle = (TextView) rowView.findViewById(R.id.tv_title);
            tvTitle.setTypeface(boldCondensed);
            tvTitle.setText(org.getLogin());

            llOrg.addView(rowView);
        }
    }

    public void updateFollowingAction(boolean following) {
        if (following) {
            mUser.setFollowers(mUser.getFollowers() + 1);
        } else {
            mUser.setFollowers(mUser.getFollowers() - 1);
        }
        TextView tvFollowersCount = (TextView) getView().findViewById(R.id.tv_followers_count);
        tvFollowersCount.setText(String.valueOf(mUser.getFollowers()));
    }
    
    private boolean checkForError(LoaderResult<?> result) {
        if (result.handleError(getActivity())) {
            hideLoading(R.id.pb_top_repos, 0);
            hideLoading();
            return true;
        }
        return false;
    }
}