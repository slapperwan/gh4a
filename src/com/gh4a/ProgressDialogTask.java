package com.gh4a;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.view.View;

public abstract class ProgressDialogTask<T> extends BackgroundTask<T>
        implements View.OnClickListener {
    private ProgressDialogFragment mFragment;
    private BaseActivity mActivity;
    private int mTitleResId;
    private int mMessageResId;

    public ProgressDialogTask(BaseActivity activity, int titleResId, int messageResId) {
        super(activity);
        mActivity = activity;
        mTitleResId = titleResId;
        mMessageResId = messageResId;
    }

    protected abstract ProgressDialogTask<T> clone();
    protected abstract String getErrorMessage();

    @Override
    protected void onPreExecute() {
        Bundle args = new Bundle();
        args.putInt("title_res", mTitleResId);
        args.putInt("message_res", mMessageResId);
        mFragment = new ProgressDialogFragment();
        mFragment.setArguments(args);
        mFragment.show(mActivity.getSupportFragmentManager(), "progressdialog");

        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(T result) {
        if (mFragment.getActivity() != null) {
            mFragment.dismissAllowingStateLoss();
        }
        super.onPostExecute(result);
        mFragment = null;
        mActivity = null;
    }

    @Override
    protected void onError(Exception e) {
        super.onError(e);
        Snackbar.make(mActivity.getRootLayout(), getErrorMessage(), Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, this)
                .show();
    }

    @Override
    public void onClick(View v) {
        clone().schedule();
    }

    public static class ProgressDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog pd = new ProgressDialog(getActivity());
            Bundle args = getArguments();
            int titleResId = args.getInt("title_res", 0);
            int messageResId = args.getInt("message_res", 0);

            pd.setMessage(getString(messageResId));
            if (titleResId != 0) {
                pd.setTitle(titleResId);
            }

            return pd;
        }
    }
}
