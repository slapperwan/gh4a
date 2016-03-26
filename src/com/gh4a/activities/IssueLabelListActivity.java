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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.IssueLabelAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.LabelService;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

public class IssueLabelListActivity extends BaseActivity implements
        RootAdapter.OnItemClickListener<IssueLabelAdapter.EditableLabel>, View.OnClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private EditActionMode mActionMode;
    private IssueLabelAdapter.EditableLabel mAddedLabel;

    private FloatingActionButton mFab;
    private RecyclerView mRecyclerView;
    private IssueLabelAdapter mAdapter;

    private static final String STATE_KEY_ADDED_LABEL = "added_label";
    private static final String STATE_KEY_EDITING_LABEL = "editing_label";

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>(this) {
        @Override
        protected Loader<LoaderResult<List<Label>>> onCreateLoader() {
            return new LabelListLoader(IssueLabelListActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(List<Label> result) {
            UiUtils.hideImeForView(getCurrentFocus());
            mAdapter.clear();
            for (Label label : result) {
                mAdapter.add(new IssueLabelAdapter.EditableLabel(label));
            }
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setContentShown(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_labels);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mAdapter = new IssueLabelAdapter(this);
        mAdapter.setOnItemClickListener(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this));
        mRecyclerView.setTag(R.id.FloatingActionButtonScrollEnabled, new Object());
        mRecyclerView.setAdapter(mAdapter);

        CoordinatorLayout rootLayout = getRootLayout();
        mFab = (FloatingActionButton) getLayoutInflater().inflate(
                R.layout.add_fab, rootLayout, false);
        mFab.setOnClickListener(this);
        rootLayout.addView(mFab);
        updateFabVisibility();

        getSupportLoaderManager().initLoader(0, null, mLabelCallback);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_KEY_ADDED_LABEL)) {
                mAddedLabel = (IssueLabelAdapter.EditableLabel)
                        savedInstanceState.getSerializable(STATE_KEY_EDITING_LABEL);
                mAdapter.add(mAddedLabel);
                startEditing(mAddedLabel);
            } else if (savedInstanceState.containsKey(STATE_KEY_EDITING_LABEL)) {
                IssueLabelAdapter.EditableLabel label = (IssueLabelAdapter.EditableLabel)
                        savedInstanceState.getSerializable(STATE_KEY_EDITING_LABEL);
                int count = mAdapter.getCount();
                for (int i = 0; i < count; i++) {
                    IssueLabelAdapter.EditableLabel item = mAdapter.getItem(i);
                    if (item.getName().equals(label.getName())) {
                        item.editedName = label.editedName;
                        item.editedColor = label.editedColor;
                        startEditing(item);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // swipe-to-refresh doesn't make much sense in the
        // interaction model of this activity
        return false;
    }

    @Override
    public void onRefresh() {
        setContentShown(false);
        mAdapter.clear();
        getSupportLoaderManager().getLoader(0).onContentChanged();
        super.onRefresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActionMode != null) {
            if (mAddedLabel != null) {
                outState.putSerializable(STATE_KEY_ADDED_LABEL, mAddedLabel);
            } else {
                outState.putSerializable(STATE_KEY_EDITING_LABEL, mActionMode.mLabel);
            }
        }
    }

    @Override
    public void onItemClick(IssueLabelAdapter.EditableLabel item) {
        if (mActionMode == null) {
            startEditing(item);
        }
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getIssueListActivityIntent(this,
                mRepoOwner, mRepoName, Constants.Issue.STATE_OPEN);
    }

    @Override
    public void onClick(View view) {
        if (mActionMode == null) {
            mAddedLabel = new IssueLabelAdapter.EditableLabel("dddddd");
            mAdapter.add(mAddedLabel);
            mAdapter.notifyDataSetChanged();
            startEditing(mAddedLabel);
        }
    }

    private void startEditing(IssueLabelAdapter.EditableLabel label) {
        mActionMode = new EditActionMode(label);
        mAdapter.notifyDataSetChanged();
        startSupportActionMode(mActionMode);
        updateFabVisibility();
    }

    private void updateFabVisibility() {
        boolean visible = Gh4Application.get().isAuthorized() && mActionMode == null;
        mFab.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private final class EditActionMode implements ActionMode.Callback {
        private IssueLabelAdapter.EditableLabel mLabel;

        public EditActionMode(IssueLabelAdapter.EditableLabel label) {
            mLabel = label;
            mLabel.isEditing = true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuItem saveItem = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.save)
                .setIcon(R.drawable.content_save);
            MenuItemCompat.setShowAsAction(saveItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

            if (mLabel != mAddedLabel) {
                MenuItem deleteItem = menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, R.string.delete)
                    .setIcon(R.drawable.content_discard);
                MenuItemCompat.setShowAsAction(deleteItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case Menu.FIRST:
                if (mLabel == mAddedLabel) {
                    new AddIssueLabelTask(mLabel.editedName, mLabel.editedColor).schedule();
                } else {
                    new EditIssueLabelTask(mLabel.getName(), mLabel.editedName, mLabel.editedColor)
                            .schedule();
                }
                break;
            case Menu.FIRST + 1:
                new AlertDialog.Builder(IssueLabelListActivity.this)
                        .setTitle(getString(R.string.issue_dialog_delete_title, mLabel.getName()))
                        .setMessage(R.string.issue_dialog_delete_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                new DeleteIssueLabelTask(mLabel.getName()).schedule();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            default:
                break;
            }

            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mLabel.isEditing = false;
            if (mLabel == mAddedLabel) {
                mAdapter.remove(mLabel);
                mAddedLabel = null;
            }
            mAdapter.notifyDataSetChanged();
            updateFabVisibility();
        }
    }

    private class DeleteIssueLabelTask extends ProgressDialogTask<Void> {
        private String mLabelName;

        public DeleteIssueLabelTask(String labelName) {
            super(IssueLabelListActivity.this, 0, R.string.deleting_msg);
            mLabelName = labelName;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new DeleteIssueLabelTask(mLabelName);
        }

        @Override
        protected Void run() throws IOException {
            LabelService labelService = (LabelService)
                    Gh4Application.get().getService(Gh4Application.LABEL_SERVICE);
            labelService.deleteLabel(mRepoOwner, mRepoName, URLEncoder.encode(mLabelName, "UTF-8"));
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            getSupportLoaderManager().getLoader(0).onContentChanged();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.issue_error_delete_label, mLabelName);
        }
    }

    private class EditIssueLabelTask extends ProgressDialogTask<Void> {
        private String mOldLabelName;
        private String mNewLabelName;
        private String mColor;

        public EditIssueLabelTask(String oldLabelName, String newLabelName, String color) {
            super(IssueLabelListActivity.this, 0, R.string.saving_msg);
            mOldLabelName = oldLabelName;
            mNewLabelName = newLabelName;
            mColor = color;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new EditIssueLabelTask(mOldLabelName, mNewLabelName, mColor);
        }

        @Override
        protected Void run() throws IOException {
            LabelService labelService = (LabelService)
                    Gh4Application.get().getService(Gh4Application.LABEL_SERVICE);

            Label label = new Label();
            label.setName(mNewLabelName);
            label.setColor(mColor);

            labelService.editLabel(new RepositoryId(mRepoOwner, mRepoName),
                    URLEncoder.encode(mOldLabelName, "UTF-8"), label);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            getSupportLoaderManager().getLoader(0).onContentChanged();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.issue_error_edit_label, mOldLabelName);
        }
    }

    private class AddIssueLabelTask extends ProgressDialogTask<Void> {
        private String mLabelName;
        private String mColor;

        public AddIssueLabelTask(String labelName, String color) {
            super(IssueLabelListActivity.this, 0, R.string.saving_msg);
            mLabelName = labelName;
            mColor = color;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new AddIssueLabelTask(mLabelName, mColor);
        }

        @Override
        protected Void run() throws IOException {
            LabelService labelService = (LabelService)
                    Gh4Application.get().getService(Gh4Application.LABEL_SERVICE);

            Label label = new Label();
            label.setName(mLabelName);
            label.setColor(mColor);

            labelService.createLabel(mRepoOwner, mRepoName, label);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            getSupportLoaderManager().getLoader(0).onContentChanged();
            mAddedLabel = null;
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.issue_error_create_label, mLabelName);
        }

        @Override
        protected void onError(Exception e) {
            super.onError(e);
            mAdapter.remove(mAddedLabel);
            mAddedLabel = null;
        }
    }
}
