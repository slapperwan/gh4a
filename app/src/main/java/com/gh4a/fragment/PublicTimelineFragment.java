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
package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.Gh4Application;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.activity.EventService;

import java.io.IOException;

public class PublicTimelineFragment extends EventListFragment {
    public static PublicTimelineFragment newInstance() {
        PublicTimelineFragment f = new PublicTimelineFragment();
        f.setArguments(new Bundle());
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    protected PageIteratorLoader<GitHubEvent> onCreateLoader() {
        final EventService service = Gh4Application.get().getGitHubService(EventService.class);
        return new PageIteratorLoader<GitHubEvent>(getActivity()) {
            @Override
            protected Page<GitHubEvent> loadPage(int page) throws IOException {
                return ApiHelpers.throwOnFailure(service.getPublicEvents(page).blockingGet());
            }
        };
    }
}