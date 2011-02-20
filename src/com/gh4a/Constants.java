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

/**
 * The Interface Constants.
 */
public interface Constants {

    /** The Constant PREF_NAME. */
    public static final String PREF_NAME = "Gh4a-pref";

    /** The Constant LOG_TAG. */
    public static final String LOG_TAG = "Gh4a";

    /** The Constant DATA_BUNDLE. */
    public static final String DATA_BUNDLE = "DATA_BUNDLE";

    /** The Constant FIND_FOLLOWER. */
    public static final String FIND_FOLLOWER = "FIND_FOLLOWER";

    /** The Constant ACTIONBAR_TITLE. */
    public static final String ACTIONBAR_TITLE = "ACTIONBAR_TITLE";

    /** The Constant SUBTITLE. */
    public static final String SUBTITLE = "SUBTITLE";

    /** The Constant VIEW_ID. */
    public static final String VIEW_ID = "VIEW_ID";

    /** The Constant GRAVATAR_ID. */
    public static final String GRAVATAR_ID = "GRAVATAR_ID";

    /**
     * User properties.
     */
    public interface User {

        /** The Constant USER_USERNAME. */
        public static final String USER_USERNAME = "USER_USERNAME";

        /** The Constant USER_PASSWORD. */
        public static final String USER_PASSWORD = "USER_PASSWORD";

        /** The Constant USER_LOGIN. */
        public static final String USER_LOGIN = "USER_LOGIN";

        /** The Constant USER_NAME. */
        public static final String USER_NAME = "USER_NAME";

        /** The Constant USER_TYPE_USER. */
        public static final String USER_TYPE_USER = "User";

        /** The Constant USER_TYPE_ORG. */
        public static final String USER_TYPE_ORG = "Organization";
    }

    /**
     * Repository properties *.
     */
    public interface Repository {

        /** The Constant REPO_NAME. */
        public static final String REPO_NAME = "REPO_NAME";

        /** The Constant REPO_OWNER. */
        public static final String REPO_OWNER = "REPO_OWNER";

        /** The Constant REPO_DESC. */
        public static final String REPO_DESC = "REPO_DESC";

        /** The Constant REPO_URL. */
        public static final String REPO_URL = "REPO_URL";

        /** The Constant REPO_WATCHERS. */
        public static final String REPO_WATCHERS = "REPO_WATCHERS";

        /** The Constant REPO_FORKS. */
        public static final String REPO_FORKS = "REPO_FORKS";

        /** The Constant REPO_HOMEPAGE. */
        public static final String REPO_HOMEPAGE = "REPO_HOMEPAGE";

        /** The Constant REPO_CREATED. */
        public static final String REPO_CREATED = "REPO_CREATED";

        /** The Constant REPO_LANGUANGE. */
        public static final String REPO_LANGUANGE = "REPO_LANGUAGE";

        /** The Constant REPO_OPEN_ISSUES. */
        public static final String REPO_OPEN_ISSUES = "REPO_OPEN_ISSUES";

        /** The Constant REPO_SIZE. */
        public static final String REPO_SIZE = "REPO_SIZE";

        /** The Constant REPO_PUSHED. */
        public static final String REPO_PUSHED = "REPO_PUSHED";

        /** The Constant REPO_IS_FORKED. */
        public static final String REPO_IS_FORKED = "REPO_IS_FORKED";

        /** The Constant REPO_PARENT. */
        public static final String REPO_PARENT = "REPO_PARENT";

        /** The Constant REPO_BRANCH. */
        public static final String REPO_BRANCH = "REPO_BRANCH";

        /** The Constant REPO_TAG. */
        public static final String REPO_TAG = "REPO_TAG";

        /** The Constant REPO_SOURCE. */
        public static final String REPO_SOURCE = "REPO_SOURCE";

        /** The Constant REPO_HAS_ISSUES. */
        public static final String REPO_HAS_ISSUES = "REPO_HAS_ISSUES";
    }

    /**
     * Issue properties.
     */
    public interface Issue {

        /** The Constant ISSUES. */
        public static final String ISSUES = "ISSUES";

        /** The Constant ISSUE_STATE. */
        public static final String ISSUE_STATE = "ISSUE_STATE";

        /** The Constant ISSUE_STATE_OPEN. */
        public static final String ISSUE_STATE_OPEN = "OPEN";

        /** The Constant ISSUE_STATE_CLOSED. */
        public static final String ISSUE_STATE_CLOSED = "CLOSED";

        /** The Constant ISSUE_TITLE. */
        public static final String ISSUE_TITLE = "ISSUE_TITLE";

        /** The Constant ISSUE_BODY. */
        public static final String ISSUE_BODY = "ISSUE_BODY";

        /** The Constant ISSUE_COMMENTS. */
        public static final String ISSUE_COMMENTS = "ISSUE_COMMENTS";

        /** The Constant ISSUE_USER. */
        public static final String ISSUE_USER = "ISSUE_USER";

        /** The Constant ISSUE_NUMBER. */
        public static final String ISSUE_NUMBER = "ISSUE_NUMBER";

        /** The Constant ISSUE_CREATED_AT. */
        public static final String ISSUE_CREATED_AT = "ISSUE_CREATED_AT";

        /** The Constant ISSUE_CREATED_BY. */
        public static final String ISSUE_CREATED_BY = "ISSUE_CREATED_BY";
    }

    /**
     * Commt *.
     */
    public interface Commit {

        /** The Constant COMMITS. */
        public static final String COMMITS = "COMMITS";

        /** The Constant COMMIT. */
        public static final String COMMIT = "COMMIT";
        
        /** The Constant DIFF. */
        public static final String DIFF = "DIFF";
    }

    /**
     * The Interface PullRequest.
     */
    public interface PullRequest {

        /** The Constant PULL_REQUESTS. */
        public static final String PULL_REQUESTS = "PULL_REQUESTS";

        /** The Constant PULL_REQUEST_NUMBER. */
        public static final String PULL_REQUEST_NUMBER = "PULL_REQUEST_NUMBER";
    }

    /**
     * The Interface Object.
     */
    public interface Object {

        /** The Constant NAME. */
        public static final String NAME = "Object.NAME";

        /** The Constant MIME_TYPE. */
        public static final String MIME_TYPE = "Object.MIME_TYPE";

        /** The Constant PATH. */
        public static final String PATH = "Object.PATH";

        /** The Constant OBJECT_SHA. */
        public static final String OBJECT_SHA = "Object.SHA";

        /** The Constant TREE_SHA. */
        public static final String TREE_SHA = "Object.TREE_SHA";

        /** The Constant TREE. */
        public static final String TREE = "Object.TREE";

        /** The Constant BRANCHES. */
        public static final String BRANCHES = "Object.BRANCHES";

        /** The Constant TAGS. */
        public static final String TAGS = "Object.TAGS";
    }
}
