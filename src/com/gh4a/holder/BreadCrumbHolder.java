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

import java.util.HashMap;

/**
 * The Class BreadCrumbHolder.
 */
public class BreadCrumbHolder {

    /** The label. */
    private String label;
    
    /** The tag. */
    private String tag;
    
    /** The data. */
    private HashMap<String, String> data = new HashMap<String, String>();

    /**
     * Gets the label.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label.
     *
     * @param label the new label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the data.
     *
     * @return the data
     */
    public HashMap<String, String> getData() {
        return data;
    }

    /**
     * Sets the data.
     *
     * @param data the data
     */
    public void setData(HashMap<String, String> data) {
        this.data = data;
    }

    /**
     * Gets the tag.
     *
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the tag.
     *
     * @param tag the new tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }
}
