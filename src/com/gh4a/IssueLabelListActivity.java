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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class IssueLabelListActivity extends BaseActivity {

    private String mRepoOwner;
    private String mRepoName;
    protected ListView mListView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.ll_placeholder);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_manage_labels);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        new LoadIssueLabelsTask(this).execute();
    }
    
    private static class LoadIssueLabelsTask extends AsyncTask<Void, Integer, List<Label>> {

        private WeakReference<IssueLabelListActivity> mTarget;
        private boolean mException;

        public LoadIssueLabelsTask(IssueLabelListActivity activity) {
            mTarget = new WeakReference<IssueLabelListActivity>(activity);
        }

        @Override
        protected List<Label> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    LabelService labelService = new LabelService(client);
                    return labelService.getLabels(mTarget.get().mRepoOwner,
                            mTarget.get().mRepoName);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(List<Label> result) {
            if (mTarget.get() != null) {
                IssueLabelListActivity activity = mTarget.get();
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillData(result);
                }
            }
        }
    }
    
    private void fillData(List<Label> result) {
        final Typeface boldCondensed = getApplicationContext().boldCondensed;
        final Typeface condensed = getApplicationContext().condensed;
        
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
        ll.removeAllViews();
        
        for (final Label label : result) {
            final View rowView = getLayoutInflater().inflate(R.layout.row_issue_label, null);
            final View viewColor = (View) rowView.findViewById(R.id.view_color);
            
            final EditText etLabel = (EditText) rowView.findViewById(R.id.et_label);
            final TextView tvLabel = (TextView) rowView.findViewById(R.id.tv_title);
            tvLabel.setTypeface(condensed);
            tvLabel.setText(label.getName());
            
            viewColor.setBackgroundColor(Color.parseColor("#" + label.getColor()));
            viewColor.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout llEdit = (LinearLayout) rowView.findViewById(R.id.ll_edit);
                    if (llEdit.getVisibility() == View.VISIBLE) {
                        llEdit.setVisibility(View.GONE);
                        
                        tvLabel.setTypeface(condensed);
                        unselectLabel(tvLabel, viewColor, label.getColor());
                    }
                    else {
                        llEdit.setVisibility(View.VISIBLE);
                        
                        tvLabel.setTypeface(boldCondensed);
                        selectLabel(tvLabel, viewColor, label.getColor());
                        etLabel.setText(label.getName());
                    }
                }
            });
            
            tvLabel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout llEdit = (LinearLayout) rowView.findViewById(R.id.ll_edit);
                    if (llEdit.getVisibility() == View.VISIBLE) {
                        llEdit.setVisibility(View.GONE);
                        
                        tvLabel.setTypeface(condensed);
                        unselectLabel(tvLabel, viewColor, label.getColor());
                    }
                    else {
                        llEdit.setVisibility(View.VISIBLE);
                        
                        tvLabel.setTypeface(boldCondensed);
                        selectLabel(tvLabel, viewColor, label.getColor());
                        etLabel.setText(label.getName());
                    }
                }
            });
            
            final View color1 = (View) rowView.findViewById(R.id.color_444444);
            color1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color1.getTag());}
            });
            
            final View color2 = (View) rowView.findViewById(R.id.color_02d7e1);
            color2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color2.getTag());}
            });
            
            final View color3 = (View) rowView.findViewById(R.id.color_02e10c);
            color3.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color3.getTag());}
            });
            
            final View color4 = (View) rowView.findViewById(R.id.color_0b02e1);
            color4.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color4.getTag());}
            });
            
            final View color5 = (View) rowView.findViewById(R.id.color_d7e102);
            color5.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color5.getTag());}
            });
            
            final View color6 = (View) rowView.findViewById(R.id.color_DDDDDD);
            color6.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color6.getTag());}
            });
            
            final View color7 = (View) rowView.findViewById(R.id.color_e102d8);
            color7.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color7.getTag());}
            });
            
            final View color8 = (View) rowView.findViewById(R.id.color_e10c02);
            color8.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { selectLabel(tvLabel, viewColor, (String) color8.getTag());}
            });
            
            Button btnSave = (Button) rowView.findViewById(R.id.btn_save);
            btnSave.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    String selectedColor = (String) tvLabel.getTag();
                    String newLabelName = etLabel.getText().toString();
                    new AddIssueLabelsTask(IssueLabelListActivity.this).execute(label.getName(), 
                            newLabelName, selectedColor, "true");
                }
            });
            
            Button btnDelete = (Button) rowView.findViewById(R.id.btn_delete);
            btnDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(IssueLabelListActivity.this,
                            android.R.style.Theme));
                    builder.setTitle("Delete " + label.getName() + "?");
                    builder.setMessage("Are you sure?");
                    builder.setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            new DeleteIssueLabelsTask(IssueLabelListActivity.this).execute(label.getName());
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
            });
            
            ll.addView(rowView);
        }
    }

    private void selectLabel(TextView tvLabel, View viewColor, String color) {
        tvLabel.setTag(color);
        
        viewColor.setBackgroundColor(Color.parseColor("#" + color));
        tvLabel.setBackgroundColor(Color.parseColor("#" + color));
        int r = Color.red(Color.parseColor("#" + color));
        int g = Color.green(Color.parseColor("#" + color));
        int b = Color.blue(Color.parseColor("#" + color));
        if (r + g + b < 383) {
            tvLabel.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
        }
        else {
            tvLabel.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        }                    
    }
    
    private void unselectLabel(TextView tvLabel, View viewColor, String color) {
        tvLabel.setTag(color);
        tvLabel.setBackgroundColor(Color.WHITE);
        tvLabel.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        viewColor.setBackgroundColor(Color.parseColor("#" + color));
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
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueLabelListActivity activity = mTarget.get();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    new LoadIssueLabelsTask(activity).execute();
                }
            }
        }
    }
    
    private static class AddIssueLabelsTask extends AsyncTask<String, Void, Void> {

        private WeakReference<IssueLabelListActivity> mTarget;
        private boolean mException;
        
        public AddIssueLabelsTask(IssueLabelListActivity activity) {
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
                    boolean edit = Boolean.valueOf(params[3]);
                    //labelName = StringUtils.encodeUrl(labelName);
                    
                    Label label = new Label();
                    label.setName(newLabelName);
                    label.setColor(color);
                    
                    if (edit) {
                        labelService.editLabel(new RepositoryId(activity.mRepoOwner, activity.mRepoName), labelName, label);
                    }
                    else {
                        labelService.createLabel(activity.mRepoOwner, activity.mRepoName, label);
                    }
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
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_create_label), false);
                }
                else {
                    new LoadIssueLabelsTask(activity).execute();
                }
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.issue_labels, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.new_label:
            Intent intent = new Intent().setClass(this, IssueLabelCreateActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            startActivity(intent);
            return true;

        default:
            return true;
        }
    }
}