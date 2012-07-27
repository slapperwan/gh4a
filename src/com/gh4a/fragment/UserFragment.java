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
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Constants;
import com.gh4a.FollowerFollowingListActivity;
import com.gh4a.Gh4Application;
import com.gh4a.GistListActivity;
import com.gh4a.LoadingDialog;
import com.gh4a.OrganizationMemberListActivity;
import com.gh4a.R;
import com.gh4a.RepositoryListActivity;
import com.gh4a.UserActivity;
import com.gh4a.loader.FollowUserLoader;
import com.gh4a.loader.IsFollowingUserLoader;
import com.gh4a.loader.OrganizationListLoader;
import com.gh4a.loader.RepositoryListLoader;
import com.gh4a.loader.UserLoader;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;

public class UserFragment extends BaseFragment implements 
    OnClickListener, LoaderManager.LoaderCallbacks<Object> {

    private String mUserLogin;
    private String mUserName;
    private User mUser;
    private boolean isFollowing;
    private LoadingDialog mLoadingDialog;

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
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onCreate UserFragment");
        super.onCreate(savedInstanceState);
        mUserLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mUserName = getArguments().getString(Constants.User.USER_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onCreateView UserFragment");
        View v = inflater.inflate(R.layout.user, container, false);
        setRetainInstance(true);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onActivityCreated UserFragment");
        super.onActivityCreated(savedInstanceState);
        
        showLoading();
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
        
        getLoaderManager().initLoader(4, null, this);
    }
    
    private void fillData() {
        View v = getView();
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        Typeface boldCondensed = app.boldCondensed;
        Typeface regular = app.regular;
        
        ImageView ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        ImageDownloader.getInstance().download(mUser.getGravatarId(), ivGravatar, 80);

        TextView tvName = (TextView) v.findViewById(R.id.tv_name);
        tvName.setTypeface(boldCondensed);
        
        TextView tvCreated = (TextView) v.findViewById(R.id.tv_created_at);
        tvCreated.setTypeface(regular);
        
        TextView tvFollowersCount = (TextView) v.findViewById(R.id.tv_followers_count);
        tvFollowersCount.setTypeface(boldCondensed);
        tvFollowersCount.setText(String.valueOf(mUser.getFollowers()));
        
        TableLayout tlOrgMembers = (TableLayout) v.findViewById(R.id.cell_org_members);
        tlOrgMembers.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        
        TableLayout tlFollowers = (TableLayout) v.findViewById(R.id.cell_followers);
        tlFollowers.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            tlFollowers.setOnClickListener(this);
            
            TextView tvFollowers = (TextView) v.findViewById(R.id.tv_followers_label);
            tvFollowers.setText(R.string.user_followers);
            
            tlOrgMembers.setVisibility(View.GONE);
        }
        else {
            tlOrgMembers.setOnClickListener(this);
            TextView tvMemberCount = (TextView) v.findViewById(R.id.tv_members_count);
            tvMemberCount.setTypeface(boldCondensed);
            
            tlFollowers.setVisibility(View.GONE);
        }
        
        //hide following if organization
        TableLayout tlFollowing = (TableLayout) v.findViewById(R.id.cell_following);
        tlFollowing.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            
            TextView tvFollowingCount = (TextView) v.findViewById(R.id.tv_following_count);
            tvFollowingCount.setTypeface(boldCondensed);
            tvFollowingCount.setText(String.valueOf(mUser.getFollowing()));
            
            tlFollowing.setOnClickListener(this);
        }
        else {
            tlFollowing.setVisibility(View.GONE);
        }
        
        TableLayout tlRepos = (TableLayout) v.findViewById(R.id.cell_repos);
        tlRepos.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        
        TextView tvReposCount = (TextView) v.findViewById(R.id.tv_repos_count);
        tvReposCount.setTypeface(boldCondensed);
        
        if (mUserLogin.equals(((BaseSherlockFragmentActivity) getSherlockActivity()).getAuthLogin())) {
            tvReposCount.setText(String.valueOf(mUser.getTotalPrivateRepos() + mUser.getPublicRepos()));    
        }
        else {
            tvReposCount.setText(String.valueOf(mUser.getPublicRepos()));
        }
        
        tlRepos.setOnClickListener(this);
        
        //hide gists repos if organization
        TableLayout tlGists = (TableLayout) v.findViewById(R.id.cell_gists);
        tlGists.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            
            TextView tvGistsCount = (TextView) v.findViewById(R.id.tv_gists_count);
            tvGistsCount.setTypeface(boldCondensed);
            tvGistsCount.setText(String.valueOf(mUser.getPublicGists()));
            
            tlGists.setOnClickListener(this);
        }
        else {
            tlGists.setVisibility(View.GONE);
        }
        
        tvName.setText(StringUtils.isBlank(mUser.getName()) ? mUser.getLogin() : mUser.getName());
        if (Constants.User.USER_TYPE_ORG.equals(mUser.getType())) {
            tvName.append(" (");
            tvName.append(Constants.User.USER_TYPE_ORG);
            tvName.append(")");
        }
        
        tvCreated.setText(mUser.getCreatedAt() != null ? 
                getResources().getString(R.string.user_created_at,
                        StringUtils.formatDate(mUser.getCreatedAt())) : "");

        //show email row if not blank
        TextView tvEmail = (TextView) v.findViewById(R.id.tv_email);
        tvEmail.setTypeface(regular);
        if (!StringUtils.isBlank(mUser.getEmail())) {
            tvEmail.setText(mUser.getEmail());
            tvEmail.setVisibility(View.VISIBLE);
        }
        else {
            tvEmail.setVisibility(View.GONE);
        }
        
        //show website if not blank
        TextView tvWebsite = (TextView) v.findViewById(R.id.tv_website);
        if (!StringUtils.isBlank(mUser.getBlog())) {
            tvWebsite.setText(mUser.getBlog());
            tvWebsite.setVisibility(View.VISIBLE);
        }
        else {
            tvWebsite.setVisibility(View.GONE);
        }
        tvWebsite.setTypeface(regular);
        
        //show company if not blank
        TextView tvCompany = (TextView) v.findViewById(R.id.tv_company);
        if (!StringUtils.isBlank(mUser.getCompany())) {
            tvCompany.setText(mUser.getCompany());
            tvCompany.setVisibility(View.VISIBLE);
        }
        else {
            tvCompany.setVisibility(View.GONE);
        }
        tvCompany.setTypeface(regular);
        
        //Show location if not blank
        TextView tvLocation = (TextView) v.findViewById(R.id.tv_location);
        if (!StringUtils.isBlank(mUser.getLocation())) {
            tvLocation.setText(mUser.getLocation());
            tvLocation.setVisibility(View.VISIBLE);
        }
        else {
            tvLocation.setVisibility(View.GONE);
        }
        tvLocation.setTypeface(regular);
        
        TextView tvPubRepo = (TextView) v.findViewById(R.id.tv_pub_repos_label);
        tvPubRepo.setTypeface(boldCondensed);
        tvPubRepo.setTextColor(Color.parseColor("#0099cc"));
        
        TextView tvOrgs = (TextView) v.findViewById(R.id.tv_orgs);
        tvOrgs.setTypeface(boldCondensed);
        tvOrgs.setTextColor(Color.parseColor("#0099cc"));
        
        UserActivity userActivity = (UserActivity) getSherlockActivity();
        Button btnFollow = (Button) getView().findViewById(R.id.btn_follow);
        ProgressBar pbFollow = (ProgressBar) getView().findViewById(R.id.pb_follow);
        
        if (mUserLogin.equals(userActivity.getAuthLogin())) {
            btnFollow.setVisibility(View.GONE);
            pbFollow.setVisibility(View.GONE);
        }
        else {
            //check if is following
            pbFollow.setVisibility(View.VISIBLE);
            getLoaderManager().initLoader(3, null, this);
            getLoaderManager().getLoader(3).forceLoad();
        }
        
        getLoaderManager().initLoader(1, null, this);
        getLoaderManager().initLoader(2, null, this);
        
        
        getLoaderManager().getLoader(1).forceLoad();
        getLoaderManager().getLoader(2).forceLoad();
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
        case R.id.cell_followers:
            getFollowers(view);
            break;
        case R.id.cell_following:
            getFollowing(view);
            break;
        case R.id.cell_repos:
            getPublicRepos(view);
            break;
        case R.id.cell_gists:
            getGists(view);
            break;
        case R.id.cell_org_members:
            getOrgMembers(view);
            break;
        default:
            break;
        }
    }

    public void getPublicRepos(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), RepositoryListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.putExtra(Constants.User.USER_NAME, mUserName);
        intent.putExtra(Constants.User.USER_TYPE, mUser.getType());
        startActivity(intent);
    }

    public void getFollowers(View view) {
        if (Constants.User.USER_TYPE_ORG.equals(mUser.getType())) {
            Intent intent = new Intent().setClass(this.getActivity(), OrganizationMemberListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            startActivity(intent);
        }
        else {
            Intent intent = new Intent().setClass(this.getActivity(), FollowerFollowingListActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
            intent.putExtra("FIND_FOLLOWERS", true);
            startActivity(intent);
        }
    }

    public void getFollowing(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), FollowerFollowingListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.putExtra("FIND_FOLLOWERS", false);
        startActivity(intent);
    }

    public void getOrgMembers(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), OrganizationMemberListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        startActivity(intent);
    }
    
    public void getGists(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), GistListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        startActivity(intent);
    }

    public void fillTopRepos(List<Repository> repos) {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        Typeface boldCondensed = app.boldCondensed;
        
        View v = getView();
        LinearLayout ll = (LinearLayout) v.findViewById(R.id.ll_top_repos);
        
        int i = 0;
        for (final Repository repository : repos) {
            View rowView = getLayoutInflater(null).inflate(R.layout.row_simple_3, null);
            rowView.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            rowView.setPadding(0, 16, 0, 16);
            rowView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
                    app.openRepositoryInfoActivity(getSherlockActivity(), repository);
                }
            });
            
            TextView tvTitle = (TextView) rowView.findViewById(R.id.tv_title);
            tvTitle.setTypeface(boldCondensed);
            
            TextView tvDesc = (TextView) rowView.findViewById(R.id.tv_desc);
            tvDesc.setSingleLine(true);
            
            TextView tvExtra = (TextView) rowView.findViewById(R.id.tv_extra);
            tvExtra.setTextAppearance(getSherlockActivity(), R.style.default_text_micro);
            
            tvTitle.setText(repository.getOwner().getLogin() + " / " + repository.getName());
            
            if (!StringUtils.isBlank(repository.getDescription())) {
                tvDesc.setVisibility(View.VISIBLE);
                tvDesc.setText(repository.getDescription());
            }
            else {
                tvDesc.setVisibility(View.GONE);
            }
            
            String extraData = (repository.getLanguage() != null ? repository.getLanguage()
                    + "   " : "")
                    + StringUtils.toHumanReadbleFormat(repository.getSize())
                    + "   "
                    + repository.getForks()
                    + " forks   "
                    + repository.getWatchers()
                    + " watchers";
            tvExtra.setText(extraData);
            
            ll.addView(rowView);
            
            //looks like the API ignore the per_page for GET /orgs/:org/repos
            if (i == 4) {
                break;
            }
            i++;
        }
        
        Button btnMore = (Button) getView().findViewById(R.id.btn_repos);
        if (!repos.isEmpty()) {
            btnMore.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    getPublicRepos(view);
                }
            });
        }
        else {
            btnMore.setVisibility(View.GONE);
            TextView noRepos = new TextView(getSherlockActivity());
            noRepos.setText("Repositories not found");
            ll.addView(noRepos);
        }
    }
    
    public void fillOrganizations(List<User> orgs) {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        Typeface boldCondensed = app.boldCondensed;
        
        View v = getView();
        LinearLayout llOrg = (LinearLayout) v.findViewById(R.id.ll_orgs);
        
        if (!orgs.isEmpty()) {
            llOrg.setVisibility(View.VISIBLE);
            for (final User org : orgs) {
                View rowView = getLayoutInflater(null).inflate(R.layout.row_simple, null);
                rowView.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
                rowView.setPadding(0, 16, 0, 16);
                rowView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
                        app.openUserInfoActivity(getSherlockActivity(), org.getLogin(), null);
                    }
                });
                
                TextView tvTitle = (TextView) rowView.findViewById(R.id.tv_title);
                tvTitle.setTypeface(boldCondensed);
                tvTitle.setText(org.getLogin());
                
                llOrg.addView(rowView);
            }
        }
        else {
            llOrg.setVisibility(View.GONE);
        }
    }
    
    private void updateFollowBtn() {
        Button btnFollow = (Button) getView().findViewById(R.id.btn_follow);
        ProgressBar pbFollow = (ProgressBar) getView().findViewById(R.id.pb_follow);
        pbFollow.setVisibility(View.GONE);
        btnFollow.setVisibility(View.VISIBLE);
        btnFollow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getLoaderManager().restartLoader(4, null, UserFragment.this);
                getLoaderManager().getLoader(4).forceLoad();
            }
        });
        
        if (isFollowing) {
            btnFollow.setText(R.string.user_unfollow_action);
        }
        else {
            btnFollow.setText(R.string.user_follow_action);
        }
    }
    
    @Override
    public Loader onCreateLoader(int id, Bundle arg1) {
        if (id == 1) {
            Map<String, String> filterData = new HashMap<String, String>();
            filterData.put("sort", "pushed");
            return new RepositoryListLoader(getSherlockActivity(), mUserLogin, 
                    mUser.getType(), filterData, 5);
        }
        else if (id == 2) {
            return new OrganizationListLoader(getSherlockActivity(), mUserLogin);
        }
        else if (id == 3) {
            return new IsFollowingUserLoader(getSherlockActivity(), mUserLogin);
        }
        else if (id == 4) {
            return new FollowUserLoader(getSherlockActivity(), mUserLogin, isFollowing);
        }
        else {
            return new UserLoader(getSherlockActivity(), mUserLogin);
            
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        if (loader.getId() == 1) {
            hideLoading(R.id.pb_top_repos, 0);
            fillTopRepos((List<Repository>) object);
        }
        else if (loader.getId() == 2) {
            fillOrganizations((List<User>) object);
        }
        else if (loader.getId() == 3) {
            isFollowing = (Boolean) object;
            updateFollowBtn();
        }
        else if (loader.getId() == 4) {
            isFollowing = (Boolean) object;
            updateFollowBtn();
        }
        else {
            hideLoading();
            mUser = (User) object;
            fillData();
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> arg0) {
        // TODO Auto-generated method stub
        
    }
    
}