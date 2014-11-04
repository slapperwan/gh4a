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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.gh4a.BasePagerActivity;
import com.gh4a.R;
import com.gh4a.adapter.DrawerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class IssueListBaseActivity extends BasePagerActivity {
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
    protected static final int ITEM_SORT_FIRST = ITEM_SORT_CREATED_DESC;

    protected static final List<DrawerAdapter.Item> DRAWER_ITEMS = Arrays.asList(
        new DrawerAdapter.SectionHeaderItem(R.string.issue_sort_order),
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
            toggleDrawer();
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