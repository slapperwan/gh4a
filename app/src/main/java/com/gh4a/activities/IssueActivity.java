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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.fragment.IssueFragment;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.SingleFactory;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.BottomSheetCompatibleScrollingViewBehavior;
import com.gh4a.widget.IssueStateTrackingFloatingActionButton;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.request.issue.IssueRequest;
import com.meisolsson.githubsdk.service.issues.IssueService;

import java.util.Locale;

public class IssueActivity extends BaseActivity implements View.OnClickListener {
    public static Intent makeIntent(Context context, String login, String repoName, int number) {
        return makeIntent(context, login, repoName, number, null);
    }
    public static Intent makeIntent(Context context, String login, String repoName,
            int number, IntentUtils.InitialCommentMarker initialComment) {
        return new Intent(context, IssueActivity.class)
                .putExtra("owner", login)
                .putExtra("repo", repoName)
                .putExtra("number", number)
                .putExtra("initial_comment", initialComment);
    }

    private static final int ID_LOADER_ISSUE = 0;
    private static final int ID_LOADER_COLLABORATOR_STATUS = 1;

    private static final int REQUEST_EDIT_ISSUE = 1000;

    private Issue mIssue;
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private IntentUtils.InitialCommentMarker mInitialComment;
    private ViewGroup mHeader;
    private Boolean mIsCollaborator;
    private IssueStateTrackingFloatingActionButton mEditFab;
    private final Handler mHandler = new Handler();
    private IssueFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.frame_layout);
        setContentShown(false);

        LayoutInflater inflater = LayoutInflater.from(UiUtils.makeHeaderThemedContext(this));
        mHeader = (ViewGroup) inflater.inflate(R.layout.issue_header, null);
        mHeader.setClickable(false);
        mHeader.setVisibility(View.GONE);
        addHeaderView(mHeader, false);

        setFragment((IssueFragment) getSupportFragmentManager().findFragmentById(R.id.details));

        setToolbarScrollable(true);
        loadIssue(false);
        loadCollaboratorStatus(false);
    }

    @NonNull
    protected String getActionBarTitle() {
        return getString(R.string.issue) + " #" + mIssueNumber;
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected AppBarLayout.ScrollingViewBehavior onCreateSwipeLayoutBehavior() {
        return new BottomSheetCompatibleScrollingViewBehavior();
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mIssueNumber = extras.getInt("number");
        mInitialComment = extras.getParcelable("initial_comment");
        extras.remove("initial_comment");
    }

    private void showUiIfDone() {
        if (mIssue == null || mIsCollaborator == null) {
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        IssueFragment newFragment = IssueFragment.newInstance(mRepoOwner, mRepoName,
                mIssue, mIsCollaborator, mInitialComment);
        if (mFragment != null) {
            Fragment.SavedState state = fm.saveFragmentInstanceState(mFragment);
            newFragment.setInitialSavedState(state);
        }
        setFragment(newFragment);
        fm.beginTransaction()
                .replace(R.id.details, mFragment)
                .commitAllowingStateLoss();
        mInitialComment = null;

        updateHeader();
        updateFabVisibility();
        setContentShown(true);
    }

    private void setFragment(IssueFragment fragment) {
        mFragment = fragment;
        setChildScrollDelegate(fragment);
    }

    private void updateHeader() {
        TextView tvState = mHeader.findViewById(R.id.tv_state);
        boolean closed = mIssue.state() == IssueState.Closed;
        int stateTextResId = closed ? R.string.closed : R.string.open;
        int stateColorAttributeId = closed ? R.attr.colorIssueClosed : R.attr.colorIssueOpen;

        tvState.setText(getString(stateTextResId).toUpperCase(Locale.getDefault()));
        transitionHeaderToColor(stateColorAttributeId,
                closed ? R.attr.colorIssueClosedDark : R.attr.colorIssueOpenDark);

        TextView tvTitle = mHeader.findViewById(R.id.tv_title);
        tvTitle.setText(mIssue.title());

        mHeader.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.issue_menu, menu);

        boolean authorized = Gh4Application.get().isAuthorized();
        boolean isCreator = mIssue != null && authorized &&
                ApiHelpers.loginEquals(mIssue.user(), Gh4Application.get().getAuthLogin());
        boolean isClosed = mIssue != null && mIssue.state() == IssueState.Closed;
        boolean isCollaborator = mIsCollaborator != null && mIsCollaborator;
        boolean closerIsCreator = mIssue != null
                && ApiHelpers.userEquals(mIssue.user(), mIssue.closedBy());
        boolean canClose = mIssue != null && authorized && (isCreator || isCollaborator);
        boolean canOpen = canClose && (isCollaborator || closerIsCreator);

        if (!canClose || isClosed) {
            menu.removeItem(R.id.issue_close);
        }
        if (!canOpen || !isClosed) {
            menu.removeItem(R.id.issue_reopen);
        }

        if (mIssue == null) {
            menu.removeItem(R.id.browser);
            menu.removeItem(R.id.share);
            menu.removeItem(R.id.copy_number);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean displayDetachAction() {
        return true;
    }

    @Override
    protected Intent navigateUp() {
        return IssueListActivity.makeIntent(this, mRepoOwner, mRepoName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.issue_close:
            case R.id.issue_reopen:
                if (checkForAuthOrExit()) {
                    showOpenCloseConfirmDialog(itemId == R.id.issue_reopen);
                }
                return true;
            case R.id.share:
                IntentUtils.share(this, getString(R.string.share_issue_subject,
                        mIssueNumber, mIssue.title(), mRepoOwner + "/" + mRepoName),
                        mIssue.htmlUrl());
                return true;
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(mIssue.htmlUrl()));
                return true;
            case R.id.copy_number:
                IntentUtils.copyToClipboard(this, "Issue #" + mIssueNumber,
                        String.valueOf(mIssueNumber));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        mIssue = null;
        mIsCollaborator = null;
        setContentShown(false);

        transitionHeaderToColor(R.attr.colorPrimary, R.attr.colorPrimaryDark);
        mHeader.setVisibility(View.GONE);

        if (mFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(mFragment)
                    .commit();
            setFragment(null);
        }

        // onRefresh() can be triggered in the draw loop, and CoordinatorLayout doesn't
        // like its child list being changed while drawing
        mHandler.post(this::updateFabVisibility);

        supportInvalidateOptionsMenu();
        loadIssue(true);
        loadCollaboratorStatus(true);
        super.onRefresh();
    }

    @Override
    public void onBackPressed() {
        if (mFragment != null && mFragment.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    private void showOpenCloseConfirmDialog(final boolean reopen) {
        @StringRes int messageResId = reopen
                ? R.string.reopen_issue_confirm : R.string.close_issue_confirm;
        @StringRes int buttonResId = reopen
                ? R.string.pull_request_reopen : R.string.pull_request_close;
        new AlertDialog.Builder(this)
                .setMessage(messageResId)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(false)
                .setPositiveButton(buttonResId, (dialog, which) -> updateIssueState(reopen))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateIssueState(boolean reopen) {
        IssueService service = ServiceFactory.get(IssueService.class, false);
        IssueRequest request = IssueRequest.builder()
                .state(reopen ? IssueState.Open : IssueState.Closed)
                .build();
        @StringRes int dialogResId = reopen ? R.string.opening_msg : R.string.closing_msg;
        @StringRes int errorMessageResId =
                reopen ? R.string.issue_error_reopen : R.string.issue_error_close;

        service.editIssue(mRepoOwner, mRepoName, mIssueNumber, request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, dialogResId, getString(errorMessageResId, mIssueNumber)))
                .subscribe(result -> {
                    mIssue = result;

                    updateHeader();
                    if (mEditFab != null) {
                        mEditFab.setState(mIssue.state());
                    }
                    if (mFragment != null) {
                        mFragment.updateState(mIssue);
                    }
                    setResult(RESULT_OK);
                    supportInvalidateOptionsMenu();
                }, error -> handleActionFailure("Updating issue state failed", error));
    }

    private void updateFabVisibility() {
        boolean isIssueOwner = mIssue != null
                && ApiHelpers.loginEquals(mIssue.user(), Gh4Application.get().getAuthLogin());
        boolean isCollaborator = mIsCollaborator != null && mIsCollaborator;
        boolean shouldHaveFab = (isIssueOwner || isCollaborator) && mIssue != null;
        CoordinatorLayout rootLayout = getRootLayout();

        if (shouldHaveFab && mEditFab == null) {
            mEditFab = (IssueStateTrackingFloatingActionButton)
                    getLayoutInflater().inflate(R.layout.issue_edit_fab, rootLayout, false);
            mEditFab.setOnClickListener(this);
            rootLayout.addView(mEditFab);
        } else if (!shouldHaveFab && mEditFab != null) {
            rootLayout.removeView(mEditFab);
            mEditFab = null;
        }
        if (mEditFab != null) {
            mEditFab.setState(mIssue.state());
        }
    }

    private boolean checkForAuthOrExit() {
        if (Gh4Application.get().isAuthorized()) {
            return true;
        }
        Intent intent = new Intent(this, Github4AndroidActivity.class);
        startActivity(intent);
        finish();
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.edit_fab && checkForAuthOrExit()) {
            Intent editIntent = IssueEditActivity.makeEditIntent(this,
                    mRepoOwner, mRepoName, mIssue);
            startActivityForResult(editIntent, REQUEST_EDIT_ISSUE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_ISSUE) {
            if (resultCode == Activity.RESULT_OK) {
                loadIssue(true);
                setResult(RESULT_OK);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadIssue(boolean force) {
        IssueService service = ServiceFactory.get(IssueService.class, force);
        service.getIssue(mRepoOwner, mRepoName, mIssueNumber)
                .map(ApiHelpers::throwOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_ISSUE, force))
                .subscribe(result -> {
                    mIssue = result;
                    showUiIfDone();
                    supportInvalidateOptionsMenu();
                }, this::handleLoadFailure);
    }

    private void loadCollaboratorStatus(boolean force) {
        SingleFactory.isAppUserRepoCollaborator(mRepoOwner, mRepoName, force)
                .compose(makeLoaderSingle(ID_LOADER_COLLABORATOR_STATUS, force))
                .subscribe(result -> {
                    mIsCollaborator = result;
                    showUiIfDone();
                    supportInvalidateOptionsMenu();
                }, this::handleLoadFailure);
    }
}
