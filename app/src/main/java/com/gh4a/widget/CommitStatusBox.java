package com.gh4a.widget;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.PullRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommitStatusBox extends LinearLayoutCompat implements View.OnClickListener {
    private final ImageView mStatusIcon;
    private final TextView mStatusLabel;
    private final ViewGroup mStatusContainer;
    private final LayoutInflater mInflater;
    private final TextView mSummaryTextView;
    private final ImageView mDropDownIcon;
    private final View mHeader;

    public CommitStatusBox(Context context) {
        this(context, null);
    }

    public CommitStatusBox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommitStatusBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(VERTICAL);

        mInflater = LayoutInflater.from(getContext());
        mInflater.inflate(R.layout.commit_status_box, this, true);

        mStatusIcon = findViewById(R.id.iv_merge_status_icon);
        mStatusLabel = findViewById(R.id.merge_status_label);
        mStatusContainer = findViewById(R.id.merge_commit_status_container);
        mSummaryTextView = findViewById(R.id.merge_commit_summary);
        mDropDownIcon = findViewById(R.id.drop_down_icon);

        mHeader = findViewById(R.id.commit_status_header);
        mHeader.setOnClickListener(this);
    }

    public void fillStatus(List<CommitStatus> statuses, String mergableState) {
        final int statusIconDrawableAttrId;
        final int statusLabelResId;
        switch (mergableState) {
            case PullRequest.MERGEABLE_STATE_BEHIND:
                statusIconDrawableAttrId = R.attr.pullRequestMergeDirtyIcon;
                statusLabelResId = R.string.pull_merge_status_behind;
                break;
            case PullRequest.MERGEABLE_STATE_BLOCKED:
                statusIconDrawableAttrId = R.attr.pullRequestMergeDirtyIcon;
                statusLabelResId = R.string.pull_merge_status_blocked;
                break;
            case PullRequest.MERGEABLE_STATE_CLEAN:
                statusIconDrawableAttrId = R.attr.pullRequestMergeOkIcon;
                statusLabelResId = statuses.isEmpty()
                        ? R.string.pull_merge_status_mergable
                        : R.string.pull_merge_status_clean;
                break;
            case PullRequest.MERGEABLE_STATE_UNSTABLE:
                statusIconDrawableAttrId = R.attr.pullRequestMergeDirtyIcon;
                statusLabelResId = R.string.pull_merge_status_unstable;
                break;
            case PullRequest.MERGEABLE_STATE_DIRTY:
                statusIconDrawableAttrId = R.attr.pullRequestMergeDirtyIcon;
                statusLabelResId = R.string.pull_merge_status_dirty;
                break;
            default:
                if (statuses.isEmpty()) {
                    // Unknown status, no commit statuses -> nothing to display
                    setVisibility(View.GONE);
                    return;
                }
                statusIconDrawableAttrId = R.attr.pullRequestMergeUnknownIcon;
                statusLabelResId = R.string.pull_merge_status_unknown;
                break;
        }

        setVisibility(View.VISIBLE);

        int statusIconResId = UiUtils.resolveDrawable(getContext(), statusIconDrawableAttrId);
        mStatusIcon.setImageResource(statusIconResId);
        mStatusLabel.setText(statusLabelResId);

        mStatusContainer.removeAllViews();

        if (statuses.isEmpty()) {
            mStatusContainer.setVisibility(View.GONE);
            mDropDownIcon.setVisibility(View.GONE);
            mHeader.setClickable(false);
            mSummaryTextView.setText(R.string.pull_no_commit_status);
            return;
        }

        mHeader.setClickable(true);
        mDropDownIcon.setVisibility(View.VISIBLE);
        mStatusContainer.setVisibility(View.VISIBLE);

        int failingCount = 0;
        int pendingCount = 0;
        int successCount = 0;

        for (CommitStatus status : statuses) {
            View statusRow = mInflater.inflate(R.layout.row_commit_status, mStatusContainer, false);
            statusRow.setTag(status);
            statusRow.setOnClickListener(this);

            String state = status.getState();
            final int iconDrawableAttrId;
            switch (state) {
                case CommitStatus.STATE_ERROR:
                case CommitStatus.STATE_FAILURE:
                    iconDrawableAttrId = R.attr.commitStatusFailIcon;
                    failingCount += 1;
                    break;
                case CommitStatus.STATE_SUCCESS:
                    iconDrawableAttrId = R.attr.commitStatusOkIcon;
                    successCount += 1;
                    break;
                default:
                    iconDrawableAttrId = R.attr.commitStatusUnknownIcon;
                    pendingCount += 1;
                    break;
            }
            ImageView icon = statusRow.findViewById(R.id.iv_status_icon);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setImageResource(UiUtils.resolveDrawable(getContext(), iconDrawableAttrId));

            TextView context = statusRow.findViewById(R.id.tv_context);
            context.setText(status.getContext());

            TextView description = statusRow.findViewById(R.id.tv_desc);
            description.setText(status.getDescription());

            mStatusContainer.addView(statusRow);
        }

        if (PullRequest.MERGEABLE_STATE_UNSTABLE.equals(mergableState) && pendingCount > 0) {
            int resId = UiUtils.resolveDrawable(getContext(), R.attr.pullRequestMergeUnknownIcon);
            mStatusIcon.setImageResource(resId);
            mStatusLabel.setText(R.string.pull_merge_status_pending);
        }

        setSummaryText(failingCount, pendingCount, successCount);
        setStatusesExpanded(failingCount + pendingCount > 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.row_commit_status:
                CommitStatus status = (CommitStatus) v.getTag();
                IntentUtils.launchBrowser(getContext(), Uri.parse(status.getTargetUrl()));
                break;
            case R.id.commit_status_header:
                setStatusesExpanded(mStatusContainer.getVisibility() != View.VISIBLE);
                break;
        }
    }

    private void setSummaryText(int failingCount, int pendingCount, int successCount) {
        ArrayList<String> summaryList = new ArrayList<>();
        if (failingCount > 0) {
            summaryList.add(getContext().getString(R.string.check_failing, failingCount));
        }
        if (pendingCount > 0) {
            summaryList.add(getContext().getString(R.string.check_pending, pendingCount));
        }
        if (successCount > 0) {
            summaryList.add(getContext().getString(R.string.check_successful, successCount));
        }
        String summary = TextUtils.join(", ", summaryList);

        int sumCount = pendingCount + failingCount + successCount;
        mSummaryTextView.setText(getResources().getQuantityString(R.plurals.checks_summary,
                sumCount, summary));
    }

    private void setStatusesExpanded(boolean expanded) {
        mStatusContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        int drawableAttr = expanded ? R.attr.dropUpArrowIcon : R.attr.dropDownArrowIcon;
        int drawableRes = UiUtils.resolveDrawable(getContext(), drawableAttr);
        mDropDownIcon.setImageResource(drawableRes);
    }
}
