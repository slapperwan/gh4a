package com.gh4a.loader;

import java.util.HashMap;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Constants.LoaderResult;

public abstract class BaseLoader extends AsyncTaskLoader<HashMap<Integer, Object>> {

    public BaseLoader(Context context) {
        super(context);
    }

    @Override
    public HashMap<Integer, Object> loadInBackground() {
        HashMap<Integer, Object> result = new HashMap<Integer, Object>();
        try {
            doLoadInBackground(result);
            return result;
        } catch (Exception e) {
            result.put(LoaderResult.ERROR, true);
            result.put(LoaderResult.ERROR_MSG, e.getMessage());
            if ("Received authentication challenge is null".equalsIgnoreCase(e.getMessage())
                    || "No authentication challenges found".equalsIgnoreCase(e.getMessage())) {
                result.put(LoaderResult.AUTH_ERROR, true);
            }
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return result;
        }
    }
    
    public abstract void doLoadInBackground(HashMap<Integer, Object> result) throws Exception;

}
