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

package com.podcatcher.deluxe.view.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.podcatcher.deluxe.R;

/**
 * A confirmation dialog for the user to make sure he/she really wants
 * downloaded episode files to be removed from the local storage. This fragment
 * will not survive context re-creation, but is dismissed
 * {@link Fragment#onPause()}.
 * <p>
 * <b>Register call-back:</b> The fragment will try to use the activity it is
 * part of as its listener. To make this work, the activity needs to implement
 * {@link OnDeleteDownloadsConfirmationListener}. Showing this fragment from
 * another context will <em>not</em> fail, but you need to use
 * {@link #setListener(OnDeleteDownloadsConfirmationListener)} to register and
 * override the call-back. Once the listener is called, the fragment will
 * auto-dismiss itself.
 * <p>
 * <b>Deletion count:</b> If you are removing multiple downloads, you might want
 * to use {@link #setArguments(Bundle)} with an integer for the number of
 * episodes set using the key {@link #EPISODE_COUNT_KEY}. This needs to be done
 * before showing the dialog. The default episode count is one (1).
 * </p>
 */
public class DeleteDownloadsConfirmationFragment extends DialogFragment {

    /**
     * Argument key for the downloads to delete count
     */
    public static final String EPISODE_COUNT_KEY = "episode_count";
    /**
     * The tag we identify our confirmation dialog fragment with
     */
    public static final String TAG = "confirm_download_delete";

    /**
     * The number episodes about to be deleted
     */
    private int episodeCount = 1;

    /**
     * Flag on whether our activity listens to us
     */
    private boolean autoDismissOnPause = false;

    /**
     * The callback we are working with
     */
    private OnDeleteDownloadsConfirmationListener listener;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        episodeCount = args.getInt(EPISODE_COUNT_KEY);

        // Cannot be negative or zero
        if (episodeCount < 1)
            episodeCount = 1;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Let's see whether the activity implements our call-back, we will only
        // pick it if the listener is not yet set:
        if (listener == null)
            try {
                this.listener = (OnDeleteDownloadsConfirmationListener) activity;
            } catch (ClassCastException e) {
                // Our activity does not listen to us, so we want to dismiss the
                // fragment when it pauses since the listener is likely to be
                // gone onRestart()
                autoDismissOnPause = true;
            }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String title = getResources()
                .getQuantityString(R.plurals.downloads_remove_title, episodeCount, episodeCount);
        final String message = getResources()
                .getQuantityString(R.plurals.downloads_remove_text, episodeCount, episodeCount);

        // Define context to use (parent activity might have no theme)
        final ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), R.style.AppDialog);

        // Inflate our custom view
        final LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        final View content = inflater.inflate(R.layout.confirm, null);

        // Set message
        final TextView messageTextView = (TextView) content.findViewById(R.id.message);
        messageTextView.setText(message);

        // Add click listeners
        final Button confirmButton = (Button) content.findViewById(R.id.confirm_button);
        confirmButton.setText(R.string.remove);
        confirmButton.setBackgroundResource(R.drawable.button_red);
        confirmButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onConfirmDeletion();

                dismiss();
            }
        });
        final Button cancelButton = (Button) content.findViewById(R.id.cancel_button);
        cancelButton.setBackgroundResource(R.drawable.button_green);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onCancel(DeleteDownloadsConfirmationFragment.this.getDialog());
                dismiss();
            }
        });

        // Build the dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setView(content);

        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (listener != null)
            listener.onCancelDeletion();
    }

    @Override
    public void onPause() {
        super.onPause();

        // We auto-dismiss here, because the fragment should not survive
        // configuration changes when the activity does not implement our
        // listener
        if (autoDismissOnPause)
            dismiss();
    }

    /**
     * Register the callback. This will override any existing listener,
     * including the owning activity that might have been or will be set as the
     * call-back {@link Fragment#onAttach(Activity)}. Setting the listener using
     * this method will cause the fragment to auto-dismiss {@link #onPause()}.
     *
     * @param listener Listener to call on user action.
     */
    public void setListener(OnDeleteDownloadsConfirmationListener listener) {
        this.listener = listener;
        this.autoDismissOnPause = true;
    }

    /**
     * The callback definition for the dialog that confirms deletion of
     * downloaded episodes.
     */
    public interface OnDeleteDownloadsConfirmationListener {

        /**
         * Called on the listener if the user confirmed the deletion.
         */
        public void onConfirmDeletion();

        /**
         * Called on the listener if the user cancelled the deletion.
         */
        public void onCancelDeletion();
    }
}
