package com.dragontek.mygpoclient;

public class Global {

    public static final boolean DEBUG = false;

    public static final String HOST = "gpodder.net";
    public static final int VERSION = 2;
    public static final int TOPLIST_DEFAULT = 25;
    public static final String WEBSITE = "http://www.podcatcher-deluxe.com";

    // TODO Make this depend on version
    public static final String USER_AGENT =
            String.format("PodcatcherDeluxe (%s)", WEBSITE);

}
