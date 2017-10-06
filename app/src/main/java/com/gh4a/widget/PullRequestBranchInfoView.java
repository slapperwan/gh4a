package com.gh4a.widget;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.gh4a.R;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.PullRequestMarker;
import com.meisolsson.githubsdk.model.git.GitReference;

public class PullRequestBranchInfoView extends RelativeLayout implements View.OnClickListener {
    private final StyleableTextView mSourceBranchView;
    private final StyleableTextView mTargetBranchView;
    private final int mAccentColor;

    private PullRequestMarker mSourceMarker;
    private PullRequestMarker mTargetMarker;

    public PullRequestBranchInfoView(Context context) {
        this(context, null);
    }

    public PullRequestBranchInfoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRequestBranchInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.view_pull_request_branch_info, this);

        mAccentColor = UiUtils.resolveColor(getContext(), android.R.attr.textColorLink);
        mSourceBranchView = findViewById(R.id.tv_pr_from);
        mSourceBranchView.setOnClickListener(this);
        mTargetBranchView = findViewById(R.id.tv_pr_to);
        mTargetBranchView.setOnClickListener(this);
    }

    public void bind(PullRequestMarker sourceMarker, PullRequestMarker targetMarker,
            GitReference sourceReference) {
        mSourceMarker = sourceMarker;
        mTargetMarker = targetMarker;
        formatMarkerText(mSourceBranchView, R.string.pull_request_from, sourceMarker,
                sourceReference != null);
        formatMarkerText(mTargetBranchView, R.string.pull_request_to, targetMarker, true);
    }

    private void formatMarkerText(StyleableTextView view, @StringRes int formatResId,
            final PullRequestMarker marker, boolean makeClickable) {
        SpannableStringBuilder builder = StringUtils.applyBoldTags(
                getContext().getString(formatResId), view.getTypefaceValue());
        int pos = builder.toString().indexOf("[ref]");
        if (pos >= 0) {
            String label = TextUtils.isEmpty(marker.label()) ? marker.ref() : marker.label();
            builder.replace(pos, pos + 5, label);
            if (marker.repo() != null && makeClickable) {
                builder.setSpan(
                        new ForegroundColorSpan(mAccentColor), pos, pos + label.length(), 0);
                view.setClickable(true);
            } else {
                view.setClickable(false);
            }
        }

        view.setText(builder);
    }

    @Override
    public void onClick(View v) {
        PullRequestMarker marker = v.getId() == R.id.tv_pr_from ? mSourceMarker : mTargetMarker;
        if (marker.repo() != null) {
            Intent intent = RepositoryActivity.makeIntent(getContext(), marker.repo(),
                    marker.ref());
            getContext().startActivity(intent);
        }
    }
}
