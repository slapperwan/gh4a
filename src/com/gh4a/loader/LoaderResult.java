package com.gh4a.loader;

import android.content.Context;
import android.widget.Toast;

import com.gh4a.Gh4Application;

public class LoaderResult<T> {
    private T mData;
    private Exception mException;

    LoaderResult(T data) {
        mData = data;
    }

    LoaderResult(Exception e) {
        mException = e;
    }

    public T getData() {
        return mData;
    }

    public boolean isSuccess() {
        return mException == null;
    }

    private boolean isAuthError() {
        if (mException == null) {
            return false;
        }
        return "Received authentication challenge is null".equalsIgnoreCase(mException.getMessage())
                || "No authentication challenges found".equalsIgnoreCase(mException.getMessage());
    }

    public Exception getException() {
        return mException;
    }

    public String getErrorMessage() {
        return mException != null ? mException.getMessage() : null;
    }

    public boolean handleError(Context context) {
        if (isSuccess()) {
            return false;
        }

        if (isAuthError()) {
            Gh4Application.get().logout();
        }
        Toast.makeText(context, getErrorMessage(), Toast.LENGTH_SHORT).show();
        return true;
    }
}