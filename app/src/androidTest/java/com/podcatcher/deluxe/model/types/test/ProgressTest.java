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
