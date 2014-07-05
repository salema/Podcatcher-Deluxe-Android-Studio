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

package com.podcatcher.deluxe.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import static com.podcatcher.deluxe.services.PlayEpisodeService.ACTION_PAUSE;

/**
 * Our audio becoming noisy receiver. Simply send a pause intent to the service.
 */
public class BecomingNoisyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Only react if this actually is a become noisy event
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            try {
                context.startService(new Intent(ACTION_PAUSE));
            } catch (SecurityException se) {
                // This might happen if called from the outside since our
                // service is not exported, just do nothing.
            }
    }
}
