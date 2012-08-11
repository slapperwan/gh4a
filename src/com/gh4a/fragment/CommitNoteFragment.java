package com.gh4a.fragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.CommitActivity;
import com.gh4a.Constants;
import com.gh4a.Constants.LoaderResult;
import com.gh4a.R;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.utils.StringUtils;

public class CommitNoteFragment extends BaseFragment
    implements LoaderManager.LoaderCallbacks<Object>, OnClickListener{

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private ListView mListView;
    private CommitNoteAdapter mAdapter;
    
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
        
        mAdapter = new CommitNoteAdapter(getSherlockActivity(), new ArrayList<CommitComment>());
        mListView.setAdapter(mAdapter);
        
        showLoading();
        
        ImageView ivComment = (ImageView) getView().findViewById(R.id.iv_comment);
        ivComment.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        ivComment.setPadding(5, 2, 5, 2);
        ivComment.setOnClickListener(this);
        
        getLoaderManager().initLoader(0, null, this);
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
    public Loader onCreateLoader(int id, Bundle args) {
        return new CommitCommentListLoader(getSherlockActivity(), mRepoOwner, mRepoName, mObjectSha);
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        CommitActivity activity = (CommitActivity) getSherlockActivity();
        HashMap<Integer, Object> result = (HashMap<Integer, Object>) object;
        hideLoading();
        activity.stopProgressDialog(activity.mProgressDialog);
        if (!((BaseSherlockFragmentActivity) getSherlockActivity()).isLoaderError(result)) {
            Object data = result.get(LoaderResult.DATA);
            fillData((List<CommitComment>) data);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onClick(View v) {
        BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) getSherlockActivity();
        EditText etComment = (EditText) getView().findViewById(R.id.et_comment);
        
        if (etComment.getText() != null && !StringUtils.isBlank(etComment.getText().toString())) {
            new CommentCommitTask(this).execute();
        }
        
        if (activity.getCurrentFocus() != null) {
            activity.hideKeyboard(activity.getCurrentFocus().getWindowToken());
        }
    }
    
    private static class CommentCommitTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<CommitNoteFragment> mTarget;
        private boolean mException;

        public CommentCommitTask(CommitNoteFragment fragment) {
            mTarget = new WeakReference<CommitNoteFragment>(fragment);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    CommitNoteFragment fragment = mTarget.get();
                    BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) fragment.getSherlockActivity();
                    EditText etComment = (EditText) fragment.getView().findViewById(R.id.et_comment);
                    
                    if (etComment.getText() != null && !StringUtils.isBlank(etComment.getText().toString())) {  
                        GitHubClient client = new GitHubClient();
                        client.setOAuth2Token(activity.getAuthToken());
                        CommitService commitService = new CommitService(client);
                        CommitComment commitComment = new CommitComment();
                        commitComment.setBody(etComment.getText().toString());
                        commitService.addComment(new RepositoryId(fragment.mRepoOwner, fragment.mRepoName),
                                fragment.mObjectSha, commitComment);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                CommitActivity activity = (CommitActivity) mTarget.get().getSherlockActivity();
                activity.mProgressDialog = activity.showProgressDialog(activity.getString(R.string.loading_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                CommitActivity activity = (CommitActivity) mTarget.get().getSherlockActivity();
                
                if (mException) {
                    activity.stopProgressDialog(activity.mProgressDialog);
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_comment),
                            false);
                }
                else {
                    if (result) {
                        mTarget.get().getLoaderManager().getLoader(0).forceLoad();
                    }
                    EditText etComment = (EditText) activity.findViewById(R.id.et_comment);
                    etComment.setText(null);
                    etComment.clearFocus();
                }
            }
        }
    }
}
