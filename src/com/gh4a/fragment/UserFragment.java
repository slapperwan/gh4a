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
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devspark.progressfragment.ProgressFragment;
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
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

public class UserFragment extends ProgressFragment implements  OnClickListener {
    private String mUserLogin;
    private String mUserName;
    private User mUser;
    private View mContentView;

    
    private LoaderCallbacks<User> mUserCallback = new LoaderCallbacks<User>() {
        @Override
        public Loader<LoaderResult<User>> onCreateLoader(int id, Bundle args) {
            return new UserLoader(getActivity(), mUserLogin);
        }
        @Override
        public void onResultReady(LoaderResult<User> result) {
            boolean success = !result.handleError(getActivity());
            if (success) {
                mUser = (User) result.getData();
                fillData();
            }
            setContentEmpty(!success);
            setContentShown(true);
            getActivity().invalidateOptionsMenu();
        }
    };

    private LoaderCallbacks<List<Repository>> mRepoListCallback = new LoaderCallbacks<List<Repository>>() {
        @Override
        public Loader<LoaderResult<List<Repository>>> onCreateLoader(int id, Bundle args) {
            Map<String, String> filterData = new HashMap<String, String>();
            filterData.put("sort", "pushed");
            return new RepositoryListLoader(getActivity(), mUserLogin,
                    mUser.getType(), filterData, 5);
        }
        @Override
        public void onResultReady(LoaderResult<List<Repository>> result) {
            getView().findViewById(R.id.pb_top_repos).setVisibility(View.GONE);
            if (!result.handleError(getActivity())) {
                fillTopRepos(result.getData());
                getView().findViewById(R.id.ll_top_repos).setVisibility(View.VISIBLE);
            }
        }
    };

    private LoaderCallbacks<List<User>> mOrganizationCallback = new LoaderCallbacks<List<User>>() {
        @Override
        public Loader<LoaderResult<List<User>>> onCreateLoader(int id, Bundle args) {
            return new OrganizationListLoader(getActivity(), mUserLogin);
        }
        @Override
        public void onResultReady(LoaderResult<List<User>> result) {
            fillOrganizations(result.getData());
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
        mContentView = inflater.inflate(R.layout.user, null);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentView(mContentView);
        setContentShown(false);

        getLoaderManager().initLoader(0, null, mUserCallback);
    }

    public void refresh() {
        setContentShown(false);
        getLoaderManager().restartLoader(0, null, mUserCallback);
    }

    private void fillData() {
        Gh4Application app = (Gh4Application) getActivity().getApplication();

        UiUtils.assignTypeface(mContentView, app.boldCondensed, new int[] {
            R.id.tv_name, R.id.tv_followers_count, R.id.tv_members_count,
            R.id.tv_repos_count, R.id.tv_gists_count, R.id.tv_following_count,
            R.id.tv_pub_repos_label, R.id.tv_orgs
        });
        UiUtils.assignTypeface(mContentView, app.regular, new int[] {
            R.id.tv_created_at, R.id.tv_email, R.id.tv_website,
            R.id.tv_company, R.id.tv_location
        });

        ImageView gravatar = (ImageView) mContentView.findViewById(R.id.iv_gravatar);
        GravatarHandler.assignGravatar(gravatar, mUser);

        TextView tvFollowersCount = (TextView) mContentView.findViewById(R.id.tv_followers_count);
        tvFollowersCount.setText(String.valueOf(mUser.getFollowers()));
        
        View llOrgMembers = mContentView.findViewById(R.id.cell_org_members);
        View llFollowers = mContentView.findViewById(R.id.cell_followers);
        
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            llFollowers.setOnClickListener(this);
            llOrgMembers.setVisibility(View.GONE);
        } else {
            llOrgMembers.setOnClickListener(this);
            llFollowers.setVisibility(View.GONE);
        }
        
        mContentView.findViewById(R.id.cell_repos).setOnClickListener(this);
        
        TextView tvReposCount = (TextView) mContentView.findViewById(R.id.tv_repos_count);
        if (mUserLogin.equals(Gh4Application.get(getActivity()).getAuthLogin())) {
            tvReposCount.setText(String.valueOf(mUser.getTotalPrivateRepos() + mUser.getPublicRepos()));    
        } else {
            tvReposCount.setText(String.valueOf(mUser.getPublicRepos()));
        }
        
        //hide gists repos if organization
        fillCountIfUser(R.id.cell_gists, R.id.tv_gists_count, mUser.getPublicGists());
        //hide following if organization
        fillCountIfUser(R.id.cell_following, R.id.tv_following_count, mUser.getFollowing());

        TextView tvName = (TextView) mContentView.findViewById(R.id.tv_name);
        tvName.setText(StringUtils.isBlank(mUser.getName()) ? mUser.getLogin() : mUser.getName());
        if (Constants.User.USER_TYPE_ORG.equals(mUser.getType())) {
            tvName.append(" (");
            tvName.append(Constants.User.USER_TYPE_ORG);
            tvName.append(")");
        }

        TextView tvCreated = (TextView) mContentView.findViewById(R.id.tv_created_at);
        if (mUser.getCreatedAt() != null) {
            tvCreated.setText(getString(R.string.user_created_at,
                    DateFormat.getMediumDateFormat(getActivity()).format(mUser.getCreatedAt())));
            tvCreated.setVisibility(View.VISIBLE);
        } else {
            tvCreated.setVisibility(View.GONE);
        }

        fillTextView(R.id.tv_email, mUser.getEmail());
        fillTextView(R.id.tv_website, mUser.getBlog());
        fillTextView(R.id.tv_company, mUser.getCompany());
        fillTextView(R.id.tv_location, mUser.getLocation());
        
        getLoaderManager().initLoader(1, null, mRepoListCallback);
        getLoaderManager().initLoader(2, null, mOrganizationCallback);
    }

