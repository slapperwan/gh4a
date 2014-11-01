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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;

import com.gh4a.Constants;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class IssueListBaseActivity extends LoadingFragmentPagerActivity {
    protected String mSortMode;
    protected boolean mSortAscending;

    protected DrawerAdapter mDrawerAdapter;
    protected ArrayList<DrawerAdapter.Item> mDrawerItems;

    protected static final String SORT_MODE_CREATED = "created";
    protected static final String SORT_MODE_UPDATED = "updated";
    protected static final String SORT_MODE_COMMENTS = "comments";

    protected static final int ITEM_FILTER_MILESTONE = 1;
    protected static final int ITEM_FILTER_ASSIGNEE = 2;
    protected static final int ITEM_FILTER_LABEL = 3;
    protected static final int ITEM_MANAGE_LABELS = 4;
    protected static final int ITEM_MANAGE_MILESTONES = 5;
    protected static final int ITEM_SORT_CREATED_DESC = 6;
    protected static final int ITEM_SORT_CREATED_ASC = 7;
    protected static final int ITEM_SORT_UPDATED_DESC = 8;
    protected static final int ITEM_SORT_UPDATED_ASC = 9;
    protected static final int ITEM_SORT_COMMENTS_DESC = 10;
    protected static final int ITEM_SORT_COMMENTS_ASC = 11;
    protected static final int ITEM_STATE_OPEN = 12;
    protected static final int ITEM_STATE_CLOSED = 13;
    protected static final int ITEM_SORT_FIRST = ITEM_SORT_CREATED_DESC;

    protected static final List<DrawerAdapter.Item> DRAWER_ITEMS = Arrays.asList(
            new DrawerAdapter.SectionItem(R.string.issue_sort_order),
            new DrawerAdapter.RadioItem(R.string.issue_sort_created_desc, ITEM_SORT_CREATED_DESC),
            new DrawerAdapter.RadioItem(R.string.issue_sort_created_asc, ITEM_SORT_CREATED_ASC),
            new DrawerAdapter.RadioItem(R.string.issue_sort_updated_desc, ITEM_SORT_UPDATED_DESC),
            new DrawerAdapter.RadioItem(R.string.issue_sort_updated_asc, ITEM_SORT_UPDATED_ASC),
            new DrawerAdapter.RadioItem(R.string.issue_sort_comments_desc, ITEM_SORT_COMMENTS_DESC),
            new DrawerAdapter.RadioItem(R.string.issue_sort_comments_asc, ITEM_SORT_COMMENTS_ASC)
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return new int[0];
    }

    @Override
    protected Fragment getFragment(int position) {
        return null;
    }

    @Override
    protected boolean isRightSideDrawer() {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.actions)
                .setIcon(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                mDrawerLayout.closeDrawer(Gravity.RIGHT);
            } else {
                mDrawerLayout.openDrawer(Gravity.RIGHT);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean onDrawerItemSelected(int position) {
        int id = mDrawerItems.get(position).getId();
        switch (id) {
            case ITEM_SORT_CREATED_ASC:
                updateSortModeAndReload(SORT_MODE_CREATED, true, id);
                return true;
            case ITEM_SORT_CREATED_DESC:
                updateSortModeAndReload(SORT_MODE_CREATED, false, id);
                return true;
            case ITEM_SORT_UPDATED_ASC:
                updateSortModeAndReload(SORT_MODE_UPDATED, true, id);
                return true;
            case ITEM_SORT_UPDATED_DESC:
                updateSortModeAndReload(SORT_MODE_UPDATED, false, id);
                return true;
            case ITEM_SORT_COMMENTS_ASC:
                updateSortModeAndReload(SORT_MODE_COMMENTS, true, id);
                return true;
            case ITEM_SORT_COMMENTS_DESC:
                updateSortModeAndReload(SORT_MODE_COMMENTS, false, id);
                return true;
        }
        return super.onDrawerItemSelected(position);
    }

    protected void updateSortDrawerItemState(int activeItemId) {
        for (DrawerAdapter.Item item : mDrawerItems) {
            if (item.getId() >= ITEM_SORT_FIRST) {
                ((DrawerAdapter.RadioItem) item).setChecked(item.getId() == activeItemId);
            }
        }
        mDrawerAdapter.notifyDataSetChanged();
    }

    protected void updateSortModeAndReload(String sortMode, boolean ascending, int itemId) {
        updateSortDrawerItemState(itemId);
        mSortAscending = ascending;
        mSortMode = sortMode;
        reloadIssueList();
    }

    public abstract void reloadIssueList();
}