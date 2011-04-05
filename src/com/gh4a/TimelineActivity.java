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

import java.util.List;

import android.util.Log;

import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.UserFeed;
import com.github.api.v2.services.FeedService;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;


public class TimelineActivity extends UserFeedActivity {

    @Override
    public List<UserFeed> getFeeds() {
        try {
            GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
            FeedService feedService = factory.createFeedService();
            return feedService.getPublicTimelineFeedJson();
        }
        catch (GitHubException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            showError();
            return null;
        }
    }
    
    @Override
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[1];

        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(getResources().getString(R.string.explore));
        b.setTag(Constants.EXPLORE);
        breadCrumbHolders[0] = b;
        
        createBreadcrumb(getResources().getStringArray(R.array.explore_item)[0], breadCrumbHolders);
    }
}
