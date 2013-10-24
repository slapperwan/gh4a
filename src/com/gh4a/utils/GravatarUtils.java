package com.gh4a.utils;

public class GravatarUtils {

    public static String getGravatarUrl(String gravatarId) {
        return "http://www.gravatar.com/avatar.php?gravatar_id=" + gravatarId + "&size=60&d=mm";
    }

    public static String extractGravatarId(String url) {
        String[] urlParts = url.split("/");
        if (urlParts.length >= 5) {
            int pos = urlParts[4].indexOf('?');
            if (pos < 0) {
                return urlParts[4];
            }
            return urlParts[4].substring(0, pos);
        }
        return null;
    }
}
