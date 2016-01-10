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

package com.podcatcher.deluxe.model.tasks;

import com.podcatcher.deluxe.model.types.Progress;

import android.os.AsyncTask;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Abstract task for file writing.
 *
 * @param <Params> Params as defined by {@link AsyncTask}
 */
public abstract class StoreFileTask<Params> extends AsyncTask<Params, Progress, Void> {

    /**
     * The file encoding
     */
    public static final String FILE_ENCODING = "utf8";
    /**
     * The indent char
     */
    protected static final char INDENT = ' ';

    /**
     * The file writer
     */
    protected BufferedWriter writer;

    /**
     * @param level Indent level to put in front of line.
     * @param line  Actual text to write.
     * @throws IOException If writing the line goes wrong.
     */
    protected void writeLine(int level, String line) throws IOException {
        for (int i = 0; i < level * 2; i++)
            writer.write(INDENT);

        writer.write(line);
        writer.newLine();
    }
}
