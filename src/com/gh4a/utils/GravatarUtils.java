package com.gh4a.utils;

public class GravatarUtils {

    public static String getGravatarUrl(String gravatarId) {
        return "http://www.gravatar.com/avatar.php?gravatar_id=" + gravatarId + "&size=60&d=mm";
    }
}
