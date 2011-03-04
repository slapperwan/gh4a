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

import com.github.api.v2.schema.UserFeed;
import com.github.api.v2.services.FeedService;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;

/**
 * The UserPrivate activity.
 */
public class UserPrivateActivity extends UserFeedActivity {

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserActivity#getFeeds()
     */
    @Override
    public List<UserFeed> getFeeds() throws GitHubException {
        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
        FeedService feedService = factory.createFeedService();
        
        Authentication auth = new LoginPasswordAuthentication(mUserLogin, getAuthPassword());
        feedService.setAuthentication(auth);
        return feedService.getPrivateUserFeedJson(mUserLogin);
    }

}