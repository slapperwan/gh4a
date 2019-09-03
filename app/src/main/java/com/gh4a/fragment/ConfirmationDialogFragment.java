package com.gh4a.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import com.gh4a.R;

public class ConfirmationDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {
    public interface Callback {
        void onConfirmed(String tag, Parcelable data);
    }

    public static <C extends Fragment & Callback> void show(C parent,
            @StringRes int dialogTextResId, @StringRes int confirmButtonTextResId,
            Parcelable data, String tag) {
        show(parent, parent.getString(dialogTextResId), confirmButtonTextResId, false, data, tag);
    }

    public static <C extends Fragment & Callback> void show(C parent,
            @StringRes int dialogTextResId, @StringRes int confirmButtonTextResId,
            boolean showWarning, Parcelable data, String tag) {
        show(parent, parent.getString(dialogTextResId), confirmButtonTextResId,
                showWarning, data, tag);
    }

    public static <C extends Fragment & Callback> void show(C parent,
            String dialogText, @StringRes int confirmButtonTextResId,
            Parcelable data, String tag) {
        show(parent, dialogText, confirmButtonTextResId, false, data, tag);
    }

    public static <C extends Fragment & Callback> void show(C parent,
            String dialogText, @StringRes int confirmButtonTextResId, boolean showWarning,
            Parcelable data, String tag) {
        ConfirmationDialogFragment f = new ConfirmationDialogFragment();
        f.setArguments(buildArgs(dialogText, confirmButtonTextResId, showWarning, data));
        f.setTargetFragment(parent, 0);
        f.show(parent.getChildFragmentManager(), tag);
    }

    public static <C extends FragmentActivity & Callback> void show(C parent,
            @StringRes int dialogTextResId, @StringRes int confirmButtonTextResId,
            Parcelable data, String tag) {
        show(parent, parent.getString(dialogTextResId), confirmButtonTextResId, false, data, tag);
    }

    public static <C extends FragmentActivity & Callback> void show(C parent,
            @StringRes int dialogTextResId, @StringRes int confirmButtonTextResId,
            boolean showWarning, Parcelable data, String tag) {
        show(parent, parent.getString(dialogTextResId), confirmButtonTextResId, showWarning, data, tag);
    }

    public static <C extends FragmentActivity & Callback> void show(C parent,
            String dialogText, @StringRes int confirmButtonTextResId, Parcelable data, String tag) {
        show(parent, dialogText, confirmButtonTextResId, false, data, tag);
    }

    public static <C extends FragmentActivity & Callback> void show(C parent,
            String dialogText, @StringRes int confirmButtonTextResId, boolean showWarning,
            Parcelable data, String tag) {
        ConfirmationDialogFragment f = new ConfirmationDialogFragment();
        f.setArguments(buildArgs(dialogText, confirmButtonTextResId, showWarning, data));
        f.show(parent.getSupportFragmentManager(), tag);
    }

    private static Bundle buildArgs(String text, @StringRes int confirmButtonTextResId,
            boolean showWarning, Parcelable data) {
        Bundle args = new Bundle();
        args.putString("text", text);
        args.putInt("confirmButtonTextResId", confirmButtonTextResId);
        args.putBoolean("showWarning", showWarning);
        args.putParcelable("data", data);
        return args;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setMessage(args.getString("text"))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(args.getInt("confirmButtonTextResId"), this);
        if (args.getBoolean("showWarning")) {
            builder.setIconAttribute(android.R.attr.alertDialogIcon);
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Parcelable data = getArguments().getParcelable("data");
        if (getTargetFragment() instanceof Callback) {
            Callback cb = (Callback) getTargetFragment();
            cb.onConfirmed(getTag(), data);
        } else if (getActivity() instanceof Callback) {
            Callback cb = (Callback) getActivity();
            cb.onConfirmed(getTag(), data);
        }
    }
}
