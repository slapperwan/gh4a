package com.gh4a.fragment;

import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.devspark.progressfragment.ProgressFragment;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.DiffViewerActivity;
import com.gh4a.loader.CommitLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.GravatarUtils;

public class CommitFragment extends ProgressFragment implements OnClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private RepositoryCommit mCommit;
    private View mContentView;

    private LoaderCallbacks<RepositoryCommit> mCommitCallback = new LoaderCallbacks<RepositoryCommit>() {
        @Override
        public Loader<LoaderResult<RepositoryCommit>> onCreateLoader(int id, Bundle args) {
            return new CommitLoader(getActivity(), mRepoOwner, mRepoName, mObjectSha);
        }

        @Override
        public void onResultReady(LoaderResult<RepositoryCommit> result) {
            setContentShown(true);
            if (!result.handleError(getActivity())) {
                mCommit = result.getData();
                fillData();
            } else {
                setContentEmpty(true);
            }
        }
    };
    
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
        mContentView = inflater.inflate(R.layout.commit, null);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setContentView(mContentView);
        setContentShown(false);

        getLoaderManager().initLoader(0, null, mCommitCallback);
    }

    private void fillData() {
        final Activity activity = getActivity();
        final Gh4Application app = Gh4Application.get(activity);
        final AQuery aq = new AQuery(activity);
        final LayoutInflater inflater = getLayoutInflater(null);
        
        LinearLayout llChanged = (LinearLayout) mContentView.findViewById(R.id.ll_changed);
        LinearLayout llAdded = (LinearLayout) mContentView.findViewById(R.id.ll_added);
        LinearLayout llDeleted = (LinearLayout) mContentView.findViewById(R.id.ll_deleted);
        int added = 0, changed = 0, deleted = 0;
        
        ImageView ivGravatar = (ImageView) mContentView.findViewById(R.id.iv_gravatar);
        
        aq.id(R.id.iv_gravatar).image(
                GravatarUtils.getGravatarUrl(CommitUtils.getAuthorGravatarId(activity, mCommit)), 
                true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);

        String login = CommitUtils.getAuthorLogin(app, mCommit);
        if (login != null) {
            ivGravatar.setOnClickListener(this);
            ivGravatar.setTag(login);
        }
        
        TextView tvMessage = (TextView) mContentView.findViewById(R.id.tv_message);
        tvMessage.setText(mCommit.getCommit().getMessage());
        
        TextView tvExtra = (TextView) mContentView.findViewById(R.id.tv_extra);
        tvExtra.setText(CommitUtils.getAuthorName(app, mCommit)
                + " "
                + Gh4Application.pt.format(mCommit.getCommit().getAuthor().getDate()));

        for (CommitFile file : mCommit.getFiles()) {
            String status = file.getStatus();
            final LinearLayout parent;

            if ("added".equals(status)) {
                parent = llAdded;
                added++;
            } else if ("modified".equals(status)) {
                parent = llChanged;
                changed++;
            } else if ("removed".equals(status)) {
                parent = llDeleted;
                deleted++;
            } else {
                continue;
            }

            TextView fileNameView = (TextView) inflater.inflate(R.layout.commit_filename, parent, false);
            fileNameView.setText(file.getFilename());
            fileNameView.setTag(file);
            if (parent != llDeleted) {
                fileNameView.setTextColor(getResources().getColor(R.color.highlight));
                fileNameView.setOnClickListener(this);
            }
            parent.addView(fileNameView);
        }
        
        if (added == 0) {
            llAdded.setVisibility(View.GONE);
        } else {
            TextView tvAddedTitle = (TextView) mContentView.findViewById(R.id.commit_added);
            tvAddedTitle.setTypeface(app.boldCondensed);
        }
        if (changed == 0) {
            llChanged.setVisibility(View.GONE);
        } else {
            TextView tvChangeTitle = (TextView) mContentView.findViewById(R.id.commit_changed);
            tvChangeTitle.setTypeface(app.boldCondensed);
        }
        if (deleted == 0) {
            llDeleted.setVisibility(View.GONE);
        } else {
            TextView tvDeletedTitle = (TextView) mContentView.findViewById(R.id.commit_deleted);
            tvDeletedTitle.setTypeface(app.boldCondensed);
        }
        
        TextView tvSummary = (TextView) mContentView.findViewById(R.id.tv_desc);
        tvSummary.setText(getString(R.string.commit_summary, mCommit.getFiles().size(),
                mCommit.getStats().getAdditions(), mCommit.getStats().getDeletions()));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            String login = (String) v.getTag();
            Activity activity = getActivity();
            
            /** Open user activity */
            Gh4Application.get(activity).openUserInfoActivity(activity, login, null);
        } else {
            CommitFile file = (CommitFile) v.getTag();

            Intent intent = new Intent(getActivity(), DiffViewerActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
            intent.putExtra(Constants.Commit.DIFF, file.getPatch());
            intent.putExtra(Constants.Object.PATH, file.getFilename());
            intent.putExtra(Constants.Object.TREE_SHA, mCommit.getCommit().getTree().getSha());
            startActivity(intent);
        }
    }
}