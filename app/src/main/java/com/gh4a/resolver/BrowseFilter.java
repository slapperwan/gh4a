package com.gh4a.resolver;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.IntentUtils;

public class BrowseFilter extends AppCompatActivity {
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_NOTIFICATION_ID = "notification_id";
    private static final String EXTRA_INITIAL_COMMENT = "initial_comment";
    private static final int ACTION_MARK_READ = 1;

    public static Intent makeMarkNotificationAsReadActionIntent(Context context,
            String notificationId) {
        return makeRedirectionIntent(context, null, null)
                .putExtra(EXTRA_ACTION, ACTION_MARK_READ)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
    }

    public static Intent makeRedirectionIntent(Context context, Uri uri,
            IntentUtils.InitialCommentMarker initialComment) {
        Intent intent = new Intent(context, BrowseFilter.class);
        intent.setData(uri);
        intent.putExtra(EXTRA_INITIAL_COMMENT, initialComment);
        return intent;
    }

    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME == R.style.DarkTheme
                ? R.style.TransparentDarkTheme : R.style.TransparentLightTheme);

        super.onCreate(savedInstanceState);

        if (handleAction()) {
            return;
        }

        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        LinkParser.ParseResult result = LinkParser.parseUri(this, uri);
        if (result == null) {
            IntentUtils.launchBrowser(this, uri);
            finish();
            return;
        }

        if (result.intent != null) {
            startActivity(result.intent);
            finish();
            return;
        }

        //noinspection ConstantConditions
        result.loadTask.execute();

        // Avoid finish() for now
    }

    private boolean handleAction() {
        Bundle extras = getIntent().getExtras();
        int action = extras.getInt(EXTRA_ACTION, -1);
        switch (action) {
            case ACTION_MARK_READ:
                markNotificationAsRead(extras);
                return true;
        }
        return false;
    }

    private void markNotificationAsRead(Bundle extras) {
        String notificationId = extras.getString(EXTRA_NOTIFICATION_ID);
        new MarkNotificationAsReadTask(this, notificationId).schedule();
        finish();
    }
}
