package com.gh4a;

import android.os.Bundle;
import android.widget.EditText;

import com.actionbarsherlock.R;
import com.actionbarsherlock.app.ActionBar;

public class EditCommentActivity extends BaseSherlockFragmentActivity {

    private long mCommentId;
    private String mText;
    private EditText mEditText;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_text);
        
        Bundle data = getIntent().getExtras();
        mCommentId = data.getLong(Constants.Comment.ID);
        mText = data.getString(Constants.Comment.BODY);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.issue_comment_title) + " " + mCommentId);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mEditText = (EditText) findViewById(R.id.et_text);
        mEditText.setText(mText);
    }
    
}
