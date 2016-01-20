/**
 * Copyright 2012-2016 Kevin Hausmann
 * <p/>
 * This file is part of Podcatcher Deluxe.
 * <p/>
 * Podcatcher Deluxe is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * <p/>
 * Podcatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Podcatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package com.podcatcher.deluxe;


import com.podcatcher.deluxe.model.types.Episode;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;

import java.util.List;

/**
 * Cast to the big screen.
 */
public abstract class CastActivity extends BaseActivity implements DiscoveryManagerListener, ConnectableDeviceListener {

    public static final String TAG = "CAST";

    private DiscoveryManager discoveryManager;
    private ConnectableDevice castDevice;
    private LaunchSession launchSession;
    private MediaControl mediaControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO Move this to the Podcatcher app class?!
        DiscoveryManager.init(getApplicationContext());
        discoveryManager = DiscoveryManager.getInstance();
        discoveryManager.addListener(this);

        final CapabilityFilter episodeFilter = new CapabilityFilter(MediaPlayer.Play_Audio);
        discoveryManager.setCapabilityFilters(episodeFilter);

        discoveryManager.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        discoveryManager.removeListener(this);
    }

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        Log.d(TAG, "Device found: " + device);

        final DevicePicker picker = new DevicePicker(this);
        picker.getPickerDialog("Cast", new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                castDevice = (ConnectableDevice) parent.getItemAtPosition(position);
                castDevice.addListener(CastActivity.this);
                castDevice.connect();
            }
        }).show();
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {

    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {

    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {

    }

    @Override
    public void onDeviceReady(ConnectableDevice device) {
        Log.d(TAG, "Device ready: " + device);

        final Episode episode = selection.getEpisode();
        if (episode != null) {
            final MediaInfo mediaInfo = new MediaInfo.Builder(episode.getMediaUrl(), episode.getMediaType())
                    .setTitle(episode.getName())
                    .setDescription(episode.getDescription())
                    .setIcon(episode.getPodcast().getLogoUrl())
                    .build();

            castDevice.getCapability(MediaPlayer.class).playMedia(mediaInfo, false, new MediaPlayer.LaunchListener() {

                @Override
                public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                    launchSession = object.launchSession;
                    mediaControl = object.mediaControl;
                }

                @Override
                public void onError(ServiceCommandError error) {
                    Log.d(TAG, "Playback failed: " + error);
                }
            });
        }
    }

    @Override
    public void onDeviceDisconnected(ConnectableDevice device) {

    }

    @Override
    public void onPairingRequired(ConnectableDevice device, DeviceService service, DeviceService.PairingType pairingType) {

    }

    @Override
    public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {

    }

    @Override
    public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {

    }
}
