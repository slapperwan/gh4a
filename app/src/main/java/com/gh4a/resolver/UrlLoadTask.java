package com.gh4a.resolver;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.gh4a.BackgroundTask;
import com.gh4a.utils.IntentUtils;

public abstract class UrlLoadTask extends BackgroundTask<Intent> {
    protected final FragmentActivity mActivity;
    private final boolean mFinishCurrentActivity;
    private ProgressDialogFragment mProgressDialog;

    public UrlLoadTask(FragmentActivity activity) {
        this(activity, true);
    }

    public UrlLoadTask(FragmentActivity activity, boolean finishCurrentActivity) {
        super(activity);
        mActivity = activity;
        mFinishCurrentActivity = finishCurrentActivity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = ProgressDialogFragment.newInstance(mFinishCurrentActivity);
        mProgressDialog.show(mActivity.getSupportFragmentManager(), "progress");
    }

    @Override
    protected void onSuccess(Intent result) {
        if (mActivity.isFinishing()) {
            return;
        }

        if (result != null) {
            mActivity.startActivity(result);
        } else {
            IntentUtils.launchBrowser(mActivity, mActivity.getIntent().getData());
        }

        dismiss();
    }

    @Override
    protected void onError(Exception e) {
        IntentUtils.launchBrowser(mActivity, mActivity.getIntent().getData());
        dismiss();
    }

    private void dismiss() {
        if (mProgressDialog != null && mProgressDialog.isAdded()) {
            mProgressDialog.dismissAllowingStateLoss();
        }
        if (mFinishCurrentActivity) {
            mActivity.finish();
        }
    }
}
