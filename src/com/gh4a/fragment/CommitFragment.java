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

import com.devspark.progressfragment.ProgressFragment;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.loader.CommitLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.GravatarHandler;

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
            boolean success = !result.handleError(getActivity());
            if (success) {
                mCommit = result.getData();
                fillData();
            }
            setContentEmpty(!success);
            setContentShown(true);
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
        final LayoutInflater inflater = getLayoutInflater(null);
        
        LinearLayout llChanged = (LinearLayout) mContentView.findViewById(R.id.ll_changed);
        LinearLayout llAdded = (LinearLayout) mContentView.findViewById(R.id.ll_added);
        LinearLayout llDeleted = (LinearLayout) mContentView.findViewById(R.id.ll_deleted);
        int added = 0, changed = 0, deleted = 0;
        
        ImageView ivGravatar = (ImageView) mContentView.findViewById(R.id.iv_gravatar);
        GravatarHandler.assignGravatar(ivGravatar, mCommit.getAuthor());

        String login = CommitUtils.getAuthorLogin(app, mCommit);
        if (login != null) {
            ivGravatar.setOnClickListener(this);
            ivGravatar.setTag(login);
        }

        TextView tvMessage = (TextView) mContentView.findViewById(R.id.tv_message);
        TextView tvTitle = (TextView) mContentView.findViewById(R.id.tv_title);

        String message = mCommit.getCommit().getMessage();
        int pos = message.indexOf('\n');
        String title = pos > 0 ? message.substring(0, pos) : message;
        int length = message.length();
        while (pos > 0 && pos < length && Character.isWhitespace(message.charAt(pos))) {
            pos++;
        }
        message = pos > 0 && pos < length ? message.substring(pos) : null;

        tvTitle.setText(title);
        if (message != null) {
            tvMessage.setText(message);
        } else {
            tvMessage.setVisibility(View.GONE);
        }
        
        TextView tvExtra = (TextView) mContentView.findViewById(R.id.tv_extra);
        tvExtra.setText(CommitUtils.getAuthorName(app, mCommit)
                + " "
                + Gh4Application.pt.format(mCommit.getCommit().getAuthor().getDate()));

        if (!CommitUtils.authorEqualsCommitter(mCommit)) {
            ViewGroup committer = (ViewGroup) mContentView.findViewById(R.id.committer_info);
            ImageView gravatar = (ImageView) committer.findViewById(R.id.iv_commit_gravatar);
            TextView extra = (TextView) committer.findViewById(R.id.tv_commit_extra);

            committer.setVisibility(View.VISIBLE);
            GravatarHandler.assignGravatar(gravatar, mCommit.getCommitter());
            extra.setText(getString(R.string.commit_details, CommitUtils.getCommitterName(app, mCommit),
                    Gh4Application.pt.format(mCommit.getCommit().getCommitter().getDate())));
        }

        int count = mCommit.getFiles() != null ? mCommit.getFiles().size() : 0;
        for (int i = 0; i < count; i++) {
            CommitFile file = mCommit.getFiles().get(i);
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
        if (mCommit.getStats() != null) {
            tvSummary.setText(getString(R.string.commit_summary, added + changed + deleted,
                    mCommit.getStats().getAdditions(), mCommit.getStats().getDeletions()));
        } else {
            tvSummary.setVisibility(View.GONE);
        }
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

            Intent intent = new Intent(getActivity(), FileViewerActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Object.REF, mObjectSha);
            intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
            intent.putExtra(Constants.Commit.DIFF, file.getPatch());
            intent.putExtra(Constants.Object.PATH, file.getFilename());
            intent.putExtra(Constants.Object.TREE_SHA, mCommit.getCommit().getTree().getSha());
            startActivity(intent);
        }
    }
}