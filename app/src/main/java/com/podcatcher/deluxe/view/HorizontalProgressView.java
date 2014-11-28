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

package com.podcatcher.deluxe.view;

import android.content.Context;
import android.util.AttributeSet;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.types.Progress;

/**
 * A sophisticated horizontal progress view.
 */
public class HorizontalProgressView extends ProgressView {

    /**
     * Create progress view.
     *
     * @param context Context view lives in.
     * @param attrs   View attributes.
     */
    public HorizontalProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getLayout() {
        return R.layout.progress_horizontal;
    }

    @Override
    public void publishProgress(Progress progress) {
        super.publishProgress(progress);

        // Show progress in progress bar
        if (progress.getPercentDone() >= 0 && progress.getPercentDone() <= 100) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(progress.getPercentDone());
        } else
            progressBar.setIndeterminate(true);
    }
}
