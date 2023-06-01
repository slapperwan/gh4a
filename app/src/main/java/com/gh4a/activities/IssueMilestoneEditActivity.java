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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.appbar.AppBarLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.appcompat.app.ActionBar;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.gh4a.BasePagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.fragment.ConfirmationDialogFragment;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IssueStateTrackingFloatingActionButton;
import com.gh4a.widget.MarkdownButtonsBar;
import com.gh4a.widget.MarkdownPreviewWebView;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.Milestone;
import com.meisolsson.githubsdk.model.request.issue.CreateMilestone;
import com.meisolsson.githubsdk.model.request.issue.EditMilestone;
import com.meisolsson.githubsdk.service.issues.IssueMilestoneService;

import java.util.Calendar;
import java.util.Date;

import io.reactivex.Single;
import retrofit2.Response;

public class IssueMilestoneEditActivity extends BasePagerActivity implements
        View.OnClickListener, View.OnFocusChangeListener,
        AppBarLayout.OnOffsetChangedListener, ConfirmationDialogFragment.Callback {
    public static Intent makeEditIntent(Context context, String repoOwner, String repoName,
            Milestone milestone, boolean fromPullRequest) {
        return makeCreateIntent(context, repoOwner, repoName, fromPullRequest)
                .putExtra("milestone", milestone);
    }

    public static Intent makeCreateIntent(Context context, String repoOwner, String repoName,
            boolean fromPullRequest) {
        return new Intent(context, IssueMilestoneEditActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("from_pr", fromPullRequest);
    }

    private static final int[] TITLES = {
        R.string.issue_body, R.string.preview, R.string.settings
    };

    private String mRepoOwner;
    private String mRepoName;
    private boolean mFromPullRequest;

    private Milestone mMilestone;

    private View mRootView;
    private IssueStateTrackingFloatingActionButton mSaveFab;
    private TextInputLayout mTitleWrapper;
    private EditText mTitleView;
    private MarkdownButtonsBar mMarkdownButtons;
    private EditText mDescriptionView;
    private TextView mDueView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Gh4Application.get().isAuthorized()) {
            Intent intent = new Intent(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        LayoutInflater headerInflater = LayoutInflater.from(this);
        View header = headerInflater.inflate(R.layout.issue_create_header, null);
        addHeaderView(header, false);

        mTitleWrapper = header.findViewById(R.id.title_wrapper);
        mTitleView = header.findViewById(R.id.et_title);
        mTitleView.setOnFocusChangeListener(this);

        mDescriptionView = findViewById(R.id.editor);
        mDueView = findViewById(R.id.tv_due);

        mMarkdownButtons = findViewById(R.id.markdown_buttons);
        mMarkdownButtons.setEditText(mDescriptionView);

        MarkdownPreviewWebView preview = findViewById(R.id.preview);
        preview.setEditText(mDescriptionView);

        CoordinatorLayout rootLayout = getRootLayout();
        mSaveFab = (IssueStateTrackingFloatingActionButton)
                getLayoutInflater().inflate(R.layout.accept_fab, rootLayout, false);
        mSaveFab.setOnClickListener(this);
        rootLayout.addView(mSaveFab);

        findViewById(R.id.due_container).setOnClickListener(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);

        if (mMilestone == null) {
            mMilestone = Milestone.builder().state(IssueState.Open).build();
        }

        mTitleView.addTextChangedListener(new UiUtils.EmptinessWatchingTextWatcher(mTitleView) {
            @Override
            public void onIsEmpty(boolean isEmpty) {
                if (isEmpty) {
                    mTitleWrapper.setError(getString(R.string.issue_error_milestone_title));
                } else {
                    mTitleWrapper.setErrorEnabled(false);
                }
                mSaveFab.setEnabled(!isEmpty);
            }
        });

        mTitleView.setText(mMilestone.title());
        mDescriptionView.setText(mMilestone.description());
        updateLabels();
        setToolbarScrollable(false);
        adjustTabsForHeaderAlignedFab(true);
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(isInEditMode()
                ? R.string.issue_milestone_edit
                : R.string.issue_milestone_new);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected PagerAdapter createAdapter(ViewGroup root) {
        mRootView = root;
        getLayoutInflater().inflate(R.layout.issue_create_milestone, root);
        return new MilestonePagerAdapter();
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mFromPullRequest = extras.getBoolean("from_pr", false);
        mMilestone = extras.getParcelable("milestone");
    }

    private boolean isInEditMode() {
        return getIntent().hasExtra("milestone");
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // swipe-to-refresh doesn't make much sense in the
        // interaction model of this activity
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isInEditMode()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.edit_milestone_menu, menu);

            if (mMilestone.state() == IssueState.Open) {
                menu.removeItem(R.id.milestone_reopen);
            } else {
                menu.removeItem(R.id.milestone_close);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected Intent navigateUp() {
        return IssueMilestoneListActivity.makeIntent(this, mRepoOwner, mRepoName, mFromPullRequest);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.due_container) {
            DialogFragment newFragment = new DatePickerFragment();
            newFragment.show(getSupportFragmentManager(), "datePicker");
        } else if (view == mSaveFab) {
            String title = mTitleView.getText().toString();
            String desc = mDescriptionView.getText() != null ?
                    mDescriptionView.getText().toString() : null;

            saveMilestone(title, desc);
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mTitleView) {
            mMarkdownButtons.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // Set the bottom padding to make the bottom appear as not moving while the
        // AppBarLayout pushes it down or up.
        mRootView.setPadding(mRootView.getPaddingLeft(), mRootView.getPaddingTop(),
                mRootView.getPaddingRight(), appBarLayout.getTotalScrollRange() + verticalOffset);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.milestone_close:
            case R.id.milestone_reopen:
                showOpenCloseConfirmDialog(item.getItemId() == R.id.milestone_reopen);
                return true;
            case R.id.delete: {
                final String message =
                        getString(R.string.issue_dialog_delete_message, mMilestone.title());
                ConfirmationDialogFragment.show(this, message, R.string.delete, null, "deleteconfirm");
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfirmed(String tag, Parcelable data) {
        switch (tag) {
            case "deleteconfirm":
                deleteMilestone();
                break;
            case "opencloseconfirm": {
                boolean reopen = ((Bundle) data).getBoolean("reopen");
                setMilestoneState(reopen);
                break;
            }
        }
    }

    private void showOpenCloseConfirmDialog(final boolean reopen) {
        @StringRes int messageResId = reopen
                ? R.string.issue_milestone_reopen_message : R.string.issue_milestone_close_message;
        @StringRes int buttonResId = reopen
                ? R.string.pull_request_reopen : R.string.pull_request_close;
        Bundle data = new Bundle();
        data.putBoolean("reopen", reopen);
        ConfirmationDialogFragment.show(this, messageResId, buttonResId, data, "opencloseconfirm");
    }

    private void setDueOn(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        mMilestone = mMilestone.toBuilder()
                .dueOn(cal.getTime())
                .build();
        updateLabels();
    }

    private void resetDueOn() {
        mMilestone = mMilestone.toBuilder()
                .dueOn(null)
                .build();
        updateLabels();
    }

    private void updateLabels() {
        Date dueOn = mMilestone.dueOn();

        if (dueOn != null) {
            mDueView.setText(DateFormat.getMediumDateFormat(this).format(dueOn));
        } else {
            mDueView.setText(R.string.issue_milestone_due_unset);
        }
    }

    private Single<Response<Milestone>> createMilestone(String title, String desc,
            IssueMilestoneService service) {
        CreateMilestone request = CreateMilestone.builder()
                .title(title)
                .description(desc)
                .state(mMilestone.state())
                .dueOn(mMilestone.dueOn())
                .build();

        return service.createMilestone(mRepoOwner, mRepoName, request);
    }

    private Single<Response<Milestone>> editMilestone(String title, String desc,
            IssueMilestoneService service) {
        EditMilestone request = EditMilestone.builder()
                .title(title)
                .description(desc)
                .state(mMilestone.state())
                .dueOn(mMilestone.dueOn())
                .build();

        return service.editMilestone(mRepoOwner, mRepoName, mMilestone.number(), request);
    }

    private void saveMilestone(String title, String desc) {
        @StringRes int errorMessageResId = isInEditMode()
                ? R.string.issue_error_edit_milestone : R.string.issue_error_create_milestone;
        String errorMessage = getString(errorMessageResId, title);
        IssueMilestoneService service = ServiceFactory.get(IssueMilestoneService.class, false);
        Single<Response<Milestone>> responseSingle = isInEditMode()
                ? editMilestone(title, desc, service) : createMilestone(title, desc, service);

        responseSingle
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, R.string.saving_msg, errorMessage))
                .subscribe(result -> {
                    mMilestone = result;
                    setResult(RESULT_OK);
                    finish();
                }, error -> handleActionFailure("Saving milestone failed", error));
    }

    private void deleteMilestone() {
        IssueMilestoneService service = ServiceFactory.get(IssueMilestoneService.class, false);
        service.deleteMilestone(mRepoOwner, mRepoName, mMilestone.number())
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, R.string.deleting_msg, R.string.issue_error_delete_milestone))
                .subscribe(result -> {
                    setResult(RESULT_OK);
                    finish();
                }, error -> handleActionFailure("Deleting milestone failed", error));
    }

    private void setMilestoneState(boolean open) {
        @StringRes int dialogMessageResId = open ? R.string.opening_msg : R.string.closing_msg;
        String errorMessage = getString(
                open ? R.string.issue_milestone_reopen_error : R.string.issue_milestone_close_error,
                mMilestone.title());
        IssueMilestoneService service = ServiceFactory.get(IssueMilestoneService.class, false);
        EditMilestone request = EditMilestone.builder()
                .state(open ? IssueState.Open : IssueState.Closed)
                .build();

        service.editMilestone(mRepoOwner, mRepoName, mMilestone.number(), request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, dialogMessageResId, errorMessage))
                .subscribe(result -> {
                    mMilestone = result;
                    supportInvalidateOptionsMenu();
                    setResult(RESULT_OK);
                }, error -> handleActionFailure("Updating milestone failed", error));
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener, DialogInterface.OnClickListener {
        private boolean mStopping;

        @Override
        public @NonNull Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
            final IssueMilestoneEditActivity activity = (IssueMilestoneEditActivity) getActivity();
            final Calendar c = Calendar.getInstance();

            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            Date dueOn = activity.mMilestone.dueOn();
            if (dueOn != null) {
                c.setTime(dueOn);
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            }
            DatePickerDialog dialog = new DatePickerDialog(activity, this, year, month, day) {
                @Override
                protected void onStop() {
                    mStopping = true;
                    super.onStop();
                    mStopping = false;
                }
            };
            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.unset), this);
            return dialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEUTRAL) {
                getEditActivity().resetDueOn();
            }
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            if (!mStopping) {
                getEditActivity().setDueOn(year, month, day);
            }
        }

        private IssueMilestoneEditActivity getEditActivity() {
            return (IssueMilestoneEditActivity) getActivity();
        }
    }

    private class MilestonePagerAdapter extends PagerAdapter {
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
            return TITLES.length;
        }
    }
}
