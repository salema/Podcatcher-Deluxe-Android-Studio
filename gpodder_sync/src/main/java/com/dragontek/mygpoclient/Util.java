package com.dragontek.mygpoclient;

public class Util {
    /**
     * Join a collection of strings and add slash as delimiters.
     *
     * @require words.size() > 0 && words != null
     */
    public static String join(String[] words) {
        StringBuilder wordList = new StringBuilder();
        for (String word : words) {
            wordList.append(word + "/");
        }
        return new String(wordList.deleteCharAt(wordList.length() - 1));
    }
}
