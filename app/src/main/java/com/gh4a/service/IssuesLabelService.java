package com.gh4a.service;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.rx.RxTools;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.LabelService;
import io.reactivex.Observable;
import io.reactivex.functions.Action;

import java.net.URLEncoder;

public class IssuesLabelService {
    public static final int DELETE_ISSUE_LABEL = 0;
    public static final int EDIT_ISSUE_LABEL = 1;
    public static final int ADD_ISSUE_LABEL = 2;

    public static Observable deleteIssueLabel(BaseActivity activity, String repoOwner, String repoName, String labelName) {
        String errorMessage = activity.getString(R.string.issue_error_delete_label, labelName);

        return RxTools.runOnErrorSnackBar(
                () -> ((LabelService) Gh4Application.get()
                        .getService(Gh4Application.LABEL_SERVICE))
                        .deleteLabel(repoOwner, repoName, URLEncoder.encode(labelName, "UTF-8")),
                activity,
                DELETE_ISSUE_LABEL,
                errorMessage,
                R.string.deleting_msg
        );
    }

    public static Observable editIssueLabel(BaseActivity activity, String repoOwner, String repoName, String oldLabelName, String newLabelName, String color) {
        String errorMessage = activity.getString(R.string.issue_error_edit_label, oldLabelName);
        Action editIssue = () -> {
            Label label = new Label();
            label.setName(newLabelName);
            label.setColor(color);

            ((LabelService) Gh4Application.get()
                .getService(Gh4Application.LABEL_SERVICE))
                .editLabel(
                        new RepositoryId(repoOwner, repoName),
                        URLEncoder.encode(oldLabelName, "UTF-8"),
                        label
                );
        };

        return RxTools.runOnErrorSnackBar(editIssue, activity, EDIT_ISSUE_LABEL,
                errorMessage, R.string.saving_msg);
    }

    public static Observable addIssue(BaseActivity activity, String repoOwner, String repoName, String labelName, String color) {
        String errorMessage = activity.getString(R.string.issue_error_create_label, labelName);
        Action addIssue = () -> {
            Label label = new Label();
            label.setName(labelName);
            label.setColor(color);
            ((LabelService) Gh4Application.get()
                    .getService(Gh4Application.LABEL_SERVICE))
                    .createLabel(repoOwner, repoName, label);
        };

        return RxTools.runOnErrorSnackBar(addIssue, activity, ADD_ISSUE_LABEL,
                errorMessage, R.string.saving_msg);
    }
}
