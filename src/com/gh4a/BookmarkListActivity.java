package com.gh4a;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.gh4a.adapter.BookmarkAdapter;
import com.gh4a.db.Bookmark;
import com.gh4a.db.BookmarkParam;
import com.gh4a.db.DbHelper;

public class BookmarkListActivity extends BaseActivity {

    private String mName;
    private String mObjectType;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();
        createBreadcrumb(getResources().getString(R.string.bookmarks), null);
        
        mName = getIntent().getStringExtra(Constants.Bookmark.NAME);
        mObjectType = getIntent().getStringExtra(Constants.Bookmark.OBJECT_TYPE);
        
        fillData();
    }
    
    private void fillData() {
        final DbHelper db = new DbHelper(this);
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Bookmark b = (Bookmark) adapterView.getAdapter().getItem(position);
                if (position == 0) {
                    setResult(Constants.Bookmark.ADD, null);
                    Toast.makeText(BookmarkListActivity.this, 
                            getResources().getString(R.string.bookmark_saved), Toast.LENGTH_LONG).show();
                    finish();
                }
                else {
                    List<BookmarkParam> params = db.findBookmarkParams(b.getId());
                    Intent intent;
                    try {
                        intent = new Intent().setClass(BookmarkListActivity.this, Class.forName(b.getObjectClass()));
                        for (BookmarkParam param : params) {
                            try {
                                int v = Integer.parseInt(param.getValue());
                                intent.putExtra(param.getKey(), v);
                            }
                            catch (NumberFormatException e) {
                                intent.putExtra(param.getKey(), param.getValue());
                            }
                        }
                        startActivity(intent);
                    }
                    catch (ClassNotFoundException e) {
                        Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    }
                }
            }
            
        });
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position != 0) {
                    final Bookmark b = (Bookmark) adapterView.getAdapter().getItem(position);
                    AlertDialog.Builder builder = new AlertDialog.Builder(BookmarkListActivity.this);
                    builder.setTitle(R.string.remove_bookmark);
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(R.string.remove_bookmark_confirm)
                           .setCancelable(false)
                           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                   dialog.dismiss();
                                   db.deleteBookmark(b.getId());
                                   fillData();
                               }
                           })
                           .setNegativeButton("No", new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                               }
                           });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                return false;
            }
        });
        
        BookmarkAdapter adapter = new BookmarkAdapter(this, new ArrayList<Bookmark>());
        listView.setAdapter(adapter);
        
        List<Bookmark> list = db.findAllBookmark();
        
        Bookmark newBookmark = new Bookmark();
        newBookmark.setName(mName);
        newBookmark.setObjectType(mObjectType);
        adapter.add(newBookmark);
        for (Bookmark b : list) {
            adapter.add(b);
            adapter.notifyDataSetChanged();
        }
    }
}
