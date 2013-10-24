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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.LabelService;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;

public class IssueLabelListActivity extends BaseSherlockFragmentActivity implements OnItemClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private ActionMode mActionMode;
    private ProgressDialog mProgressDialog;
    private LabelAdapter mAdapter;

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
                mAdapter.setNotifyOnChange(false);
                mAdapter.clear();
                for (Label label : result.getData()) {
                    mAdapter.add(label);
                }
                mAdapter.notifyDataSetChanged();
            }
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
        mAdapter = new LabelAdapter(this);
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
    public boolean setMenuOptionItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.create_new) {
            Intent intent = new Intent().setClass(this, IssueLabelCreateActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            startActivity(intent);
        }
        return true;
    }

    private static class LabelAdapter extends ArrayAdapter<Label> implements OnClickListener {
        public LabelAdapter(Context context) {
            super(context, R.layout.row_issue_label);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Label label = getItem(position);
            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.row_issue_label, parent, false);

                holder = new ViewHolder();
                holder.color = convertView.findViewById(R.id.view_color);
                holder.label = (TextView) convertView.findViewById(R.id.tv_title);
                holder.editor = (EditText) convertView.findViewById(R.id.et_label);
                convertView.setTag(holder);

                Gh4Application app = (Gh4Application) getContext().getApplicationContext();
                holder.label.setTypeface(app.condensed);

                ViewGroup colors = (ViewGroup) convertView.findViewById(R.id.colors);
                int count = colors.getChildCount();
                for (int i = 0; i < count; i++) {
                    colors.getChildAt(i).setOnClickListener(this);
                }
                colors.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            assignColor(holder, label.getColor());

            holder.label.setText(label.getName());
            holder.editor.setText(label.getName());

            return convertView;
        }

        private static class ViewHolder {
            View color;
            TextView label;
            EditText editor;
        }

        private void assignColor(ViewHolder holder, String colorString) {
            int color = Color.parseColor("#" + colorString);
            boolean dark = Color.red(color) + Color.green(color) + Color.blue(color) < 383;

            holder.color.setBackgroundColor(color);
            holder.editor.setBackgroundColor(color);
            holder.editor.setTag(colorString);
            holder.editor.setTextColor(getContext().getResources().getColor(
                    dark ? R.color.abs__primary_text_holo_dark : R.color.abs__primary_text_holo_light));
        }

        @Override
        public void onClick(View v) {
            ViewHolder holder = (ViewHolder) ((View) v.getParent()).getTag();
            assignColor(holder, (String) v.getTag());
        }
    }

    private final class EditActionMode implements ActionMode.Callback {
        private String mCurrentLabelName;
        private Label mLabel;
        private View mItemContainer;
        
        public EditActionMode(Label label, View itemContainer) {
            mCurrentLabelName = label.getName();
            mLabel = label;
            mItemContainer = itemContainer;

            itemContainer.findViewById(R.id.collapsed).setVisibility(View.GONE);
            itemContainer.findViewById(R.id.expanded).setVisibility(View.VISIBLE);
        }
        
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            boolean isLight = Gh4Application.THEME == R.style.LightTheme;
            menu.add(0, 0, 0, R.string.save)
                .setIcon(isLight ? R.drawable.content_save : R.drawable.content_save_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.add(0, 1, 1, R.string.delete)
                .setIcon(isLight ? R.drawable.content_discard: R.drawable.content_discard_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case 0:
                EditText editor = (EditText) mItemContainer.findViewById(R.id.et_label);
                String color = (String) editor.getTag();

                mLabel.setColor(color);
                mProgressDialog = showProgressDialog(getResources().getString(R.string.saving_msg), true);
                new EditIssueLabelsTask(IssueLabelListActivity.this).execute(mCurrentLabelName, 
                        editor.getText().toString(), color);
                break;
            case 1:
                AlertDialog.Builder builder = createDialogBuilder();
                builder.setTitle(getString(R.string.issue_dialog_delete_title, mCurrentLabelName));
                builder.setMessage(R.string.issue_dialog_delete_message);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new DeleteIssueLabelsTask(IssueLabelListActivity.this).execute(mCurrentLabelName);
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
            mAdapter.notifyDataSetChanged();
        }
        
    }
    
    private static class DeleteIssueLabelsTask extends AsyncTask<String, Void, Void> {
        private WeakReference<IssueLabelListActivity> mTarget;
        private boolean mException;
        
        public DeleteIssueLabelsTask(IssueLabelListActivity activity) {
            mTarget = new WeakReference<IssueLabelListActivity>(activity);
        }

        @Override
        protected Void doInBackground(String... params) {
            IssueLabelListActivity activity = mTarget.get();
            if (activity == null) {
                return null;
            }
            try {
                GitHubClient client = new GitHubClient();
                client.setOAuth2Token(activity.getAuthToken());
                LabelService labelService = new LabelService(client);

                String labelName = params[0];
                labelService.deleteLabel(activity.mRepoOwner, activity.mRepoName, labelName);
            }
            catch (IOException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                mException = true;
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            IssueLabelListActivity activity = mTarget.get();
            if (activity != null) {
                activity.mProgressDialog = activity.showProgressDialog(
                        activity.getString(R.string.deleting_msg), true);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            IssueLabelListActivity activity = mTarget.get();
            if (activity != null) {
                if (mException) {
                    activity.stopProgressDialog(activity.mProgressDialog);
                    activity.showError();
                }
                else {
                    activity.getSupportLoaderManager().restartLoader(0, null, activity.mLabelCallback).forceLoad();
                }
            }
        }
    }
    
    private static class EditIssueLabelsTask extends AsyncTask<String, Void, Void> {
        private WeakReference<IssueLabelListActivity> mTarget;
        private boolean mException;
        
        public EditIssueLabelsTask(IssueLabelListActivity activity) {
            mTarget = new WeakReference<IssueLabelListActivity>(activity);
        }

        @Override
        protected Void doInBackground(String... params) {
            IssueLabelListActivity activity = mTarget.get();
            if (activity == null) {
                return null;
            }
            try {
                GitHubClient client = new GitHubClient();
                client.setOAuth2Token(activity.getAuthToken());
                LabelService labelService = new LabelService(client);

                String labelName = params[0];
                String newLabelName = params[1];
                String color = params[2];

                Label label = new Label();
                label.setName(newLabelName);
                label.setColor(color);

                labelService.editLabel(new RepositoryId(activity.mRepoOwner, activity.mRepoName), labelName, label);
            }
            catch (IOException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                mException = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            IssueLabelListActivity activity = mTarget.get();
            if (activity != null) {
                if (mException) {
                    activity.stopProgressDialog(activity.mProgressDialog);
                    activity.showMessage(activity.getString(R.string.issue_error_edit_label), false);
                }
                else {
                    activity.getSupportLoaderManager().restartLoader(0, null, activity.mLabelCallback).forceLoad();
                }
            }
        }
    }
}