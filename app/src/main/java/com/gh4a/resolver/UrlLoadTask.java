package com.gh4a.resolver;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
import com.gh4a.utils.UiUtils;

import io.reactivex.Single;

public abstract class UrlLoadTask extends AsyncTask<Void, Void, Optional<Intent>> {
    protected final FragmentActivity mActivity;
    private ProgressDialogFragment mProgressDialog;
    private int mIntentFlags;

    public UrlLoadTask(FragmentActivity activity) {
        super();
        mActivity = activity;
    }

    public void setIntentFlags(int flags) {
        mIntentFlags = flags;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialogFragment();
        mProgressDialog.show(mActivity.getSupportFragmentManager(), "progress");
    }

    @Override
    protected Optional<Intent> doInBackground(Void... params) {
        try {
            return getSingle().blockingGet();
        } catch (Exception e) {
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
            mActivity.startActivity(result.get().setFlags(mIntentFlags));
        } else {
            IntentUtils.launchBrowser(mActivity, mActivity.getIntent().getData(), mIntentFlags);
        }

        if (mProgressDialog != null && mProgressDialog.isAdded()) {
            mProgressDialog.dismissAllowingStateLoss();
        }
        mActivity.finish();
    }

    protected abstract Single<Optional<Intent>> getSingle();

    public static class ProgressDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return UiUtils.createProgressDialog(getActivity(), R.string.loading_msg);
        }
    }
}
