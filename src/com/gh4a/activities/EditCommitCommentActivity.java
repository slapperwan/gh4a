package com.gh4a.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;

public class EditCommitCommentActivity extends EditCommentActivity {
    @Override
    protected void editComment(RepositoryId repoId, long id, String body) throws IOException {
        Gh4Application app = Gh4Application.get(this);
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);

        CommitComment comment = new CommitComment();
        comment.setId(id);
        comment.setBody(body);

        commitService.editComment(repoId, comment);
    }

    @Override
    protected void deleteComment(RepositoryId repoId, long id) throws IOException {
        Gh4Application app = Gh4Application.get(this);
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);

        commitService.deleteComment(repoId, id);
    }
}