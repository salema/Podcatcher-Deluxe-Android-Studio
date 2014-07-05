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

package com.podcatcher.deluxe.view.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.listeners.PlayerListener;
import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.types.Episode;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * The player fragment.
 */
public class PlayerFragment extends Fragment {

    /**
     * The listener for the title click
     */
    private PlayerListener listener;

    /**
     * Flag for the show load menu item state
     */
    private boolean showLoadMenuItem = false;
    /**
     * Flag for the state of the load menu item
     */
    private boolean loadMenuItemState = true;
    /**
     * Flag for the resume/play state of the load menu item
     */
    private boolean loadMenuItemResume = false;
    /**
     * Flag for the show player state
     */
    private boolean showPlayer = false;
    /**
     * Flag for the show player title state
     */
    private boolean showPlayerTitle = false;
    /**
     * Flag for the show player seek bar state
     */
    private boolean showPlayerSeekbar = true;
    /**
     * Flag for the position/duration information state
     */
    private boolean showShortPlaybackPosition = false;
    /**
     * Flag for the show next button state
     */
    private boolean showNextButton = false;
    /**
     * Flag for the show error view state
     */
    private boolean showError = false;

    /**
     * Status flag indicating that our view is created
     */
    private boolean viewCreated = false;

    /**
     * The load episode menu bar item
     */
    private MenuItem loadMenuItem;

    /**
     * Title view showing current episode title
     */
    private TextView titleView;
    /**
     * The player's seek bar
     */
    private SeekBar seekBar;
    /**
     * The rewind button
     */
    private ImageButton rewindButton;
    /**
     * The player main button
     */
    private Button playPauseButton;
    /**
     * The fast-forward button
     */
    private ImageButton forwardButton;
    /**
     * The next button
     */
    private ImageButton nextButton;
    /**
     * The error view
     */
    private TextView errorView;

    /**
     * The handler posting rewind and forward events
     */
    private Handler transportationHandler = new Handler();
    /**
     * Rewind or forward active flag (button held down)
     */
    private boolean transportActive = false;
    /**
     * Delay in between rewind or forward call-backs
     */
    private static long TRANSPORT_DELAY = 500;
    /**
     * The rewind runnable
     */
    private Runnable rewindRunnable = new Runnable() {

        @Override
        public void run() {
            listener.onRewind();

            if (rewindButton.isPressed())
                transportationHandler.postDelayed(rewindRunnable, TRANSPORT_DELAY);
        }
    };
    /**
     * The forward runnable
     */
    private Runnable forwardRunnable = new Runnable() {

        @Override
        public void run() {
            listener.onFastForward();

            if (forwardButton.isPressed())
                transportationHandler.postDelayed(forwardRunnable, TRANSPORT_DELAY);
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (PlayerListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PlayerListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleView = (TextView) view.findViewById(R.id.player_title);
        titleView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onReturnToPlayingEpisode();
            }
        });

        seekBar = (SeekBar) view.findViewById(R.id.player_seekbar);
        seekBar.setOnSeekBarChangeListener(listener);

        rewindButton = (ImageButton) view.findViewById(R.id.player_rewind);
        prepareTransportButton(rewindButton, true);

