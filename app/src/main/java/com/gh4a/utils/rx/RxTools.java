package com.gh4a.utils.rx;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import com.gh4a.BasePagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;

public class RxTools {
    public static <T> ObservableTransformer<T, T> bind(Gh4Application app) {
        return observable -> observable.doOnSubscribe(disposable -> { // Add observable Subscription to composite disposable
            ObservableManager manager = app.getObservableManager();
            manager.addSubscription(disposable);
        });
    }

    public static <T> ObservableTransformer<T, T> handleCache(Gh4Application app, String cacheKey) {
        return observable -> {
            ObservableManager manager = app.getObservableManager();

            // If cache contains object, use it
            if(manager.isCached(cacheKey)) {
                return manager.getCached(cacheKey);
            }

            manager.addCache(cacheKey, observable);
            return observable;
        };
    }

    public static <T> ObservableTransformer<T, T> handleNoCache(Gh4Application app, Activity activity) {
        return observable -> observable
                .compose(bind(app))
                .compose(applySchedulers())
                .compose(handleError(activity));
    }

    public static <T> ObservableTransformer<T, T> handle(Gh4Application app, Activity activity, String cacheKey) {
        return observable -> observable.cache()
                .compose(handleCache(app, cacheKey))
                .compose(bind(app))
                .compose(applySchedulers())
                .compose(handleError(activity));
    }

    // Error handler showing SnackBar
    public static <T> ObservableTransformer<T, T> onErrorSnackbar(Activity activity, View rootLayout, String errorMessage, int messageRes) {
        return observable -> observable
                .compose(RxTools.applySchedulers())
                .doOnSubscribe(disposable -> {
                    showProgressDialog(activity, messageRes);
                })
                .doOnTerminate(() -> {
                    dismissProgressDialog();
                })
                .doOnError(error -> {
                    Snackbar.make(rootLayout, errorMessage, Snackbar.LENGTH_LONG)
                            .setAction(R.string.retry, (View.OnClickListener) activity)
                            .show();
                });
    }

    public static <T> ObservableTransformer<T, T> handleError(Activity activity) {
        return observable -> observable
                .doOnError(error -> {
                    if(activity instanceof BasePagerActivity) {
                        BasePagerActivity act = (BasePagerActivity) activity;
                        act.setErrorViewVisibility(true);
                    } else {
                        // other cases
                    }
                });
    }

    public static Observable emptyCache(Gh4Application app, String key) {
        return Observable.fromCallable(() -> {
            app.getObservableManager().emptyCache(key);
            return true;
        });
    }

    public static Observable emptyAllCache(Gh4Application app) {
        return Observable.fromCallable(() -> {
            app.getObservableManager().emptyCache(null);
            return true;
        });
    }

    public static <T> ObservableTransformer<T, T> applySchedulers() {
        return observable -> ((Observable)observable).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    public static ProgressDialogFragment mProgressDialogFragment;

    public static class ProgressDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int messageResId = getArguments().getInt("message_res", 0);
            return UiUtils.createProgressDialog(getActivity(), messageResId);
        }
    }

    public static void dismissProgressDialog() {
        if (mProgressDialogFragment.getActivity() != null) {
            mProgressDialogFragment.dismissAllowingStateLoss();
        }
        mProgressDialogFragment = null;
    }

    public static void showProgressDialog(Activity activity, int messageRes) {
        Bundle args = new Bundle();
        args.putInt("message_res", messageRes);
        mProgressDialogFragment = new ProgressDialogFragment();
        mProgressDialogFragment.setArguments(args);
        mProgressDialogFragment.show(((FragmentActivity)activity).getSupportFragmentManager(), "progressdialog");
    }
}
