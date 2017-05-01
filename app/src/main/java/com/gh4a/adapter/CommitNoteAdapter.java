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
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.widget.ReactionBar;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.Reactions;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class CommitNoteAdapter extends CommentAdapterBase<CommitComment> {
    public CommitNoteAdapter(Context context, String repoOwner, String repoName,
            OnCommentAction actionCallback) {
        super(context, repoOwner, repoName, actionCallback);
    }

    @Override
    protected User getUser(CommitComment item) {
        return item.getUser();
    }

    @Override
    protected Date getCreatedAt(CommitComment item) {
        return item.getCreatedAt();
    }

    @Override
    protected Date getUpdatedAt(CommitComment item) {
        return item.getUpdatedAt();
    }

    @Override
    protected String getUrl(CommitComment item) {
        return item.getHtmlUrl();
    }

    @Override
    protected String getShareSubject(CommitComment item) {
        return mContext.getString(R.string.share_commit_comment_subject,
                item.getId(), mRepoOwner + "/" + mRepoName);
    }

    @Override
    protected void bindBodyView(CommitComment item, StyleableTextView view,
            HttpImageGetter imageGetter) {
        imageGetter.bind(view, item.getBodyHtml(), item.getId());
    }

    @Override
    protected void bindExtraView(CommitComment item, StyleableTextView view) {
        String login = ApiHelpers.getUserLogin(mContext, item.getUser());
        SpannableString userName = new SpannableString(login);
        userName.setSpan(new StyleSpan(Typeface.BOLD), 0, userName.length(), 0);
        view.setText(userName);
    }

    @Override
    protected void bindFileView(CommitComment item, StyleableTextView view) {
        view.setVisibility(View.GONE);
    }

    @Override
    protected void bindEventIcon(CommitComment item, ImageView view) {
        view.setVisibility(View.GONE);
    }

    @Override
    protected void bindReactions(CommitComment item, ReactionBar view) {
        view.setReactions(item.getReactions());
    }

    @Override
    protected boolean hasActionMenu(CommitComment item) {
        return true;
    }

    @Override
    protected boolean canQuote(CommitComment item) {
        return true;
    }

    @Override
    protected boolean canReact(CommitComment item) {
        return true;
    }

    @Override
    public List<Reaction> loadReactionDetailsInBackground(ReactionBar.Item item) throws IOException {
        CommitComment comment = (CommitComment) item;
        CommitService service = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        return service.getCommentReactions(new RepositoryId(mRepoOwner, mRepoName), comment.getId());
    }

    @Override
    public Reaction addReactionInBackground(ReactionBar.Item item, String content) throws IOException {
        CommitComment comment = (CommitComment) item;
        CommitService service = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        return service.addCommentReaction(new RepositoryId(mRepoOwner, mRepoName), comment.getId(), content);
    }

    @Override
    protected void updateReactions(CommitComment item, Reactions reactions) {
        item.setReactions(reactions);
    }
}