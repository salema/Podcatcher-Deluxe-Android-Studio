/**
 * Copyright 2012-2015 Kevin Hausmann
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
package com.podcatcher.labs.sync.gpodder.types;

import com.google.gson.annotations.SerializedName;

/**
 * POJO for an episode action as used by gpodder.net
 */
public class EpisodeAction {

    public enum Action {
        @SerializedName("download")DOWNLOAD, @SerializedName("delete")DELETE,
        @SerializedName("play")PLAY, @SerializedName("new")RESET, @SerializedName("flattr")FLATTR
    }

    private String podcast;
    private String episode;
    private String device;
    private Action action;
    private String timestamp;
    private Integer started;
    private Integer position;
    private Integer total;

    public String getPodcast() {
        return podcast;
    }

    public void setPodcast(String podcast) {
        this.podcast = podcast;
    }

    public String getEpisode() {
        return episode;
    }

    public void setEpisode(String episode) {
        this.episode = episode;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStarted() {
        return started;
    }

    public void setStarted(Integer started) {
        this.started = started;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EpisodeAction that = (EpisodeAction) o;

        if (podcast != null ? !podcast.equals(that.podcast) : that.podcast != null) return false;
        if (episode != null ? !episode.equals(that.episode) : that.episode != null) return false;
        if (device != null ? !device.equals(that.device) : that.device != null) return false;
        if (action != that.action) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null)
            return false;
        if (started != null ? !started.equals(that.started) : that.started != null) return false;
        if (position != null ? !position.equals(that.position) : that.position != null)
            return false;
        return !(total != null ? !total.equals(that.total) : that.total != null);

    }

    @Override
    public int hashCode() {
        int result = podcast != null ? podcast.hashCode() : 0;
        result = 31 * result + (episode != null ? episode.hashCode() : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (started != null ? started.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        result = 31 * result + (total != null ? total.hashCode() : 0);
        return result;
    }
}
