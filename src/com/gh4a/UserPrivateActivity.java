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

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;

/**
 * The UserPrivate activity.
 */
public class UserPrivateActivity {

    public List<Event> getFeeds() throws IOException {
        GitHubClient client = new GitHubClient();
        EventService eventService = new EventService(client);
        return (List<Event>) eventService.pageUserReceivedEvents(null, true).next();    
    }
}