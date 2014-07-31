/*
 *  Copyright (C) 2013 - 2014 Alexander "Evisceration" Martinz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.namelessrom.devicecontrol.utils;

import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.R;

/**
 * Helps with ddddddrawwabllessr5hhwr5hbwb
 */
public class DrawableHelper {

    public static void applyColorFilter(final Drawable d, final int color, final boolean isRes) {
        applyColorFilter(d, isRes ? Application.getColor(color) : color);
    }

    public static void applyColorFilter(final Drawable drawable, final int color) {
        final LightingColorFilter lightingColorFilter = new LightingColorFilter(Color.BLACK, color);
        drawable.setColorFilter(lightingColorFilter);
    }

    public static void applyAccentColorFilter(final Drawable drawable) {
        applyColorFilter(drawable,
                PreferenceHelper.getInt("pref_color", Application.getColor(R.color.accent)));
    }

}
