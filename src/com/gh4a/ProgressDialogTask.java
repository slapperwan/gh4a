package com.gh4a;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class ProgressDialogTask<T> extends AsyncTask<Void, Void, T> {
    protected Context mContext;
    private ProgressDialog mProgressDialog;
    private Exception mException;
    private int mTitleResId;
    private int mMessageResId;

    public ProgressDialogTask(Context context, int titleResId, int messageResId) {
        mContext = context;
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
    protected T doInBackground(Void... params) {
        try {
            return run();
        } catch (Exception e) {
            mException = e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(T result) {
        super.onPostExecute(result);
        mProgressDialog.dismiss();
        if (mException != null) {
            onError(mException);
        } else {
            onSuccess(result);
        }
    }

    protected abstract T run() throws Exception;

    protected abstract void onSuccess(T result);

    protected void onError(Exception e) { }
}
