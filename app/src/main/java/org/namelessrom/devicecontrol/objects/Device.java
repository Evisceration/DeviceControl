/*
 *  Copyright (C) 2014 Alexander "Evisceration" Martinz
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
package org.namelessrom.devicecontrol.objects;

import android.os.Build;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.namelessrom.devicecontrol.utils.Utils;

public class Device {

    @SerializedName("platform_version") public final String platformVersion;
    @SerializedName("platform_id") public final String platformId;
    @SerializedName("platform_type") public final String platformType;
    @SerializedName("platform_tags") public final String platformTags;
    @SerializedName("platform_build_date") public final String platformBuildType;

    @SerializedName("vm_library") public final String vmLibrary;
    @SerializedName("vm_version") public final String vmVersion;

    @SerializedName("android_id") public final String androidId;
    @SerializedName("device_manufacturer") public final String manufacturer;
    @SerializedName("device_model") public final String model;
    @SerializedName("device_product") public final String product;
    @SerializedName("device_board") public final String board;
    @SerializedName("device_bootloader") public final String bootloader;
    @SerializedName("device_radio_version") public final String radio;

    private static Device sInstance;

    private Device() {
        platformVersion = Build.VERSION.RELEASE;
        platformId = Build.DISPLAY;
        platformType = Build.VERSION.CODENAME + " " + Build.TYPE;
        platformTags = Build.TAGS;
        platformBuildType = Utils.getDate(Build.TIME);

        vmVersion = System.getProperty("java.vm.version", "-");
        vmLibrary = getRuntime();

        androidId = Utils.getAndroidId();
        manufacturer = Build.MANUFACTURER;
        model = Build.MODEL;
        product = Build.PRODUCT;
        board = Build.BOARD;
        bootloader = Build.BOOTLOADER;
        radio = Build.getRadioVersion();
    }

    public static Device get() {
        if (sInstance == null) {
            sInstance = new Device();
        }
        return sInstance;
    }

    private String getRuntime() {
        // check the vm lib
        String tmp = Utils.getCommandResult("getprop persist.sys.dalvik.vm.lib", "-");
        if (TextUtils.equals(tmp, "-")) {
            // if we do not get a result, try falling back to the new property (API 21+)
            tmp = Utils.getCommandResult("getprop persist.sys.dalvik.vm.lib.2", "-");
        }

        if (TextUtils.equals(tmp, "-")) {
            // if we still did not get a result, lets cheat a bit.
            // we know that ART starts with vm version 2.x
            tmp = vmVersion.startsWith("1") ? "libdvm.so" : "libart.so";
        }

        final String runtime = TextUtils.equals(tmp, "libdvm.so")
                ? "Dalvik" : TextUtils.equals(tmp, "libart.so") ? "ART" : "-";
        tmp = String.format("%s (%s)", runtime, tmp);

        return tmp;
    }
}
