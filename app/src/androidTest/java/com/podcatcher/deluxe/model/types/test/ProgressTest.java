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

package com.podcatcher.deluxe.model.types.test;

import com.podcatcher.deluxe.model.types.Progress;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class ProgressTest extends TestCase {

    public void testPercentageDone() {
        Progress p = new Progress(-1, -1);
        assertEquals(p.getPercentDone(), -1);

        p = new Progress(0, -1);
        assertEquals(p.getPercentDone(), -1);

        p = new Progress(-1, 0);
        assertEquals(p.getPercentDone(), -1);

        p = new Progress(0, 0);
        assertEquals(p.getPercentDone(), -1);

        p = new Progress(0, 1);
        assertEquals(p.getPercentDone(), 0);

        p = new Progress(1, 1);
        assertEquals(p.getPercentDone(), 100);

        p = new Progress(5, 10);
        assertEquals(p.getPercentDone(), 50);

        p = new Progress(150, 100);
        assertEquals(p.getPercentDone(), 150);
    }
}
