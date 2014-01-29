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

import java.util.ArrayList;


/**
 * The Interface Constants.
 */
public interface Constants {
    public static final String PREF_NAME = "Gh4a-pref";
    public static final String LOG_TAG = "Gh4a";

    public interface User {
        public static final String LOGIN = "USER_LOGIN";
        public static final String NAME = "USER_NAME";
        public static final String TYPE_USER = "User";
        public static final String TYPE_ORG = "Organization";
        public static final String TYPE = "Type";
        public static final String AUTH_TOKEN = "Token";
    }

    public interface Repository {
        public static final String NAME = "REPO_NAME";
        public static final String OWNER = "REPO_OWNER";
        public static final String REPOSITORY = "REPO_REPOSITORY";
        public static final String BASE = "BASE";
        public static final String HEAD = "HEAD";
        public static final String SELECTED_REF = "SELECTED_REF";
        public static final String SELECTED_BRANCHTAG_NAME = "SELECTED_BRANCHTAG_NAME";
        public static final String TYPE = "REPO_TYPE";
    }

    public interface Issue {
        public static final String NUMBER = "ISSUE_NUMBER";
        public static final String STATE = "state";
        public static final String STATE_OPEN = "open";
        public static final String STATE_CLOSED = "closed";
    }

    public interface Commit {
        public static final String COMMIT = "Commit.COMMIT";
        public static final String DIFF = "Commit.DIFF";
    }

    public interface PullRequest {
        public static final String NUMBER = "PullRequest.NUMBER";
        public static final String STATE = "PullRequest.STATE";
    }

    public interface Object {
        public static final String NAME = "Object.NAME";
        public static final String PATH = "Object.PATH";
        public static final String OBJECT_SHA = "Object.SHA";
        public static final String TREE_SHA = "Object.TREE_SHA";
        public static final String REF = "Object.REF";
    }
    
    public interface Gist {
        public static final String ID = "Gist.id";
        public static final String FILENAME = "Gist.filename";
    }
    
    public interface Blog {
        public static final String BLOG = "Blog";
        public static final String CONTENT = "Blog.content";
        public static final String TITLE = "Blog.title";
        public static final String LINK = "Blog.link";
    }
    
    public interface Bookmark {
        public static final int ADD = 100;
        public static final String NAME = "Bookmark.name";
        public static final String OBJECT_TYPE = "Bookmark.objectType";
        public static final String OBJECT_TYPE_USER = "User";
        public static final String OBJECT_TYPE_REPO = "Repo";
        public static final String OBJECT_TYPE_ISSUE = "Issue";
        public static final String HIDE_ADD = "Bookmark.hideAdd";
    }
    
    public interface Milestone {
        public static final String NUMBER = "Milestone.number";
        public static final String STATE = "Milestone.state";
    }
    
    public interface Comment {
        public static final String ID = "Comment.id";
        public static final String BODY = "Comment.body";
    }

    public interface Release {
        public static final String RELEASE = "Release.release";
        public static final String RELEASER = "Release.releaser";
    }

    public static final ArrayList<String> SKIP_PRETTIFY_EXT = new ArrayList<String>() {
        private static final long serialVersionUID = -9195888220037295330L;
    {
        add("txt");
        add("rdoc");
        add("texttile");
        add("org");
        add("creole");
        add("rst");
        add("asciidoc");
        add("pod");
        add("");
    }};
    public static final ArrayList<String> MARKDOWN_EXT = new ArrayList<String>() {
        private static final long serialVersionUID = -3693714294514389145L;
    {
        add("markdown");
        add("md");
        add("mdown");
        add("mkdn");
        add("mkd");
    }};
}