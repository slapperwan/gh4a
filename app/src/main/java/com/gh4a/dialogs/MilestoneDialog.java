package com.gh4a.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.gh4a.R;
import com.gh4a.activities.IssueMilestoneListActivity;
import com.gh4a.fragment.IssueMilestoneListFragment;
import com.meisolsson.githubsdk.model.Milestone;

public class MilestoneDialog extends BasePagerDialog
        implements IssueMilestoneListFragment.SelectionCallback {
    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_SHOW_ANY_MILESTONE = "show_any_milestone";
    private static final String EXTRA_SHOW_MANAGE_MILESTONES_BUTTON = "show_manage_milestones_button";
    private static final String EXTRA_FROM_PULL_REQUEST = "from_pull_request";
    private static final int[] TITLES = new int[]{
            R.string.open, R.string.closed
    };
    private static final int REQUEST_MANAGE_MILESTONES = 3000;

    public static MilestoneDialog newInstance(String repoOwner, String repoName,
            boolean fromPullRequest, boolean showAnyMilestoneButton,
            boolean showManageMilestonesButton) {
        MilestoneDialog dialog = new MilestoneDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_OWNER, repoOwner);
        args.putString(EXTRA_REPO, repoName);
        args.putBoolean(EXTRA_FROM_PULL_REQUEST, fromPullRequest);
        args.putBoolean(EXTRA_SHOW_ANY_MILESTONE, showAnyMilestoneButton);
        args.putBoolean(EXTRA_SHOW_MANAGE_MILESTONES_BUTTON, showManageMilestonesButton);
        dialog.setArguments(args);
        return dialog;
    }

    private String mRepoOwner;
    private String mRepoName;
    private boolean mFromPullRequest;
    private boolean mShowAnyMilestoneButton;
    private boolean mShowManageMilestonesButton;
    private Button mNoMilestoneButton;
    private Button mAnyMilestoneButton;
    private Button mManageMilestonesButton;
    private SelectionCallback mSelectionCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString(EXTRA_OWNER);
        mRepoName = args.getString(EXTRA_REPO);
        mFromPullRequest = args.getBoolean(EXTRA_FROM_PULL_REQUEST);
        mShowAnyMilestoneButton = args.getBoolean(EXTRA_SHOW_ANY_MILESTONE);
        mShowManageMilestonesButton = args.getBoolean(EXTRA_SHOW_MANAGE_MILESTONES_BUTTON);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof SelectionCallback)) {
            throw new IllegalStateException("Parent of " + MilestoneDialog.class.getSimpleName()
                    + " must implement " + SelectionCallback.class.getSimpleName());
        }
        mSelectionCallback = (SelectionCallback) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (mShowAnyMilestoneButton) {
            mAnyMilestoneButton = addButton(R.string.issue_filter_by_any_milestone);
        }
        mNoMilestoneButton = addButton(R.string.issue_filter_by_no_milestone);
        if (mShowManageMilestonesButton) {
            mManageMilestonesButton = addButton(R.string.issue_manage_milestones);
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        if (v == mNoMilestoneButton) {
            onMilestoneSelected(MilestoneSelection.Type.NO_MILESTONE);
        } else if (v == mAnyMilestoneButton) {
            onMilestoneSelected(MilestoneSelection.Type.ANY_MILESTONE);
        } else if (v == mManageMilestonesButton) {
            Intent intent = IssueMilestoneListActivity.makeIntent(
                    getContext(), mRepoOwner, mRepoName, mFromPullRequest);
            startActivityForResult(intent, REQUEST_MANAGE_MILESTONES);
        } else {
            super.onClick(v);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MANAGE_MILESTONES) {
            if (resultCode == Activity.RESULT_OK) {
                refreshPages();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        return IssueMilestoneListFragment.newInstance(mRepoOwner, mRepoName, position == 1, mFromPullRequest);
    }

    @Override
    public void onMilestoneSelected(@Nullable Milestone milestone) {
        onMilestoneSelected(MilestoneSelection.Type.MILESTONE, milestone);
    }

    private void onMilestoneSelected(MilestoneSelection.Type type) {
        onMilestoneSelected(type, null);
    }

    private void onMilestoneSelected(MilestoneSelection.Type type, Milestone milestone) {
        mSelectionCallback.onMilestoneSelected(new MilestoneSelection(type, milestone));
        dismiss();
    }

    public interface SelectionCallback {
        void onMilestoneSelected(MilestoneSelection milestoneSelection);
    }

    public static class MilestoneSelection {
        public final Type type;
        public final Milestone milestone;

        MilestoneSelection(Type type, Milestone milestone) {
            this.type = type;
            this.milestone = milestone;
        }

        public enum Type {
            NO_MILESTONE,
            ANY_MILESTONE,
            MILESTONE
        }
    }
}
