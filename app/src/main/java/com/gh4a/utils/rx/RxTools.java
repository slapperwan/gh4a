package com.gh4a.utils.rx;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Action;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.view.View;
import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.philosophicalhacker.lib.RxLoader;
import java.util.concurrent.Callable;

public class RxTools {
    public static Observable runCallable(Callable call, BaseActivity activity, int loaderId, boolean refresh) {
        return Observable.fromCallable(call)
                .compose(RxTools.handle(activity, loaderId, refresh));
    }

    public static Observable runCallable(Callable call, BaseActivity activity, int loaderId) {
        return Observable.fromCallable(call)
                .compose(RxTools.handle(activity, loaderId, false));
    }

    public static Observable runOnErrorSnackBar(Action action, BaseActivity activity, int loaderId,
            String errorMessage, int errorRes) {
        return Completable.fromAction(action).toObservable()
                .compose(RxTools.onErrorSnackbar(activity, loaderId, errorMessage, errorRes));
    }

    public static <T> ObservableTransformer<T, T> handle(BaseActivity activity, int id) {
        return observable -> {
            final RxLoader rxLoader = new RxLoader(activity, activity.getSupportLoaderManager());
            return observable
                .compose(handleError(activity))
                .compose(rxLoader.makeObservableTransformer(id));
        };
    }

    public static <T> ObservableTransformer<T, T> handle(BaseActivity activity, int id, boolean refresh) {
        return observable -> {
            final RxLoader rxLoader = new RxLoader(activity, activity.getSupportLoaderManager());

            if (refresh) {
                return observable
                    .compose(handleError(activity))
                    .compose(rxLoader.makeObservableTransformer(id, refresh));
            } else {
                return observable.compose(handle(activity, id));
            }
        };
    }

    // Error handler showing SnackBar
    public static <T> ObservableTransformer<T, T> onErrorSnackbar(BaseActivity activity, int id, String errorMessage, int messageRes) {
        return observable -> {
            final RxLoader rxLoader = new RxLoader(activity, activity.getSupportLoaderManager());
            return observable
                .compose(rxLoader.makeObservableTransformer(id))
                .doOnSubscribe(disposable -> showProgressDialog(activity, messageRes))
                .doOnTerminate(() -> dismissProgressDialog())
                .doOnError(error -> {
                    Snackbar.make(activity.getRootLayout(), errorMessage, Snackbar.LENGTH_LONG)
                            .setAction(R.string.retry, (View.OnClickListener) activity)
                            .show();
                });
        };
    }

    public static <T> ObservableTransformer<T, T> handleError(BaseActivity activity) {
        return observable -> observable
            .doOnError(error -> activity.setErrorViewVisibility(true));
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

    public static void showProgressDialog(BaseActivity activity, int messageRes) {
        Bundle args = new Bundle();
        args.putInt("message_res", messageRes);
        mProgressDialogFragment = new ProgressDialogFragment();
        mProgressDialogFragment.setArguments(args);
        mProgressDialogFragment.show(activity.getSupportFragmentManager(), "progressdialog");
    }
}
