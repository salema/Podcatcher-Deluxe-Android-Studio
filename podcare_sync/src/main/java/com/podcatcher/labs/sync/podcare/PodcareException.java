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
package com.podcatcher.labs.sync.podcare;

import android.support.annotation.Nullable;

import java.io.IOException;

import retrofit2.Response;

/**
 * A Podcare exception type. Used to wrap both IO and network problems
 * (getCause() != null) and actual Podcare service replies on bad requests.
 */
public class PodcareException extends Exception {

    private int httpCode = -1;
    private String reply = null;

    PodcareException(Throwable error) {
        super(error);
    }

    PodcareException(Response response) {
        super(response.message());

        this.httpCode = response.code();
        try {
            this.reply = response.errorBody().string();
        } catch (IOException ioe) {
            // pass
        }
    }

    /**
     * @return The HTTP status code sent by Podcare, -1 if none.
     * @see #getCause()
     */
    public int getHttpCode() {
        return httpCode;
    }

    /**
     * @return The message sent by Podcare, <code>null</code> if none.
     * @see #getCause()
     */
    @Nullable
    public String getReply() {
        return reply;
    }
}
