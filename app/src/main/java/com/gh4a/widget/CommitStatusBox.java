package com.gh4a.widget;

import android.content.Context;
import android.net.Uri;
import androidx.appcompat.widget.LinearLayoutCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.model.StatusWrapper;
import com.gh4a.utils.IntentUtils;
import com.meisolsson.githubsdk.model.PullRequest;

import java.util.ArrayList;
import java.util.List;

public class CommitStatusBox extends LinearLayoutCompat implements View.OnClickListener {
    private final ImageView mStatusIcon;
    private final ProgressBar mLoadingIndicator;
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
        mLoadingIndicator = findViewById(R.id.status_progress);
        mStatusLabel = findViewById(R.id.merge_status_label);
        mStatusContainer = findViewById(R.id.merge_commit_status_container);
        mSummaryTextView = findViewById(R.id.merge_commit_summary);
        mDropDownIcon = findViewById(R.id.drop_down_icon);
        mHeader = findViewById(R.id.commit_status_header);
    }

    public void fillStatus(List<StatusWrapper> statuses, PullRequest.MergeableState mergableState) {
        final int statusIconDrawableResId;
        final int statusLabelResId;
        switch (mergableState) {
            case Behind:
                statusIconDrawableResId = R.drawable.pull_request_merge_dirty;
                statusLabelResId = R.string.pull_merge_status_behind;
                break;
            case Blocked:
                statusIconDrawableResId = R.drawable.pull_request_merge_dirty;
                statusLabelResId = R.string.pull_merge_status_blocked;
                break;
            case Clean:
                statusIconDrawableResId = R.drawable.pull_request_merge_ok;
                statusLabelResId = statuses.isEmpty()
                        ? R.string.pull_merge_status_mergable
                        : R.string.pull_merge_status_clean;
                break;
            case Unstable:
                statusIconDrawableResId = R.drawable.pull_request_merge_dirty;
                statusLabelResId = R.string.pull_merge_status_unstable;
                break;
            case Dirty:
                statusIconDrawableResId = R.drawable.pull_request_merge_dirty;
                statusLabelResId = R.string.pull_merge_status_dirty;
                break;
            case Draft:
                statusIconDrawableResId = R.drawable.pull_request_merge_dirty;
                statusLabelResId = R.string.pull_merge_status_draft;
                break;
            default:
                if (statuses.isEmpty()) {
                    // Unknown status, no commit statuses -> nothing to display
                    setVisibility(View.GONE);
                    return;
                }
                statusIconDrawableResId = R.drawable.pull_request_merge_unknown;
                statusLabelResId = R.string.pull_merge_status_unknown;
                break;
        }

        mLoadingIndicator.setVisibility(GONE);
        mStatusIcon.setImageResource(statusIconDrawableResId);
        mStatusIcon.setVisibility(VISIBLE);
        mStatusLabel.setText(statusLabelResId);

        mStatusContainer.removeAllViews();

        if (statuses.isEmpty()) {
            mStatusContainer.setVisibility(View.GONE);
            mDropDownIcon.setVisibility(View.GONE);
            mHeader.setClickable(false);
            mSummaryTextView.setText(R.string.pull_no_commit_status);
            return;
        }

        mHeader.setOnClickListener(this);
        mDropDownIcon.setVisibility(View.VISIBLE);
        mStatusContainer.setVisibility(View.VISIBLE);

        int failingCount = 0;
        int pendingCount = 0;
        int successCount = 0;

        for (StatusWrapper status : statuses) {
            View statusRow = mInflater.inflate(R.layout.row_commit_status, mStatusContainer, false);
            if (status.targetUrl() != null) {
                statusRow.setTag(status);
                statusRow.setOnClickListener(this);
            }

            final int iconDrawableResId;
            switch (status.state()) {
                case Failed:
                    iconDrawableResId = R.drawable.commit_status_fail;
                    failingCount += 1;
                    break;
                case Success:
                    iconDrawableResId = R.drawable.commit_status_ok;
                    successCount += 1;
                    break;
                default:
                    iconDrawableResId = R.drawable.commit_status_unknown;
                    pendingCount += 1;
                    break;
            }
            ImageView icon = statusRow.findViewById(R.id.iv_status_icon);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setImageResource(iconDrawableResId);

            TextView label = statusRow.findViewById(R.id.tv_label);
            label.setText(status.label());

            TextView description = statusRow.findViewById(R.id.tv_desc);
            description.setText(status.description());

            mStatusContainer.addView(statusRow);
        }

        if (mergableState == PullRequest.MergeableState.Unstable && pendingCount > 0) {
            mStatusIcon.setImageResource(R.drawable.pull_request_merge_unknown);
            mStatusLabel.setText(R.string.pull_merge_status_pending);
        }

        setSummaryText(failingCount, pendingCount, successCount);
        setStatusesExpanded(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.row_commit_status:
                StatusWrapper status = (StatusWrapper) v.getTag();
                BaseActivity activity = (BaseActivity) getContext();
                IntentUtils.openInCustomTabOrBrowser(activity, Uri.parse(status.targetUrl()));
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
        mDropDownIcon.setImageResource(expanded
                ? R.drawable.drop_up_arrow : R.drawable.drop_down_arrow);
    }
}
