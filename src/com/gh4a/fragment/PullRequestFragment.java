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
package com.gh4a.fragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.BaseSherlockFragmentActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.adapter.CommentAdapter;
import com.gh4a.loader.IssueCommentsLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.utils.GravatarUtils;
import com.gh4a.utils.StringUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class PullRequestFragment extends BaseFragment {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private LinearLayout mHeader;
    private CommentAdapter mCommentAdapter;

    private LoaderCallbacks<PullRequest> mPullRequestCallback = new LoaderCallbacks<PullRequest>() {
        @Override
        public Loader<LoaderResult<PullRequest>> onCreateLoader(int id, Bundle args) {
            return new PullRequestLoader(getSherlockActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
        }
        @Override
        public void onResultReady(LoaderResult<PullRequest> result) {
            if (!checkForError(result)) {
                hideLoading();
                fillData(result.getData());
            }
        }
    };
    private LoaderCallbacks<List<Comment>> mCommentCallback = new LoaderCallbacks<List<Comment>>() {
        @Override
        public Loader<LoaderResult<List<Comment>>> onCreateLoader(int id, Bundle args) {
            return new IssueCommentsLoader(getSherlockActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
        }
        @Override
        public void onResultReady(LoaderResult<List<Comment>> result) {
            PullRequestActivity activity = (PullRequestActivity) getSherlockActivity();
            if (!checkForError(result)) {
                activity.stopProgressDialog(activity.mProgressDialog);
                fillDiscussion(result.getData());
            }
        }
    };

    public static PullRequestFragment newInstance(String repoOwner, String repoName, int pullRequestNumber) {
        PullRequestFragment f = new PullRequestFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putInt(Constants.Issue.ISSUE_NUMBER, pullRequestNumber);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        mPullRequestNumber = getArguments().getInt(Constants.Issue.ISSUE_NUMBER);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.pull_request, container, false);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) getActivity();
        RelativeLayout rlComment = (RelativeLayout) getView().findViewById(R.id.rl_comment);
        if (!activity.isAuthorized()) {
            rlComment.setVisibility(View.GONE);
        }
        
        getLoaderManager().initLoader(0, null, mPullRequestCallback);
        getLoaderManager().getLoader(0).forceLoad();
        
        getLoaderManager().initLoader(1, null, mCommentCallback);
    }
    
    private void fillData(final PullRequest pullRequest) {
        //load comments
        getLoaderManager().getLoader(1).forceLoad();
        
        final Gh4Application app = Gh4Application.get(getActivity());
        
        View v = getView();
        ListView lvComments = (ListView) v.findViewById(R.id.list_view);
        
        // set details inside listview header
        LayoutInflater inflater = LayoutInflater.from(getSherlockActivity());
        mHeader = (LinearLayout) inflater.inflate(R.layout.pull_request_header, lvComments, false);
        mHeader.setClickable(false);
        lvComments.addHeaderView(mHeader, null, true);

        TextView tvCommentTitle = (TextView) mHeader.findViewById(R.id.comment_title);
        mCommentAdapter = new CommentAdapter(getSherlockActivity(), pullRequest.getNumber(),
                pullRequest.getState(), mRepoOwner, mRepoName);
        lvComments.setAdapter(mCommentAdapter);
        
        ImageView ivGravatar = (ImageView) mHeader.findViewById(R.id.iv_gravatar);
        if (pullRequest.getUser() != null) {
            AQuery aq = new AQuery(getSherlockActivity());
            aq.id(R.id.iv_gravatar).image(GravatarUtils.getGravatarUrl(pullRequest.getUser().getGravatarId()), 
                    true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
            
            ivGravatar.setOnClickListener(new OnClickListener() {
    
                @Override
                public void onClick(View arg0) {
                    app.openUserInfoActivity(getSherlockActivity(),
                            pullRequest.getUser().getLogin(), pullRequest.getUser().getName());
                }
            });
        }
        
        TextView tvExtra = (TextView) mHeader.findViewById(R.id.tv_extra);
        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        
        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);
        tvDesc.setMovementMethod(LinkMovementMethod.getInstance());
        
        tvCommentTitle.setTypeface(app.boldCondensed);
        tvCommentTitle.setTextColor(getResources().getColor(R.color.highlight));
        tvCommentTitle.setText(getResources().getString(R.string.issue_comment_title) + " (" + pullRequest.getComments() + ")");
        
        tvState.setText(pullRequest.getState());
        tvState.setTextColor(Color.WHITE);
        if ("closed".equals(pullRequest.getState())) {
            tvState.setBackgroundResource(R.drawable.default_red_box);
            tvState.setText(getString(R.string.closed).toUpperCase(Locale.getDefault()));
        }
        else {
            tvState.setBackgroundResource(R.drawable.default_green_box);
            tvState.setText(getString(R.string.open).toUpperCase(Locale.getDefault()));
        }
        tvTitle.setText(pullRequest.getTitle());
        tvTitle.setTypeface(app.boldCondensed);
        
        String body = pullRequest.getBodyHtml();
        if (!StringUtils.isBlank(body)) {
            HttpImageGetter imageGetter = new HttpImageGetter(getSherlockActivity());
            body = HtmlUtils.format(body).toString();
            imageGetter.bind(tvDesc, body, pullRequest.getId());
        }
        tvExtra.setText(getResources().getString(R.string.issue_open_by_user,
                pullRequest.getUser() != null ? pullRequest.getUser().getLogin() : "",
                Gh4Application.pt.format(pullRequest.getCreatedAt())));
        
        ImageView ivComment = (ImageView) v.findViewById(R.id.iv_comment);
        if (Gh4Application.THEME == R.style.DefaultTheme) {
            ivComment.setImageResource(R.drawable.social_send_now_dark);
        }
        ivComment.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        ivComment.setPadding(5, 2, 5, 2);
        ivComment.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                EditText etComment = (EditText) PullRequestFragment.this.getView().findViewById(R.id.et_comment);
                if (etComment.getText() != null && !StringUtils.isBlank(etComment.getText().toString())) {
                    new CommentIssueTask(PullRequestFragment.this).execute();
                }
                BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) getActivity();
                if (activity.getCurrentFocus() != null) {
                    activity.hideKeyboard(activity.getCurrentFocus().getWindowToken());
                }
            }
        });
    }
    
    private static class CommentIssueTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<PullRequestFragment> mTarget;
        private boolean mException;

        public CommentIssueTask(PullRequestFragment fragment) {
            mTarget = new WeakReference<PullRequestFragment>(fragment);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    PullRequestFragment fragment = mTarget.get();
                    EditText etComment = (EditText) fragment.getView().findViewById(R.id.et_comment);
                    BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) fragment.getSherlockActivity();
                    
                    if (etComment.getText() != null && !StringUtils.isBlank(etComment.getText().toString())) {  
                        String comment = etComment.getText().toString();
                        GitHubClient client = new GitHubClient();
                        client.setOAuth2Token(activity.getAuthToken());
                        IssueService issueService = new IssueService(client);
                        issueService.createComment(fragment.mRepoOwner, 
                                fragment.mRepoName,
                                fragment.mPullRequestNumber,
                                comment);
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
                PullRequestFragment fragment = mTarget.get();
                PullRequestActivity activity = (PullRequestActivity) fragment.getSherlockActivity();
                activity.mProgressDialog = activity.showProgressDialog(activity.getString(R.string.loading_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                PullRequestFragment fragment = mTarget.get();
                PullRequestActivity activity = (PullRequestActivity) fragment.getSherlockActivity();
                
                if (mException) {
                    activity.stopProgressDialog(activity.mProgressDialog);
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_comment),
                            false);
                }
                else {
                    if (result) {
                        activity.showMessage(activity.getResources().getString(R.string.issue_success_comment),
                                false);
                        //reload comments
                        fragment.getLoaderManager().restartLoader(1, null, fragment.mCommentCallback);
                        fragment.getLoaderManager().getLoader(1).forceLoad();
                    }
                    EditText etComment = (EditText) activity.findViewById(R.id.et_comment);
                    etComment.setText(null);
                    etComment.clearFocus();
                }
            }
        }
    }

    private void fillDiscussion(List<Comment> comments) {
        if (comments != null && !comments.isEmpty()) {
            mCommentAdapter.clear();
            mCommentAdapter.addAll(comments);
            mCommentAdapter.notifyDataSetChanged();
        }
    }

    private boolean checkForError(LoaderResult<?> result) {
        PullRequestActivity activity = (PullRequestActivity) getSherlockActivity();
        if (activity.isLoaderError(result)) {
            hideLoading();
            activity.stopProgressDialog(activity.mProgressDialog);
            return true;
        }
        return false;
    }
}