package com.gh4a.utils;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.gh4a.R;

import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ApiHelpers {
    public interface IssueState {
        String OPEN = "open";
        String CLOSED = "closed";
        String MERGED = "merged";
        String UNMERGED = "unmerged";
    }

    public interface UserType {
        String USER = "User";
        String ORG = "Organization";
    }

    public interface MilestoneState {
        String OPEN = "open";
        String CLOSED = "closed";
    }

    //RepositoryCommit
    public static String getAuthorName(Context context, RepositoryCommit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getLogin();
        }
        if (commit.getCommit().getAuthor() != null) {
            return commit.getCommit().getAuthor().getName();
        }
        return context.getString(R.string.unknown);
    }

    public static String getAuthorLogin(RepositoryCommit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getLogin();
        }
        return null;
    }

    public static String getCommitterName(Context context, RepositoryCommit commit) {
        if (commit.getCommitter() != null) {
            return commit.getCommitter().getLogin();
        }
        if (commit.getCommit().getCommitter() != null) {
            return commit.getCommit().getCommitter().getName();
        }
        return context.getString(R.string.unknown);
    }

    public static boolean authorEqualsCommitter(RepositoryCommit commit) {
        if (commit.getCommitter() != null && commit.getAuthor() != null) {
            return TextUtils.equals(commit.getCommitter().getLogin(), commit.getAuthor().getLogin());
        }

        CommitUser author = commit.getCommit().getAuthor();
        CommitUser committer = commit.getCommit().getCommitter();
        if (author.getEmail() != null && committer.getEmail() != null) {
            return TextUtils.equals(author.getEmail(), committer.getEmail());
        }
        return TextUtils.equals(author.getName(), committer.getName());
    }

    public static String getUserLogin(Context context, User user) {
        if (user != null && user.getLogin() != null) {
            return user.getLogin();
        }
        return context.getString(R.string.unknown);
    }

    public static int colorForLabel(Label label) {
        return Color.parseColor("#" + label.getColor());
    }

    public static boolean userEquals(User lhs, User rhs) {
        if (lhs == null || rhs == null) {
            return false;
        }
        return loginEquals(lhs.getLogin(), rhs.getLogin());
    }

    public static boolean loginEquals(User user, String login) {
        if (user == null) {
            return false;
        }
        return loginEquals(user.getLogin(), login);
    }

    public static boolean loginEquals(String user, String login) {
        return user != null && user.equalsIgnoreCase(login);
    }

    public static Uri normalizeUri(Uri uri) {
        if (uri == null || uri.getAuthority() == null) {
            return uri;
        }

        // Only normalize API links
        if (!uri.getPath().contains("/api/v3/") && !uri.getAuthority().contains("api.")) {
            return uri;
        }

        String path = uri.getPath()
                .replace("/api/v3/", "/")
                .replace("repos/", "")
                .replace("commits/", "commit/")
                .replace("pulls/", "pull/");

        String authority = uri.getAuthority()
                .replace("api.", "");

        return uri.buildUpon()
                .path(path)
                .authority(authority)
                .build();
    }

    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes());
            byte[] messageDigest = digest.digest();

            StringBuilder builder = new StringBuilder();
            for (byte b : messageDigest) {
                String hexString = Integer.toHexString(0xFF & b);
                while (hexString.length() < 2) {
                    hexString = "0" + hexString;
                }
                builder.append(hexString);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static class DiffHighlightId implements Parcelable {
        public final String fileHash;
        public final int startLine;
        public final int endLine;
        public final boolean right;

        private DiffHighlightId(String fileHash, int startLine, int endLine, boolean right) {
            this.fileHash = fileHash;
            this.startLine = startLine;
            this.endLine = endLine;
            this.right = right;
        }

        public DiffHighlightId(Parcel p) {
            this.fileHash = p.readString();
            this.startLine = p.readInt();
            this.endLine = p.readInt();
            this.right = p.readInt() != 0;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel p, int flags) {
            p.writeString(fileHash);
            p.writeInt(startLine);
            p.writeInt(endLine);
            p.writeInt(right ? 1 : 0);
        }

        public static final Parcelable.Creator<DiffHighlightId> CREATOR
                = new Parcelable.Creator<DiffHighlightId>() {
            public DiffHighlightId createFromParcel(Parcel in) {
                return new DiffHighlightId(in);
            }

            public DiffHighlightId[] newArray(int size) {
                return new DiffHighlightId[size];
            }
        };

        public static DiffHighlightId fromUriFragment(String fragment) {
            boolean right = false;

            int typePos = fragment.indexOf('L');
            if (typePos < 0) {
                right = true;
                typePos = fragment.indexOf('R');
            }

            String fileHash = typePos > 0 ? fragment.substring(0, typePos) : fragment;
            if (fileHash.length() != 32) { // MD5 hash length
                return null;
            }
            if (typePos < 0) {
                return new DiffHighlightId(fileHash, -1, -1, false);
            }

            try {
                char type = fragment.charAt(typePos);
                String linePart = fragment.substring(typePos + 1);
                int startLine, endLine, dashPos = linePart.indexOf("-" + type);
                if (dashPos > 0) {
                    startLine = Integer.valueOf(linePart.substring(0, dashPos));
                    endLine = Integer.valueOf(linePart.substring(dashPos + 2));
                } else {
                    startLine = Integer.valueOf(linePart);
                    endLine = startLine;
                }
                return new DiffHighlightId(fileHash, startLine, endLine, right);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}