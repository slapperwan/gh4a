package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.request.CommentRequest;
import com.meisolsson.githubsdk.model.request.repository.CreateCommitComment;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommentService;

import io.reactivex.Single;

public class EditDiffCommentActivity extends EditCommentActivity {

    private static final String EXTRA_COMMIT_ID = "commit_id";
    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_LINE = "line";
    private static final String EXTRA_LEFT_LINE = "left_line";
    private static final String EXTRA_RIGHT_LINE = "right_line";
    private static final String EXTRA_POSITION = "position";

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String commitId, String path, String line, int leftLine, int rightLine, int position,
            long id, String body) {
        Intent intent = new Intent(context, EditDiffCommentActivity.class)
                .putExtra(EXTRA_COMMIT_ID, commitId)
                .putExtra(EXTRA_PATH, path)
                .putExtra(EXTRA_LINE, line)
                .putExtra(EXTRA_LEFT_LINE, leftLine)
                .putExtra(EXTRA_RIGHT_LINE, rightLine)
                .putExtra(EXTRA_POSITION, position);
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
        line.setText(extras.getString(EXTRA_LINE));

        TextView title = header.findViewById(R.id.title);
        title.setText(getString(R.string.commit_comment_dialog_title, extras.getInt(EXTRA_LEFT_LINE),
                extras.getInt(EXTRA_RIGHT_LINE)));
    }

    @Override
    protected Single<GitHubCommentBase> createComment(String repoOwner, String repoName,
            String body, long replyToCommentId) {
        Bundle extras = getIntent().getExtras();
        String commitId = extras.getString(EXTRA_COMMIT_ID);
        RepositoryCommentService service = ServiceFactory.get(RepositoryCommentService.class, false);
        CreateCommitComment request = CreateCommitComment.builder()
                .body(body)
                .path(extras.getString(EXTRA_PATH))
                .position(extras.getInt(EXTRA_POSITION))
                .build();
        return service.createCommitComment(repoOwner, repoName, commitId, request)
                .map(ApiHelpers::throwOnFailure);
    }

    @Override
    protected Single<GitHubCommentBase> editComment(String repoOwner, String repoName,
            long commentId, String body) {
        RepositoryCommentService service = ServiceFactory.get(RepositoryCommentService.class, false);
        CommentRequest request = CommentRequest.builder().body(body).build();
        return service.editCommitComment(repoOwner, repoName, commentId, request)
                .map(ApiHelpers::throwOnFailure);
    }
}