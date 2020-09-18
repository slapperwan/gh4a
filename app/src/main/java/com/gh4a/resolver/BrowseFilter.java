package com.gh4a.resolver;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.gh4a.R;
import com.gh4a.utils.IntentUtils;

public class BrowseFilter extends AppCompatActivity {
    private static final String EXTRA_INITIAL_COMMENT = "initial_comment";

    public static Intent makeRedirectionIntent(Context context, Uri uri,
            IntentUtils.InitialCommentMarker initialComment) {
        Intent intent = new Intent(context, BrowseFilter.class);
        intent.setData(uri);
        intent.putExtra(EXTRA_INITIAL_COMMENT, initialComment);
        return intent;
    }

    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.TransparentTheme);

        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        int flags = getIntent().getFlags() & ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
        IntentUtils.InitialCommentMarker initialComment =
                getIntent().getParcelableExtra(EXTRA_INITIAL_COMMENT);

        LinkParser.ParseResult result = LinkParser.parseUri(this, uri, initialComment);
        if (result == null) {
            IntentUtils.launchBrowser(this, uri, flags);
            finish();
            return;
        }

        if (result.intent != null) {
            startActivity(result.intent.setFlags(flags));
            finish();
            return;
        }

        result.loadTask.setIntentFlags(flags);
        result.loadTask.execute();

        // Avoid finish() for now
    }
}
