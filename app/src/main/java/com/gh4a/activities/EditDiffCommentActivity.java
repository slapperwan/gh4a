package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;

public class EditDiffCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String commitId, String path, String line, int leftLine, int rightLine, int position,
            long id, String body) {
        Intent intent = new Intent(context, EditDiffCommentActivity.class)
                .putExtra("commit_id", commitId)
                .putExtra("path", path)
                .putExtra("line", line)
                .putExtra("left_line", leftLine)
                .putExtra("right_line", rightLine)
                .putExtra("position", position);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, id, 0L, body, R.attr.colorIssueOpen);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View header = getLayoutInflater().inflate(R.layout.edit_commit_comment_header, null);
        mEditorSheet.addHeaderView(header);

        TextView line = header.findViewById(R.id.line);
        Bundle extras = getIntent().getExtras();
        line.setText(extras.getString("line"));

        TextView title = header.findViewById(R.id.title);
        title.setText(getString(R.string.commit_comment_dialog_title, extras.getInt("left_line"),
                extras.getInt("right_line")));
    }

    @Override
    protected void createComment(RepositoryId repoId, CommitComment comment,
            long replyToCommentId) throws IOException {
        Bundle extras = getIntent().getExtras();
        String commitId = extras.getString("commit_id");

        comment.setPosition(extras.getInt("position"));
        comment.setCommitId(commitId);
        comment.setPath(extras.getString("path"));

        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);
        commitService.addComment(repoId, commitId, comment);
    }

    @Override
    protected void editComment(RepositoryId repoId, CommitComment comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);
        commitService.editComment(repoId, comment);
    }
}