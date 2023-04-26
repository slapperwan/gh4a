package com.gh4a.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.gh4a.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class SingleChoiceDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_TITLE_RES_ID = "titleResId";
    private static final String KEY_SELECTED_POSITION = "selectedPosition";

    private List<String> mEntries;

    public interface Callback {
        void onItemSelected(String tag, int position, String entry);
    }

    public static <C extends FragmentActivity & Callback> void show(C parent,
            List<String> entries, @StringRes int titleResId, int selectedPosition, String tag) {
        Bundle args = new Bundle();
        args.putStringArrayList(KEY_ENTRIES, new ArrayList<>(entries));
        args.putInt(KEY_TITLE_RES_ID, titleResId);
        args.putInt(KEY_SELECTED_POSITION, selectedPosition);

        SingleChoiceDialogFragment f = new SingleChoiceDialogFragment();
        f.setArguments(args);
        f.show(parent.getSupportFragmentManager(), tag);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        mEntries = args.getStringArrayList(KEY_ENTRIES);

        String[] entryArray = mEntries.toArray(new String[0]);
        int selectedPosition = args.getInt(KEY_SELECTED_POSITION);

        return new MaterialAlertDialogBuilder(getContext())
                .setTitle(args.getInt(KEY_TITLE_RES_ID))
                .setSingleChoiceItems(entryArray, selectedPosition, this)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Callback cb = (Callback) getActivity();
        cb.onItemSelected(getTag(), which, mEntries.get(which));
        dialog.dismiss();
    }
}
