/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The BranchTagList activity.
 */
public class ChooseIssueStateDialog extends BaseActivity {

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list_dialog);

        ListView listView = (ListView) findViewById(R.id.list_view);
        List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("state", "Open");
        list.add(map);

        map = new HashMap<String, String>();
        map.put("state", "Closed");
        list.add(map);
        listView.setAdapter(new SimpleAdapter(getApplication(), list, R.layout.generic_row_dialog,
                new String[] { "state" }, new int[] { R.id.tv_text }));
    }

    /**
     * Callback to be invoked when branch in the AdapterView has been clicked.
     */
    private static class OnBranchClickListener implements OnItemClickListener {

        /** The target. */
        private WeakReference<ChooseIssueStateDialog> mTarget;

        /**
         * Instantiates a new on branch click listener.
         *
         * @param activity the activity
         */
        public OnBranchClickListener(ChooseIssueStateDialog activity) {
            mTarget = new WeakReference<ChooseIssueStateDialog>(activity);
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AdapterView.OnItemClickListener#onItemClick(android
         * .widget.AdapterView, android.view.View, int, long)
         */
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            ChooseIssueStateDialog activity = mTarget.get();
            SimpleAdapter adapter = (SimpleAdapter) adapterView.getAdapter();
            HashMap<String, String> data = (HashMap<String, String>) adapter.getItem(position);
            Intent intent = activity.getIntent();
            intent.putExtra(Constants.Issue.ISSUE_STATE, data.get("state"));
            activity.setResult(RESULT_OK, intent);
            activity.finish();
        }
    }
}
