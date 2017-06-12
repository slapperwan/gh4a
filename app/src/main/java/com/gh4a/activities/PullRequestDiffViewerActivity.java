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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Gh4Application;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCommentsLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.ReactionBar;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.List;

public class PullRequestDiffViewerActivity extends DiffViewerActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName, int number,
            String commitSha, String path, String diff, List<CommitComment> comments,
            int initialLine, int highlightStartLine, int highlightEndLine, boolean highlightIsRight,
            IntentUtils.InitialCommentMarker initialComment) {
        Intent intent = new Intent(context, PullRequestDiffViewerActivity.class)
                .putExtra("number", number);
        return DiffViewerActivity.fillInIntent(intent, repoOwner, repoName, commitSha, path,
                diff, comments, initialLine, highlightStartLine, highlightEndLine,
                highlightIsRight, initialComment);
    }

    private int mPullRequestNumber;

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mPullRequestNumber = extras.getInt("number", -1);
    }

    @Override
    protected Loader<LoaderResult<List<CommitComment>>> createCommentLoader() {
        return new PullRequestCommentsLoader(this, mRepoOwner, mRepoName, mPullRequestNumber);
    }

    @Override
    protected String createUrl(String lineId, long replyId) {
        String link = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/pull/"
                + mPullRequestNumber + "/files";
        if (replyId > 0L) {
            link += "#r" + replyId;
        } else {
            link += "#diff-" + ApiHelpers.md5(mPath) + lineId;
        }
        return link;
    }

    @Override
    protected Intent navigateUp() {
        return PullRequestActivity.makeIntent(this, mRepoOwner, mRepoName, mPullRequestNumber);
    }

    @Override
    protected boolean canReply() {
        return true;
    }

    @Override
    protected void createComment(CommitComment comment, long replyToCommentId) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullRequestService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
        if (replyToCommentId != 0L) {
            pullRequestService.replyToComment(repoId, mPullRequestNumber, replyToCommentId,
                    comment.getBody());
        } else {
            comment.setCommitId(mSha);
            comment.setPath(mPath);
            pullRequestService.createComment(repoId, mPullRequestNumber, comment);
        }
    }

    @Override
    protected void editComment(CommitComment comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullRequestService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);
        pullRequestService.editComment(new RepositoryId(mRepoOwner, mRepoName), comment);
    }

    @Override
    protected void deleteComment(long id) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullRequestService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);

        pullRequestService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), id);
    }

    @Override
    public List<Reaction> loadReactionDetailsInBackground(ReactionBar.Item item) throws IOException {
        CommitCommentWrapper comment = (CommitCommentWrapper) item;
        Gh4Application app = Gh4Application.get();
        PullRequestService pullRequestService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);

        return pullRequestService.getCommentReactions(new RepositoryId(mRepoOwner, mRepoName),
                comment.comment.getId());
    }

    @Override
    public Reaction addReactionInBackground(ReactionBar.Item item, String content) throws IOException {
        CommitCommentWrapper comment = (CommitCommentWrapper) item;
        Gh4Application app = Gh4Application.get();
        PullRequestService pullRequestService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);

        return pullRequestService.addCommentReaction(new RepositoryId(mRepoOwner, mRepoName),
                comment.comment.getId(), content);
    }
}
