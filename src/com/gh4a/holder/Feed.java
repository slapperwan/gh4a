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
package com.gh4a.holder;

import java.util.Date;

public class Feed {

    private String id;
    private Date published;
    private String link;
    private String title;
    private String content;
    private String preview;
    private String author;
    private String gravatarId;
    private String gravatarUrl;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Date getPublished() {
        return published;
    }
    public void setPublished(Date published) {
        this.published = published;
    }

    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return content;
    }
    public String getPreview() {
        return preview;
    }
    public void setContent(String content) {
        this.content = content;
        if (content != null) {
            preview = content.length() > 2000 ? content.substring(0, 2000) : content;
            preview = content.replaceAll("<(.|\n)*?>","");
            if (preview.length() > 500) {
                preview = preview.substring(0,  500);
            }
        } else {
            preview = null;
        }
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getGravatarId() {
        return gravatarId;
    }
    public String getGravatarUrl() {
        return gravatarUrl;
    }
    public void setGravatar(String id, String url) {
        this.gravatarId = id;
        this.gravatarUrl = url;
    }
}
