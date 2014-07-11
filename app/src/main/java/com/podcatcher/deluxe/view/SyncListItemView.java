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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.model.sync.SyncController;
import com.podcatcher.deluxe.model.sync.SyncController.SyncMode;
import com.podcatcher.deluxe.view.fragments.ConfigureSyncFragment.ConfigureSyncDialogListener;

/**
 * A list item view to represent a sync controller.
 */
public class SyncListItemView extends LinearLayout {

    /**
     * The controller progress view
     */
    private ProgressBar progressBar;
    /**
     * The controller icon view
     */
    private ImageView iconView;
    /**
     * The name text view
     */
    private TextView nameTextView;
    /**
     * The update settings button divider (large screens only)
     */
    private View settingsButtonDivider;
    /**
     * The update settings button
     */
    private ImageButton settingsButton;
    /**
     * The controller mode radio toggles
     */
    private RadioGroup modeRadioGroup;

    /**
     * Create a sync controller item list view.
     *
     * @param context Context for the view to live in.
     * @param attrs   View attributes.
     */
    public SyncListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        progressBar = (ProgressBar) findViewById(R.id.sync_progress);
        iconView = (ImageView) findViewById(R.id.sync_controller_icon);
        nameTextView = (TextView) findViewById(R.id.sync_controller_name);
        settingsButton = (ImageButton) findViewById(R.id.sync_controller_settings);
        settingsButtonDivider = findViewById(R.id.sync_controller_settings_divider);
        modeRadioGroup = (RadioGroup) findViewById(R.id.sync_controller_mode);
    }

    /**
     * Make the view update all its children to represent input given.
     *
     * @param impl     {@link SyncController} to represent.
     * @param linked   Whether the controller can be used.
     * @param mode     The current controller sync mode.
     * @param listener The listener to call-back when the user interacts with
     *                 the list item's UI.
     */
    public void show(final ControllerImpl impl, boolean linked, SyncMode mode,
                     final ConfigureSyncDialogListener listener) {
        // 1. Set icon and name
        final boolean isRunning = SyncManager.getInstance().isSyncRunning(impl);
        progressBar.setVisibility(isRunning ? View.VISIBLE : View.GONE);
        iconView.setVisibility(isRunning ? View.GONE : View.VISIBLE);
        iconView.setImageResource(impl.getLogoResourceId());
        nameTextView.setText(impl.getLabel());

        // 2. Show radio controls if linked
        modeRadioGroup.setVisibility(linked ? View.VISIBLE : View.GONE);
        // The divider is only present on larger screens
        if (settingsButtonDivider != null)
            settingsButtonDivider.setVisibility(modeRadioGroup.getVisibility());

        // 3. Apply mode setting
        modeRadioGroup.check(mode == null || !linked ? R.id.sync_controller_off :
                SyncMode.SEND_ONLY.equals(mode) ? R.id.sync_controller_out
                        : R.id.sync_controller_in_out);

        // 4. Add listeners
        settingsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onUpdateSettings(impl);
            }
        });
        // The onCheckedChanged listener for the radio group fires too much, we
        // simply attach onClick listeners to each radio button...
        for (int index = 0; index < modeRadioGroup.getChildCount(); index++)
            modeRadioGroup.getChildAt(index)
                    .setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View radioButton) {
                            final int id = radioButton.getId();
                            listener.onUpdateMode(impl, id == R.id.sync_controller_off ? null
                                    : id == R.id.sync_controller_out ? SyncMode.SEND_ONLY
                                    : SyncMode.SEND_RECEIVE);
                        }
                    });
    }
}
