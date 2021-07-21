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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gh4a.utils.ActivityResultHelpers;
import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.appcompat.app.AlertDialog;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Flowable;
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

    private interface OnAssigneesLoaded {
        void handleLoad(List<User> assignees);
    }
    private interface OnLabelsLoaded {
        void handleLoad(List<Label> labels);
    }
    private interface OnMilestonesLoaded {
        void handleLoad(List<Milestone> milestones);
    }

    private static final int ID_LOADER_COLLABORATOR_STATUS = 0;

    private static final int[] TITLES = {
        R.string.issue_body, R.string.preview, R.string.settings
    };

    private final ActivityResultLauncher<Intent> mLabelManagerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> mLabelSingle = null)
    );
    private final ActivityResultLauncher<Intent> mMilestoneManagerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> mMilestoneSingle = null)
    );

    private String mRepoOwner;
    private String mRepoName;

    private boolean mIsCollaborator;

    private Single<List<User>> mAssigneeSingle;
    private Single<List<Label>> mLabelSingle;
    private Single<List<Milestone>> mMilestoneSingle;

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
        if (savedInstanceState != null) {
            mEditIssue = savedInstanceState.getParcelable(STATE_KEY_ISSUE);
            mOriginalIssue = savedInstanceState.getParcelable(STATE_KEY_ORIGINAL_ISSUE);
        }

        super.onCreate(savedInstanceState);

        if (!Gh4Application.get().isAuthorized()) {
            Intent intent = new Intent(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        LayoutInflater headerInflater =
                LayoutInflater.from(new ContextThemeWrapper(this, R.style.HeaderTheme));
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

        if (savedInstanceState == null && !isInEditMode()) {
            loadIssueTemplates();
            mTitleView.setEnabled(false);
            mDescView.setEnabled(false);
            mDescView.setHint(getString(R.string.issue_loading_template_hint));
        }

        mTitleView.setText(mEditIssue.title());
        mDescView.setText(mEditIssue.body());

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
        // If mEditIssue is != null here, it was restored from saved state
        if (mEditIssue == null) {
            if (extras.containsKey("issue")) {
                mEditIssue = extras.getParcelable("issue");
                // Save only editable fields
                mOriginalIssue = Issue.builder()
                        .title(mEditIssue.title())
                        .body(mEditIssue.body())
                        .milestone(mEditIssue.milestone())
                        .assignees(mEditIssue.assignees())
                        .labels(mEditIssue.labels())
                        .build();
            } else {
                mEditIssue = Issue.builder().build();
                mOriginalIssue = Issue.builder().build();
            }
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
        mAssigneeSingle = null;
        mLabelSingle = null;
        mMilestoneSingle = null;
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
        outState.putParcelable(STATE_KEY_ISSUE, mEditIssue);
        outState.putParcelable(STATE_KEY_ORIGINAL_ISSUE, mOriginalIssue);
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

    private void showMilestonesDialog() {
        loadMilestones(milestones -> {
            MilestoneEditDialogFragment
                    .newInstance(mEditIssue.milestone(), milestones)
                    .show(getSupportFragmentManager(), "milestoneedit");
        });
    }

    private void showAssigneesDialog() {
        loadPotentialAssignees(assignees -> {
            final List<User> oldAssigneeList = mEditIssue.assignees() != null
                    ? mEditIssue.assignees() : Collections.emptyList();
            AssigneeEditDialogFragment
                    .newInstance(oldAssigneeList, assignees)
                    .show(getSupportFragmentManager(), "assigneeedit");
        });
    }

    private void showLabelDialog() {
        loadLabels(labels -> {
            final List<Label> selectedLabels = mEditIssue.labels() != null
                    ? mEditIssue.labels() : Collections.emptyList();
            LabelEditDialogFragment
                    .newInstance(selectedLabels, labels)
                    .show(getSupportFragmentManager(), "labeledit");
        });
    }

    private void updateMilestone(Milestone newMilestone) {
        mEditIssue = mEditIssue.toBuilder()
                .milestone(newMilestone)
                .build();
        updateOptionViews();
    }

    private void updateAssignees(List<User> newAssignees) {
        mEditIssue = mEditIssue.toBuilder()
                .assignees(newAssignees)
                .build();
        updateOptionViews();
    }

    private void updateLabels(List<Label> newLabels) {
        mEditIssue = mEditIssue.toBuilder()
                .labels(newLabels)
                .build();
        updateOptionViews();
    }

    private void manageMilestones() {
        Intent intent = IssueMilestoneListActivity.makeIntent(this, mRepoOwner, mRepoName,
                mEditIssue.pullRequest() != null);
        mMilestoneManagerLauncher.launch(intent);
    }

    private void manageLabels() {
        Intent intent = IssueLabelListActivity.makeIntent(this, mRepoOwner, mRepoName,
                mEditIssue.pullRequest() != null);
        mLabelManagerLauncher.launch(intent);
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
            builder.milestone(milestone != null ? milestone.number() : null);
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

    private void loadLabels(OnLabelsLoaded callback) {
        final IssueLabelService service = ServiceFactory.get(IssueLabelService.class, false);
        if (mLabelSingle == null) {
            mLabelSingle = ApiHelpers.PageIterator
                    .toSingle(page -> service.getRepositoryLabels(mRepoOwner, mRepoName, page))
                    .compose(RxUtils::doInBackground)
                    .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                    .cache();
        }
        registerTemporarySubscription(
                mLabelSingle.subscribe(result -> callback.handleLoad(result), this::handleLoadFailure));
    }

    private void loadMilestones(OnMilestonesLoaded callback) {
        final IssueMilestoneService service = ServiceFactory.get(IssueMilestoneService.class, false);
        if (mMilestoneSingle == null) {
            mMilestoneSingle = ApiHelpers.PageIterator
                    .toSingle(page -> service
                            .getRepositoryMilestones(mRepoOwner, mRepoName, "open", page))
                    .compose(RxUtils::doInBackground)
                    .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                    .cache();
        }
        registerTemporarySubscription(
                mMilestoneSingle.subscribe(result -> callback.handleLoad(result), this::handleLoadFailure));
    }

    private void loadPotentialAssignees(OnAssigneesLoaded callback) {
        final RepositoryCollaboratorService service =
                ServiceFactory.get(RepositoryCollaboratorService.class, false);

        if (mAssigneeSingle == null) {
            mAssigneeSingle = ApiHelpers.PageIterator
                    .toSingle(page -> service.getCollaborators(mRepoOwner, mRepoName, page))
                    .compose(RxUtils::doInBackground)
                    .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                    .map(assignees -> {
                        User creator = mEditIssue.user();
                        if (creator != null && !assignees.contains(creator)) {
                            assignees.add(creator);
                        }
                        return assignees;
                    })
                    .cache();
        }

        registerTemporarySubscription(
                mAssigneeSingle.subscribe(result -> callback.handleLoad(result), this::handleLoadFailure));
    }

    private void loadIssueTemplates() {
        registerTemporarySubscription(getIssueTemplatesSingle("/.github")
                .flatMap(opt -> opt.orOptionalSingle(() -> getIssueTemplatesSingle("")))
                .flatMap(opt -> opt.orOptionalSingle(() -> getIssueTemplatesSingle("/docs")))
                .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                .subscribe(result -> {
                    if (result.isPresent() && !result.get().isEmpty()) {
                        List<IssueTemplate> templates = result.get();
                        if (templates.size() == 1) {
                            handleIssueTemplateSelected(templates.get(0));
                        } else {
                            List<IssueTemplate> namedTemplates = new ArrayList<>();
                            for (IssueTemplate t : templates) {
                                if (t.name != null) {
                                    namedTemplates.add(t);
                                }
                            }
                            IssueTemplateSelectionDialogFragment f =
                                    IssueTemplateSelectionDialogFragment.newInstance(namedTemplates);
                            f.show(getSupportFragmentManager(), "template-selection");
                        }
                    } else {
                        handleIssueTemplateSelected(null);
                    }
                }, this::handleLoadFailure));
    }

    private void handleIssueTemplateSelected(IssueTemplate template) {
        mTitleView.setEnabled(true);
        mDescView.setHint(null);
        mDescView.setEnabled(true);
        if (template == null) {
            return;
        }

        mTitleView.setText(template.title);
        mDescView.setText(template.content);
        if (!template.defaultAssignees.isEmpty()) {
            loadPotentialAssignees(assignees -> {
                final List<User> validAssignees = new ArrayList<>();
                for (User potentialAssignee : assignees) {
                    if (template.defaultAssignees.contains(potentialAssignee.login())) {
                        validAssignees.add(potentialAssignee);
                    }
                }
                if (!validAssignees.isEmpty()) {
                    mEditIssue = mEditIssue.toBuilder()
                            .assignees(validAssignees)
                            .build();
                }
            });
        }
        if (!template.defaultLabels.isEmpty()) {
            loadLabels(labels -> {
                final List<Label> validLabels = new ArrayList<>();
                for (Label label : labels) {
                    if (template.defaultLabels.contains(label.name())) {
                        validLabels.add(label);
                    }
                }
                if (!validLabels.isEmpty()) {
                    mEditIssue = mEditIssue.toBuilder()
                            .labels(validLabels)
                            .build();
                }
            });
        }
    }

    private Single<Optional<List<IssueTemplate>>> getIssueTemplatesSingle(String path) {
        RepositoryContentService service = ServiceFactory.get(RepositoryContentService.class, false);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getDirectoryContents(mRepoOwner, mRepoName, path, null, page))
                .compose(RxUtils.filterAndMapToFirst(c -> c.name().toLowerCase(Locale.US).startsWith("issue_template")))
                .flatMap(contentOpt -> contentOpt.flatMap(content -> {
                    if (content.type() == ContentType.Directory) {
                        return ApiHelpers.PageIterator
                                .toSingle(page -> service.getDirectoryContents(mRepoOwner, mRepoName, content.path(), null, page));
                    } else {
                        return Single.just(Collections.singletonList(content));
                    }
                }))
                .map(contentsOpt -> contentsOpt.map(contents -> {
                    List<Content> files = new ArrayList<>();
                    for (Content c : contents) {
                        if (c.type() == ContentType.File && c.name().endsWith(".md")) {
                            files.add(c);
                        }
                    }
                    return files;
                }))
                .flatMap(contentsOpt -> contentsOpt.flatMap(contents -> {
                    List<Single<IssueTemplate>> result = new ArrayList<>();
                    for (Content c : contents) {
                        result.add(parseTemplate(service, c));
                    }
                    return Flowable.fromIterable(result)
                            .flatMap(flowable -> flowable.toFlowable())
                            .toList();
                }))
                .compose(RxUtils::doInBackground)
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, Optional.absent()));
    }

    private Single<IssueTemplate> parseTemplate(RepositoryContentService service, Content content) {
        return service.getContents(mRepoOwner, mRepoName, content.path(), null)
            .map(ApiHelpers::throwOnFailure)
            .map(fileContent -> StringUtils.fromBase64(fileContent.content()))
            .map(contentString -> new IssueTemplate(contentString))
            .compose(RxUtils::doInBackground);
    }

    private static class IssueTemplate implements Parcelable {
        private static final Pattern FRONT_MATTER_PATTERN =
                Pattern.compile("(---\n)(.*?\n)((---)|(\\.\\.\\.))\n?(.*)", Pattern.DOTALL);

        String content;
        String name;
        String description;
        String title;
        final List<String> defaultLabels = new ArrayList<>();
        final List<String> defaultAssignees = new ArrayList<>();

        IssueTemplate(String contentString) {
            Matcher matcher = FRONT_MATTER_PATTERN.matcher(contentString);
            if (matcher.matches()) {
                content = matcher.group(6);
                for (String line : matcher.group(2).split("\n")) {
                    int colonPos = line.indexOf(": ");
                    if (colonPos > 0) {
                        String key = line.substring(0, colonPos);
                        boolean isQuoted = line.charAt(colonPos + 2) == '"'
                                || line.charAt(colonPos + 2) == '\'';
                        String value = isQuoted
                                ? line.substring(colonPos + 3, line.length() - 1)
                                : line.substring(colonPos + 2);
                        switch (key) {
                            case "name": name = value; break;
                            case "about": description = value; break;
                            case "title": title = value; break;
                            case "labels": splitAndFillList(value, defaultLabels); break;
                            case "assignees": splitAndFillList(value, defaultAssignees); break;
                        }
                    }
                }
            } else {
                content = contentString;
            }
        }

        private IssueTemplate(Parcel parcel) {
            content = parcel.readString();
            name = parcel.readString();
            description = parcel.readString();
            title = parcel.readString();
            parcel.readStringList(defaultLabels);
            parcel.readStringList(defaultAssignees);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(content);
            parcel.writeString(name);
            parcel.writeString(description);
            parcel.writeString(title);
            parcel.writeStringList(defaultLabels);
            parcel.writeStringList(defaultAssignees);
        }

        public static Parcelable.Creator CREATOR = new Parcelable.Creator<IssueTemplate>() {
            @Override
            public IssueTemplate createFromParcel(Parcel parcel) {
                return new IssueTemplate(parcel);
            }

            @Override
            public IssueTemplate[] newArray(int count) {
                return new IssueTemplate[count];
            }
        };

        private static void splitAndFillList(String input, List<String> list) {
            for (String part : input.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
    }

    public static class IssueTemplateSelectionDialogFragment extends DialogFragment
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        private List<IssueTemplate> mTemplates;

        public static IssueTemplateSelectionDialogFragment newInstance(List<IssueTemplate> templates) {
            IssueTemplateSelectionDialogFragment f = new IssueTemplateSelectionDialogFragment();
            Bundle args = new Bundle();
            args.putParcelableArrayList("templates", new ArrayList<>(templates));
            f.setArguments(args);
            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            mTemplates = args.getParcelableArrayList("templates");

            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.issue_template_dialog_title)
                    .setSingleChoiceItems(new Adapter(getContext(), mTemplates), -1, this)
                    .setOnCancelListener(this)
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            ((IssueEditActivity) getActivity()).handleIssueTemplateSelected(mTemplates.get(which));
            dialog.dismiss();
        }

        @Override
        public void onCancel(@NonNull DialogInterface dialog) {
            super.onCancel(dialog);
            ((IssueEditActivity) getActivity()).handleIssueTemplateSelected(null);
        }

        private static class Adapter extends ArrayAdapter<IssueTemplate> {
            private LayoutInflater mInflater;

            public Adapter(Context context, List<IssueTemplate> templates) {
                super(context, R.layout.row_issue_template, templates);
                mInflater = LayoutInflater.from(context);
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                final View view;
                if (convertView == null) {
                    view = mInflater.inflate(R.layout.row_issue_template, parent, false);
                } else {
                    view = convertView;
                }

                TextView title = view.findViewById(R.id.title);
                TextView description = view.findViewById(R.id.description);
                IssueTemplate template = getItem(position);

                title.setText(template.name);
                description.setText(template.description);
                description.setVisibility(TextUtils.isEmpty(template.description) ? View.GONE : View.VISIBLE);

                return view;
            }
        }
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

    public static class MilestoneEditDialogFragment extends DialogFragment {
        public static MilestoneEditDialogFragment newInstance(
                Milestone selected, List<Milestone> all) {
            MilestoneEditDialogFragment f = new MilestoneEditDialogFragment();
            Bundle args = new Bundle();
            args.putParcelable("selected", selected);
            args.putParcelableArrayList("all", new ArrayList<>(all));
            f.setArguments(args);
            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ArrayList<Milestone> all = getArguments().getParcelableArrayList("all");
            final Milestone selectedMilestone = getArguments().getParcelable("selected");
            final String[] milestones = new String[all.size() + 1];
            int selected = 0;

            milestones[0] = getString(R.string.issue_clear_milestone);
            for (int i = 1; i <= all.size(); i++) {
                Milestone m = all.get(i - 1);
                milestones[i] = m.title();
                if (selectedMilestone != null && m.number().equals(selectedMilestone.number())) {
                    selected = i;
                }
            }

            final IssueEditActivity activity = (IssueEditActivity) getContext();
            final DialogInterface.OnClickListener selectCb = (dialog, which) -> {
                activity.updateMilestone(which == 0 ? null : all.get(which - 1));
                dialog.dismiss();
            };

            return new AlertDialog.Builder(activity)
                    .setCancelable(true)
                    .setTitle(R.string.issue_milestone_hint)
                    .setSingleChoiceItems(milestones, selected, selectCb)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.issue_manage_milestones, (dialog, which) -> {
                        activity.manageMilestones();
                    })
                    .create();
        }
    }

    public static class AssigneeEditDialogFragment extends DialogFragment {
        public static AssigneeEditDialogFragment newInstance(
                List<User> selected, List<User> allPotentialAssignees) {
            AssigneeEditDialogFragment f = new AssigneeEditDialogFragment();
            Bundle args = new Bundle();
            args.putParcelableArrayList("selected", new ArrayList<>(selected));
            args.putParcelableArrayList("all", new ArrayList<>(allPotentialAssignees));
            f.setArguments(args);
            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ArrayList<User> selected = getArguments().getParcelableArrayList("selected");
            final ArrayList<User> all = getArguments().getParcelableArrayList("all");

            List<String> assigneeLogins = new ArrayList<>();
            for (User assignee : selected) {
                assigneeLogins.add(assignee.login());
            }

            final String[] assigneeNames = new String[all.size()];
            final boolean[] selection = new boolean[all.size()];

            for (int i = 0; i < all.size(); i++) {
                String login = all.get(i).login();
                assigneeNames[i] = login;
                selection[i] = assigneeLogins.contains(login);
            }

            final IssueEditActivity activity = (IssueEditActivity) getContext();

            DialogInterface.OnMultiChoiceClickListener selectCb =
                    (dialogInterface, which, isChecked) -> selection[which] = isChecked;
            DialogInterface.OnClickListener okCb = (dialog, which) -> {
                List<User> newAssigneeList = new ArrayList<>();
                for (int i = 0; i < selection.length; i++) {
                    if (selection[i]) {
                        newAssigneeList.add(all.get(i));
                    }
                }
                activity.updateAssignees(newAssigneeList);
                dialog.dismiss();
            };

            return new AlertDialog.Builder(activity)
                    .setCancelable(true)
                    .setTitle(R.string.issue_assignee_hint)
                    .setMultiChoiceItems(assigneeNames, selection, selectCb)
                    .setPositiveButton(R.string.ok, okCb)
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        }
    }

    public static class LabelEditDialogFragment extends DialogFragment {
        public static LabelEditDialogFragment newInstance(
                List<Label> selectedLabels, List<Label> allLabels) {
            LabelEditDialogFragment f = new LabelEditDialogFragment();
            Bundle args = new Bundle();
            args.putParcelableArrayList("selected", new ArrayList<>(selectedLabels));
            args.putParcelableArrayList("all", new ArrayList<>(allLabels));
            f.setArguments(args);
            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getLayoutInflater();
            final List<Label> selectedLabels = getArguments().getParcelableArrayList("selected");
            final List<Label> allLabels = getArguments().getParcelableArrayList("all");
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

            for (final Label label : allLabels) {
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

            IssueEditActivity activity = (IssueEditActivity) getContext();
            return new AlertDialog.Builder(activity)
                    .setCancelable(true)
                    .setTitle(R.string.issue_labels)
                    .setView(labelContainerView)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        activity.updateLabels(selectedLabels);
                    })
                    .setNeutralButton(R.string.issue_manage_labels, (dialog, which) -> {
                        activity.manageLabels();
                    })
                    .create();
        }

        private void setLabelSelection(TextView view, boolean selected) {
            Label label = (Label) view.getTag();
            if (selected) {
                int color = ApiHelpers.colorForLabel(label);
                view.setTypeface(view.getTypeface(), Typeface.BOLD);
                view.setBackgroundColor(color);
                view.setTextColor(UiUtils.textColorForBackground(getContext(), color));
            } else {
                view.setTypeface(view.getTypeface(), Typeface.NORMAL);
                view.setBackgroundColor(0);
                view.setTextColor(ContextCompat.getColor(getContext(), R.color.label_fg));
            }
        }
    }
}