        playPauseButton = (Button) view.findViewById(R.id.player_button);
        playPauseButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onTogglePlay();
            }
        });

        forwardButton = (ImageButton) view.findViewById(R.id.player_forward);
        prepareTransportButton(forwardButton, false);

        nextButton = (ImageButton) view.findViewById(R.id.player_next);
        nextButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onNext();
            }
        });

        errorView = (TextView) view.findViewById(R.id.player_error);

        viewCreated = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        setLoadMenuItemVisibility(showLoadMenuItem, loadMenuItemState, loadMenuItemResume);
        setPlayerVisibility(showPlayer);
        setPlayerTitleVisibility(showPlayerTitle);
        setPlayerSeekbarVisibility(showPlayerSeekbar);
        setNextButtonVisibility(showNextButton);
        setErrorViewVisibility(showError);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.player, menu);

        loadMenuItem = menu.findItem(R.id.episode_load_menuitem);
        setLoadMenuItemVisibility(showLoadMenuItem, loadMenuItemState, loadMenuItemResume);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.episode_load_menuitem:
                // Tell activity to load/unload the current episode
                listener.onToggleLoad();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set whether the fragment should show the load menu item. You can call
     * this any time and can expect it to happen on menu creation at the latest.
     * You also have to set the load menu state, <code>true</code> for "Play" /
     * "Load" and <code>false</code> for "Stop" / "Unload".
     *
     * @param show   Whether to show the load menu item.
     * @param load   State of the load menu item (load / unload)
     * @param resume Whether to use the resume or the play icon as load
     *               indicator
     */
    public void setLoadMenuItemVisibility(boolean show, boolean load, boolean resume) {
        this.showLoadMenuItem = show;
        this.loadMenuItemState = load;
        this.loadMenuItemResume = resume;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (isResumed() && loadMenuItem != null) {
            loadMenuItem.setVisible(show);

            loadMenuItem.setTitle(load ? R.string.play : R.string.stop);
            loadMenuItem.setIcon(load ? resume ? R.drawable.ic_media_resume
                    : R.drawable.ic_media_play : R.drawable.ic_media_stop);
        }
    }

    /**
     * Set whether the fragment should show the player view at all. You can call
     * this any time and can expect it to happen on resume at the latest.
     *
     * @param show Whether to show the player view.
     */
    public void setPlayerVisibility(boolean show) {
        this.showPlayer = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed())
            getView().setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Set whether the fragment should show the player title view. You can call
     * this any time and can expect it to happen on resume at the latest. This
     * only makes a difference if the player itself is visible.
     *
     * @param show Whether to show the player title view.
     */
    public void setPlayerTitleVisibility(boolean show) {
        this.showPlayerTitle = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed())
            titleView.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Set whether the fragment should show the player seek bar view. You can
     * call this any time and can expect it to happen on resume at the latest.
     * This only makes a difference if the player itself is visible.
     *
     * @param show Whether to show the player title view.
     */
    public void setPlayerSeekbarVisibility(boolean show) {
        this.showPlayerSeekbar = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed())
            seekBar.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Set whether the fragment should show the long or the short
     * playback/duration string label on its play/pause button. This has effect
     * only after you update the button for the next time.
     *
     * @param showShortPosition The flag, give <code>true</code> for a short
     *                          label.
     * @see PlayerFragment#updateButton
     */
    public void setShowShortPosition(boolean showShortPosition) {
        this.showShortPlaybackPosition = showShortPosition;
    }

    /**
     * Set whether the fragment should show the next button view. You can call
     * this any time and can expect it to happen on resume at the latest. This
     * only makes a difference if the player itself is visible.
     *
     * @param show Whether to show the next button view.
     */
    public void setNextButtonVisibility(boolean show) {
        this.showNextButton = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed())
            nextButton.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Update the player title view to show name and link to the given episode.
     *
     * @param playingEpisode Episode to show link to.
     */
    public void updatePlayerTitle(Episode playingEpisode) {
        // We can only do this after the fragment's widgets are created
        if (viewCreated && playingEpisode != null)
            titleView.setText(Html.fromHtml("<a href=\"\">" + playingEpisode.getName() + " - "
                    + playingEpisode.getPodcast().getName() + "</a>"));
    }

    /**
     * Update the player seek bar to show current progress.
     *
     * @param enabled  Whether the seek bar is enabled.
     * @param max      Max value of the seek bar.
     * @param progress Progress to set.
     */
    public void updateSeekBar(boolean enabled, int max, int progress) {
        // We can only do this after the fragment's widgets are created
        if (viewCreated) {
            seekBar.setEnabled(enabled);

            seekBar.setMax(max);
            seekBar.setProgress(progress);
        }
    }

    /**
     * Update the player seek bar's secondary progress.
     *
     * @param secondaryProgress 2ndary progress to set.
     */
    public void updateSeekBarSecondaryProgress(int secondaryProgress) {
        // We can only do this after the fragment's widgets are created
        if (viewCreated)
            seekBar.setSecondaryProgress(secondaryProgress);
    }

    /**
     * Update the player button(s) to show current state and progress.
     *
     * @param buffering Whether the player is currently buffering.
     * @param playing   Whether the player is currently playing.
     * @param duration  Full duration of current episode.
     * @param position  Player position in current episode.
     */
    public void updateButton(boolean buffering, boolean playing, int duration, int position) {
        // We can only do this after the fragment's widgets are created
        if (viewCreated) {
            final String formattedPosition = ParserUtils.formatTime(position / 1000);
            final String formattedDuration = ParserUtils.formatTime(duration / 1000);

            if (duration == 0)
                playPauseButton.setText(getString(R.string.player_buffering));
            else if (showShortPlaybackPosition)
                playPauseButton.setText(getString(R.string.player_label_short,
                        formattedPosition, formattedDuration));
            else
                playPauseButton.setText(getString(playing ? R.string.pause : R.string.resume)
                        + " " + getString(R.string.player_label, formattedPosition,
                        formattedDuration));

            playPauseButton.setBackgroundResource(playing ?
                    R.drawable.button_red : R.drawable.button_green);
            playPauseButton.setCompoundDrawablesWithIntrinsicBounds(buffering ?
                    R.drawable.ic_media_buffering : playing ?
                    R.drawable.ic_media_pause : R.drawable.ic_media_resume, 0, 0, 0);
        }
    }

    /**
     * Set whether the fragment should show the error view. You can call this
     * any time and can expect it to happen on resume at the latest.
     *
     * @param show Whether to show the player view.
     */
    public void setErrorViewVisibility(boolean show) {
        this.showError = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed()) {
            titleView.setVisibility(show ? GONE : showPlayerTitle ? VISIBLE : GONE);
            seekBar.setVisibility(show ? GONE : showPlayerSeekbar ? VISIBLE : GONE);
            if (rewindButton != null)
                rewindButton.setVisibility(show ? GONE : VISIBLE);
            playPauseButton.setVisibility(show ? GONE : VISIBLE);
            if (forwardButton != null)
                forwardButton.setVisibility(show ? GONE : VISIBLE);
            nextButton.setVisibility(show ? GONE : showNextButton ? VISIBLE : GONE);

            errorView.setVisibility(show ? VISIBLE : GONE);
        }
    }

    private void prepareTransportButton(ImageButton button, final boolean rewind) {
        // Button might not be present since some layouts (e.g. for small
        // screens) might not include it
        if (button != null) {
            // The long click listener starts regular rewind/forward actions as
            // long as the user keeps holding the button down
            button.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    transportationHandler.post(rewind ? rewindRunnable : forwardRunnable);
                    transportActive = true;

                    return false;
                }
            });

            // The click listener detects that the user let the button go. If it
            // had been long-clicked, we stop sending actions otherwise we run
            // the action once
            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (transportActive) {
                        transportationHandler.removeCallbacks(
                                rewind ? rewindRunnable : forwardRunnable);

                        transportActive = false;
                    } else {
                        // No long click, run once
                        if (rewind)
                            listener.onRewind();
                        else
                            listener.onFastForward();
                    }
                }
            });
        }
    }
}
