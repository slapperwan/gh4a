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
package com.gh4a.model;

import com.squareup.moshi.Json;

public class Trend {
    @Json(name = "owner")
    private final String repoOwner;
    @Json(name = "repo")
    private final String repoName;
    private final String description;
    private final String language;
    private final int stars;
    @Json(name = "new_stars")
    private final int newStars;
    private final int forks;

    public Trend(String owner, String repo, String desc, String lang, int stars, int newStars, int forks) {
        this.repoOwner = owner;
        this.repoName = repo;
        this.description = desc;
        this.language = lang;
        this.forks = forks;
        this.stars = stars;
        this.newStars = newStars;
    }

    public String getRepoOwner() {
        return repoOwner;
    }
    public String getRepoName() {
        return repoName;
    }
    public String getDescription() {
        return description;
    }
    public String getLanguage() {
        return language;
    }
    public int getStars() {
        return stars;
    }
    public int getNewStars() {
        return newStars;
    }
    public int getForks() {
        return forks;
    }
}
