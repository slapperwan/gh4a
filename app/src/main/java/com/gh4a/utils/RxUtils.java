package com.gh4a.utils;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.SearchPage;

import java.util.Iterator;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Predicate;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

public class RxUtils {
    public static <T> SingleTransformer<List<T>, List<T>> filter(Predicate<T> predicate) {
        return upstream -> upstream.map(list -> {
            Iterator<T> iter = list.iterator();
            while (iter.hasNext()) {
                if (!predicate.test(iter.next())) {
                    iter.remove();
                }
            }
            return list;
        });
    }

    public static <T> Single<T> doInBackground(Single<T> upstream) {
        return upstream.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static <T> SingleTransformer<T, T> wrapForBackgroundTask(final BaseActivity activity,
            final @StringRes int dialogMessageResId, final @StringRes int errorMessageResId) {
        return wrapForBackgroundTask(activity, activity.getRootLayout(), dialogMessageResId,
                activity.getString(errorMessageResId));
    }

    public static <T> SingleTransformer<T, T> wrapForBackgroundTask(final BaseActivity activity,
            final @StringRes int dialogMessageResId, final String errorMessage) {
        return wrapForBackgroundTask(activity, activity.getRootLayout(),
                dialogMessageResId, errorMessage);
    }

    public static <T> SingleTransformer<T, T> wrapForBackgroundTask(final FragmentActivity activity,
            final CoordinatorLayout rootLayout, final @StringRes int dialogMessageResId,
            final @StringRes int errorMessageResId) {
        return wrapForBackgroundTask(activity, rootLayout, dialogMessageResId,
                activity.getString(errorMessageResId));
    }

    public static <T> SingleTransformer<T, T> wrapForBackgroundTask(final FragmentActivity activity,
            final CoordinatorLayout rootLayout, final @StringRes int dialogMessageResId,
            final String errorMessage) {
        return upstream -> upstream
                .compose(RxUtils::doInBackground)
                .compose(wrapWithProgressDialog(activity, dialogMessageResId))
                .compose(wrapWithRetrySnackbar(rootLayout, errorMessage));
    }

    public static <T> SingleTransformer<T, T> wrapWithProgressDialog(final FragmentActivity activity,
            final @StringRes int messageResId) {
        return new SingleTransformer<T, T>() {
            private ProgressDialogFragment mFragment;

            @Override
            public SingleSource<T> apply(Single<T> upstream) {
                return upstream
                        .doOnSubscribe(disposable -> showDialog())
                        .doOnError(throwable -> hideDialog())
                        .doOnSuccess(result -> hideDialog());
            }

            private void showDialog() {
                Bundle args = new Bundle();
                args.putInt("message_res", messageResId);
                mFragment = new ProgressDialogFragment();
                mFragment.setArguments(args);
                mFragment.show(activity.getSupportFragmentManager(), "progressdialog");
            }

            private void hideDialog() {
                if (mFragment.getActivity() != null) {
                    mFragment.dismissAllowingStateLoss();
                }
                mFragment = null;
            }
        };
    }

    public static <T> SingleTransformer<T, T> wrapWithRetrySnackbar(
            final CoordinatorLayout rootLayout, final String errorMessage) {
        return new SingleTransformer<T, T>() {
            private final PublishProcessor<Integer> mRetryProcessor = PublishProcessor.create();
            @Override
            public SingleSource<T> apply(Single<T> upstream) {
                return upstream
                        .doOnError(error -> showSnackbar())
                        .retryWhen(handler -> handler.flatMap(error -> mRetryProcessor));
            }

            private void showSnackbar() {
                Snackbar.make(rootLayout, errorMessage, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry, view -> mRetryProcessor.onNext(0))
                        .show();
            }
        };
    }

    public static <T> Single<Response<Page<T>>> searchPageAdapter(Single<Response<SearchPage<T>>> upstream) {
        return upstream.map(response -> {
            if (response.isSuccessful()) {
                return Response.success(new ApiHelpers.SearchPageAdapter<>(response.body()));
            }
            return Response.error(response.errorBody(), response.raw());
        });
    }

    public static class ProgressDialogFragment extends DialogFragment {
        @android.support.annotation.NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int messageResId = getArguments().getInt("message_res", 0);
            return UiUtils.createProgressDialog(getActivity(), messageResId);
        }
    }
}
