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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.adapter.IssueLabelAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.fragment.ConfirmationDialogFragment;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.service.issues.IssueLabelService;

public class IssueLabelListActivity extends BaseActivity implements
        RootAdapter.OnItemClickListener<IssueLabelAdapter.EditableLabel>,
        ConfirmationDialogFragment.Callback, View.OnClickListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            boolean fromPullRequest) {
        return new Intent(context, IssueLabelListActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("from_pr", fromPullRequest);
    }

    private static final int ID_LOADER_LABELS = 0;

    private String mRepoOwner;
    private String mRepoName;
    private boolean mParentIsPullRequest;
    private EditActionMode mActionMode;
    private IssueLabelAdapter.EditableLabel mPendingEditingLabel;

    private FloatingActionButton mFab;
    private IssueLabelAdapter mAdapter;

    private static final String STATE_KEY_EDITING_LABEL = "editing_label";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setContentShown(false);

        mAdapter = new IssueLabelAdapter(this);
        mAdapter.setOnItemClickListener(this);

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        recyclerView.setTag(R.id.FloatingActionButtonScrollEnabled, new Object());
        recyclerView.setAdapter(mAdapter);

        CoordinatorLayout rootLayout = getRootLayout();
        mFab = (FloatingActionButton) getLayoutInflater().inflate(
                R.layout.add_fab, rootLayout, false);
        mFab.setOnClickListener(this);
        rootLayout.addView(mFab);
        updateFabVisibility();

        loadLabels(false);

        if (savedInstanceState != null) {
            mPendingEditingLabel = savedInstanceState.getParcelable(STATE_KEY_EDITING_LABEL);
        }
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.issue_labels);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mParentIsPullRequest = extras.getBoolean("from_pr", false);
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
        loadLabels(true);
        super.onRefresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActionMode != null) {
            outState.putParcelable(STATE_KEY_EDITING_LABEL, mActionMode.mLabel);
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
        return IssueListActivity.makeIntent(this, mRepoOwner, mRepoName, mParentIsPullRequest);
    }

    @Override
    public void onClick(View view) {
        if (mActionMode == null) {
            startEditing(addOrGetNewLabelItem());
        }
    }

    @Override
    public void onConfirmed(String tag, Parcelable data) {
        IssueLabelAdapter.EditableLabel label = (IssueLabelAdapter.EditableLabel) data;
        deleteLabel(label);
    }

    public IssueLabelAdapter.EditableLabel addOrGetNewLabelItem() {
        int size = mAdapter.getCount();
        for (int i = 0; i < size; i++) {
            IssueLabelAdapter.EditableLabel label = mAdapter.getItem(i);
            if (label.newlyAdded) {
                return label;
            }
        }
        IssueLabelAdapter.EditableLabel newLabel = new IssueLabelAdapter.EditableLabel("dddddd");
        mAdapter.add(newLabel);
        mAdapter.notifyDataSetChanged();
        return newLabel;
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
        private final IssueLabelAdapter.EditableLabel mLabel;

        public EditActionMode(IssueLabelAdapter.EditableLabel label) {
            mLabel = label;
            mLabel.isEditing = true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.save)
                    .setIcon(R.drawable.menu_save)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

            if (!mLabel.newlyAdded) {
                menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, R.string.delete)
                        .setIcon(R.drawable.menu_delete)
                        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
                if (mLabel.newlyAdded) {
                    addLabel(mLabel);
                } else {
                    editLabel(mLabel);
                }
                break;
            case Menu.FIRST + 1:
                ConfirmationDialogFragment.show(IssueLabelListActivity.this,
                        getString(R.string.issue_dialog_delete_message, mLabel.name()),
                        R.string.delete, mLabel, "deleteconfirm");
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
            if (mLabel.newlyAdded) {
                mAdapter.remove(mLabel);
            } else {
                mLabel.restoreOriginalProperties();
            }
            mAdapter.notifyDataSetChanged();
            updateFabVisibility();
        }
    }

    private void applyPendingEditedLabel() {
        if (mPendingEditingLabel == null) {
            return;
        }
        IssueLabelAdapter.EditableLabel label = null;
        if (mPendingEditingLabel.newlyAdded) {
            label = addOrGetNewLabelItem();
        } else {
            int count = mAdapter.getCount();
            for (int i = 0; i < count; i++) {
                IssueLabelAdapter.EditableLabel item = mAdapter.getItem(i);
                if (item.name().equals(mPendingEditingLabel.name())) {
                    label = item;
                    break;
                }
            }
        }
        if (label != null) {
            label.editedColor = mPendingEditingLabel.editedColor;
            label.editedName = mPendingEditingLabel.editedName;
            startEditing(label);
        }
        mPendingEditingLabel = null;
    }

    private void deleteLabel(IssueLabelAdapter.EditableLabel label) {
        String errorMessage = getString(R.string.issue_error_delete_label, label.base().name());
        IssueLabelService service = ServiceFactory.get(IssueLabelService.class, false);
        service.deleteLabel(mRepoOwner, mRepoName, label.base().name())
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, R.string.deleting_msg, errorMessage))
                .subscribe(result -> {
                    loadLabels(true);
                    setResult(RESULT_OK);
                }, error -> handleActionFailure("Deleting label failed", error));
    }

    private void editLabel(IssueLabelAdapter.EditableLabel label) {
        Label oldLabel = label.base();
        String errorMessage = getString(R.string.issue_error_edit_label, oldLabel.name());
        IssueLabelService service = ServiceFactory.get(IssueLabelService.class, false);
        Label newLabel = Label.builder()
                .name(label.editedName)
                .color(label.editedColor)
                .build();

        service.editLabel(mRepoOwner, mRepoName, oldLabel.name(), newLabel)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, R.string.saving_msg, errorMessage))
                .subscribe(result -> {
                    loadLabels(true);
                    setResult(RESULT_OK);
                }, error -> handleActionFailure("Editing label failed", error));
    }

    private void addLabel(IssueLabelAdapter.EditableLabel label) {
        String errorMessage = getString(R.string.issue_error_create_label, label.name());
        IssueLabelService service = ServiceFactory.get(IssueLabelService.class, false);
        Label newLabel = Label.builder()
                .name(label.name())
                .color(label.color())
                .build();

        service.createLabel(mRepoOwner, mRepoName, newLabel)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, R.string.saving_msg, errorMessage))
                .subscribe(result -> {
                    loadLabels(true);
                    setResult(RESULT_OK);
                }, error -> handleActionFailure("Adding label failed", error));
    }

    private void loadLabels(boolean force) {
        final IssueLabelService service = ServiceFactory.get(IssueLabelService.class, false);
        ApiHelpers.PageIterator
                .toSingle(page -> service.getRepositoryLabels(mRepoOwner, mRepoName, page))
                .compose(RxUtils.mapList(IssueLabelAdapter.EditableLabel::new))
                .compose(makeLoaderSingle(ID_LOADER_LABELS, force))
                .subscribe(result -> {
                    UiUtils.hideImeForView(getCurrentFocus());
                    mAdapter.clear();
                    mAdapter.addAll(result);
                    applyPendingEditedLabel();
                    setContentShown(true);
                }, this::handleLoadFailure);
    }
}
