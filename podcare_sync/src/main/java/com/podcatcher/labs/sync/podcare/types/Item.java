/**
 * Copyright 2012-2016 Kevin Hausmann
 *
 * This file is part of Podcatcher Deluxe.
 *
 * Podcatcher Deluxe is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Podcatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Podcatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */
package com.podcatcher.labs.sync.podcare.types;

import com.google.gson.annotations.SerializedName;

/**
 * POJO for a Podcare episode.
 */
public class Item {

    private String feed;
    @SerializedName("episode")
    private String title;
    private String duration;
    private String progress;
    private String guid;
    private boolean favourite;
    private String file;
    private boolean finished;
    private String flattr;


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFeed() {
        return feed;
    }

    public void setFeed(String feed) {
        this.feed = feed;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getFlattr() {
        return flattr;
    }

    public void setFlattr(String flattr) {
        this.flattr = flattr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;

        if (favourite != item.favourite) return false;
        if (finished != item.finished) return false;
        if (feed != null ? !feed.equals(item.feed) : item.feed != null) return false;
        if (title != null ? !title.equals(item.title) : item.title != null) return false;
        if (duration != null ? !duration.equals(item.duration) : item.duration != null)
            return false;
        if (progress != null ? !progress.equals(item.progress) : item.progress != null)
            return false;
        if (guid != null ? !guid.equals(item.guid) : item.guid != null) return false;
        if (file != null ? !file.equals(item.file) : item.file != null) return false;
        return !(flattr != null ? !flattr.equals(item.flattr) : item.flattr != null);
    }

    @Override
    public int hashCode() {
        int result = feed != null ? feed.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        result = 31 * result + (progress != null ? progress.hashCode() : 0);
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        result = 31 * result + (favourite ? 1 : 0);
        result = 31 * result + (file != null ? file.hashCode() : 0);
        result = 31 * result + (finished ? 1 : 0);
        result = 31 * result + (flattr != null ? flattr.hashCode() : 0);
        return result;
    }
}
