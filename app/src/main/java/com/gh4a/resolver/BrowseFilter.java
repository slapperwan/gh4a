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
    public static Intent makeRedirectionIntent(Context context, Uri uri,
            IntentUtils.InitialCommentMarker initialComment) {
        Intent intent = new Intent(context, BrowseFilter.class);
        intent.setData(uri);
        intent.putExtra("initial_comment", initialComment);
        return intent;
    }

    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME == R.style.DarkTheme
                ? R.style.TransparentDarkTheme : R.style.TransparentLightTheme);

        super.onCreate(savedInstanceState);

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
}
