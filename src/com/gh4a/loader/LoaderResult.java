package com.gh4a.loader;

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

    public boolean isAuthError() {
        if (mException == null) {
            return false;
        }
        return "Received authentication challenge is null".equalsIgnoreCase(mException.getMessage())
                || "No authentication challenges found".equalsIgnoreCase(mException.getMessage());
    }

    public Exception getException() {
        return mException;
    }
}