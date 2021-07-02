package com.gh4a.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gh4a.activities.SettingsActivity;

public class ActivityResultHelpers {
    public static class StartSettingsContract extends ActivityResultContract<Void, Boolean> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, Void input) {
            return new Intent(context, SettingsActivity.class);
        }

        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            return intent != null && intent.getBooleanExtra(SettingsActivity.RESULT_EXTRA_THEME_CHANGED, false);
        }
    }

    public static class ActivityResultSuccessCallback implements ActivityResultCallback<ActivityResult> {
        public interface Callback {
            void onActivityResultOk();
        }

        private final Callback mCallback;

        public ActivityResultSuccessCallback(Callback cb) {
            mCallback = cb;
        }
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                mCallback.onActivityResultOk();
            }
        }
    }
}
