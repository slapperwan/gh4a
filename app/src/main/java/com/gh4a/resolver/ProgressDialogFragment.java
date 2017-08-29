package com.gh4a.resolver;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.gh4a.R;
import com.gh4a.utils.UiUtils;

public class ProgressDialogFragment extends DialogFragment {
    public static ProgressDialogFragment newInstance(boolean finishCurrentActivity) {
        Bundle args = new Bundle();
        args.putBoolean("finish_activity", finishCurrentActivity);
        ProgressDialogFragment dialogFragment = new ProgressDialogFragment();
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return UiUtils.createProgressDialog(getActivity(), R.string.loading_msg);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Bundle args = getArguments();
        if (args != null && args.getBoolean("finish_activity")) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
