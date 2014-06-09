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
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ThemeUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommitDiffViewerActivity extends DiffViewerActivity {

    private LoaderCallbacks<List<CommitComment>> mCommitCommentCallback =
            new LoaderCallbacks<List<CommitComment>>() {
        @Override
        public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
            return new CommitCommentListLoader(CommitDiffViewerActivity.this, mRepoOwner, mRepoName, mSha);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitComment>> result) {
            setContentEmpty(true);
            if (!result.handleError(CommitDiffViewerActivity.this)) {
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
    public void doInitLoader() {
        getSupportLoaderManager().initLoader(1, null, mCommitCommentCallback);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openCommitInfoActivity(this, mRepoOwner, mRepoName,
            mSha, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void updateComment(long id, String body, int position) {
        new CommentTask(id, body, position).execute();
    }

    @Override
    public void deleteComment(long id) {
        new DeleteCommentTask(id).execute();
    }

    @Override
    public void doRestartLoader() {
        getSupportLoaderManager().restartLoader(1, null, mCommitCommentCallback);
    }

    private class CommentTask extends ProgressDialogTask<Void> {
        private String mBody;
        private int mPosition;
        private long mId;

        public CommentTask(long id, String body, int position) {
            super(CommitDiffViewerActivity.this, 0, R.string.saving_msg);
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

            CommitService commitService = (CommitService)
                    Gh4Application.get(mContext).getService(Gh4Application.COMMIT_SERVICE);

            if (isEdit) {
                commitService.editComment(new RepositoryId(mRepoOwner, mRepoName), commitComment);
            } else {
                commitService.addComment(new RepositoryId(mRepoOwner, mRepoName), mSha, commitComment);
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
            super(CommitDiffViewerActivity.this, 0, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            CommitComment commitComment = new CommitComment();
            commitComment.setId(mId);

            CommitService commitService = (CommitService)
                    Gh4Application.get(mContext).getService(Gh4Application.COMMIT_SERVICE);

            commitService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
        }
    }
}
