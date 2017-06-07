/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core.client;

/**
 * GitHub constants
 */
public interface IGitHubConstants {

	/** */
	String AUTH_TOKEN = "token"; //$NON-NLS-1$

	/** */
	String CHARSET_UTF8 = "UTF-8"; //$NON-NLS-1$
	/** */
	String CHARSET_ISO_8859_1 = "ISO-8859-1"; //$NON-NLS-1$
	/** */
	String CONTENT_TYPE_JSON = "application/json"; //$NON-NLS-1$

	/** */
	String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"; //$NON-NLS-1$
	/** */
	String DATE_FORMAT_V2_1 = "yyyy/MM/dd HH:mm:ss Z"; //$NON-NLS-1$
	/** */
	String DATE_FORMAT_V2_2 = "yyyy-MM-dd'T'HH:mm:ss"; //$NON-NLS-1$

	/** */
	String HEADER_LINK = "Link"; //$NON-NLS-1$
	/** */
	String HEADER_NEXT = "X-Next"; //$NON-NLS-1$
	/** */
	String HEADER_LAST = "X-Last"; //$NON-NLS-1$

	/** */
	String HOST_API = "api.github.com"; //$NON-NLS-1$
	/** */
	String HOST_DEFAULT = "github.com"; //$NON-NLS-1$
	/** */
	String HOST_GISTS = "gist.github.com"; //$NON-NLS-1$

	/** */
	String META_REL = "rel"; //$NON-NLS-1$
	/** */
	String META_LAST = "last"; //$NON-NLS-1$
	/** */
	String META_NEXT = "next"; //$NON-NLS-1$
	/** */
	String META_FIRST = "first"; //$NON-NLS-1$
	/** */
	String META_PREV = "prev"; //$NON-NLS-1$

	/** */
	String PARAM_LANGUAGE = "language"; //$NON-NLS-1$
	/** */
	String PARAM_PAGE = "page"; //$NON-NLS-1$
	/** */
	String PARAM_PER_PAGE = "per_page"; //$NON-NLS-1$
	/** */
	String PARAM_QUERY = "q"; //$NON-NLS-1$
	/** */
	String PARAM_START_PAGE = "start_page"; //$NON-NLS-1$

	/** */
	String PROTOCOL_HTTPS = "https"; //$NON-NLS-1$

	/** */
	String SCHEME_OAUTH2 = "oauth2"; //$NON-NLS-1$

