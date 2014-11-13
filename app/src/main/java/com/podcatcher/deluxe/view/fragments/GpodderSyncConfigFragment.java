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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.sync.gpodder.GpodderSyncController;

/**
 * The gpodder.net user config dialog. Let's the user supply a name/password
 * combination and select a device id.
 * <p/>
 * <b>Register call-back:</b> The fragment will use the activity it is part of
 * as its listener. To make this work, the activity needs to implement
 * {@link ConfigureGpodderSyncDialogListener}.
 * <p/>
 */
public class GpodderSyncConfigFragment extends DialogFragment {

    /**
     * The username text view
     */
    private EditText usernameEditText;
    /**
     * The password text view
     */
    private EditText passwordEditText;
    /**
     * The device id label view
     */
    private TextView deviceIdLabel;
    /**
     * The device id text view
     */
    private EditText deviceIdEditText;
    /**
     * The login checking progress view
     */
    private View loginCheckProgressView;
    /**
     * The login failed text view
     */
    private View loginFailedTextView;
    /**
     * The submit button
     */
    private Button submitButton;

    /**
     * Resources we need
     */
    private String deviceIdLabelText;
    private int deviceIdValidColor;
    private int deviceIdInvalidColor;

    /**
     * The callback we are working with
     */
    private ConfigureGpodderSyncDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (ConfigureGpodderSyncDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ConfigureGpodderSyncDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Define context to use (parent activity might have no theme)
        final ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), R.style.AppDialog);

        // Inflate our custom view
        final LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        final View content = inflater.inflate(R.layout.sync_gpodder_config, null);

        // Get a few resources
        deviceIdLabelText = getString(R.string.sync_gpodder_device_id);
        deviceIdValidColor = getResources().getColor(R.color.text_secondary);
        deviceIdInvalidColor = getResources().getColor(R.color.text_error);

        // We read the text field presets from the preferences
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Find the widgets and pre-fill them
        this.usernameEditText = (EditText) content.findViewById(R.id.username);
        usernameEditText.setText(preferences.getString(GpodderSyncController.USERNAME_KEY, ""));
        this.passwordEditText = (EditText) content.findViewById(R.id.password);
        passwordEditText.setTypeface(Typeface.SANS_SERIF);
        passwordEditText.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (EditorInfo.IME_ACTION_SEND == actionId)
                    submitConfiguration();

                return EditorInfo.IME_ACTION_SEND == actionId;
            }
        });
        this.loginCheckProgressView = content.findViewById(R.id.check_login_progress);
        this.loginFailedTextView = content.findViewById(R.id.login_failed);
        this.deviceIdLabel = (TextView) content.findViewById(R.id.device_id_label);
        // We add this as a trick, if you know what you are doing and really
        // want to change the device id after it has been set initially
        deviceIdLabel.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                preferences.edit().remove(GpodderSyncController.DEVICE_ID_KEY).apply();
                deviceIdEditText.setEnabled(true);

                return true;
            }
        });
        this.deviceIdEditText = (EditText) content.findViewById(R.id.device_id);
        // Pre-fill the device ID and disable editing if already set
        deviceIdEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkDeviceId(!preferences.contains(GpodderSyncController.DEVICE_ID_KEY));
            }
        });
        deviceIdEditText.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (EditorInfo.IME_ACTION_SEND == actionId)
                    submitConfiguration();

                return EditorInfo.IME_ACTION_SEND == actionId;
            }
        });

        // Add click listeners
        this.submitButton = (Button) content.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                submitConfiguration();
            }
        });
        final Button cancelButton = (Button) content.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onCancel(getDialog());
                dismiss();
            }
        });

        // Set the device id
        deviceIdEditText.setText(preferences.getString(GpodderSyncController.DEVICE_ID_KEY,
                GpodderSyncController.getDefaultDeviceId(getActivity())));

        // Build the dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.sync_gpodder_config_title)
                .setView(content);

        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        listener.onCancel(dialog);
    }

    /**
     * Show/hide the progress bar and the login failed text.
     *
     * @param showBar   Whether the progress bar should be shown.
     * @param showError Whether the error text should be shown, setting this to
     *                  <code>true</code> will hide the progress bar.
     */
    public void showProgress(boolean showBar, boolean showError) {
        if (loginCheckProgressView != null)
            loginCheckProgressView.setVisibility(showError ? View.GONE :
                    showBar ? View.VISIBLE : View.INVISIBLE);

        if (loginFailedTextView != null)
            loginFailedTextView.setVisibility(showError ? View.VISIBLE : View.GONE);
    }

    private void submitConfiguration() {
        final String username = usernameEditText.getText().toString();
        final String password = passwordEditText.getText().toString();
        final String deviceId = deviceIdEditText.getText().toString();

        listener.onSubmitConfiguration(username, password, deviceId);
    }

    private void checkDeviceId(boolean enableInputIfValid) {
        // Make sure the device id is okay
        final boolean valid = deviceIdEditText.getText().toString().matches("[\\w][\\w.-]+");

        deviceIdLabel.setText(deviceIdLabelText + (valid ? "" : " (A-Z, a-z, .-_)"));
        deviceIdLabel.setTextColor(valid ? deviceIdValidColor : deviceIdInvalidColor);
        deviceIdEditText.setEnabled(!valid || enableInputIfValid);

        submitButton.setEnabled(valid);
    }

    /**
     * The callback definition, needs to implemented by the activity showing
     * this dialog.
     */
    public interface ConfigureGpodderSyncDialogListener extends OnCancelListener {
        /**
         * Called on the listener if the user submitted a gpodder.net
         * configuration.
         *
         * @param username User name entered.
         * @param password Password entered.
         * @param deviceId Device ID to use for gpodder.net.
         */
        public void onSubmitConfiguration(String username, String password, String deviceId);
    }
}
