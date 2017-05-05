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
import android.support.v4.content.Loader;

import com.gh4a.Gh4Application;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.ReactionBar;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;
import java.util.List;

public class CommitDiffViewerActivity extends DiffViewerActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String commitSha, String path, String diff, List<CommitComment> comments,
            int highlightStartLine, int highlightEndLine, boolean highlightIsRight,
            IntentUtils.InitialCommentMarker initialComment) {
        return DiffViewerActivity.fillInIntent(new Intent(context, CommitDiffViewerActivity.class),
                repoOwner, repoName, commitSha, path, diff, comments, -1,
                highlightStartLine, highlightEndLine, highlightIsRight, initialComment);
    }

    @Override
    protected Intent navigateUp() {
        return CommitActivity.makeIntent(this, mRepoOwner, mRepoName, mSha);
    }

    @Override
    protected String createUrl() {
        return "https://github.com/" + mRepoOwner + "/" + mRepoName + "/commit/" + mSha
                + "#diff-" + ApiHelpers.md5(mPath);
    }

    @Override
    protected boolean canReply() {
        return false;
    }

    @Override
    protected void createComment(CommitComment comment, long replyToCommentId) throws IOException {
        comment.setPath(mPath);

        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);
        commitService.addComment(new RepositoryId(mRepoOwner, mRepoName), mSha, comment);
    }

    @Override
    protected void editComment(CommitComment comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);
        commitService.editComment(new RepositoryId(mRepoOwner, mRepoName), comment);
    }

    @Override
    public void deleteComment(long id) throws IOException {
        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);

        commitService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), id);
    }

    @Override
    protected Loader<LoaderResult<List<CommitComment>>> createCommentLoader() {
        return new CommitCommentListLoader(this, mRepoOwner, mRepoName, mSha, false, true);
    }

    @Override
    public List<Reaction> loadReactionDetailsInBackground(ReactionBar.Item item) throws IOException {
        CommitCommentWrapper comment = (CommitCommentWrapper) item;
        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);

        return commitService.getCommentReactions(new RepositoryId(mRepoOwner, mRepoName),
                comment.comment.getId());
    }

    @Override
    public Reaction addReactionInBackground(ReactionBar.Item item, String content) throws IOException {
        CommitCommentWrapper comment = (CommitCommentWrapper) item;
        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);

        return commitService.addCommentReaction(new RepositoryId(mRepoOwner, mRepoName),
                comment.comment.getId(), content);
    }
}
