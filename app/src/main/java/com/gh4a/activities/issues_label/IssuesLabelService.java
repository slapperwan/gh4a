package com.gh4a.activities.issues_label;

import android.app.Activity;
import android.view.View;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.rx.RxTools;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.LabelService;
import java.net.URLEncoder;
import io.reactivex.Observable;

public class IssuesLabelService {
    public static Observable deleteIssueLabel(Activity activity, View rootView, String repoOwner, String repoName, String labelName) {
        String errorMessage = activity.getString(R.string.issue_error_delete_label, labelName);

        return Observable.fromCallable(() -> {
            LabelService labelService = (LabelService)
                    Gh4Application.get().getService(Gh4Application.LABEL_SERVICE);
            labelService.deleteLabel(repoOwner, repoName, URLEncoder.encode(labelName, "UTF-8"));
            return true;
        })
        .compose(RxTools.onErrorSnackbar(activity, rootView, errorMessage, R.string.deleting_msg));
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
        .compose(RxTools.onErrorSnackbar(activity, rootLayout, errorMessage, R.string.saving_msg));
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
        .compose(RxTools.onErrorSnackbar(activity, rootView, errorMessage, R.string.saving_msg));
    }}
