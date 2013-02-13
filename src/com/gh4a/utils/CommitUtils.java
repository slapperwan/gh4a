package com.gh4a.utils;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.RepositoryCommit;

public class CommitUtils {

    //RepositoryCommit
    public static String getAuthorName(RepositoryCommit repositoryCommit) {
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
    
    public static String getAuthorLogin(RepositoryCommit repositoryCommit) {
        if (repositoryCommit.getAuthor() != null) {
            return repositoryCommit.getAuthor().getLogin();
        }
        else {
            return null;
        }
    }
    
    public static String getCommitterName(RepositoryCommit repositoryCommit) {
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
    
    public static String getCommitterLogin(RepositoryCommit repositoryCommit) {
        if (repositoryCommit.getCommitter() != null) {
            return repositoryCommit.getCommitter().getLogin();
        }
        else {
            return null;
        }
    }
    
    public static String getAuthorGravatarId(RepositoryCommit repositoryCommit) {
        if (repositoryCommit.getAuthor() != null) {
            return repositoryCommit.getAuthor().getGravatarId();
        }
        else if (repositoryCommit.getCommit().getAuthor() != null
                && repositoryCommit.getCommit().getAuthor().getEmail() != null) {
            return StringUtils.md5Hex(repositoryCommit.getCommit().getAuthor().getEmail());
        }
        else {
            return null;
        }
    }
    
    public static String getCommitterGravatarId(RepositoryCommit repositoryCommit) {
        if (repositoryCommit.getCommitter() != null) {
            return repositoryCommit.getCommitter().getGravatarId();
        }
        else if (repositoryCommit.getCommit().getCommitter() != null
                && repositoryCommit.getCommit().getCommitter().getEmail() != null) {
            return StringUtils.md5Hex(repositoryCommit.getCommit().getCommitter().getEmail());
        }
        else {
            return null;
        }
    }
    
    //Commit
    public static String getAuthorName(Commit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getName();
        }
        else {
            return "unknown";
        }
    }
    
    public static String getCommitterName(Commit commit) {
        if (commit.getCommitter() != null) {
            return commit.getCommitter().getName();
        }
        else {
            return "unknown";
        }
    }
    
    public static String getAuthorEmail(Commit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getEmail();
        }
        else {
            return null;
        }
    }
    
    public static String getCommitterEmail(Commit commit) {
        if (commit.getCommitter() != null) {
            return commit.getCommitter().getEmail();
        }
        else {
            return null;
        }
    }
}