    private void fillCountIfUser(int layoutId, int countId, int count) {
        View layout = mContentView.findViewById(layoutId);
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            TextView countView = (TextView) mContentView.findViewById(countId);
            countView.setText(String.valueOf(count));
            layout.setOnClickListener(this);
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void fillTextView(int id, String text) {
        TextView view = (TextView) mContentView.findViewById(id);
        if (!StringUtils.isBlank(text)) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @Override
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
            Gh4Application app = Gh4Application.get(getActivity());
            app.openRepositoryInfoActivity(getActivity(), (Repository) view.getTag());
        } else if (view.getTag() instanceof User) {
            Gh4Application app = Gh4Application.get(getActivity());
            User user = (User) view.getTag();
            app.openUserInfoActivity(getActivity(), user.getLogin(), null);
        }
        if (intent != null) {
            intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
            intent.putExtra(Constants.User.USER_NAME, mUserName);
            intent.putExtra(Constants.User.USER_TYPE, mUser.getType());
            startActivity(intent);
        }
    }

    public void fillTopRepos(List<Repository> topRepos) {
        Gh4Application app = Gh4Application.get(getActivity());
        
        LinearLayout ll = (LinearLayout) mContentView.findViewById(R.id.ll_top_repos);
        ll.removeAllViews();
        
        int count = topRepos != null ? topRepos.size() : 0;
        LayoutInflater inflater = getLayoutInflater(null);

        for (int i = 0; i < count; i++) {
            final Repository repo = topRepos.get(i); 
            View rowView = inflater.inflate(R.layout.top_repo, null);
            rowView.setOnClickListener(this);
            rowView.setTag(repo);

            TextView tvTitle = (TextView) rowView.findViewById(R.id.tv_title);
            tvTitle.setTypeface(app.boldCondensed);
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
            btnMore.setVisibility(View.VISIBLE);
        } else {
            TextView noRepos = new TextView(getActivity());
            noRepos.setText(R.string.no_repos_found);
            ll.addView(noRepos);
        }
    }
    
    public void fillOrganizations(List<User> organizations) {
        Gh4Application app = Gh4Application.get(getActivity());
        LinearLayout llOrgs = (LinearLayout) mContentView.findViewById(R.id.ll_orgs);
        LinearLayout llOrg = (LinearLayout) mContentView.findViewById(R.id.ll_org);
        int count = organizations != null ? organizations.size() : 0;
        LayoutInflater inflater = getLayoutInflater(null);

        llOrg.removeAllViews();
        llOrgs.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < count; i++) {
            User org = organizations.get(i);
            View rowView = inflater.inflate(R.layout.selectable_label, null);
            
            rowView.setOnClickListener(this);
            rowView.setTag(org);
                
            TextView tvTitle = (TextView) rowView.findViewById(R.id.tv_title);
            tvTitle.setTypeface(app.boldCondensed);
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
        TextView tvFollowersCount = (TextView) mContentView.findViewById(R.id.tv_followers_count);
        tvFollowersCount.setText(String.valueOf(mUser.getFollowers()));
    }
}