package com.dragontek.mygpoclient.feeds;

public class Feed {
    private String title;
    private String link;
    private String description;
    private String author;
    private String language;
    private String url;
    private String[] urls;
    private String new_location;
    private String logo;
    private String logo_data;
    private String[] content_types;
    private String hub;
    /*
     * These are disabled because the new feed service doesn't support them the
     * same way that the old service did. Errors and warnings aren't coming back
     * as dictionary and http_last_modified is a date string rather than UNIX
     * timestamp
     */
    // private Dictionary<String, String> errors;
    // private Dictionary<String, String> warnings;
    // private long http_last_modified;
    private String http_etag;
    private Episode[] episodes;

    public String getNewLocation() {
        return this.new_location;
    }

    public String getLogoData() {
        return this.logo_data;
    }

    public String[] getContentTypes() {
        return this.content_types;
    }

    public String getHub() {
        // HAHA GitHub!
        return this.hub;
    }

    public String getHttpEtag() {
        return this.http_etag;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLink() {
        return this.link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getUrl() {
        if (this.url != null)
            return this.url;
        else if (this.urls.length > 0)
            return this.urls[0];
        else
            return "";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogoUrl() {
        return this.logo;
    }

    public void setLogoUrl(String logo) {
        this.logo = logo;
    }

    public Episode[] getEpisodes() {
        return this.episodes;
    }

    public class Episode {
        private String guid;
        private String title;
        private String short_title;
        private String number;
        private String description;
        private String link;
        private long released;
        private String author;
        private long duration;
        private String language;
        private Enclosure[] files;

        public Enclosure getEnclosure() {
            if (files.length > 0)
                return files[0];
            else
                return null;
        }

        public Enclosure[] getEnclosures() {
            return files;
        }

        public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public long getReleased() {
            return released;
        }

        public void setReleased(long released) {
            this.released = released;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getShortTitle() {
            return short_title;
        }

        public void setShortTitle(String short_title) {
            this.short_title = short_title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public class Enclosure {
            private String url;
            private String[] urls;
            private String mimetype;
            private long filesize;

            public String getUrl() {
                if (this.url != null)
                    return this.url;
                else if (this.urls.length > 0)
                    return this.urls[0];
                else
                    return null;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getMimetype() {
                return mimetype;
            }

            public void setMimetype(String mimetype) {
                this.mimetype = mimetype;
            }

            public long getFilesize() {
                return filesize;
            }

            public void setFilesize(long filesize) {
                this.filesize = filesize;
            }
        }
    }
}
