package com.gh4a.fragment;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.BaseSherlockFragmentActivity;
import com.gh4a.activities.CommitActivity;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;

public class CommitNoteFragment extends BaseFragment implements OnClickListener {

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private ListView mListView;
    private CommitNoteAdapter mAdapter;

    private LoaderCallbacks<List<CommitComment>> mCommentCallback = new LoaderCallbacks<List<CommitComment>>() {
        @Override
        public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
            return new CommitCommentListLoader(getSherlockActivity(), mRepoOwner, mRepoName, mObjectSha);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitComment>> result) {
            CommitActivity activity = (CommitActivity) getSherlockActivity();
            hideLoading();
            activity.stopProgressDialog(activity.mProgressDialog);
            if (!((BaseSherlockFragmentActivity) getSherlockActivity()).isLoaderError(result)) {
                fillData(result.getData());
            }
        }
    };
    
    public static CommitNoteFragment newInstance(String repoOwner, String repoName, String objectSha) {
        CommitNoteFragment f = new CommitNoteFragment();

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
        View v = inflater.inflate(R.layout.commit_comment_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        mListView.setClickable(false);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mAdapter = new CommitNoteAdapter(getSherlockActivity());
        mListView.setAdapter(mAdapter);
        
        showLoading();
        
        BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) getSherlockActivity();
        
        RelativeLayout rlComment = (RelativeLayout) getView().findViewById(R.id.rl_comment);
        if (!activity.isAuthorized()) {
            rlComment.setVisibility(View.GONE);
        }
        
        ImageView ivComment = (ImageView) getView().findViewById(R.id.iv_comment);
        if (Gh4Application.THEME == R.style.DefaultTheme) {
            ivComment.setImageResource(R.drawable.social_send_now_dark);
        }
        ivComment.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        ivComment.setPadding(5, 2, 5, 2);
        ivComment.setOnClickListener(this);
        
        getLoaderManager().initLoader(0, null, mCommentCallback);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData(List<CommitComment> comments) {
        if (comments != null && !comments.isEmpty()) {
            mAdapter.clear();
            mAdapter.addAll(comments);
            mAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void onClick(View v) {
        BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) getSherlockActivity();
        EditText etComment = (EditText) getView().findViewById(R.id.et_comment);
        String text = etComment.getText() == null ? null : etComment.getText().toString();
        
        if (!StringUtils.isBlank(text)) {
            new CommentCommitTask(text).execute();
        }
        
        if (activity.getCurrentFocus() != null) {
            activity.hideKeyboard(activity.getCurrentFocus().getWindowToken());
        }
    }
    
    private class CommentCommitTask extends ProgressDialogTask<Void> {
        private String mText;

        public CommentCommitTask(String text) {
            super(getActivity(), 0, R.string.loading_msg);
            mText = text;
        }

        @Override
        protected Void run() throws IOException {
            CommitService commitService = (CommitService)
                    mContext.getApplicationContext().getSystemService(Gh4Application.COMMIT_SERVICE);
            CommitComment commitComment = new CommitComment();
            commitComment.setBody(mText);
            commitService.addComment(new RepositoryId(mRepoOwner, mRepoName), mObjectSha, commitComment);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            CommitActivity activity = (CommitActivity) getSherlockActivity();
            getLoaderManager().getLoader(0).forceLoad();
            
            EditText etComment = (EditText) activity.findViewById(R.id.et_comment);
            etComment.setText(null);
            etComment.clearFocus();
        }

        @Override
        protected void onError(Exception e) {
            CommitActivity activity = (CommitActivity) getSherlockActivity();
            activity.showMessage(getString(R.string.issue_error_comment), false);
        }
    }
}
