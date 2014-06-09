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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCommentsLoader;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ThemeUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PullFileDiffViewerActivity extends DiffViewerActivity {
    private int mPullRequestNumber;

    private LoaderCallbacks<List<CommitComment>> mPullRequestCommentCallback =
            new LoaderCallbacks<List<CommitComment>>() {
        @Override
        public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
            return new PullRequestCommentsLoader(PullFileDiffViewerActivity.this, mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitComment>> result) {
            setContentEmpty(true);
            if (!result.handleError(PullFileDiffViewerActivity.this)) {
                for (CommitComment comment : result.getData()) {
                    if (!TextUtils.equals(comment.getPath(), mPath)) {
                        continue;
                    }
                    int position = comment.getPosition();
                    List<CommitComment> comments = mCommitCommentsByPos.get(position);
                    if (comments == null) {
                        comments = new ArrayList<CommitComment>();
                        mCommitCommentsByPos.put(position, comments);
                    }
                    comments.add(comment);
                }
                showDiff();
                setContentEmpty(false);
            }
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mPullRequestNumber = data.getInt(Constants.PullRequest.NUMBER);
    }

    @Override
    public void doInitLoader() {
        getSupportLoaderManager().initLoader(0, null, mPullRequestCommentCallback);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openPullRequestListActivity(this, mRepoOwner, mRepoName, Constants.Issue.STATE_OPEN);
    }

    @Override
    public void updateComment(long id, String body, int position) {
        new CommentTask(id, body, position).execute();
    }

    @Override
    public void deleteComment(long id) {
        new DeleteCommentTask(id);
    }

    @Override
    public void doRestartLoader() {
        getSupportLoaderManager().restartLoader(0, null, mPullRequestCommentCallback);
    }

    private class CommentTask extends ProgressDialogTask<Void> {
        private String mBody;
        private int mPosition;
        private long mId;

        public CommentTask(long id, String body, int position) {
            super(PullFileDiffViewerActivity.this, 0, R.string.saving_msg);
            mBody = body;
            mPosition = position;
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            boolean isEdit = mId != 0L;
            CommitComment commitComment = new CommitComment();

            if (isEdit) {
                commitComment.setId(mId);
            }
            commitComment.setPosition(mPosition);
            commitComment.setCommitId(mSha);
            commitComment.setPath(mPath);
            commitComment.setBody(mBody);

            PullRequestService pullRequestService = (PullRequestService)
                    Gh4Application.get(mContext).getService(Gh4Application.PULL_SERVICE);

            if (isEdit) {
                pullRequestService.editComment(new RepositoryId(mRepoOwner, mRepoName), commitComment);
            } else {
                pullRequestService.createComment(new RepositoryId(mRepoOwner, mRepoName), mPullRequestNumber, commitComment);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private long mId;

        public DeleteCommentTask(long id) {
            super(PullFileDiffViewerActivity.this, 0, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            CommitComment commitComment = new CommitComment();
            commitComment.setId(mId);

            PullRequestService pullRequestService = (PullRequestService)
                    Gh4Application.get(mContext).getService(Gh4Application.PULL_SERVICE);

            pullRequestService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
        }
    }


}