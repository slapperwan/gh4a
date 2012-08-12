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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.LabelService;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants.LoaderResult;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.utils.StringUtils;

public class IssueLabelListActivity extends BaseSherlockFragmentActivity 
    implements LoaderManager.LoaderCallbacks<Object> {

    private String mRepoOwner;
    private String mRepoName;
    protected ListView mListView;
    private ActionMode mActionMode;
    private String mSelectedColor;
    private List<Map<String, Object>> mAllLabelLayout;
    private String mSelectedLabel;
    private ProgressDialog mProgressDialog;
    private List<Label> mLabels;
    private EditText mSelectedEtLabel;

    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.ll_placeholder);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_manage_labels);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData() {
        final Typeface condensed = getApplicationContext().condensed;
        LinearLayout ll = (LinearLayout) findViewById(R.id.main_content);
        ll.removeAllViews();
        
        mAllLabelLayout = new ArrayList<Map<String, Object>>();
        for (final Label label : mLabels) {
            Map<String, Object> selectedLabelItems = new HashMap<String, Object>();
            selectedLabelItems.put("label", label);
            
            final View rowView = getLayoutInflater().inflate(R.layout.row_issue_label, null);
            final View viewColor = (View) rowView.findViewById(R.id.view_color);

            final LinearLayout llEdit = (LinearLayout) rowView.findViewById(R.id.ll_edit);
            selectedLabelItems.put("llEdit", llEdit);
            
            final EditText etLabel = (EditText) rowView.findViewById(R.id.et_label);
            selectedLabelItems.put("etLabel", etLabel);
            
            final TextView tvLabel = (TextView) rowView.findViewById(R.id.tv_title);
            selectedLabelItems.put("tvLabel", tvLabel);
            
            tvLabel.setTypeface(condensed);
            tvLabel.setText(label.getName());
            
            viewColor.setBackgroundColor(Color.parseColor("#" + label.getColor()));
            selectedLabelItems.put("viewColor", viewColor);
            mAllLabelLayout.add(selectedLabelItems);
            
            viewColor.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (llEdit.getVisibility() == View.VISIBLE) {
                        llEdit.setVisibility(View.GONE);
                        unselectLabel(tvLabel, viewColor, label.getColor());
                    }
                    else {
                        llEdit.setVisibility(View.VISIBLE);
                        selectLabel(tvLabel, viewColor, label.getColor(), true, etLabel);
                        etLabel.setText(label.getName());
                        mActionMode = startActionMode(
                                new EditActionMode(label.getName(), etLabel));
                    }
                }
            });
            
            tvLabel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout llEdit = (LinearLayout) rowView.findViewById(R.id.ll_edit);
                    if (llEdit.getVisibility() == View.VISIBLE) {
                        llEdit.setVisibility(View.GONE);
                        unselectLabel(tvLabel, viewColor, label.getColor());
                    }
                    else {
                        llEdit.setVisibility(View.VISIBLE);
                        selectLabel(tvLabel, viewColor, label.getColor(), true, etLabel);
                        etLabel.setText(label.getName());
                        mActionMode = startActionMode(
                                new EditActionMode(label.getName(), etLabel));
                    }
                }
            });
            
            if (!StringUtils.isBlank(mSelectedLabel)
                    && mSelectedLabel.equals(label.getName())) {
                selectLabel(tvLabel, viewColor, label.getColor(), false, etLabel);
                llEdit.setVisibility(View.VISIBLE);
                etLabel.setText(label.getName());
            }
            
            final View color1 = (View) rowView.findViewById(R.id.color_444444);
            color1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color1.getTag(), false, etLabel); }
            });
            
            final View color2 = (View) rowView.findViewById(R.id.color_02d7e1);
            color2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color2.getTag(), false, etLabel); }
            });
            
            final View color3 = (View) rowView.findViewById(R.id.color_02e10c);
            color3.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color3.getTag(), false, etLabel); }
            });
            
            final View color4 = (View) rowView.findViewById(R.id.color_0b02e1);
            color4.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color4.getTag(), false, etLabel); }
            });
            
            final View color5 = (View) rowView.findViewById(R.id.color_d7e102);
            color5.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color5.getTag(), false, etLabel); }
            });
            
            final View color6 = (View) rowView.findViewById(R.id.color_DDDDDD);
            color6.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color6.getTag(), false, etLabel); }
            });
            
            final View color7 = (View) rowView.findViewById(R.id.color_e102d8);
            color7.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color7.getTag(), false, etLabel); }
            });
            
            final View color8 = (View) rowView.findViewById(R.id.color_e10c02);
            color8.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color8.getTag(), false, etLabel); }
            });
            
            ll.addView(rowView);
        }
    }

    private void selectLabel(TextView tvLabel, View viewColor, String color, 
            boolean clearOtherSelected, EditText etLabel) {
        final Typeface boldCondensed = getApplicationContext().boldCondensed;
        tvLabel.setTag(color);
        
        viewColor.setBackgroundColor(Color.parseColor("#" + color));
        tvLabel.setBackgroundColor(Color.parseColor("#" + color));
        int r = Color.red(Color.parseColor("#" + color));
        int g = Color.green(Color.parseColor("#" + color));
        int b = Color.blue(Color.parseColor("#" + color));
        if (r + g + b < 383) {
            tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_dark));
        }
        else {
            tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));
        }
        tvLabel.setTypeface(boldCondensed);
        
        mSelectedColor = color;
        mSelectedEtLabel = etLabel;
        mSelectedLabel = tvLabel.getText().toString();
        if (clearOtherSelected) {
            clearOtherSelected(false);
        }
    }
    
    private void unselectLabel(TextView tvLabel, View viewColor, String color) {
        final Typeface condensed = getApplicationContext().condensed;
        tvLabel.setTag(color);
        tvLabel.setBackgroundColor(0);
        if (Gh4Application.THEME == R.style.LightTheme) {
            tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));                            
        }
        else {
            tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_dark));
        }
        tvLabel.setTypeface(condensed);
        viewColor.setBackgroundColor(Color.parseColor("#" + color));
    }
    
    private void clearOtherSelected(boolean all) {
        for (Map<String, Object> m : mAllLabelLayout) {
            LinearLayout llEdit2 = (LinearLayout) m.get("llEdit");
            TextView tvLabel2 = (TextView) m.get("tvLabel");
            View viewColor2 = (View) m.get("viewColor");
            Label label2 = (Label) m.get("label");
            if (all) {
                unselectLabel(tvLabel2, viewColor2, label2.getColor());
                llEdit2.setVisibility(View.GONE);
            }
            else {
                if (!tvLabel2.getText().toString().equals(mSelectedLabel)) {
                    unselectLabel(tvLabel2, viewColor2, label2.getColor());
                    llEdit2.setVisibility(View.GONE);
                }
                else {
                    llEdit2.setVisibility(View.VISIBLE);
                }
            }
        }
        if (mActionMode != null) {
            mActionMode.finish();
        }
        mSelectedLabel = null;
    }
    
    private final class EditActionMode implements ActionMode.Callback {

        private String mCurrentLabelName;
        private EditText mNewLabelText;
        
        public EditActionMode(String currentLabelName, EditText newLabelText) {
            mCurrentLabelName = currentLabelName;
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
                mProgressDialog = showProgressDialog(getResources().getString(R.string.saving_msg), true);
                new EditIssueLabelsTask(IssueLabelListActivity.this).execute(mCurrentLabelName, 
                        mSelectedEtLabel.getText().toString(), IssueLabelListActivity.this.mSelectedColor);
                break;
            case 1:
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(IssueLabelListActivity.this,
                        android.R.style.Theme));
                builder.setTitle("Delete " + mCurrentLabelName + "?");
                builder.setMessage("Are you sure?");
                builder.setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        new DeleteIssueLabelsTask(IssueLabelListActivity.this).execute(mCurrentLabelName);
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
                break;
            default:
                break;
            }
            
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            fillData();
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
            if (mTarget.get() != null) {
                try {
                    IssueLabelListActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    LabelService labelService = new LabelService(client);
                    
                    String labelName = params[0];
                    labelService.deleteLabel(activity.mRepoOwner, activity.mRepoName, labelName);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mProgressDialog =
                        mTarget.get().showProgressDialog(mTarget.get().getResources().getString(R.string.deleting_msg), true);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueLabelListActivity activity = mTarget.get();
    
                if (mException) {
                    activity.stopProgressDialog(activity.mProgressDialog);
                    activity.showError();
                }
                else {
                    activity.getSupportLoaderManager().restartLoader(0, null, activity).forceLoad();
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
            if (mTarget.get() != null) {
                try {
                    IssueLabelListActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
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
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueLabelListActivity activity = mTarget.get();
                
                if (mException) {
                    activity.stopProgressDialog(activity.mProgressDialog);
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_edit_label), false);
                }
                else {
                    activity.getSupportLoaderManager().restartLoader(0, null, activity).forceLoad();
                }
            }
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
        switch (item.getItemId()) {
        case android.R.id.home:
            getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName, 
                    Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return true;
        case R.id.create_new:
            Intent intent = new Intent().setClass(this, IssueLabelCreateActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            startActivity(intent);
            return true;

        default:
            return true;
        }
    }

    @Override
    public Loader onCreateLoader(int arg0, Bundle arg1) {
        return new LabelListLoader(this, mRepoOwner, mRepoName);
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        HashMap<Integer, Object> result = (HashMap<Integer, Object>) object;
        hideLoading();
        stopProgressDialog(mProgressDialog);
        if (getCurrentFocus() != null) {
            hideKeyboard(getCurrentFocus().getWindowToken());
        }
        if (!isLoaderError(result)) {
            mLabels = (List<Label>) result.get(LoaderResult.DATA);
            fillData();
        }
    }

    @Override
    public void onLoaderReset(Loader arg0) {
        // TODO Auto-generated method stub
        
    }
}