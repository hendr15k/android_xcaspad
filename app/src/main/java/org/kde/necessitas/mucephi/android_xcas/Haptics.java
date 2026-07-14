/*
 *  Copyright (C) 2011 Leonel Hernández Sandoval.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.kde.necessitas.mucephi.android_xcas;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Small wrapper around {@link Vibrator} that gracefully handles API differences.
 * <p>
 * Feedback is intentionally light so it does not feel jarring when the user
 * chains many computations.
 */
public final class Haptics {

    private static final long SUCCESS_DURATION_MS = 18;
    private static final long ERROR_DURATION_MS = 60;

    private Haptics() {
    }

    public static void success(Context context) {
        perform(context, SUCCESS_DURATION_MS, false);
    }

    public static void error(Context context) {
        perform(context, ERROR_DURATION_MS, true);
    }

    private static void perform(Context context, long durationMs, boolean pattern) {
        if (context == null) {
            return;
        }
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int amplitude = pattern ? 200 : 120;
                VibrationEffect effect = VibrationEffect.createOneShot(durationMs, amplitude);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(durationMs);
            }
        } catch (Exception ignored) {
        }
    }
}
