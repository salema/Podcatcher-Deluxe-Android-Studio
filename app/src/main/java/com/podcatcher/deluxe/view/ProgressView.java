/** Copyright 2012-2014 Kevin Hausmann
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
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.types.Progress;

/**
 * A sophisticated progress view.
 */
public class ProgressView extends LinearLayout {

    /**
     * The progress bar
     */
    protected ProgressBar progressBar;
    /**
     * The progress bar text
     */
    protected TextView progressTextView;

    /**
     * Create a new progress view.
     *
     * @param context Context for the view to live in.
     * @param attrs   View attributes.
     */
    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View.inflate(context, getLayout(), this);
    }

    /**
     * @return The layout resource id to inflate for this view. Sub-classes
     * might want to overwrite this.
     */
    protected int getLayout() {
        return R.layout.progress;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressTextView = (TextView) findViewById(R.id.progress_text);
    }

    /**
     * Show a textual progress information. Beyond actual percentages this also
     * works with flags from {@link Progress}.
     *
     * @param progress Progress to visualize.
     * @see Progress
     */
    public void publishProgress(Progress progress) {
        progressBar.setVisibility(VISIBLE);

        progressTextView.setTextColor(getColor(R.color.text_secondary));

        if (progress.equals(Progress.WAIT))
            progressTextView.setText(R.string.wait);
        else if (progress.equals(Progress.CONNECT))
            progressTextView.setText(R.string.connect);
        else if (progress.equals(Progress.LOAD))
            progressTextView.setText(R.string.load);
        else if (progress.equals(Progress.PARSE))
            progressTextView.setText(R.string.parse);
        else if (progress.getPercentDone() >= 0 && progress.getPercentDone() <= 100)
            progressTextView.setText(progress.getPercentDone() + "%");
        else
            progressTextView.setText(R.string.load);
    }

    /**
     * Show an error and abort progress.
     *
     * @param errorId Resource id for error message.
     */
    public void showError(int errorId) {
        progressBar.setVisibility(GONE);

        progressTextView.setVisibility(VISIBLE);
        progressTextView.setText(errorId);
        progressTextView.setTextColor(getColor(R.color.text_error));
        progressTextView.setSingleLine(false);
    }

    /**
     * Reset to initial UI state.
     */
    public void reset() {
        progressBar.setVisibility(VISIBLE);

        progressTextView.setVisibility(VISIBLE);
        progressTextView.setText(R.string.wait);
        progressTextView.setTextColor(getColor(R.color.text_secondary));
        progressTextView.setSingleLine(true);
    }

    /**
     * Short cut to color resource.
     *
     * @param id Color resources id key.
     * @return The color resource.
     */
    protected int getColor(int id) {
        return getResources().getColor(id);
    }
}
