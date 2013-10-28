package com.gh4a;

import com.gh4a.utils.ToastUtils;

import android.app.ProgressDialog;
import android.content.Context;

public abstract class ProgressDialogTask<T> extends BackgroundTask<T> {
    private ProgressDialog mProgressDialog;
    private int mTitleResId;
    private int mMessageResId;

    public ProgressDialogTask(Context context, int titleResId, int messageResId) {
        super(context);
        mTitleResId = titleResId;
        mMessageResId = messageResId;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = ProgressDialog.show(mContext,
                mTitleResId != 0 ? mContext.getString(mTitleResId) : null,
                mContext.getString(mMessageResId), false);
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(T result) {
        mProgressDialog.dismiss();
        super.onPostExecute(result);
    }

    @Override
    protected void onError(Exception e) {
        super.onError(e);
        ToastUtils.showError(mContext);
    }
}
