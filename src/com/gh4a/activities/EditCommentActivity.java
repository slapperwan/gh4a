package com.gh4a.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public abstract class EditCommentActivity extends BaseSherlockFragmentActivity {
    protected String mRepoOwner;
    protected String mRepoName;
    protected long mCommentId;
    private EditText mEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_text);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mCommentId = data.getLong(Constants.Comment.ID);
        String text = data.getString(Constants.Comment.BODY);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.issue_comment_title) + " " + mCommentId);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mEditText = (EditText) findViewById(R.id.et_text);
        mEditText.setText(text);

        setResult(RESULT_CANCELED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.accept_delete, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.accept:
            String text = mEditText.getText().toString();
            if (!StringUtils.isBlank(text)) {
                updateComment(mCommentId, text);
            }
            return true;
        case R.id.delete:
            new AlertDialog.Builder(this)
                    .setMessage(R.string.delete_comment_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            deleteComment(mCommentId);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract void updateComment(long id, String text);
    protected abstract void deleteComment(long id);
}