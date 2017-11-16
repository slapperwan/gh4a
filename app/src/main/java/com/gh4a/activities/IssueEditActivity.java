/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ObjectsCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BasePagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.SingleFactory;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.MarkdownButtonsBar;
import com.gh4a.widget.MarkdownPreviewWebView;
import com.meisolsson.githubsdk.model.Content;
import com.meisolsson.githubsdk.model.ContentType;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.Milestone;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.request.issue.IssueRequest;
import com.meisolsson.githubsdk.service.issues.IssueLabelService;
import com.meisolsson.githubsdk.service.issues.IssueMilestoneService;
import com.meisolsson.githubsdk.service.issues.IssueService;
import com.meisolsson.githubsdk.service.repositories.RepositoryCollaboratorService;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import retrofit2.Response;

public class IssueEditActivity extends BasePagerActivity implements
        AppBarLayout.OnOffsetChangedListener, View.OnClickListener,
        View.OnFocusChangeListener {
    public static Intent makeCreateIntent(Context context, String repoOwner, String repoName) {
        // can't reuse makeEditIntent here, because even a null extra counts for hasExtra()
        return new Intent(context, IssueEditActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName);
    }

    public static Intent makeEditIntent(Context context, String repoOwner,
            String repoName, Issue issue) {
        return new Intent(context, IssueEditActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("issue", issue);
    }

    private static final int REQUEST_MANAGE_LABELS = 1000;
    private static final int REQUEST_MANAGE_MILESTONES = 1001;

    private static final int ID_LOADER_COLLABORATOR_STATUS = 0;

    private static final int[] TITLES = {
        R.string.issue_body, R.string.preview, R.string.settings
    };

    private String mRepoOwner;
    private String mRepoName;

    private boolean mIsCollaborator;
    private List<Milestone> mAllMilestone;
    private List<User> mAllAssignee;
    private List<Label> mAllLabels;
    private Issue mEditIssue;
    private Issue mOriginalIssue;

    private TextInputLayout mTitleWrapper;
    private EditText mTitleView;
    private EditText mDescView;
    private FloatingActionButton mFab;

    private View mRootView;
    private MarkdownButtonsBar mMarkdownButtons;
    private TextView mSelectedMilestoneView;
    private ViewGroup mSelectedAssigneeContainer;
    private TextView mLabelsView;

    private static final String STATE_KEY_ISSUE = "issue";
    private static final String STATE_KEY_ORIGINAL_ISSUE = "original_issue";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Gh4Application.get().isAuthorized()) {
            Intent intent = new Intent(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        LayoutInflater headerInflater = LayoutInflater.from(UiUtils.makeHeaderThemedContext(this));
        View header = headerInflater.inflate(R.layout.issue_create_header, null);
        addHeaderView(header, false);

        mTitleWrapper = header.findViewById(R.id.title_wrapper);
        mTitleView = header.findViewById(R.id.et_title);
        mTitleView.setOnFocusChangeListener(this);

        mDescView = findViewById(R.id.editor);
        mSelectedMilestoneView = findViewById(R.id.tv_milestone);
        mSelectedAssigneeContainer = findViewById(R.id.assignee_list);
        mLabelsView = findViewById(R.id.tv_labels);

        mMarkdownButtons = findViewById(R.id.markdown_buttons);
        mMarkdownButtons.setEditText(mDescView);

        View topLeftShadow = findViewById(R.id.markdown_buttons_top_left_shadow);
        if (topLeftShadow != null) {
            topLeftShadow.setVisibility(View.GONE);
        }
        View topShadow = findViewById(R.id.markdown_buttons_top_shadow);
        if (topShadow != null) {
            topShadow.setVisibility(View.GONE);
        }

        MarkdownPreviewWebView preview = findViewById(R.id.preview);
        preview.setEditText(mDescView);

        findViewById(R.id.milestone_container).setOnClickListener(this);
        findViewById(R.id.assignee_container).setOnClickListener(this);
        findViewById(R.id.label_container).setOnClickListener(this);

        CoordinatorLayout rootLayout = getRootLayout();
        mFab = (FloatingActionButton)
                getLayoutInflater().inflate(R.layout.accept_fab, rootLayout, false);
        mFab.setOnClickListener(this);
        rootLayout.addView(mFab);

        loadCollaboratorStatus(false);

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY_ISSUE)) {
            mEditIssue = savedInstanceState.getParcelable(STATE_KEY_ISSUE);
            mOriginalIssue = savedInstanceState.getParcelable(STATE_KEY_ORIGINAL_ISSUE);
        } else if (!isInEditMode()) {
            loadIssueTemplate();
            mDescView.setEnabled(false);
            mDescView.setHint(getString(R.string.issue_loading_template_hint));
        }

        if (mEditIssue == null) {
            mEditIssue = Issue.builder().build();
            mOriginalIssue = Issue.builder().build();
        }

        mTitleView.setText(mEditIssue.title());
        mDescView.setText(mEditIssue.body());

        mTitleView.addTextChangedListener(new UiUtils.ButtonEnableTextWatcher(mTitleView, mFab));
        mTitleView.addTextChangedListener(new UiUtils.EmptinessWatchingTextWatcher(mTitleView) {
            @Override
            public void onIsEmpty(boolean isEmpty) {
                if (isEmpty) {
                    mTitleWrapper.setError(getString(R.string.issue_error_title));
                } else {
                    mTitleWrapper.setErrorEnabled(false);
                }
                mFab.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        });

        adjustTabsForHeaderAlignedFab(true);
        setToolbarScrollable(false);
        updateOptionViews();

        addAppBarOffsetListener(this);
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        boolean isPullRequest = mEditIssue.pullRequest() != null;
        return !isInEditMode()
                ? getString(R.string.issue_create)
                : isPullRequest
                        ? getString(R.string.pull_request_edit_title, mEditIssue.number())
                        : getString(R.string.issue_edit_title, mEditIssue.number());
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected PagerAdapter createAdapter(ViewGroup root) {
        mRootView = root;
        getLayoutInflater().inflate(R.layout.issue_create, root);
        return new EditPagerAdapter();
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mEditIssue = extras.getParcelable("issue");
        if (mEditIssue != null) {
            // Save only editable fields
            mOriginalIssue = Issue.builder()
                    .title(mEditIssue.title())
                    .body(mEditIssue.body())
                    .milestone(mEditIssue.milestone())
                    .assignees(mEditIssue.assignees())
                    .labels(mEditIssue.labels())
                    .build();
        }
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // swipe-to-refresh doesn't make much sense in the
        // interaction model of this activity
        return false;
    }

    @Override
    public void onRefresh() {
        mAllAssignee = null;
        mAllMilestone = null;
        mAllLabels = null;
        mIsCollaborator = false;
        loadCollaboratorStatus(true);
        super.onRefresh();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // Set the bottom padding to make the bottom appear as not moving while the
        // AppBarLayout pushes it down or up.
        mRootView.setPadding(mRootView.getPaddingLeft(), mRootView.getPaddingTop(),
                mRootView.getPaddingRight(), appBarLayout.getTotalScrollRange() + verticalOffset);
    }

    private boolean isInEditMode() {
        return getIntent().hasExtra("issue");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.milestone_container) {
            showMilestonesDialog();
        } else if (id == R.id.assignee_container) {
            showAssigneesDialog();
        } else if (id == R.id.label_container) {
            showLabelDialog();
        } else if (view instanceof FloatingActionButton) {
            mEditIssue = mEditIssue.toBuilder()
                    .title(mTitleView.getText().toString())
                    .body(mDescView.getText().toString())
                    .build();
            saveIssue();
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mTitleView) {
            mMarkdownButtons.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEditIssue != null) {
            outState.putParcelable(STATE_KEY_ISSUE, mEditIssue);
        }
        if (mOriginalIssue != null) {
            outState.putParcelable(STATE_KEY_ORIGINAL_ISSUE, mOriginalIssue);
        }
    }

    @Override
    protected Intent navigateUp() {
        if (!isInEditMode()) {
            return IssueListActivity.makeIntent(this, mRepoOwner, mRepoName);
        }
        if (mEditIssue.pullRequest() != null) {
            return PullRequestActivity.makeIntent(this, mRepoOwner, mRepoName,
                    mEditIssue.number());
        }
        return IssueActivity.makeIntent(this, mRepoOwner, mRepoName, mEditIssue.number());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MANAGE_LABELS) {
            if (resultCode == RESULT_OK) {
                // Require reload of labels
                mAllLabels = null;
            }
        } else if (requestCode == REQUEST_MANAGE_MILESTONES) {
            if (resultCode == RESULT_OK) {
                // Require reload of milestones
                mAllMilestone = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showMilestonesDialog() {
        if (mAllMilestone == null) {
            loadMilestones();
        } else {
            final String[] milestones = new String[mAllMilestone.size() + 1];
            Milestone selectedMilestone = mEditIssue.milestone();
            int selected = 0;

            milestones[0] = getResources().getString(R.string.issue_clear_milestone);
            for (int i = 1; i <= mAllMilestone.size(); i++) {
                Milestone m = mAllMilestone.get(i - 1);
                milestones[i] = m.title();
                if (selectedMilestone != null && m.number().equals(selectedMilestone.number())) {
                    selected = i;
                }
            }

            final DialogInterface.OnClickListener selectCb = (dialog, which) -> {
                mEditIssue = mEditIssue.toBuilder()
                        .milestone(which == 0 ? null : mAllMilestone.get(which - 1))
                        .build();
                updateOptionViews();
                dialog.dismiss();
            };

            new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.issue_milestone_hint)
                    .setSingleChoiceItems(milestones, selected, selectCb)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.issue_manage_milestones, (dialog, which) -> {
                        Intent intent = IssueMilestoneListActivity.makeIntent(
                                IssueEditActivity.this, mRepoOwner, mRepoName,
                                mEditIssue.pullRequest() != null);
                        startActivityForResult(intent, REQUEST_MANAGE_MILESTONES);
                    })
                    .show();
        }
    }

    private void showAssigneesDialog() {
        if (mAllAssignee == null) {
            loadPotentialAssignees();
        } else {
            final String[] assigneeNames = new String[mAllAssignee.size()];
            final boolean[] selection = new boolean[mAllAssignee.size()];
            final List<User> oldAssigneeList = mEditIssue.assignees() != null
                    ? mEditIssue.assignees() : new ArrayList<>();
            List<String> assigneeLogins = new ArrayList<>();
            for (User assignee : oldAssigneeList) {
                assigneeLogins.add(assignee.login());
            }

            for (int i = 0; i < mAllAssignee.size(); i++) {
                String login = mAllAssignee.get(i).login();
                assigneeNames[i] = login;
                selection[i] = assigneeLogins.contains(login);
            }

            DialogInterface.OnMultiChoiceClickListener selectCb =
                    (dialogInterface, which, isChecked) -> selection[which] = isChecked;
            DialogInterface.OnClickListener okCb = (dialog, which) -> {
                List<User> newAssigneeList = new ArrayList<>();
                for (int i = 0; i < selection.length; i++) {
                    if (selection[i]) {
                        newAssigneeList.add(mAllAssignee.get(i));
                    }
                }
                mEditIssue = mEditIssue.toBuilder()
                        .assignees(newAssigneeList)
                        .build();
                updateOptionViews();
                dialog.dismiss();
            };

            new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.issue_assignee_hint)
                    .setMultiChoiceItems(assigneeNames, selection, selectCb)
                    .setPositiveButton(R.string.ok, okCb)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    private void showLabelDialog() {
        if (mAllLabels == null) {
            loadLabels();
        } else {
            LayoutInflater inflater = getLayoutInflater();
            final List<Label> selectedLabels = mEditIssue.labels() != null
                    ? new ArrayList<>(mEditIssue.labels()) : new ArrayList<>();
            View labelContainerView = inflater.inflate(R.layout.generic_linear_container, null);
            ViewGroup container = labelContainerView.findViewById(R.id.container);

            View.OnClickListener clickListener = view -> {
                Label label = (Label) view.getTag();
                if (selectedLabels.contains(label)) {
                    selectedLabels.remove(label);
                    setLabelSelection((TextView) view, false);
                } else {
                    selectedLabels.add(label);
                    setLabelSelection((TextView) view, true);
                }
            };

            for (final Label label : mAllLabels) {
                final View rowView = inflater.inflate(R.layout.row_issue_create_label, container, false);
                View viewColor = rowView.findViewById(R.id.view_color);
                viewColor.setBackgroundColor(ApiHelpers.colorForLabel(label));

                final TextView tvLabel = rowView.findViewById(R.id.tv_title);
                tvLabel.setText(label.name());
                tvLabel.setOnClickListener(clickListener);
                tvLabel.setTag(label);

                setLabelSelection(tvLabel, selectedLabels.contains(label));
                container.addView(rowView);
            }

            new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.issue_labels)
                    .setView(labelContainerView)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        mEditIssue = mEditIssue.toBuilder()
                                .labels(selectedLabels)
                                .build();
                        updateOptionViews();
                    })
                    .setNeutralButton(R.string.issue_manage_labels, (dialog, which) -> {
                        Intent intent = IssueLabelListActivity.makeIntent(
                                IssueEditActivity.this, mRepoOwner, mRepoName,
                                mEditIssue.pullRequest() != null);
                        startActivityForResult(intent, REQUEST_MANAGE_LABELS);
                    })
                    .show();
        }
    }

    private void setLabelSelection(TextView view, boolean selected) {
        Label label = (Label) view.getTag();
        if (selected) {
            int color = ApiHelpers.colorForLabel(label);
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
            view.setBackgroundColor(color);
            view.setTextColor(UiUtils.textColorForBackground(this, color));
        } else {
            view.setTypeface(view.getTypeface(), 0);
            view.setBackgroundColor(0);
            view.setTextColor(ContextCompat.getColor(this, Gh4Application.THEME != R.style.LightTheme
                    ? R.color.label_fg_light : R.color.label_fg_dark));
        }
    }

    private void updateOptionViews() {
        if (mEditIssue.milestone() != null) {
            mSelectedMilestoneView.setText(mEditIssue.milestone().title());
        } else {
            mSelectedMilestoneView.setText(R.string.issue_clear_milestone);
        }

        List<User> assignees = mEditIssue.assignees();
        LayoutInflater inflater = getLayoutInflater();

        mSelectedAssigneeContainer.removeAllViews();
        if (assignees != null && !assignees.isEmpty()) {
            for (User assignee : assignees) {
                View row = inflater.inflate(R.layout.row_assignee, mSelectedAssigneeContainer, false);
                TextView tvAssignee = row.findViewById(R.id.tv_assignee);
                tvAssignee.setText(ApiHelpers.getUserLogin(this, assignee));

                ImageView ivAssignee = row.findViewById(R.id.iv_assignee);
                AvatarHandler.assignAvatar(ivAssignee, assignee);

                mSelectedAssigneeContainer.addView(row);
            }
        } else {
            View row = inflater.inflate(R.layout.row_assignee, mSelectedAssigneeContainer, false);
            TextView tvAssignee = row.findViewById(R.id.tv_assignee);
            tvAssignee.setText(R.string.issue_clear_assignee);
            row.findViewById(R.id.iv_assignee).setVisibility(View.GONE);
            mSelectedAssigneeContainer.addView(row);
        }

        List<Label> labels = mEditIssue.labels();
        if (labels == null || labels.isEmpty()) {
            mLabelsView.setText(R.string.issue_no_labels);
        } else {
            mLabelsView.setText(UiUtils.formatLabelList(this, labels));
        }
    }

    private void saveIssue() {
        Milestone milestone = mEditIssue.milestone();
        IssueRequest.Builder builder = IssueRequest.builder();

        if (!ObjectsCompat.equals(mEditIssue.title(), mOriginalIssue.title())) {
            builder.title(mEditIssue.title());
        }
        if (!ObjectsCompat.equals(mEditIssue.body(), mOriginalIssue.body())) {
            builder.body(mEditIssue.body());
        }
        if (!ObjectsCompat.equals(mEditIssue.milestone(), mOriginalIssue.milestone())) {
            builder.milestone(milestone != null ? milestone.id() : null);
        }
        if (!ObjectsCompat.equals(mEditIssue.assignees(), mOriginalIssue.assignees())) {
            List<String> assignees = new ArrayList<>();
            for (User assignee : mEditIssue.assignees()) {
                assignees.add(assignee.login());
            }
            builder.assignees(assignees);
        }
        if (!ObjectsCompat.equals(mEditIssue.labels(), mOriginalIssue.labels())) {
            List<String> labels = new ArrayList<>();
            for (Label label : mEditIssue.labels()) {
                labels.add(label.name());
            }
            builder.labels(labels);
        }

        Integer issueNumber = mEditIssue.number();
        String errorMessage = issueNumber != null
                ? getString(R.string.issue_error_edit, issueNumber)
                : getString(R.string.issue_error_create);

        IssueService service = ServiceFactory.get(IssueService.class, false);
        Single<Response<Issue>> single = isInEditMode()
                ? service.editIssue(mRepoOwner, mRepoName, issueNumber, builder.build())
                : service.createIssue(mRepoOwner, mRepoName, builder.build());

        single.map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, R.string.saving_msg, errorMessage))
                .subscribe(result -> {
                    Intent data = new Intent();
                    Bundle extras = new Bundle();
                    extras.putParcelable("issue", result);
                    data.putExtras(extras);
                    setResult(RESULT_OK, data);
                    finish();
                }, error -> handleActionFailure("Saving issue failed", error));
    }

    private void loadCollaboratorStatus(boolean force) {
        SingleFactory.isAppUserRepoCollaborator(mRepoOwner, mRepoName, force)
                .compose(makeLoaderSingle(ID_LOADER_COLLABORATOR_STATUS, force))
                .subscribe(result -> {
                    mIsCollaborator = result;
                    invalidatePages();
                }, this::handleLoadFailure);
    }

    private void loadLabels() {
        final IssueLabelService service = ServiceFactory.get(IssueLabelService.class, false);
        registerTemporarySubscription(ApiHelpers.PageIterator
                .toSingle(page -> service.getRepositoryLabels(mRepoOwner, mRepoName, page))
                .compose(RxUtils::doInBackground)
                .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                .subscribe(result -> {
                    mAllLabels = result;
                    showLabelDialog();
                }, this::handleLoadFailure));
    }

    private void loadMilestones() {
        final IssueMilestoneService service = ServiceFactory.get(IssueMilestoneService.class, false);
        registerTemporarySubscription(ApiHelpers.PageIterator
                .toSingle(page -> service.getRepositoryMilestones(mRepoOwner, mRepoName, "open", page))
                .compose(RxUtils::doInBackground)
                .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                .subscribe(result -> {
                    mAllMilestone = result;
                    showMilestonesDialog();
                }, this::handleLoadFailure));
    }

    private void loadPotentialAssignees() {
        final RepositoryCollaboratorService service =
                ServiceFactory.get(RepositoryCollaboratorService.class, false);
        registerTemporarySubscription(ApiHelpers.PageIterator
                .toSingle(page -> service.getCollaborators(mRepoOwner, mRepoName, page))
                .compose(RxUtils::doInBackground)
                .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                .subscribe(result -> {
                    mAllAssignee = result;
                    User creator = mEditIssue.user();
                    if (creator != null && !mAllAssignee.contains(creator)) {
                        mAllAssignee.add(creator);
                    }
                    showAssigneesDialog();
                }, this::handleLoadFailure));
    }

    private void loadIssueTemplate() {
        RepositoryContentService service = ServiceFactory.get(RepositoryContentService.class, false);

        registerTemporarySubscription(getIssueTemplateContentSingle("")
                .flatMap(opt -> opt.orOptionalSingle(() -> getIssueTemplateContentSingle("/.github")))
                .flatMap(opt -> opt.flatMap(c -> {
                    //noinspection CodeBlock2Expr
                    return service.getContents(mRepoOwner, mRepoName, c.path(), null)
                            .map(ApiHelpers::throwOnFailure)
                            .compose(RxUtils::doInBackground);
                }))
                .map(opt -> opt.map(c -> StringUtils.fromBase64(c.content())))
                .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                .subscribe(result -> {
                    mDescView.setHint(null);
                    mDescView.setEnabled(true);
                    mDescView.setText(result.orNull());
                }, this::handleLoadFailure));
    }

    private Single<Optional<Content>> getIssueTemplateContentSingle(String path) {
        RepositoryContentService service = ServiceFactory.get(RepositoryContentService.class, false);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getDirectoryContents(mRepoOwner, mRepoName, path, null, page))
                .compose(RxUtils::doInBackground)
                .compose(RxUtils.filterAndMapToFirst(
                        c -> c.type() == ContentType.File && c.name().startsWith("ISSUE_TEMPLATE")))
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, Optional.absent()));
    }

    private class EditPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            @IdRes int resId = 0;
            switch (position) {
                case 0: resId = R.id.editor_container; break;
                case 1: resId = R.id.preview; break;
                case 2: resId = R.id.options; break;
            }
            return container.findViewById(resId);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(TITLES[position]);
        }

        @Override
        public int getCount() {
            return mIsCollaborator ? TITLES.length : TITLES.length - 1;
        }
    }
}
