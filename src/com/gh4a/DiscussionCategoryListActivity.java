package com.gh4a;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.DiscussionCategoryAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;

public class DiscussionCategoryListActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();

        setBreadCrumb();

        try {
            fillData();
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            showError();
        }
    }
    
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[1];

        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(getResources().getString(R.string.explore));
        b.setTag(Constants.EXPLORE);
        breadCrumbHolders[0] = b;
        
        createBreadcrumb(getResources().getStringArray(R.array.explore_item)[3], breadCrumbHolders);
    }
    
    private void fillData() throws IOException, JSONException {
        AssetManager asset = getAssets();
        InputStream is = asset.open("discussion-category.json");
        String jsonCategory = StringUtils.convertStreamToString(is);

        JSONArray jsonCats = new JSONArray(jsonCategory);
        
        List<JSONObject> categories = new ArrayList<JSONObject>();
        for (int i = 0; i < jsonCats.length(); i++) {
            categories.add(jsonCats.getJSONObject(i));
        }
        ListView listView = (ListView) findViewById(R.id.list_view);
        DiscussionCategoryAdapter adapter = new DiscussionCategoryAdapter(this, categories);
        
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    JSONObject category = (JSONObject) adapterView.getAdapter().getItem(position);
                    try {
                        Intent intent = new Intent().setClass(DiscussionCategoryListActivity.this, DiscussionListActivity.class);
                        intent.putExtra(Constants.Discussion.URL, category.getString("url"));
                        intent.putExtra(Constants.Discussion.TITLE, category.getString("title"));
                        startActivity(intent);
                    }
                    catch (JSONException e) {
                        Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        showError();
                    }
                }
        });
    }
}
