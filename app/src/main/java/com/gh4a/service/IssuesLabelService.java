package com.gh4a.service;

import android.app.Activity;
import android.view.View;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.rx.RxTools;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.LabelService;
import io.reactivex.Observable;

import java.net.URLEncoder;

public class IssuesLabelService {
    public static final int DELETE_ISSUE_LABEL = 0;
    public static final int EDIT_ISSUE_LABEL = 1;
    public static final int ADD_ISSUE_LABEL = 2;

    public static Observable deleteIssueLabel(Activity activity, View rootView, String repoOwner, String repoName, String labelName) {
        String errorMessage = activity.getString(R.string.issue_error_delete_label, labelName);

        return Observable.fromCallable(() -> {
            LabelService labelService = (LabelService)
                    Gh4Application.get().getService(Gh4Application.LABEL_SERVICE);
            labelService.deleteLabel(repoOwner, repoName, URLEncoder.encode(labelName, "UTF-8"));
            return true;
        })
        .compose(RxTools.onErrorSnackbar(activity, DELETE_ISSUE_LABEL, rootView, errorMessage, R.string.deleting_msg));
    }

    public static Observable editIssueLabel(Activity activity, View rootLayout, String repoOwner, String repoName, String oldLabelName, String newLabelName, String color) {
        String errorMessage = activity.getString(R.string.issue_error_edit_label, oldLabelName);

        return Observable.fromCallable(() -> {
            LabelService labelService = (LabelService)
                    Gh4Application.get().getService(Gh4Application.LABEL_SERVICE);

            Label label = new Label();
            label.setName(newLabelName);
            label.setColor(color);

            labelService.editLabel(new RepositoryId(repoOwner, repoName),
                    URLEncoder.encode(oldLabelName, "UTF-8"), label);
            return true;
        })
        .compose(RxTools.onErrorSnackbar(activity, EDIT_ISSUE_LABEL, rootLayout, errorMessage, R.string.saving_msg));
    }

    public static Observable addIssue(Activity activity, View rootView, String repoOwner, String repoName, String labelName, String color) {
        String errorMessage = activity.getString(R.string.issue_error_create_label, labelName);

        return Observable.fromCallable(() -> {
            LabelService labelService = (LabelService)
                    Gh4Application.get().getService(Gh4Application.LABEL_SERVICE);

            Label label = new Label();
            label.setName(labelName);
            label.setColor(color);
            labelService.createLabel(repoOwner, repoName, label);
            return true;
        })
        .compose(RxTools.onErrorSnackbar(activity, ADD_ISSUE_LABEL, rootView, errorMessage, R.string.saving_msg));
    }}
