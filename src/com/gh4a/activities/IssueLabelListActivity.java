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
import android.view.View;
import android.view.ViewGroup;
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
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.IssueLabelAdapter;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;

public class IssueLabelListActivity extends BaseSherlockFragmentActivity implements OnItemClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private ActionMode mActionMode;
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
            hideLoading();
            stopProgressDialog(mProgressDialog);
            if (getCurrentFocus() != null) {
                hideKeyboard(getCurrentFocus().getWindowToken());
            }
            if (!isLoaderError(result)) {
                mAdapter.clear();
                for (Label label : result.getData()) {
                    mAdapter.add(label);
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private IssueLabelAdapter mAdapter = new IssueLabelAdapter(this) {
        @Override
        public View doGetView(int position, View convertView, ViewGroup parent) {
            View view = super.doGetView(position, convertView, parent);
            ViewHolder holder = (ViewHolder) view.getTag();
            Label label = getItem(position);

            if (label == mAddedLabel && mShouldStartAdding) {
                holder.editor.setHint(R.string.issue_label_new);
                mActionMode = startActionMode(new EditActionMode(label, view));
                mShouldStartAdding = false;
            } else {
                holder.editor.setHint(null);
            }

            return view;
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.issue_label_list);

        ListView listView = (ListView) findViewById(R.id.main_content);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_manage_labels);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        getSupportLoaderManager().initLoader(0, null, mLabelCallback);
        getSupportLoaderManager().getLoader(0).forceLoad();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionMode == null) {
            mActionMode = startActionMode(new EditActionMode(mAdapter.getItem(position), view));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.create_new, menu);
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.content_new_dark);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openIssueListActivity(this, mRepoOwner, mRepoName,
                Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
        private View mItemContainer;
        private EditText mEditor;
        private Label mLabel;

        public EditActionMode(Label label, View itemContainer) {
            mCurrentLabelName = label.getName();
            mItemContainer = itemContainer;
            mLabel = label;

            mEditor = (EditText) itemContainer.findViewById(R.id.et_label);
            mEditor.setText(mCurrentLabelName);

            itemContainer.findViewById(R.id.collapsed).setVisibility(View.GONE);
            itemContainer.findViewById(R.id.expanded).setVisibility(View.VISIBLE);
        }
        
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            boolean isLight = Gh4Application.THEME == R.style.LightTheme;
            menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.save)
                .setIcon(isLight ? R.drawable.content_save : R.drawable.content_save_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            if (mLabel != mAddedLabel) {
                menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, R.string.delete)
                    .setIcon(isLight ? R.drawable.content_discard: R.drawable.content_discard_dark)
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
                AlertDialog.Builder builder = createDialogBuilder();
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
            mItemContainer.findViewById(R.id.collapsed).setVisibility(View.VISIBLE);
            mItemContainer.findViewById(R.id.expanded).setVisibility(View.GONE);
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
                    mContext.getApplicationContext().getSystemService(Gh4Application.LABEL_SERVICE);
            labelService.deleteLabel(mRepoOwner, mRepoName, mLabelName);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            getSupportLoaderManager().restartLoader(0, null, mLabelCallback).forceLoad();
        }
        
        @Override
        protected void onError(Exception e) {
            showError();
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
                    mContext.getApplicationContext().getSystemService(Gh4Application.LABEL_SERVICE);

            Label label = new Label();
            label.setName(mNewLabelName);
            label.setColor(mColor);

            labelService.editLabel(new RepositoryId(mRepoOwner, mRepoName), mOldLabelName, label);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            getSupportLoaderManager().restartLoader(0, null, mLabelCallback).forceLoad();
        }
        
        @Override
        protected void onError(Exception e) {
            showMessage(getString(R.string.issue_error_edit_label), false);
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
                    mContext.getApplicationContext().getSystemService(Gh4Application.LABEL_SERVICE);

            Label label = new Label();
            label.setName(mLabelName);
            label.setColor(mColor);

            labelService.createLabel(mRepoOwner, mRepoName, label);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            getSupportLoaderManager().restartLoader(0, null, mLabelCallback).forceLoad();
            mAddedLabel = null;
        }
        
        @Override
        protected void onError(Exception e) {
            showMessage(getString(R.string.issue_error_create_label), false);
            mAdapter.remove(mAddedLabel);
            mAddedLabel = null;
        }
    }
}