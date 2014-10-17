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
package org.namelessrom.devicecontrol.hardware;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.text.TextUtils;

import com.stericson.roottools.RootTools;
import com.stericson.roottools.execution.CommandCapture;
import com.stericson.roottools.execution.Shell;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.Logger;
import org.namelessrom.devicecontrol.database.DataItem;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GpuUtils implements Constants {

    public static class Gpu {
        public final String[] available;
        public final String   max;
        public final String   governor;

        public Gpu(final String[] availFreqs, final String maxFreq, final String gov) {
            available = availFreqs;
            max = maxFreq;
            governor = gov;
        }
    }

    public interface GpuListener {
        public void onGpu(final Gpu gpu);
    }

    private static GpuUtils sInstance;

    private GpuUtils() { }

    public static GpuUtils get() {
        if (sInstance == null) {
            sInstance = new GpuUtils();
        }
        return sInstance;
    }

    public String[] getAvailableFrequencies(final boolean sorted) {
        final String freqsRaw = Utils.readOneLine(GPU_FREQUENCIES_FILE);
        if (freqsRaw != null && !freqsRaw.isEmpty()) {
            final String[] freqs = freqsRaw.split(" ");
            if (!sorted) {
                return freqs;
            }
            Arrays.sort(freqs, new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                    return Utils.tryValueOf(object1, 0).compareTo(Utils.tryValueOf(object2, 0));
                }
            });
            Collections.reverse(Arrays.asList(freqs));
            return freqs;
        }
        return null;
    }

    public boolean containsGov(final String gov) {
        for (final String s : GPU_GOVS) {
            if (gov.toLowerCase().equals(s.toLowerCase())) { return true; }
        }
        return false;
    }

    public void getGpu(final GpuListener listener) {
        try {
            final Shell mShell = RootTools.getShell(true);
            if (mShell == null) { throw new Exception("Shell is null"); }

            final StringBuilder cmd = new StringBuilder();
            cmd.append("command=$(");
            cmd.append("cat ").append(GPU_FREQUENCIES_FILE).append(" 2> /dev/null;");
            cmd.append("echo -n \"[\";");
            cmd.append("cat ").append(GPU_MAX_FREQ_FILE).append(" 2> /dev/null;");
            cmd.append("echo -n \"]\";");
            cmd.append("cat ").append(GPU_GOV_PATH).append(" 2> /dev/null;");
            cmd.append(");").append("echo $command | tr -d \"\\n\"");
            Logger.v(GpuUtils.class, cmd.toString());

            final StringBuilder outputCollector = new StringBuilder();
            final CommandCapture cmdCapture = new CommandCapture(0, false, cmd.toString()) {
                @Override
                public void commandOutput(int id, String line) {
                    outputCollector.append(line);
                    Logger.v(GpuUtils.class, line);
                }

                @Override
                public void commandCompleted(int id, int exitcode) {
                    final List<String> result =
                            Arrays.asList(outputCollector.toString().split(" "));
                    final List<String> tmpList = new ArrayList<String>();
                    String tmpMax = "", tmpGov = "";

                    for (final String s : result) {
                        if (TextUtils.isEmpty(s)) {
                            Logger.w(GpuUtils.class, "empty");
                            continue;
                        }
                        if (s.charAt(0) == '[') {
                            tmpMax = s.substring(1, s.length());
                        } else if (s.charAt(0) == ']') {
                            tmpGov = s.substring(1, s.length());
                        } else {
                            tmpList.add(s);
                        }
                    }

                    final String[] avail = tmpList.toArray(new String[tmpList.size()]);
                    final String max = tmpMax;
                    final String gov = tmpGov;
                    Application.HANDLER.post(new Runnable() {
                        @Override public void run() {
                            listener.onGpu(new Gpu(avail, max, gov));
                        }
                    });

                }
            };

            if (mShell.isClosed()) { throw new Exception("Shell is closed"); }
            mShell.add(cmdCapture);
        } catch (Exception exc) {
            Logger.e(GpuUtils.class, String.format("Error: %s", exc.getMessage()));
        }
    }

    public String restore() {
        final StringBuilder sbCmd = new StringBuilder();

        final List<DataItem> items = DatabaseHandler.getInstance().getAllItems(
                DatabaseHandler.TABLE_BOOTUP, DatabaseHandler.CATEGORY_GPU);
        for (final DataItem item : items) {
            sbCmd.append(Utils.getWriteCommand(item.getFileName(), item.getValue()));
        }

        return sbCmd.toString();
    }

    public static String toMhz(final String mhz) {
        int mhzInt;
        try {
            mhzInt = Utils.parseInt(mhz);
        } catch (Exception exc) {
            Logger.e(GpuUtils.get(), exc.getMessage());
            mhzInt = 0;
        }
        return (String.valueOf(mhzInt / 1000000) + " MHz");
    }

    public static String fromMHz(final String mhzString) {
        if (mhzString != null && !mhzString.isEmpty()) {
            try {
                return String.valueOf(Utils.parseInt(mhzString.replace(" MHz", "")) * 1000000);
            } catch (Exception exc) {
                Logger.e(GpuUtils.get(), exc.getMessage());
            }
        }
        return "0";
    }

    public static String[] freqsToMhz(final String[] frequencies) {
        final int length = frequencies.length;
        final String[] names = new String[length];

        for (int i = 0; i < length; i++) {
            names[i] = toMhz(frequencies[i]);
        }

        return names;
    }

    public static boolean isOpenGLES20Supported() {
        final ActivityManager am = (ActivityManager)
                Application.get().getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return (info != null && info.reqGlEsVersion >= 0x20000);
    }

}