	/** */
	String SEGMENT_ASSIGNEES = "/assignees"; //$NON-NLS-1$
	/** */
	String SEGMENT_AUTHORIZATIONS = "/authorizations"; //$NON-NLS-1$
	/** */
	String SEGMENT_BLOBS = "/blobs"; //$NON-NLS-1$
	/** */
	String SEGMENT_BRANCHES = "/branches"; //$NON-NLS-1$
	/** */
	String SEGMENT_COLLABORATORS = "/collaborators"; //$NON-NLS-1$
	/** */
	String SEGMENT_CODE = "/code"; //$NON-NLS-1$
	/** */
	String SEGMENT_COMMENTS = "/comments"; //$NON-NLS-1$
	/** */
	String SEGMENT_CONTENTS= "/contents"; //$NON-NLS-1$
	/** */
	String SEGMENT_CONTRIBUTORS = "/contributors"; //$NON-NLS-1$
	/** */
	String SEGMENT_COMMITS = "/commits"; //$NON-NLS-1$
	/** */
	String SEGMENT_COMPARE = "/compare"; //$NON-NLS-1$
	/** */
	String SEGMENT_CREATE = "/create"; //$NON-NLS-1$
	/** */
	String SEGMENT_DOWNLOADS = "/downloads"; //$NON-NLS-1$
	/** */
	String SEGMENT_EMAILS = "/emails"; //$NON-NLS-1$
	/** */
	String SEGMENT_EVENTS = "/events"; //$NON-NLS-1$
	/** */
	String SEGMENT_FILES = "/files"; //$NON-NLS-1$
	/** */
	String SEGMENT_FOLLOWERS = "/followers"; //$NON-NLS-1$
	/** */
	String SEGMENT_FOLLOWING = "/following"; //$NON-NLS-1$
	/** */
	String SEGMENT_FORK = "/fork"; //$NON-NLS-1$
	/** */
	String SEGMENT_FORKS = "/forks"; //$NON-NLS-1$
	/** */
	String SEGMENT_GISTS = "/gists"; //$NON-NLS-1$
	/** */
	String SEGMENT_GIT = "/git"; //$NON-NLS-1$
	/** */
	String SEGMENT_HOOKS = "/hooks"; //$NON-NLS-1$
	/** */
	String SEGMENT_ISSUES = "/issues"; //$NON-NLS-1$
	/** */
	String SEGMENT_KEYS = "/keys"; //$NON-NLS-1$
	/** */
	String SEGMENT_LABELS = "/labels"; //$NON-NLS-1$
	/** */
	String SEGMENT_LEGACY = "/legacy"; //$NON-NLS-1$
	/** */
	String SEGMENT_LANGUAGES = "/languages"; //$NON-NLS-1$
	/** */
	String SEGMENT_MARKDOWN = "/markdown"; //$NON-NLS-1$
	/** */
	String SEGMENT_MEMBERS = "/members"; //$NON-NLS-1$
	/** */
	String SEGMENT_MERGE = "/merge"; //$NON-NLS-1$
	/** */
	String SEGMENT_MILESTONES = "/milestones"; //$NON-NLS-1$
	/** */
	String SEGMENT_NETWORKS = "/networks"; //$NON-NLS-1$
	/** */
	String SEGMENT_NOTIFICATIONS = "/notifications"; //$NON-NLS-1$
	/** */
	String SEGMENT_ORGANIZATIONS = "/organizations"; //$NON-NLS-1$
	/** */
	String SEGMENT_MEMBERSHIPS = "/memberships"; //$NON-NLS-1$
	/** */
	String SEGMENT_ORGS = "/orgs"; //$NON-NLS-1$
	/** */
	String SEGMENT_PUBLIC = "/public"; //$NON-NLS-1$
	/** */
	String SEGMENT_PUBLIC_MEMBERS = "/public_members"; //$NON-NLS-1$
	/** */
	String SEGMENT_PULLS = "/pulls"; //$NON-NLS-1$
	/** */
	String SEGMENT_REACTIONS = "/reactions"; //$NON-NLS-1$
	/** */
	String SEGMENT_README = "/readme"; //$NON-NLS-1$
	/** */
	String SEGMENT_RECEIVED_EVENTS = "/received_events"; //$NON-NLS-1$
	/** */
	String SEGMENT_REFS = "/refs"; //$NON-NLS-1$
	/** */
	String SEGMENT_RELEASES = "/releases"; //$NON-NLS-1$
	/** */
	String SEGMENT_REPOS = "/repos"; //$NON-NLS-1$
	/** */
	String SEGMENT_REPOSITORIES = "/repositories"; //$NON-NLS-1$
	/** */
	String SEGMENT_REVIEWS = "/reviews"; //$NON-NLS-1$
	/** */
	String SEGMENT_SEARCH = "/search"; //$NON-NLS-1$
	/** */
	String SEGMENT_SHOW = "/show"; //$NON-NLS-1$
	/** */
	String SEGMENT_SUBSCRIBERS = "/subscribers"; //$NON-NLS-1$
	/** */
	String SEGMENT_SUBSCRIPTION = "/subscription"; //$NON-NLS-1$
	/** */
	String SEGMENT_SUBSCRIPTIONS = "/subscriptions"; //$NON-NLS-1$
	/** */
	String SEGMENT_STAR = "/star"; //$NON-NLS-1$
	/** @since 4.2 */
	String SEGMENT_STARGAZERS = "/stargazers"; //$NON-NLS-1$
	/** */
	String SEGMENT_STARRED = "/starred"; //$NON-NLS-1$
	/** */
	String SEGMENT_STATUSES = "/statuses"; //$NON-NLS-1$
	/** */
	String SEGMENT_TAGS = "/tags"; //$NON-NLS-1$
	/** */
	String SEGMENT_TEAMS = "/teams"; //$NON-NLS-1$
	/** */
	String SEGMENT_TEST = "/test"; //$NON-NLS-1$
	/** */
	String SEGMENT_THREADS = "/threads"; //$NON-NLS-1$
	/** */
	String SEGMENT_TREES = "/trees"; //$NON-NLS-1$
	/** */
	String SEGMENT_USER = "/user"; //$NON-NLS-1$
	/** */
	String SEGMENT_USERS = "/users"; //$NON-NLS-1$
	/** */
	String SEGMENT_V3_API = "/api/v3"; //$NON-NLS-1$

	/** */
	String SUBDOMAIN_API = "api"; //$NON-NLS-1$

	/** */
	String SUFFIX_GIT = ".git"; //$NON-NLS-1$

	/** */
	String URL_API = PROTOCOL_HTTPS + "://" + HOST_API; //$NON-NLS-1$
}
