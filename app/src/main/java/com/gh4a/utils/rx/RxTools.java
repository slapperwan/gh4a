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
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.gh4a.BasePagerActivity;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.philosophicalhacker.lib.RxLoader;

public class RxTools {
    public static <T> ObservableTransformer<T, T> handle(Activity activity, int id) {
        return observable -> {
            final RxLoader rxLoader = new RxLoader(activity, ((AppCompatActivity)activity).getSupportLoaderManager());
            return observable
                    .compose(handleError(activity))
                    .compose(rxLoader.makeObservableTransformer(id));
        };
    }

    public static <T> ObservableTransformer<T, T> handle(Activity activity, int id, boolean refresh) {
        return observable -> {
            final RxLoader rxLoader = new RxLoader(activity, ((AppCompatActivity)activity).getSupportLoaderManager());
            return observable
                    .compose(handleError(activity))
                    .compose(rxLoader.makeObservableTransformer(id, refresh));
        };
    }

    // Error handler showing SnackBar
    public static <T> ObservableTransformer<T, T> onErrorSnackbar(Activity activity, int id, View rootLayout, String errorMessage, int messageRes) {
        return observable -> {
            final RxLoader rxLoader = new RxLoader(activity, ((AppCompatActivity)activity).getSupportLoaderManager());
            return observable
                    .compose(rxLoader.makeObservableTransformer(id))
                    .doOnSubscribe(disposable -> showProgressDialog(activity, messageRes))
                    .doOnTerminate(() -> dismissProgressDialog())
                    .doOnError(error -> {
                        Snackbar.make(rootLayout, errorMessage, Snackbar.LENGTH_LONG)
                                .setAction(R.string.retry, (View.OnClickListener) activity)
                                .show();
                    });
        };
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
