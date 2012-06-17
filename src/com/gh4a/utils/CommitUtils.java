package com.gh4a.utils;

import org.eclipse.egit.github.core.RepositoryCommit;

public class CommitUtils {

    public static String getCommitterName(RepositoryCommit repositoryCommit) {
        if (repositoryCommit.getAuthor() != null) {
            return repositoryCommit.getAuthor().getLogin();
        }
        else if (repositoryCommit.getCommit().getAuthor() != null) {
            return repositoryCommit.getCommit().getAuthor().getName();
        }
        else {
            return "unknown";
        }
    }
    
    public static String getAuthorName(RepositoryCommit repositoryCommit) {
        if (repositoryCommit.getCommitter() != null) {
            return repositoryCommit.getCommitter().getLogin();
        }
        else if (repositoryCommit.getCommit().getCommitter() != null) {
            return repositoryCommit.getCommit().getCommitter().getName();
        }
        else {
            return "unknown";
        }
    }
    
    public static String getAuthorGravatarId(RepositoryCommit repositoryCommit) {
        if (repositoryCommit.getAuthor() != null) {
            return repositoryCommit.getAuthor().getGravatarId();
        }
        else {
            return null;
        }
    }
    
    public static String getCommitterGravatarId(RepositoryCommit repositoryCommit) {
        if (repositoryCommit.getCommitter() != null) {
            return repositoryCommit.getCommitter().getGravatarId();
        }
        else {
            return null;
        }
    }
}
