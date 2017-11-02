package com.gh4a.resolver;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;

import io.reactivex.Single;

public abstract class UrlLoadTask extends AsyncTask<Void, Void, Optional<Intent>> {
    protected final FragmentActivity mActivity;
    private final boolean mFinishCurrentActivity;
    private ProgressDialogFragment mProgressDialog;

    public UrlLoadTask(FragmentActivity activity) {
        this(activity, true);
    }

    public UrlLoadTask(FragmentActivity activity, boolean finishCurrentActivity) {
        super();
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
    protected Optional<Intent> doInBackground(Void... params) {
        try {
            return getSingle().blockingGet();
        } catch (ApiRequestException e) {
            Log.e(Gh4Application.LOG_TAG, "Failure during intent resolving", e);
            return Optional.absent();
        }
    }

    @Override
    protected void onPostExecute(Optional<Intent> result) {
        if (mActivity.isFinishing()) {
            return;
        }

        if (result.isPresent()) {
            mActivity.startActivity(result.get());
        } else {
            IntentUtils.launchBrowser(mActivity, mActivity.getIntent().getData());
        }

        if (mProgressDialog != null && mProgressDialog.isAdded()) {
            mProgressDialog.dismissAllowingStateLoss();
        }
        if (mFinishCurrentActivity) {
            mActivity.finish();
        }
    }

    protected abstract Single<Optional<Intent>> getSingle();
}
