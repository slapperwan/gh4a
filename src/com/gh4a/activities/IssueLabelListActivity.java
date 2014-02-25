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

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.LabelService;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.IssueLabelAdapter;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;

public class IssueLabelListActivity extends LoadingFragmentActivity implements OnItemClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private EditActionMode mActionMode;
    private ProgressDialog mProgressDialog;
    private Label mAddedLabel;
    private boolean mShouldStartAdding;

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>() {
        @Override
        public Loader<LoaderResult<List<Label>>> onCreateLoader(int id, Bundle args) {
            return new LabelListLoader(IssueLabelListActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<Label>> result) {
            boolean success = !result.handleError(IssueLabelListActivity.this);
            stopProgressDialog(mProgressDialog);
            UiUtils.hideImeForView(getCurrentFocus());
            if (success) {
                mAdapter.clear();
                mAdapter.addAll(result.getData());
                mAdapter.notifyDataSetChanged();
            }
            setContentEmpty(!success);
            setContentShown(true);
        }
    };

    private IssueLabelAdapter mAdapter = new IssueLabelAdapter(this) {
        @Override
        protected void bindView(View view, Label label) {
            ViewHolder holder = (ViewHolder) view.getTag();

            super.bindView(view, label);

            if (label == mAddedLabel && mShouldStartAdding) {
                holder.editor.setHint(R.string.issue_label_new);
                mActionMode = new EditActionMode(label, holder.editor);
                startActionMode(mActionMode);
                mShouldStartAdding = false;
            } else {
                holder.editor.setHint(null);
            }

            Label editingLabel = mActionMode != null ? mActionMode.mLabel : null;
            if (label.equals(editingLabel)) {
                setExpanded(view, true);
                mActionMode.setEditor(holder.editor);
            } else {
                setExpanded(view, false);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.NAME);

        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.issue_label_list);
        setContentShown(false);

        ListView listView = (ListView) findViewById(R.id.main_content);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_manage_labels);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mLabelCallback);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionMode == null) {
            mActionMode = new EditActionMode(mAdapter.getItem(position),
                    (EditText) view.findViewById(R.id.et_label));
            mAdapter.setExpanded(view, true);
            startActionMode(mActionMode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.create_new, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openIssueListActivity(this, mRepoOwner, mRepoName,
                Constants.Issue.STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.create_new:
            if (mActionMode == null) {
                mAddedLabel = new Label();
                mAddedLabel.setColor("dddddd");
                mAdapter.add(mAddedLabel);
                mShouldStartAdding = true;
                mAdapter.notifyDataSetChanged();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final class EditActionMode implements ActionMode.Callback {
        private String mCurrentLabelName;
        private EditText mEditor;
        private Label mLabel;

        public EditActionMode(Label label, EditText editor) {
            mCurrentLabelName = label.getName();
            mEditor = editor;
            mLabel = label;

            mEditor.setText(mCurrentLabelName);
        }

        public void setEditor(EditText editor) {
            Editable currentText = mEditor.getText();
            mEditor = editor;
            mEditor.setText(currentText);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.save)
                .setIcon(UiUtils.resolveDrawable(IssueLabelListActivity.this, R.attr.saveIcon))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            if (mLabel != mAddedLabel) {
                menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, R.string.delete)
                    .setIcon(UiUtils.resolveDrawable(IssueLabelListActivity.this, R.attr.discardIcon))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
                String labelName = mEditor.getText().toString();
                String color = (String) mEditor.getTag();
                if (mLabel == mAddedLabel) {
                    new AddIssueLabelTask(labelName, color).execute();
                    mLabel = null;
                } else {
                    new EditIssueLabelTask(mCurrentLabelName, labelName, color).execute();
                }
                break;
            case Menu.FIRST + 1:
                AlertDialog.Builder builder = UiUtils.createDialogBuilder(IssueLabelListActivity.this);
                builder.setTitle(getString(R.string.issue_dialog_delete_title, mCurrentLabelName));
                builder.setMessage(R.string.issue_dialog_delete_message);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new DeleteIssueLabelTask(mCurrentLabelName).execute();
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
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
            if (mLabel == mAddedLabel) {
                mAdapter.remove(mLabel);
                mAddedLabel = null;
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    private class DeleteIssueLabelTask extends ProgressDialogTask<Void> {
        private String mLabelName;

        public DeleteIssueLabelTask(String labelName) {
            super(IssueLabelListActivity.this, 0, R.string.deleting_msg);
            mLabelName = labelName;
        }

        @Override
        protected Void run() throws IOException {
            LabelService labelService = (LabelService)
                    Gh4Application.get(mContext).getService(Gh4Application.LABEL_SERVICE);
            labelService.deleteLabel(mRepoOwner, mRepoName, mLabelName);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            getSupportLoaderManager().getLoader(0).onContentChanged();
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
        protected Void run() throws IOException {
            LabelService labelService = (LabelService)
                    Gh4Application.get(mContext).getService(Gh4Application.LABEL_SERVICE);

            Label label = new Label();
            label.setName(mNewLabelName);
            label.setColor(mColor);

            labelService.editLabel(new RepositoryId(mRepoOwner, mRepoName), mOldLabelName, label);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            getSupportLoaderManager().getLoader(0).onContentChanged();
        }

        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_edit_label);
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
        protected Void run() throws IOException {
            LabelService labelService = (LabelService)
                    Gh4Application.get(mContext).getService(Gh4Application.LABEL_SERVICE);

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
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_create_label);
            mAdapter.remove(mAddedLabel);
            mAddedLabel = null;
        }
    }
}