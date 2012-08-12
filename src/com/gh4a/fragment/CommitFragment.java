package com.gh4a.fragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Constants;
import com.gh4a.DiffViewerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.GravatarUtils;

public class CommitFragment extends BaseFragment {

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
 
    public static CommitFragment newInstance(String repoOwner, String repoName, String objectSha) {
        CommitFragment f = new CommitFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putString(Constants.Object.OBJECT_SHA, objectSha);
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        mObjectSha = getArguments().getString(Constants.Object.OBJECT_SHA);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.commit, container, false);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showLoading();
        new LoadCommitInfoTask(this).execute();
    }
    
    private static class LoadCommitInfoTask extends AsyncTask<Void, Integer, RepositoryCommit> {

        private WeakReference<CommitFragment> mTarget;
        private boolean mException;

        public LoadCommitInfoTask(CommitFragment fragment) {
            mTarget = new WeakReference<CommitFragment>(fragment);
        }

        @Override
        protected RepositoryCommit doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    CommitFragment fragment = mTarget.get();
                    BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) fragment.getSherlockActivity();
                    
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(activity.getAuthToken());
                    CommitService commitService = new CommitService(client);
                    return commitService.getCommit(new RepositoryId(fragment.mRepoOwner, fragment.mRepoName),
                            fragment.mObjectSha);
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
        protected void onPostExecute(RepositoryCommit result) {
            if (mTarget.get() != null) {
                CommitFragment fragment = mTarget.get();
                fragment.hideLoading();
                BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) fragment.getSherlockActivity();
                if (mException) {
                    activity.showError();
                }
                else {
                    mTarget.get().fillData(result);
                }
            }
        }

    }

    private void fillData(final RepositoryCommit commit) {
        if (getSherlockActivity() == null) {
            return;
        }
        View v = getView();
        final BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) getSherlockActivity();
        final Gh4Application context = activity.getApplicationContext();
        LinearLayout llChanged = (LinearLayout) v.findViewById(R.id.ll_changed);
        LinearLayout llAdded = (LinearLayout) v.findViewById(R.id.ll_added);
        LinearLayout llDeleted = (LinearLayout) v.findViewById(R.id.ll_deleted);

        ImageView ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        
        AQuery aq = new AQuery(getSherlockActivity());
        aq.id(R.id.iv_gravatar).image(GravatarUtils.getGravatarUrl(CommitUtils.getAuthorGravatarId(commit)), 
                true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
        
        if (CommitUtils.getAuthorLogin(commit) != null) {
            ivGravatar.setOnClickListener(new OnClickListener() {
    
                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    context.openUserInfoActivity(activity,
                            CommitUtils.getAuthorLogin(commit), null);
                }
            });
        }
        
        TextView tvMessage = (TextView) v.findViewById(R.id.tv_message);
        TextView tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        TextView tvSummary = (TextView) v.findViewById(R.id.tv_desc);
        
        TextView tvChangeTitle = (TextView) v.findViewById(R.id.commit_changed);
        tvChangeTitle.setTypeface(context.boldCondensed);
        tvChangeTitle.setTextColor(Color.parseColor("#0099cc"));
        
        TextView tvAddedTitle = (TextView) v.findViewById(R.id.commit_added);
        tvAddedTitle.setTypeface(context.boldCondensed);
        tvAddedTitle.setTextColor(Color.parseColor("#0099cc"));
        
        TextView tvDeletedTitle = (TextView) v.findViewById(R.id.commit_deleted);
        tvDeletedTitle.setTypeface(context.boldCondensed);
        tvDeletedTitle.setTextColor(Color.parseColor("#0099cc"));
        
        tvMessage.setText(commit.getCommit().getMessage());
        
        long now = System.currentTimeMillis();
        
        tvExtra.setText(CommitUtils.getAuthorName(commit)
                + " "
                + context.pt.format(CommitUtils.convertCommitDateTime(commit.getCommit().getAuthor().getDate())));

        List<CommitFile> addedFiles = new ArrayList<CommitFile>();
        List<CommitFile> removedFiles = new ArrayList<CommitFile>();
        List<CommitFile> modifiedFiles = new ArrayList<CommitFile>();
        
        //List<String> addedList = commit.getAdded();
        List<CommitFile> commitFiles = commit.getFiles();
        for (CommitFile commitFile : commitFiles) {
            String status = commitFile.getStatus();
            if ("added".equals(status)) {
                addedFiles.add(commitFile);
            }
            else if ("modified".equals(status)) {
                modifiedFiles.add(commitFile);
            }
            else if ("removed".equals(status)) {
                removedFiles.add(commitFile);
            }
        }
        
        for (final CommitFile file: addedFiles) {
            TextView tvFilename = new TextView(context);
            tvFilename.setText(file.getFilename());
            tvFilename.setTypeface(Typeface.MONOSPACE);
            tvFilename.setTextColor(Color.parseColor("#0099cc"));
            tvFilename.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            tvFilename.setPadding(0, 10, 0, 10);
            tvFilename.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent().setClass(activity,
                            DiffViewerActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
                    intent.putExtra(Constants.Commit.DIFF, file.getPatch());
                    intent.putExtra(Constants.Object.PATH, file.getFilename());
                    intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());
                    startActivity(intent);
                }
            });
            
            llAdded.addView(tvFilename);
        }
        
        for (final CommitFile file: removedFiles) {
            TextView tvFilename = new TextView(context);
            tvFilename.setText(file.getFilename());
            tvFilename.setPadding(0, 10, 0, 10);
            tvFilename.setTypeface(Typeface.MONOSPACE);
            
            llDeleted.addView(tvFilename);
        }

        for (final CommitFile file: modifiedFiles) {
            TextView tvFilename = new TextView(context);
            tvFilename.setText(file.getFilename());
            tvFilename.setTypeface(Typeface.MONOSPACE);
            tvFilename.setTextColor(Color.parseColor("#0099cc"));
            tvFilename.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            tvFilename.setPadding(0, 10, 0, 10);
            tvFilename.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent().setClass(activity,
                            DiffViewerActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
                    intent.putExtra(Constants.Commit.DIFF, file.getPatch());
                    intent.putExtra(Constants.Object.PATH, file.getFilename());
                    intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());
                    startActivity(intent);
                }
            });
            
            llChanged.addView(tvFilename);
        }

        if (addedFiles.size() == 0) {
            TextView tvFilename = new TextView(context);
            tvFilename.setText(R.string.commit_no_files);
            llAdded.addView(tvFilename);
        }
        
        if (removedFiles.size() == 0) {
            TextView tvFilename = new TextView(context);
            tvFilename.setText(R.string.commit_no_files);
            llDeleted.addView(tvFilename);
        }
        
        if (modifiedFiles.size() == 0) {
            TextView tvFilename = new TextView(context);
            tvFilename.setText(R.string.commit_no_files);
            llChanged.addView(tvFilename);
        }
        
        tvSummary.setText(String.format(getResources().getString(R.string.commit_summary),
                commit.getFiles().size(), commit.getStats().getAdditions(), commit.getStats().getDeletions()));
    }
}
