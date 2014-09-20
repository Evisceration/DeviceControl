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
package org.namelessrom.devicecontrol.fragments.device;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateFormat;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.Logger;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.objects.CpuInfo;
import org.namelessrom.devicecontrol.objects.KernelInfo;
import org.namelessrom.devicecontrol.preferences.CustomPreference;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.views.AttachPreferenceFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class DeviceInformationFragment extends AttachPreferenceFragment implements DeviceConstants {

    private static final String KEY_PLATFORM_VERSION = "platform_version";
    private static final String KEY_ANDROID_ID       = "android_id";

    private long[] mHits = new long[3];

    //==============================================================================================
    // Overridden Methods
    //==============================================================================================

    @Override protected int getFragmentId() { return ID_DEVICE; }

    @Override public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.device_information);

        final SensorManager sensorManager =
                (SensorManager) Application.get().getSystemService(Context.SENSOR_SERVICE);

        // Platform
        PreferenceCategory category = (PreferenceCategory) findPreference("platform");

        addPreference(category, KEY_PLATFORM_VERSION, R.string.version, Build.VERSION.RELEASE)
                .setSelectable(true); // selectable because of the easter egg
        addPreference(category, "platform_id", R.string.build_id, Build.DISPLAY);
        addPreference(category, "platform_type", R.string.type,
                Build.VERSION.CODENAME + " " + Build.TYPE);
        addPreference(category, "platform_tags", R.string.tags, Build.TAGS);
        addPreference(category, "platform_build_date", R.string.build_date, getDate(Build.TIME));

        // Runtime
        category = (PreferenceCategory) findPreference("runtime");

        String summary = Utils.getCommandResult("getprop persist.sys.dalvik.vm.lib", "-");
        if (!TextUtils.equals(summary, "-")) {
            final String runtime = TextUtils.equals(summary, "libdvm.so")
                    ? "Dalvik" : TextUtils.equals(summary, "libart.so") ? "ART" : "-";
            summary = String.format("%s (%s)", runtime, summary);
        }
        addPreference(category, "vm_library", R.string.type, summary);
        addPreference(category, "vm_version", R.string.version,
                System.getProperty("java.vm.version", "-"));

        // Device
        category = (PreferenceCategory) findPreference("device_information");

        // TODO: save / restore / check --> ANDROID ID
        addPreference(category, KEY_ANDROID_ID, R.string.android_id, Utils.getAndroidId());
        addPreference(category, "device_manufacturer", R.string.manufacturer, Build.MANUFACTURER);
        addPreference(category, "device_model", R.string.model, Build.MODEL);
        addPreference(category, "device_product", R.string.product, Build.PRODUCT);
        addPreference(category, "device_board", R.string.board, Build.BOARD);
        addPreference(category, "device_bootloader", R.string.bootloader, Build.BOOTLOADER);
        addPreference(category, "device_radio_version", R.string.radio_version,
                Build.getRadioVersion());

        // Processor
        category = (PreferenceCategory) findPreference("processor");

        addPreference(category, "cpu_abi", R.string.cpu_abi, Build.CPU_ABI);
        addPreference(category, "cpu_abi2", R.string.cpu_abi2, Build.CPU_ABI2);
        new CpuInfoTask(category).execute();

        // Kernel
        category = (PreferenceCategory) findPreference("kernel");
        new KernelInfoTask(category).execute();

        // Sensors
        category = (PreferenceCategory) findPreference("sensors");

        // we need an array list to be able to sort it, a normal list throws
        // java.lang.UnsupportedOperationException when sorting
        final ArrayList<Sensor> sensorList =
                new ArrayList<Sensor>(sensorManager.getSensorList(Sensor.TYPE_ALL));

        Collections.sort(sensorList, new SortIgnoreCase());

        Preference preference;
        for (final Sensor s : sensorList) {
            preference = new CustomPreference(getActivity());
            preference.setTitle(s.getName());
            preference.setSummary(s.getVendor());
            preference.setSelectable(false);
            category.addPreference(preference);
        }

        if (category.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(category);
        }
    }

    private CustomPreference addPreference(final PreferenceCategory category, final String key,
            final int titleResId, final String summary) {
        final CustomPreference preference = new CustomPreference(getActivity());
        preference.setKey(key);
        preference.setTitle(titleResId);
        preference.setSummary(TextUtils.isEmpty(summary) ? getString(R.string.unknown) : summary);
        preference.setSelectable(false);
        category.addPreference(preference);
        return preference;
    }

    @Override public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            @NonNull final Preference preference) {
        if (TextUtils.equals(KEY_PLATFORM_VERSION, preference.getKey())) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
            mHits[mHits.length - 1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                Utils.runRootCommand("am start android/com.android.internal.app.PlatLogoActivity");
                preference.setSelectable(false);
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private class SortIgnoreCase implements Comparator<Sensor> {
        public int compare(final Sensor sensor1, final Sensor sensor2) {
            final String s1 = sensor1 != null ? sensor1.getName() : "";
            final String s2 = sensor2 != null ? sensor2.getName() : "";
            return s1.compareToIgnoreCase(s2);
        }
    }

    private String getDate(final long time) {
        final Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time);
        return DateFormat.format("dd-MM-yyyy", cal).toString();
    }

    private class CpuInfoTask extends AsyncTask<Void, Void, Boolean> {
        private final CpuInfo            cpuInfo;
        private final PreferenceCategory category;

        public CpuInfoTask(final PreferenceCategory category) {
            this.cpuInfo = new CpuInfo();
            this.category = category;
        }

        @Override protected Boolean doInBackground(Void... voids) {
            return cpuInfo.feedWithInformation();
        }

        @Override protected void onPostExecute(final Boolean success) {
            if (success && category != null) {
                Logger.i(this, cpuInfo.toString());
                addPreference(category, "cpu_hardware", R.string.hardware, cpuInfo.hardware);
                addPreference(category, "cpu_processor", R.string.processor, cpuInfo.processor);
                addPreference(category, "cpu_features", R.string.features, cpuInfo.features);
                addPreference(category, "cpu_bogomips", R.string.bogomips, cpuInfo.bogomips);
            }
        }
    }

    private class KernelInfoTask extends AsyncTask<Void, Void, Boolean> {
        private final KernelInfo         kernelInfo;
        private final PreferenceCategory category;

        public KernelInfoTask(final PreferenceCategory category) {
            this.kernelInfo = new KernelInfo();
            this.category = category;
        }

        @Override protected Boolean doInBackground(Void... voids) {
            return kernelInfo.feedWithInformation();
        }

        @Override protected void onPostExecute(final Boolean success) {
            if (success && category != null) {
                Logger.i(this, kernelInfo.toString());
                addPreference(category, "kernel_version", R.string.version,
                        String.format("%s %s", kernelInfo.version, kernelInfo.revision));
                addPreference(category, "kernel_extras", R.string.extras, kernelInfo.extras);
                addPreference(category, "kernel_gcc", R.string.toolchain, kernelInfo.gcc);
                addPreference(category, "kernel_date", R.string.build_date, kernelInfo.date);
                addPreference(category, "kernel_host", R.string.host, kernelInfo.host);
            }
        }
    }

}
