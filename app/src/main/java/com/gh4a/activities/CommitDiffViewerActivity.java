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
import android.net.Uri;

import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.widget.ReactionBar;
import com.meisolsson.githubsdk.model.PositionalCommentBase;
import com.meisolsson.githubsdk.model.Reaction;
import com.meisolsson.githubsdk.model.Reactions;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.model.request.ReactionRequest;
import com.meisolsson.githubsdk.service.reactions.ReactionService;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommentService;

import java.util.List;

import io.reactivex.Single;
import retrofit2.Response;

public class CommitDiffViewerActivity extends DiffViewerActivity<GitComment> {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String commitSha, String path, String diff, List<GitComment> comments,
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
    protected Uri createUrl(String lineId, long replyId) {
        Uri.Builder builder = IntentUtils.createBaseUriForRepo(mRepoOwner, mRepoName)
                .appendPath("commit")
                .appendPath(mSha);
        if (replyId > 0L) {
            builder.fragment("commitcomment-" + replyId);
        } else {
            builder.fragment("diff-" + ApiHelpers.md5(mPath) + lineId);
        }
        return builder.build();
    }

    @Override
    protected boolean canReply() {
        return false;
    }

    @Override
    protected PositionalCommentBase onUpdateReactions(PositionalCommentBase comment,
            Reactions reactions) {
        return ((GitComment) comment).toBuilder()
                .reactions(reactions)
                .build();
    }

    @Override
    protected void openCommentDialog(long id, long replyToId, String line, int position,
            int leftLine, int rightLine, PositionalCommentBase commitComment) {
        String body = commitComment == null ? "" : commitComment.body();
        Intent intent = EditDiffCommentActivity.makeIntent(this, mRepoOwner, mRepoName,
                mSha, mPath, line, leftLine, rightLine, position, id, body);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    public Single<Response<Void>> doDeleteComment(long id) {
        RepositoryCommentService service = ServiceFactory.get(RepositoryCommentService.class, false);

        return service.deleteCommitComment(mRepoOwner, mRepoName, id);
    }

    @Override
    protected Single<List<GitComment>> createCommentSingle(boolean bypassCache) {
        final RepositoryCommentService service =
                ServiceFactory.get(RepositoryCommentService.class, bypassCache);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getCommitComments(mRepoOwner, mRepoName, mSha, page))
                .compose(RxUtils.filter(c -> c.position() != null));
    }

    @Override
    public Single<List<Reaction>> loadReactionDetails(ReactionBar.Item item, boolean bypassCache) {
        final CommitCommentWrapper comment = (CommitCommentWrapper) item;
        final ReactionService service = ServiceFactory.get(ReactionService.class, bypassCache);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getCommitCommentReactions(mRepoOwner, mRepoName, comment.comment.id(), page));
    }

    @Override
    public Single<Reaction> addReaction(ReactionBar.Item item, String content) {
        CommitCommentWrapper comment = (CommitCommentWrapper) item;
        final ReactionService service = ServiceFactory.get(ReactionService.class, false);
        ReactionRequest request = ReactionRequest.builder().content(content).build();

        return service.createCommitCommentReaction(mRepoOwner, mRepoName, comment.comment.id(), request)
                .map(ApiHelpers::throwOnFailure);
    }
}
