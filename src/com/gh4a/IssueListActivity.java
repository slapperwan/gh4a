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
package com.gh4a;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.fragment.IssueListByCommentsFragment;
import com.gh4a.fragment.IssueListBySubmittedFragment;
import com.gh4a.fragment.IssueListByUpdatedFragment;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.MilestoneListLoader;

public class IssueListActivity extends BaseSherlockFragmentActivity
    implements OnClickListener, LoaderManager.LoaderCallbacks {

    private String mRepoOwner;
    private String mRepoName;
    private String mState;
    private ThisPageAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private int tabCount;
    private Map<String, String> mFilterData;
    private boolean isCollaborator;
    private ProgressDialog mProgressDialog;
    private int mCurrentTab;
    private List<Label> mLabels;
    private List<Milestone> mMilestones;
    private List<User> mAssignees;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        
        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mState = data.getString(Constants.Issue.ISSUE_STATE);
        
        mFilterData = new HashMap<String, String>();
        mFilterData.put("state", mState);
        
        mActionBar = getSupportActionBar();
        
        fillTabs();
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().initLoader(1, null, this);
        getSupportLoaderManager().initLoader(2, null, this);
        getSupportLoaderManager().initLoader(3, null, this);
        getSupportLoaderManager().getLoader(3).forceLoad();
    }

    private void fillTabs() {
        mActionBar.removeAllTabs();
        tabCount = 3;
        
        mAdapter = new ThisPageAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {}
            
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            @Override
            public void onPageSelected(int position) {
                mActionBar.setSelectedNavigationItem(position);
            }
        });
        
        if (mState == null || "open".equals(mState)) {
            mActionBar.setTitle(R.string.issue_open);
        }
        else {
            mActionBar.setTitle(R.string.issue_closed);
        }
        
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.issues_submitted)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab, mCurrentTab == 0);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.issues_updated)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab, mCurrentTab == 1);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.issues_comments)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
        mActionBar.addTab(tab, mCurrentTab == 2);
    }
    
    public class ThisPageAdapter extends FragmentStatePagerAdapter {

        public ThisPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return tabCount;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 0) {
                return IssueListBySubmittedFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            }
            
            else if (position == 1) {
                return IssueListByUpdatedFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            }
            
            else if (position == 2) {
                return IssueListByCommentsFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            }
            return null;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.issues_menu, menu);
        if ("open".equals(mState)) {
            menu.getItem(4).setTitle(R.string.issue_view_closed_issues);
        }
        else {
            menu.getItem(4).setTitle(R.string.issue_view_open_issues);
        }
        if (!isCollaborator) {
            menu.removeItem(R.id.view_labels);
            menu.removeItem(R.id.view_milestones);
        }
        
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.navigation_expand_dark);
            menu.getItem(1).setIcon(R.drawable.collections_labels_dark);
            menu.getItem(2).setIcon(R.drawable.collections_view_as_list_dark);
            menu.getItem(3).setIcon(R.drawable.social_person_dark);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openRepositoryInfoActivity(this, mRepoOwner, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.view_open_closed:
                if ("open".equals(mState)) {
                    mState = Constants.Issue.ISSUE_STATE_CLOSED;
                    mFilterData.put("state", Constants.Issue.ISSUE_STATE_CLOSED);
                    item.setTitle(R.string.issue_view_open_issues);
                }
                else {
                    mState = Constants.Issue.ISSUE_STATE_OPEN;
                    mFilterData.put("state", Constants.Issue.ISSUE_STATE_OPEN);
                    item.setTitle(R.string.issue_view_closed_issues);
                }
                
                reloadIssueList();
                return true;
            case R.id.create_issue:
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            case R.id.view_labels:
                Intent intent = new Intent().setClass(IssueListActivity.this, IssueLabelListActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                startActivity(intent);
                return true;
            case R.id.view_milestones:
                intent = new Intent().setClass(IssueListActivity.this, IssueMilestoneListActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                startActivity(intent);
                return true;    
            case R.id.sort:
                String direction = mFilterData.get("direction");
                if ("desc".equals(direction) || direction == null) {
                    if (Gh4Application.THEME == R.style.LightTheme) {
                        item.setIcon(R.drawable.navigation_collapse);
                    }
                    else {
                        item.setIcon(R.drawable.navigation_collapse_dark);
                    }
                    mFilterData.put("direction", "asc");
                }
                else {
                    if (Gh4Application.THEME == R.style.LightTheme) {
                        item.setIcon(R.drawable.navigation_expand);
                    }
                    else {
                        item.setIcon(R.drawable.navigation_expand_dark);
                    }
                    mFilterData.put("direction", "desc");
                }
                reloadIssueList();
                return true;
            case R.id.labels:
                if (mLabels == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().getLoader(0).forceLoad();
                }
                else {
                    showLabelsDialog();
                }
                return true;
            case R.id.milestones:
                if (mMilestones == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().getLoader(1).forceLoad();
                }
                else {
                    showMilestonesDialog();
                }
                return true;
            case R.id.assignees:
                if (mAssignees == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().getLoader(2).forceLoad();
                }
                else {
                    showAssigneesDialog();
                }
                return true;
            default:
                return true;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        default:
            break;
        }
    }
    
    private void reloadIssueList() {
        mCurrentTab = mPager.getCurrentItem();
        fillTabs();
    }
    
    private void showLabelsDialog() {
        String selectedLabels = mFilterData.get("labels");
        String[] checkedLabels = new String[] {};
        
        if (selectedLabels != null) {
            checkedLabels = selectedLabels.split(",");
        }
        List<String> checkLabelStringList = Arrays.asList(checkedLabels);
        final boolean[] checkedItems = new boolean[mLabels.size()];

        final String[] allLabelArray = new String[mLabels.size()];
        
        for (int i = 0; i < mLabels.size(); i++) {
            Label l = mLabels.get(i);
            allLabelArray[i] = l.getName();
            if(checkLabelStringList.contains(l.getName())) {
                checkedItems[i] = true;
            }
            else {
                checkedItems[i] = false;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme));
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_filter_by_labels);
        builder.setMultiChoiceItems(allLabelArray, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                if (isChecked) {
                    checkedItems[whichButton] = true;
                }
                else {
                    checkedItems[whichButton] = false;
                }
            }
        });
        
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                String labels = "";
                for (int i = 0; i < allLabelArray.length; i++) {
                    if (checkedItems[i]) {
                        labels += allLabelArray[i] + ",";
                    }
                }
                mFilterData.put("labels", labels);
                reloadIssueList();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }
    
    private void showMilestonesDialog() {
        String[] milestones = new String[mMilestones.size() + 1];
        final int[] milestoneIds = new int[mMilestones.size() + 1];
        
        milestones[0] = getResources().getString(R.string.issue_filter_by_any_milestone);
        milestoneIds[0] = 0;
        
        String checkedMilestoneNumber = mFilterData.get("milestone");
        int checkedItem = checkedMilestoneNumber != null && !"".equals(checkedMilestoneNumber) ? 
                 Integer.parseInt(checkedMilestoneNumber) : 0;
        
        for (int i = 1; i <= mMilestones.size(); i++) {
            Milestone m = mMilestones.get(i - 1);
            milestones[i] = m.getTitle();
            milestoneIds[i] = m.getNumber();
            if (m.getNumber() == checkedItem) {
                checkedItem = i;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme));
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_filter_by_milestone);
        builder.setSingleChoiceItems(milestones, checkedItem, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mFilterData.remove("milestone");
                }
                else {
                    mFilterData.put("milestone", String.valueOf(milestoneIds[which]));
                }
            }
        });
        
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                reloadIssueList();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }
    
    private void showAssigneesDialog() {
        final String[] assignees = new String[mAssignees.size() + 1];
        
        assignees[0] = getResources().getString(R.string.issue_filter_by_any_assignee);
        
        String checkedAssignee = mFilterData.get("assignee");
        int checkedItem = 0;
        
        for (int i = 1; i <= mAssignees.size(); i++) {
            User u = mAssignees.get(i - 1);
            assignees[i] = u.getLogin();
            if (u.getLogin().equalsIgnoreCase(checkedAssignee)) {
                checkedItem = i;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme));
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_filter_by_assignee);
        builder.setSingleChoiceItems(assignees, checkedItem, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mFilterData.remove("assignee");
                }
                else {
                    mFilterData.put("assignee", assignees[which]);
                }
            }
        });
        
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                reloadIssueList();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        if (id == 0) {
            return new LabelListLoader(this, mRepoOwner, mRepoName);
        }
        else if (id == 1) {
            return new MilestoneListLoader(this, mRepoOwner, mRepoName, "open");
        }
        else if (id == 2) {
            return new CollaboratorListLoader(this, mRepoOwner, mRepoName);
        }
        else {
            return new IsCollaboratorLoader(this, mRepoOwner, mRepoName);            
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        if (loader.getId() == 0) {
            stopProgressDialog(mProgressDialog);
            mLabels = (List<Label>) object;
            showLabelsDialog();
        }
        else if (loader.getId() == 1) {
            stopProgressDialog(mProgressDialog);
            mMilestones = (List<Milestone>) object;
            showMilestonesDialog();
        }
        else if (loader.getId() == 2) {
            stopProgressDialog(mProgressDialog);
            mAssignees = (List<User>) object;
            showAssigneesDialog();
        }
        else {
            isCollaborator = (Boolean) object;
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // TODO Auto-generated method stub
        
    }
}
