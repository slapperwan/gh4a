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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
import android.widget.ImageButton;

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
import com.gh4a.utils.StringUtils;

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
    private ImageButton mBtnSort;
    private ImageButton mBtnFilterByLabels;
    private ImageButton mBtnFilterByMilestone;
    private ImageButton mBtnFilterByAssignee;
    private boolean isCollaborator;
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.issue_list_view_pager);
        
        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mState = data.getString(Constants.Issue.ISSUE_STATE);
        int position = data.getInt("position");
        
        mFilterData = new HashMap<String, String>();
        Iterator<String> filter = data.keySet().iterator();
        while (filter.hasNext()) {
            String key = filter.next();
            if (!Constants.Repository.REPO_OWNER.equals(key)
                    && !Constants.Repository.REPO_NAME.equals(key)
                    && !"position".equals(key)) {
                mFilterData.put(key, data.getString(key));
            }
        }
        
        tabCount = 3;
        
        mActionBar = getSupportActionBar();
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
        
        mActionBar.setTitle(R.string.issues);
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.issues_submitted)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab, position == 0);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.issues_updated)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab, position == 1);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.issues_comments)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
        mActionBar.addTab(tab, position == 2);
        
        mBtnSort = (ImageButton) findViewById(R.id.btn_sort);
        mBtnSort.setOnClickListener(this);
        
        String direction = mFilterData.get("direction");
        if ("desc".equals(direction) || direction == null) {
            mBtnSort.setImageDrawable(getResources().getDrawable(R.drawable.navigation_expand));
        }
        else {
            mBtnSort.setImageDrawable(getResources().getDrawable(R.drawable.navigation_collapse));
        }
        
        mBtnFilterByLabels = (ImageButton) findViewById(R.id.btn_labels);
        mBtnFilterByLabels.setOnClickListener(this);
        
        mBtnFilterByMilestone = (ImageButton) findViewById(R.id.btn_milestone);
        mBtnFilterByMilestone.setOnClickListener(this);
        
        mBtnFilterByAssignee = (ImageButton) findViewById(R.id.btn_assignee);
        mBtnFilterByAssignee.setOnClickListener(this);
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().initLoader(1, null, this);
        getSupportLoaderManager().initLoader(2, null, this);
        getSupportLoaderManager().initLoader(3, null, this);
        getSupportLoaderManager().getLoader(3).forceLoad();
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
            menu.removeItem(R.id.view_open_issues);
        }
        else {
            menu.removeItem(R.id.view_closed_issues);
        }
        if (!isCollaborator) {
            menu.removeItem(R.id.view_labels);
            menu.removeItem(R.id.view_milestones);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openRepositoryInfoActivity(this, mRepoOwner, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.view_open_issues:
                getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName, 
                        Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.view_closed_issues:
                getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName,
                        Constants.Issue.ISSUE_STATE_CLOSED, Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                
            default:
                return true;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.btn_sort:
            String direction = mFilterData.get("direction");
            if ("desc".equals(direction) || direction == null) {
                mFilterData.put("direction", "asc");
            }
            else {
                mFilterData.put("direction", "desc");
            }
            reloadIssueList();
            break;
            
        case R.id.btn_labels:
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            if (mFilterData.get("labelsPassToNextActivity") == null) {
                getSupportLoaderManager().getLoader(0).forceLoad();
            }
            else {
                String[] labelString = mFilterData.get("labelsPassToNextActivity").split(",");
                List<Label> labels = new ArrayList<Label>();
                for (String labelName : labelString) {
                    Label l = new Label();
                    l.setName(labelName);
                    labels.add(l);
                }
                showLabelsDialog(labels);
            }
            break;
            
        case R.id.btn_milestone:
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            if (mFilterData.get("milestonesPassToNextActivity") == null) {
                getSupportLoaderManager().getLoader(1).forceLoad();
            }
            else {
                String[] milestoneString = mFilterData.get("milestonesPassToNextActivity").split(",");
                String[] milestoneIdString = mFilterData.get("milestonesIdPassToNextActivity").split(",");
                List<Milestone> milestones = new ArrayList<Milestone>();
                for (int i = 0; i < milestoneString.length; i++) {
                    if (!StringUtils.isBlank(milestoneIdString[i])) {
                        Milestone m = new Milestone();
                        m.setTitle(milestoneString[i]);
                        m.setNumber(Integer.parseInt(milestoneIdString[i]));
                        milestones.add(m);
                    }
                }
                showMilestonesDialog(milestones);
            }
            break;
            
        case R.id.btn_assignee:
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            if (mFilterData.get("assigneesPassToNextActivity") == null) {
                getSupportLoaderManager().getLoader(2).forceLoad();
            }
            else {
                String[] assigneeString = mFilterData.get("assigneesPassToNextActivity").split(",");
                List<User> users = new ArrayList<User>();
                for (String login : assigneeString) {
                    User user = new User();
                    user.setLogin(login);
                    users.add(user);
                }
                showAssigneesDialog(users);
            }
            break;

        default:
            break;
        }
    }
    
    private void reloadIssueList() {
        Intent intent = new Intent().setClass(this, IssueListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Issue.ISSUE_STATE, mState);
        intent.putExtra("position", mPager.getCurrentItem());
        intent.putExtra("direction", mFilterData.get("direction"));
        intent.putExtra("labels", mFilterData.get("labels"));
        if (mFilterData.get("milestone") != null) {
            intent.putExtra("milestone", mFilterData.get("milestone"));
        }
        if (mFilterData.get("assignee") != null) {
            intent.putExtra("assignee", mFilterData.get("assignee"));
        }
        
        intent.putExtra("labelsPassToNextActivity", mFilterData.get("labelsPassToNextActivity"));
        intent.putExtra("milestonesPassToNextActivity", mFilterData.get("milestonesPassToNextActivity"));
        intent.putExtra("milestonesIdPassToNextActivity", mFilterData.get("milestonesIdPassToNextActivity"));
        intent.putExtra("assigneesPassToNextActivity", mFilterData.get("assigneesPassToNextActivity"));
        
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    
    private void showLabelsDialog(List<Label> allLabels) {
        String selectedLabels = mFilterData.get("labels");
        String[] checkedLabels = new String[] {};
        
        if (selectedLabels != null) {
            checkedLabels = selectedLabels.split(",");
        }
        List<String> checkLabelStringList = Arrays.asList(checkedLabels);
        final boolean[] checkedItems = new boolean[allLabels.size()];

        final String[] allLabelArray = new String[allLabels.size()];
        
        String labelsPassToNextActivity = "";
        
        for (int i = 0; i < allLabels.size(); i++) {
            Label l = allLabels.get(i);
            allLabelArray[i] = l.getName();
            if(checkLabelStringList.contains(l.getName())) {
                checkedItems[i] = true;
            }
            else {
                checkedItems[i] = false;
            }
            labelsPassToNextActivity += l.getName() + ",";
        }
        
        mFilterData.put("labelsPassToNextActivity", labelsPassToNextActivity);
        
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
    
    private void showMilestonesDialog(List<Milestone> allMilestones) {
        String[] milestones = new String[allMilestones.size() + 1];
        final int[] milestoneIds = new int[allMilestones.size() + 1];
        
        milestones[0] = getResources().getString(R.string.issue_filter_by_any_milestone);
        milestoneIds[0] = 0;
        
        String checkedMilestoneNumber = mFilterData.get("milestone");
        int checkedItem = checkedMilestoneNumber != null && !"".equals(checkedMilestoneNumber) ? 
                 Integer.parseInt(checkedMilestoneNumber) : 0;
        
        String milestonesPassToNextActivity = "";
        String milestonesIdPassToNextActivity = "";
        
        for (int i = 1; i <= allMilestones.size(); i++) {
            Milestone m = allMilestones.get(i - 1);
            milestones[i] = m.getTitle();
            milestoneIds[i] = m.getNumber();
            if (m.getNumber() == checkedItem) {
                checkedItem = i;
            }
            milestonesPassToNextActivity += m.getTitle() + ",";
            milestonesIdPassToNextActivity += m.getNumber() + ",";
        }
        
        mFilterData.put("milestonesPassToNextActivity", milestonesPassToNextActivity);
        mFilterData.put("milestonesIdPassToNextActivity", milestonesIdPassToNextActivity);
        
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
    
    private void showAssigneesDialog(List<User> allAssignees) {
        final String[] assignees = new String[allAssignees.size() + 1];
        
        assignees[0] = getResources().getString(R.string.issue_filter_by_any_assignee);
        
        String checkedAssignee = mFilterData.get("assignee");
        int checkedItem = 0;
        
        String assigneesPassToNextActivity = "";
        for (int i = 1; i <= allAssignees.size(); i++) {
            User u = allAssignees.get(i - 1);
            assignees[i] = u.getLogin();
            if (u.getLogin().equalsIgnoreCase(checkedAssignee)) {
                checkedItem = i;
            }
            assigneesPassToNextActivity += u.getLogin() + ",";
        }
        
        mFilterData.put("assigneesPassToNextActivity", assigneesPassToNextActivity);
        
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
            showLabelsDialog((List<Label>) object);
        }
        else if (loader.getId() == 1) {
            stopProgressDialog(mProgressDialog);
            showMilestonesDialog((List<Milestone>) object);
        }
        else if (loader.getId() == 2) {
            stopProgressDialog(mProgressDialog);
            showAssigneesDialog((List<User>) object);
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
