package com.gh4a;

import com.gh4a.utils.ToastUtils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import java.lang.ref.WeakReference;

public abstract class ProgressDialogTask<T> extends BackgroundTask<T> {
    private ProgressDialogFragment mFragment;
    private WeakReference<FragmentActivity> mActivity;
    private int mTitleResId;
    private int mMessageResId;

    public ProgressDialogTask(FragmentActivity activity, int titleResId, int messageResId) {
        super(activity);
        mActivity = new WeakReference<>(activity);
        mTitleResId = titleResId;
        mMessageResId = messageResId;
    }

    @Override
    protected void onPreExecute() {
        FragmentActivity fa = mActivity.get();
        if (fa != null) {
            Bundle args = new Bundle();
            args.putInt("title_res", mTitleResId);
            args.putInt("message_res", mMessageResId);
            mFragment = new ProgressDialogFragment();
            mFragment.setArguments(args);
            mFragment.show(fa.getSupportFragmentManager(), "progressdialog");
        }

        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(T result) {
        if (mFragment != null) {
            mFragment.dismissAllowingStateLoss();
            mFragment = null;
        }
        super.onPostExecute(result);
    }

    @Override
    protected void onError(Exception e) {
        super.onError(e);
        ToastUtils.showError(mContext);
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
