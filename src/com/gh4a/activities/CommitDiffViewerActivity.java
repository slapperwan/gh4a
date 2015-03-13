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

import android.content.Intent;
import android.support.v4.content.Loader;

import com.gh4a.Gh4Application;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;
import java.util.List;

public class CommitDiffViewerActivity extends DiffViewerActivity {
    @Override
    protected Intent navigateUp() {
        return IntentUtils.getCommitInfoActivityIntent(this, mRepoOwner, mRepoName, mSha);
    }

    @Override
    public void updateComment(long id, String body, int position) throws IOException {
        boolean isEdit = id != 0L;
        CommitComment commitComment = new CommitComment();

        if (isEdit) {
            commitComment.setId(id);
        }
        commitComment.setPosition(position);
        commitComment.setCommitId(mSha);
        commitComment.setPath(mPath);
        commitComment.setBody(body);

        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

        if (isEdit) {
            commitService.editComment(repoId, commitComment);
        } else {
            commitService.addComment(repoId, mSha, commitComment);
        }
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
}
