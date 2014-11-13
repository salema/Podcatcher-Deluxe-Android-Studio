/** Copyright 2012, 2013 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package com.podcatcher.deluxe.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.LinearLayout;

import com.podcatcher.deluxe.R;

/**
 * A linear layout sub-class that allows for extra drop shadows to be drawn on
 * the edges of the view. USe the XML declaration to enable these.
 */
public class DropShadowLinearLayout extends LinearLayout {

    /**
     * The dark side of the shadow
     */
    private final static int DARK_COLOR = Color.DKGRAY;
    /**
     * The light side of the shadow
     */
    private final static int LIGHT_COLOR = Color.LTGRAY;
    /**
     * The shadow color array
     */
    private final static int[] SHADOW = {
            DARK_COLOR, LIGHT_COLOR
    };
    /**
     * The actual shadow drawable (right side)
     */
    private final static GradientDrawable RIGHT_SHADOW_DRAWABLE =
            new GradientDrawable(Orientation.RIGHT_LEFT, SHADOW);
    /**
     * The actual shadow drawable (bottom edge)
     */
    private final static GradientDrawable BOTTOM_SHADOW_DRAWABLE =
            new GradientDrawable(Orientation.BOTTOM_TOP, SHADOW);
    /**
     * The shadow's transparency
     */
    private final static int ALPHA = 80;
    /**
     * The pixel width/height of the shadow (to be converted to dpi)
     */
    private final static int PIXELS_DPI = 4;
    /**
     * The actual width/height of the shadow in pixels
     */
    private final int pixels;
    /**
     * Whether a shadow should be drawn on the right side of the view group.
     */
    private final boolean shadowRight;
    /**
     * Whether a shadow should be drawn on the bottom edge of the view group.
     */
    private final boolean shadowBottom;

    /**
     * Create new drop shadow layout.
     *
     * @param context Our context.
     * @param attrs   The attributes, use these in the XML to enable/disable
     *                shadows.
     */
    public DropShadowLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get and read attributes
        final TypedArray attributes = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.DropShadowLinearLayout, 0, 0);

        this.shadowRight = attributes
                .getBoolean(R.styleable.DropShadowLinearLayout_shadowRight, false);
        this.shadowBottom = attributes
                .getBoolean(R.styleable.DropShadowLinearLayout_shadowBottom, false);

        attributes.recycle();

        // Set some attributes
        if (shadowRight)
            RIGHT_SHADOW_DRAWABLE.setAlpha(ALPHA);
        if (shadowBottom)
            BOTTOM_SHADOW_DRAWABLE.setAlpha(ALPHA);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        this.pixels = (int) (PIXELS_DPI * (metrics.densityDpi / 160f));
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        // Draw right hand side shadow
        final Rect clipRight = new Rect(canvas.getClipBounds());
        if (shadowRight && getWidth() == clipRight.width()) {
            clipRight.left = clipRight.right - pixels;

            RIGHT_SHADOW_DRAWABLE.setBounds(clipRight);
            RIGHT_SHADOW_DRAWABLE.draw(canvas);
        }

        // Draw bottom edge shadow
        final Rect clipBottom = new Rect(canvas.getClipBounds());
        if (shadowBottom && getHeight() == clipBottom.height()) {
            clipBottom.top = clipBottom.bottom - pixels;

            BOTTOM_SHADOW_DRAWABLE.setBounds(clipBottom);
            BOTTOM_SHADOW_DRAWABLE.draw(canvas);
        }
    }
}
