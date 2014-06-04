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
package org.namelessrom.devicecontrol.objects;

import static org.namelessrom.devicecontrol.Application.logDebug;

public class CpuCore {

    public final String mCore;
    public final int    mCoreMax;
    public final int    mCoreCurrent;
    public final String mCoreGov;

    public CpuCore(final String core, final int coreCurrent,
            final int coreMax, final String coreGov) {
        mCore = ((core != null && !core.isEmpty()) ? core : "0");
        mCoreMax = coreMax;
        mCoreCurrent = coreCurrent;
        mCoreGov = ((coreGov != null && !coreGov.isEmpty()) ? coreGov : "0");
        logDebug("mCore: [" + mCore + ']');
        logDebug("mCoreMax: [" + mCoreMax + ']');
        logDebug("mCoreCurrent: [" + mCoreCurrent + ']');
        logDebug("mCoreGov: [" + mCoreGov + ']');
    }

}
