package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;

public class EditCommitCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String commitSha, CommitComment comment) {
        Intent intent = new Intent(context, EditCommitCommentActivity.class)
                .putExtra("commit", commitSha);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, comment.getId(), comment.getBody());
    }

    @Override
    protected CharSequence getSubtitle() {
        String commitSha = getIntent().getStringExtra("commit").substring(0, 7);
        return getString(R.string.commit_in_repo, commitSha, mRepoOwner, mRepoName);
    }

    @Override
    protected void editComment(RepositoryId repoId, long id, String body) throws IOException {
        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);

        CommitComment comment = new CommitComment();
        comment.setId(id);
        comment.setBody(body);

        commitService.editComment(repoId, comment);
    }
}