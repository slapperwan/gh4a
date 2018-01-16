package com.gh4a.resolver;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.gh4a.BuildConfig;
import com.gh4a.activities.BlogListActivity;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.CompareActivity;
import com.gh4a.activities.DownloadsActivity;
import com.gh4a.activities.GistActivity;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.IssueEditActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.OrganizationMemberListActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.TrendingActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.IntentUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class LinkParserTest {
    private FragmentActivity mActivity;

    @Before
    public void createActivity() {
        mActivity = Robolectric.buildActivity(BrowseFilter.class).get();
    }

    @Test
    public void gistLink__opensBrowser() {
        assertRedirectsToBrowser(parseLink("https://gist.github.com"));
    }

    @Test
    public void linkToGist__opensGistActivity() {
        LinkParser.ParseResult result = parseLink("https://gist.github.com/gistId");
        assertRedirectsTo(result, GistActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("Gist id is incorrect", extras.getString("id"), is("gistId"));
    }

    @Test
    public void linkToGist_withUserInPath__opensGistActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://gist.github.com/user/gistId");
        assertRedirectsTo(result, GistActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("Gist id is incorrect", extras.getString("id"), is("gistId"));
    }

    @Test
    public void githubLink__opensMainActivity() {
        assertRedirectsToBrowser(parseLink("https://github.com"));
    }

    @Test
    public void reservedPaths__openBrowser() {
        String[] reservedPaths = {
                "apps", "integrations", "login", "logout", "marketplace", "sessions", "settings",
                "updates", "support", "contact", "about", "personal", "open-source", "business",
                "site", "security", "features"
        };
        for (String reservedPath : reservedPaths) {
            assertRedirectsToBrowser(parseLink("https://github.com/" + reservedPath));
        }
    }

    @Test
    public void exploreLink__opensTrendingActivity() throws Exception {
        assertRedirectsTo(parseLink("https://github.com/explore"), TrendingActivity.class);
    }

    @Test
    public void blogLink__opensBlogListActivity() throws Exception {
        assertRedirectsTo(parseLink("https://github.com/blog"), BlogListActivity.class);
    }

    @Test
    public void blogLink_withBlogInPath__opensBrowser() throws Exception {
        assertRedirectsToBrowser(parseLink("https://github.com/blog/blog-title"));
    }

    @Test
    public void organizationLink__opensUserActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/orgs/android");
        assertRedirectsTo(result, UserActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("Organization name is incorrect", extras.getString("login"), is("android"));
    }

    @Test
    public void organizationLink_withoutName__opensBrowser() throws Exception {
        assertRedirectsToBrowser(parseLink("https://github.com/orgs"));
    }

    @Test
    public void organisationLink_leadingToMembers__opensOrganizationMemberListActivity()
            throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/orgs/android/people");
        assertRedirectsTo(result, OrganizationMemberListActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("Organization name is incorrect", extras.getString("login"), is("android"));
    }

    @Test
    public void userLink__opensUserActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan");
        assertRedirectsTo(result, UserActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("login"), is("slapperwan"));
    }

    @Test
    public void userLink_withRepositoriesTab__loadsUserRepos() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan?tab=repositories");
        UserReposLoadTask loadTask = assertThatLoadTaskIs(result.loadTask, UserReposLoadTask.class);
        assertThat("Loading starred repos is set to true", loadTask.mShowStars, is(false));
        assertThat("User name is incorrect", loadTask.mUserLogin, is("slapperwan"));
    }

    @Test
    public void userLink_withStarsTab__loadsUserStarredRepos() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan?tab=stars");
        UserReposLoadTask loadTask = assertThatLoadTaskIs(result.loadTask, UserReposLoadTask.class);
        assertThat("Loading starred repos is set to false", loadTask.mShowStars, is(true));
        assertThat("User name is incorrect", loadTask.mUserLogin, is("slapperwan"));
    }

    @Test
    public void userLink_withFollowersTab__loadsUserFollowers() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan?tab=followers");
        UserFollowersLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, UserFollowersLoadTask.class);
        assertThat("Loading followers is set to false", loadTask.mShowFollowers, is(true));
        assertThat("User name is incorrect", loadTask.mUserLogin, is("slapperwan"));
    }

    @Test
    public void userLink_withFollowingTab__loadsUserFollows() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan?tab=following");
        UserFollowersLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, UserFollowersLoadTask.class);
        assertThat("Loading followers is set to true", loadTask.mShowFollowers, is(false));
        assertThat("User name is incorrect", loadTask.mUserLogin, is("slapperwan"));
    }

    @Test
    public void userLink_withUnknownTab__opensUserActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan?tab=unknown");
        assertRedirectsTo(result, UserActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("login"), is("slapperwan"));
    }

    @Test
    public void repositoryLink__opensRepositoryActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a");
        assertRedirectsTo(result, RepositoryActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Ref is set", extras.getString("ref"), is(nullValue()));
        assertThat("Initial path is set", extras.getString("initial_path"), is(nullValue()));
        assertThat("Initial page did not lead to overview", extras.getInt("initial_page"),
                is(RepositoryActivity.PAGE_REPO_OVERVIEW));
    }

    @Test
    public void downloadsLink__opensDownloadsActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/downloads");
        assertRedirectsTo(result, DownloadsActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
    }

    @Test
    public void releasesLink__opensReleaseListActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/releases");
        assertRedirectsTo(result, ReleaseListActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
    }

    @Test
    public void releaseLink_withoutTagId__opensReleaseListActivity() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/releases/tag");
        assertRedirectsTo(result, ReleaseListActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
    }

    @Test
    public void releaseLink_withTagId__loadsRelease() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/releases/tag/tagName");
        ReleaseLoadTask loadTask = assertThatLoadTaskIs(result.loadTask, ReleaseLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Tag name is incorrect", loadTask.mTagName, is("tagName"));
    }

    @Test
    public void issuesLink__opensIssueListActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/issues");
        assertRedirectsTo(result, IssueListActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Issue type is pull request", extras.getBoolean("is_pull_request"), is(false));
    }

    @Test
    public void newIssueLink__opensIssueEditActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/issues/new");
        assertRedirectsTo(result, IssueEditActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Issue to edit is set", extras.getSerializable("issue"), is(nullValue()));
    }

    @Test
    public void issueLink__opensIssueActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/issues/42");
        assertRedirectsTo(result, IssueActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Issue number is incorrect", extras.getInt("number"), is(42));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void issueLink_withIncorrectNumber__opensBrowser() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/issues/34no");
        assertRedirectsToBrowser(result);
    }

    @Test
    public void issueLink_withCommentMarker__opensIssueActivity_andHasCommentMarker()
            throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/issues/42#issuecomment-1234");
        assertRedirectsTo(result, IssueActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Issue number is incorrect", extras.getInt("number"), is(42));
        IntentUtils.InitialCommentMarker commentMarker = extras.getParcelable("initial_comment");
        assertThat("Comment marker is missing", commentMarker, is(notNullValue()));
        assertThat("Comment id is incorrect", commentMarker.commentId, is(1234L));
    }

    @Test
    public void issueLink_withIncorrectCommentMarker_opensIssueActivity() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/issues/42#issuecomment-A3");
        assertRedirectsTo(result, IssueActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Issue number is incorrect", extras.getInt("number"), is(42));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void pullRequestsLink__opensIssueListActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/pulls");
        assertRedirectsTo(result, IssueListActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Issue type is not pull request", extras.getBoolean("is_pull_request"),
                is(true));
    }

    @Test
    public void wikiLink__opensWikiListActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/wiki");
        assertRedirectsTo(result, WikiListActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Initial page is set", extras.getString("initial_page"), is(nullValue()));
    }

    @Test
    public void pullRequestLink_withoutId__opensBrowser() throws Exception {
        assertRedirectsToBrowser(parseLink("https://github.com/slapperwan/gh4a/pull"));
    }

    @Test
    public void pullRequestLink_withInvalidId__opensBrowser() throws Exception {
        assertRedirectsToBrowser(parseLink("https://github.com/slapperwan/gh4a/pull/-1"));
        assertRedirectsToBrowser(parseLink("https://github.com/slapperwan/gh4a/pull/fwbi"));
        assertRedirectsToBrowser(parseLink("https://github.com/slapperwan/gh4a/pull/0"));
    }

    @Test
    public void pullRequestLink__opensPullRequestActivity() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/pull/12");
        assertRedirectsTo(result, PullRequestActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Pull request number is incorrect", extras.getInt("number"), is(12));
        assertThat("Initial page is set", extras.getInt("initial_page"), is(-1));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void pullRequestLink_withCommentMarker__opensPullRequestActivity_andHasCommentMarker()
            throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/14#issuecomment-7546");
        assertRedirectsTo(result, PullRequestActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Pull request number is incorrect", extras.getInt("number"), is(14));
        assertThat("Initial page is set", extras.getInt("initial_page"), is(-1));
        IntentUtils.InitialCommentMarker commentMarker = extras.getParcelable("initial_comment");
        assertThat("Comment marker is missing", commentMarker, is(notNullValue()));
        assertThat("Comment id is incorrect", commentMarker.commentId, is(7546L));
    }

    @Test
    public void pullRequestLink_withCommitsPage__opensPullRequestCommits() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/23/commits");
        assertRedirectsTo(result, PullRequestActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Pull request number is incorrect", extras.getInt("number"), is(23));
        assertThat("Initial page is incorrect", extras.getInt("initial_page"),
                is(PullRequestActivity.PAGE_COMMITS));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void pullRequestLink_withFilesPage__opensPullRequestFiles() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/23/files");
        assertRedirectsTo(result, PullRequestActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Pull request number is incorrect", extras.getInt("number"), is(23));
        assertThat("Initial page is incorrect", extras.getInt("initial_page"),
                is(PullRequestActivity.PAGE_FILES));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void pullRequestLink_withUnknownPage__opensPullRequest() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/23/unknown");
        assertRedirectsTo(result, PullRequestActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Pull request number is incorrect", extras.getInt("number"), is(23));
        assertThat("Initial page is set", extras.getInt("initial_page"), is(-1));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void pullRequestLink_withDiffMarker__loadsDiff() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665/files" +
                        "#diff-38f43208e0c158ca7b78e175b8846bc6");
        PullRequestDiffLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, PullRequestDiffLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        DiffHighlightId diffId = loadTask.mDiffId;
        assertThat("Diff id is missing", diffId, is(notNullValue()));
        assertThat("File hash is incorrect", diffId.fileHash,
                is("38f43208e0c158ca7b78e175b8846bc6"));
        assertThat("Start line is set", diffId.startLine, is(-1));
        assertThat("End line is set", diffId.endLine, is(-1));
        assertThat("Is right line", diffId.right, is(false));
    }

    @Test
    public void pullRequestLink_withDiffMarker_andLeftNumber__loadsDiff() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665/files" +
                        "#diff-38f43208e0c158ca7b78e175b8846bc6L24");
        PullRequestDiffLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, PullRequestDiffLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        DiffHighlightId diffId = loadTask.mDiffId;
        assertThat("Diff id is missing", diffId, is(notNullValue()));
        assertThat("File hash is incorrect", diffId.fileHash,
                is("38f43208e0c158ca7b78e175b8846bc6"));
        assertThat("Start line is incorrect", diffId.startLine, is(24));
        assertThat("End line is incorrect", diffId.endLine, is(24));
        assertThat("Is right line", diffId.right, is(false));
    }

    @Test
    public void pullRequestLink_withDiffMarker_andLineRange__loadsDiff() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665/files" +
                        "#diff-38f43208e0c158ca7b78e175b8846bc6L24-L26");
        PullRequestDiffLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, PullRequestDiffLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        DiffHighlightId diffId = loadTask.mDiffId;
        assertThat("Diff id is missing", diffId, is(notNullValue()));
        assertThat("File hash is incorrect", diffId.fileHash,
                is("38f43208e0c158ca7b78e175b8846bc6"));
        assertThat("Start line is incorrect", diffId.startLine, is(24));
        assertThat("End line is incorrect", diffId.endLine, is(26));
        assertThat("Is right line", diffId.right, is(false));
    }

    @Test
    public void pullRequestLink_withDiffMarker_andInvalidNumber__opensPullRequest() throws
            Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665/files" +
                        "#diff-38f43208e0c158ca7b78e175b8846bc6LA3");
        assertRedirectsTo(result, PullRequestActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Pull request number is incorrect", extras.getInt("number"), is(665));
        assertThat("Initial page is incorrect", extras.getInt("initial_page"),
                is(PullRequestActivity.PAGE_FILES));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void pullRequestLink_withDiffMarker_andRightNumber__loadsDiff() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665/files" +
                        "#diff-38f43208e0c158ca7b78e175b8846bc6R24");
        PullRequestDiffLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, PullRequestDiffLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        DiffHighlightId diffId = loadTask.mDiffId;
        assertThat("Diff id is missing", diffId, is(notNullValue()));
        assertThat("File hash is incorrect", diffId.fileHash,
                is("38f43208e0c158ca7b78e175b8846bc6"));
        assertThat("Start line is incorrect", diffId.startLine, is(24));
        assertThat("End line is incorrect", diffId.endLine, is(24));
        assertThat("Is not right line", diffId.right, is(true));
    }

    @Test
    public void pullRequestLink_withDiffMarker_andIncorrectHash__loadsDiff() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665/files" +
                        "#diff-38f43208e0c158ca7b78e1");
        assertRedirectsTo(result, PullRequestActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Pull request number is incorrect", extras.getInt("number"), is(665));
        assertThat("Initial page is incorrect", extras.getInt("initial_page"),
                is(PullRequestActivity.PAGE_FILES));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void pullRequestReviewLink__loadsReview() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665#pullrequestreview-59822171");
        PullRequestReviewLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, PullRequestReviewLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Pull request number is incorrect", loadTask.mPullRequestNumber, is(665));
        assertThat("Comment marker is missing", loadTask.mMarker, is(notNullValue()));
        assertThat("Comment id is incorrect", loadTask.mMarker.commentId, is(59822171L));
    }

    @Test
    public void pullRequestReviewCommentLink__loadsReviewComment() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665#discussion_r136306029");
        PullRequestReviewCommentLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, PullRequestReviewCommentLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Pull request number is incorrect", loadTask.mPullRequestNumber, is(665));
        assertThat("Comment marker is missing", loadTask.mMarker, is(notNullValue()));
        assertThat("Comment id is incorrect", loadTask.mMarker.commentId, is(136306029L));
    }

    @Test
    public void pullRequestReviewDiffLink__loadsReviewDiff() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/pull/665" +
                "#discussion-diff-136304421R590");
        PullRequestReviewDiffLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, PullRequestReviewDiffLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Pull request number is incorrect", loadTask.mPullRequestNumber, is(665));
        assertThat("Diff id is missing", loadTask.mDiffId, is(notNullValue()));
        assertThat("Comment id is incorrect", loadTask.mDiffId.fileHash, is("136304421"));
        assertThat("Start line is incorrect", loadTask.mDiffId.startLine, is(590));
        assertThat("End line is incorrect", loadTask.mDiffId.endLine, is(590));
        assertThat("Is left line", loadTask.mDiffId.right, is(true));
    }

    @Test
    public void pullRequestDiffCommentLink__loadsReviewDiff() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/pull/665/files#r136306029");
        PullRequestDiffCommentLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, PullRequestDiffCommentLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Pull request number is incorrect", loadTask.mPullRequestNumber, is(665));
        assertThat("Page is incorrect", loadTask.mPage, is(PullRequestActivity.PAGE_FILES));
        assertThat("Comment marker is missing", loadTask.mMarker, is(notNullValue()));
        assertThat("Comment id is incorrect", loadTask.mMarker.commentId, is(136306029L));
    }

    @Test
    public void commitLink__opensCommitActivity() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/commit/commitSha");
        assertRedirectsTo(result, CommitActivity.class);
        Bundle extras = result.intent.getExtras();
        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Pull request number is set", extras.getInt("pr"), is(-1));
        assertThat("Commit sha is incorrect", extras.getString("sha"), is("commitSha"));
        assertThat("Comment marker is set", extras.getParcelable("initial_comment"),
                is(nullValue()));
    }

    @Test
    public void commitLink_withoutCommitSha__opensBrowser() throws Exception {
        assertRedirectsToBrowser(parseLink("https://github.com/slapperwan/gh4a/commit"));
    }

    @Test
    public void commitLink_withDiffMarker__loadsCommitDiff() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/commit/" +
                        "57a054f85ade77ebb80eecd671aace770b312bf5" +
                        "#diff-1d86a926c5d4666eb76b2326aa28ee89L160");
        CommitDiffLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, CommitDiffLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Commit sha is incorrect", loadTask.mSha,
                is("57a054f85ade77ebb80eecd671aace770b312bf5"));
        DiffHighlightId diffId = loadTask.mDiffId;
        assertThat("Diff id is missing", diffId, is(notNullValue()));
        assertThat("File has is incorrect", diffId.fileHash,
                is("1d86a926c5d4666eb76b2326aa28ee89"));
        assertThat("Start line is incorrect", diffId.startLine, is(160));
        assertThat("End line is incorrect", diffId.endLine, is(160));
        assertThat("Is right line", diffId.right, is(false));
    }

    @Test
    public void commitLink_withCommentMarker__opensCommitActivity() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/commit/commitSha#commitcomment-12");
        CommitCommentLoadTask loadTask =
                assertThatLoadTaskIs(result.loadTask, CommitCommentLoadTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Commit sha is incorrect", loadTask.mCommitSha, is("commitSha"));
        IntentUtils.InitialCommentMarker commentMarker = loadTask.mMarker;
        assertThat("Comment marker is missing", commentMarker, is(notNullValue()));
        assertThat("Comment id is incorrect", commentMarker.commentId, is(12L));
    }

    @Test
    public void commitsLink__handlesRefAndPath_andRedirectsToCommitsPage() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/commits");
        RefPathDisambiguationTask loadTask =
                assertThatLoadTaskIs(result.loadTask, RefPathDisambiguationTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Ref and path is not empty", loadTask.mRefAndPath, isEmptyString());
        assertThat("Page does not lead to commits", loadTask.mInitialPage,
                is(RepositoryActivity.PAGE_COMMITS));
        assertThat("Fragment is set", loadTask.mFragment, is(nullValue()));
        assertThat("Goes to file viewer", loadTask.mGoToFileViewer, is(false));
    }

    @Test
    public void commitsLink_withBranch__handlesRefAndPath_andRedirectsToCommitsPage()
            throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/commits/master");
        RefPathDisambiguationTask loadTask =
                assertThatLoadTaskIs(result.loadTask, RefPathDisambiguationTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Ref and path is incorrect", loadTask.mRefAndPath, is("master"));
        assertThat("Page does not lead to commits", loadTask.mInitialPage,
                is(RepositoryActivity.PAGE_COMMITS));
        assertThat("Fragment is set", loadTask.mFragment, is(nullValue()));
        assertThat("Goes to file viewer", loadTask.mGoToFileViewer, is(false));
    }

    @Test
    public void commitsLink_withRefsHeads__handlesRefAndPath_andRedirectsToCommitsPage()
            throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/commits/refs/heads/master");
        RefPathDisambiguationTask loadTask =
                assertThatLoadTaskIs(result.loadTask, RefPathDisambiguationTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Ref and path is incorrect", loadTask.mRefAndPath, is("master"));
        assertThat("Page does not lead to commits", loadTask.mInitialPage,
                is(RepositoryActivity.PAGE_COMMITS));
        assertThat("Fragment is set", loadTask.mFragment, is(nullValue()));
        assertThat("Goes to file viewer", loadTask.mGoToFileViewer, is(false));
    }

    @Test
    public void treeLink__handlesRefAndPath_andRedirectsToFilesPage() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/tree");
        RefPathDisambiguationTask loadTask =
                assertThatLoadTaskIs(result.loadTask, RefPathDisambiguationTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Ref and path is not empty", loadTask.mRefAndPath, isEmptyString());
        assertThat("Page does not lead to files", loadTask.mInitialPage,
                is(RepositoryActivity.PAGE_FILES));
        assertThat("Fragment is set", loadTask.mFragment, is(nullValue()));
        assertThat("Goes to file viewer", loadTask.mGoToFileViewer, is(false));
    }

    @Test
    public void treeLink_withBranch__handlesRefAndPath_andRedirectsToFilesPage() throws Exception {
        LinkParser.ParseResult result = parseLink("https://github.com/slapperwan/gh4a/tree/master");
        RefPathDisambiguationTask loadTask =
                assertThatLoadTaskIs(result.loadTask, RefPathDisambiguationTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Ref and path is incorrect", loadTask.mRefAndPath, is("master"));
        assertThat("Page does not lead to files", loadTask.mInitialPage,
                is(RepositoryActivity.PAGE_FILES));
        assertThat("Fragment is set", loadTask.mFragment, is(nullValue()));
        assertThat("Goes to file viewer", loadTask.mGoToFileViewer, is(false));
    }

    @Test
    public void treeLink_withRefsHeads__handlesRefAndPath_andRedirectsToFilesPage()
            throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/tree/refs/heads/master");
        RefPathDisambiguationTask loadTask =
                assertThatLoadTaskIs(result.loadTask, RefPathDisambiguationTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Ref and path is incorrect", loadTask.mRefAndPath, is("master"));
        assertThat("Page does not lead to files", loadTask.mInitialPage,
                is(RepositoryActivity.PAGE_FILES));
        assertThat("Fragment is set", loadTask.mFragment, is(nullValue()));
        assertThat("Goes to file viewer", loadTask.mGoToFileViewer, is(false));
    }

    @Test
    public void blobLink__handlesRefAndPath_andRedirectsToFileViewer() throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/blob/master/README.md");
        RefPathDisambiguationTask loadTask =
                assertThatLoadTaskIs(result.loadTask, RefPathDisambiguationTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Ref and path is incorrect", loadTask.mRefAndPath, is("master/README.md"));
        assertThat("Page is set", loadTask.mInitialPage, is(-1));
        assertThat("Fragment is set", loadTask.mFragment, is(nullValue()));
        assertThat("Does not go to file viewer", loadTask.mGoToFileViewer, is(true));
    }

    @Test
    public void blobLink_withLineMarker__handlesRefAndPath_andRedirectsToFileViewer()
            throws Exception {
        LinkParser.ParseResult result =
                parseLink("https://github.com/slapperwan/gh4a/blob/master/build.gradle#L10");
        RefPathDisambiguationTask loadTask =
                assertThatLoadTaskIs(result.loadTask, RefPathDisambiguationTask.class);
        assertThat("User name is incorrect", loadTask.mRepoOwner, is("slapperwan"));
        assertThat("Repo name is incorrect", loadTask.mRepoName, is("gh4a"));
        assertThat("Ref and path is incorrect", loadTask.mRefAndPath, is("master/build.gradle"));
        assertThat("Page is set", loadTask.mInitialPage, is(-1));
        assertThat("Fragment is incorrect", loadTask.mFragment, is("L10"));
        assertThat("Does not go to file viewer", loadTask.mGoToFileViewer, is(true));
    }

    @Test
    public void blobLink_withoutBranchAndPath__opensBrowser() throws Exception {
        assertRedirectsToBrowser(parseLink("https://github.com/slapperwan/gh4a/blob"));
    }

    @Test
    public void compareLink__redirectsToCompareView() throws Exception {

    }

    @Test
    public void compareLink_withoutRefs__opensBrowser() throws Exception {
        LinkParser.ParseResult result =
                (parseLink("https://github.com/slapperwan/gh4a/compare/v4.2.0...v4.2.1"));
        assertRedirectsTo(result, CompareActivity.class);
        Bundle extras = result.intent.getExtras();

        assertThat("Extras are missing", extras, is(notNullValue()));
        assertThat("User name is incorrect", extras.getString("owner"), is("slapperwan"));
        assertThat("Repo name is incorrect", extras.getString("repo"), is("gh4a"));
        assertThat("Base ref is incorrect", extras.getString("base"), is("v4.2.0"));
        assertThat("Head ref is incorrect", extras.getString("head"), is("v4.2.1"));
    }

    @Test
    public void compareLink_withIncompleteRefs__opensBrowser() throws Exception {
        assertRedirectsToBrowser(parseLink("https://github.com/slapperwan/gh4a/compare/v4.2.0..."));
    }

    @Test
    public void unknownRepositoryLink__opensBrowser() throws Exception {
        assertRedirectsToBrowser(parseLink("https://github.com/slapperwan/gh4a/unknown"));
    }

    private LinkParser.ParseResult parseLink(String uriString) {
        return LinkParser.parseUri(mActivity, Uri.parse(uriString), null);
    }

    private static void assertRedirectsTo(LinkParser.ParseResult result, Class<?> cls) {
        assertThat("Parse result must not be null to redirect to activity", result,
                is(notNullValue()));
        assertThat("Load task must be null to redirect to activity", result.loadTask,
                is(nullValue()));
        assertThat("Target intent is missing", result.intent, is(notNullValue()));
        assertThat("Target intent is incorrect", result.intent.getComponent().getClassName(),
                is(cls.getName()));
    }

    private static void assertRedirectsToBrowser(LinkParser.ParseResult result) {
        assertThat("Parse result does not redirect to browser (must be null)", result,
                is(nullValue()));
    }

    private static <T extends UrlLoadTask> T assertThatLoadTaskIs(UrlLoadTask loadTask,
            Class<T> cls) {
        assertThat("Load task is of incorrect type", loadTask, is(instanceOf(cls)));
        //noinspection unchecked
        return (T) loadTask;
    }
}