package com.gh4a.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "feed", strict = false)
public class GitHubFeed {
    @ElementList(name = "entry", inline = true)
    public List<Feed> feed;
}
