/** Copyright 2012-2014 Kevin Hausmann
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

package com.podcatcher.deluxe.model.types;

import java.util.Locale;

/**
 * Type to indicate load progress. This has two modes: By default a progress is
 * given as the amount of work done towards a total workload. In addition, this
 * class defines a number of static progress events to use.
 */
public class Progress {

    /**
     * Flag indicating waiting state
     */
    private static final int PROGRESS_WAIT = -5;
    /**
     * Waiting state
     */
    public static final Progress WAIT = new Progress(PROGRESS_WAIT, -1);
    /**
     * Flag indicating connection state
     */
    private static final int PROGRESS_CONNECT = -4;
    /**
     * Connecting state
     */
    public static final Progress CONNECT = new Progress(PROGRESS_CONNECT, -1);
    /**
     * Flag indicating loading state
     */
    private static final int PROGRESS_LOAD = -3;
    /**
     * Loading state
     */
    public static final Progress LOAD = new Progress(PROGRESS_LOAD, -1);
    /**
     * Flag indicating parsing state
     */
    private static final int PROGRESS_PARSE = -2;
    /**
     * Parsing state
     */
    public static final Progress PARSE = new Progress(PROGRESS_PARSE, -1);
    /**
     * Flag indicating parsing state
     */
    private static final int PROGRESS_DONE = -1;
    /**
     * Done state
     */
    public static final Progress DONE = new Progress(PROGRESS_DONE, -1);

    /**
     * The actual amount of progress made
     */
    protected final int progress;
    /**
     * The total amount of work
     */
    protected final int total;

    /**
     * Create new progress information.
     *
     * @param progress Amount done.
     * @param total    Amount to do in total.
     */
    public Progress(int progress, int total) {
        this.progress = progress;
        this.total = total;
    }

    /**
     * @return The amount of work already done.
     */
    public int getProgress() {
        return progress;
    }

    /**
     * @return The total amount of work.
     */
    public int getTotal() {
        return total;
    }

    /**
     * @return The amount of work done in percent of total. Returns -1 if no
     * valid percentage could be calculated.
     */
    public int getPercentDone() {
        if (total <= 0)
            return -1;
        else
            return (int) ((float) progress / (float) total * 100);
    }

    @Override
    public String toString() {
        // Predefined state
        if (total < 0) {
            switch (progress) {
                case PROGRESS_WAIT:
                    return "Wait";
                case PROGRESS_CONNECT:
                    return "Connect";
                case PROGRESS_LOAD:
                    return "Load";
                case PROGRESS_PARSE:
                    return "Parse";
                case PROGRESS_DONE:
                    return "Done";
                default:
                    return "Unknown progress";
            }
        } // Standard case
        else
            return String.format(Locale.US, "%d/%d (%d%%)", progress, total, getPercentDone());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof Progress))
            return false;

        Progress other = (Progress) o;

        return progress == other.progress && total == other.total;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + progress;
        hash = 31 * hash + total;

        return hash;
    }
}
