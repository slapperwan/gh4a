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

import java.util.Arrays;
import java.util.List;


/**
 * The Interface Constants.
 */
public interface Constants {
    public static final String LOG_TAG = "Gh4a";

    public interface Theme {
        public static final int DARK = 0;
        public static final int LIGHT = 1;
        public static final int LIGHTDARK = 2; /* backwards compat with old settings */
    }

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
        public static final String BASE = "BASE";
        public static final String HEAD = "HEAD";
        public static final String SELECTED_REF = "SELECTED_REF";
        public static final String TYPE = "REPO_TYPE";
    }

    public interface Issue {
        public static final String NUMBER = "ISSUE_NUMBER";
        public static final String STATE = "state";
        public static final String STATE_OPEN = "open";
        public static final String STATE_CLOSED = "closed";
    }

    public interface Commit {
        public static final String DIFF = "Commit.DIFF";
        public static final String COMMENTS = "Commit.COMMENTS";
    }

    public interface PullRequest {
        public static final String NUMBER = "PullRequest.NUMBER";
        public static final String STATE = "PullRequest.STATE";
    }

    public interface Object {
        public static final String PATH = "Object.PATH";
        public static final String OBJECT_SHA = "Object.SHA"; // SHA of the commit object
        public static final String REF = "Object.REF"; // SHA or branch/tag name of tree
    }

    public interface Gist {
        public static final String ID = "Gist.id";
        public static final String FILENAME = "Gist.filename";
    }

    public interface Blog {
        public static final String CONTENT = "Blog.content";
        public static final String TITLE = "Blog.title";
        public static final String LINK = "Blog.link";
    }

    public interface Comment {
        public static final String ID = "Comment.id";
        public static final String BODY = "Comment.body";
    }

    public interface Release {
        public static final String RELEASE = "Release.release";
        public static final String RELEASER = "Release.releaser";
    }

    public static final List<String> SKIP_PRETTIFY_EXT = Arrays.asList(
        "txt", "rdoc", "texttile", "org", "creole", "rst",
        "asciidoc", "pod", "");

    public static final List<String> MARKDOWN_EXT = Arrays.asList(
        "markdown", "md", "mdown", "mkdn", "mkd");
}