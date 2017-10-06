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
package com.gh4a.adapter;

import android.content.Context;

import com.gh4a.R;
import com.meisolsson.githubsdk.model.Issue;

public class RepositoryIssueAdapter extends IssueAdapter {
    public RepositoryIssueAdapter(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Issue issue) {
        super.onBindViewHolder(holder, issue);

        // https://api.github.com/repos/batterseapower/pinyin-toolkit/issues/132
        String[] urlPart = issue.url().split("/");
        holder.tvNumber.setText(mContext.getString(R.string.repo_issue_on,
                issue.number(), urlPart[4], urlPart[5]));
    }
}
