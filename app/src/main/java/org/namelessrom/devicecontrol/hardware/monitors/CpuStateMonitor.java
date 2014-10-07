/*
 * Performance Control - An Android CPU Control application
 * Copyright (C) Brandon Valosek, 2011 <bvalosek@gmail.com>
 * Copyright (C) Modified by 2012 James Roberts
 * Copyright (C) Modified 2013 - 2014 by Alexander "Evisceration" Martinz
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.namelessrom.devicecontrol.hardware.monitors;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.hardware.CpuUtils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

public class CpuStateMonitor implements DeviceConstants {

    private final ArrayList<CpuState> mStates = new ArrayList<CpuState>();

    private static CpuStateMonitor mCpuStateMonitor;

    private static CpuUtils.StateListener mListener;

    private CpuStateMonitor(final CpuUtils.StateListener listener) { mListener = listener; }

    public static CpuStateMonitor getInstance(final CpuUtils.StateListener listener) {
        if (mCpuStateMonitor == null) {
            mCpuStateMonitor = new CpuStateMonitor(listener);
        }
        return mCpuStateMonitor;
    }

    public class CpuState implements Comparable<CpuState> {
        public final int  freq;
        public final long duration;

        public CpuState(final int freq, final long duration) {
            this.freq = freq;
            this.duration = duration;
        }

        public int compareTo(@NonNull final CpuState state) {
            final Integer a = freq;
            final Integer b = state.freq;
            return a.compareTo(b);
        }
    }

    private long getTotalStateTime(final ArrayList<CpuState> states) {
        long sum = 0;
        for (final CpuState state : states) {
            sum += state.duration;
        }
        return (sum < 0 ? 0 : sum);
    }

    public void updateStates() throws IOException {
        if (mListener == null) return;

        InputStream is = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        try {
            is = new FileInputStream(CpuUtils.FREQ_TIME_IN_STATE_PATH);
            ir = new InputStreamReader(is);
            br = new BufferedReader(ir);
            mStates.clear();
            readInStates(br);
        } finally {
            if (br != null) br.close();
            if (ir != null) ir.close();
            if (is != null) is.close();
        }

        final long deepSleep = ((SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()) / 10);
        mStates.add(new CpuState(0, (deepSleep < 0 ? 0 : deepSleep)));

        Collections.sort(mStates, Collections.reverseOrder());

        Application.HANDLER.post(new Runnable() {
            @Override
            public void run() {
                mListener.onStates(new CpuUtils.State(mStates, getTotalStateTime(mStates)));
            }
        });
    }

    private void readInStates(final BufferedReader br) throws IOException {
        String line;
        String[] nums;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                nums = line.split(" ");
                mStates.add(new CpuState(Integer.parseInt(nums[0]), Long.parseLong(nums[1])));
            }
        }
    }
}
