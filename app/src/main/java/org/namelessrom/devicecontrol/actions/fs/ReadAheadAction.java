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
package org.namelessrom.devicecontrol.actions.fs;

import android.text.TextUtils;

import org.namelessrom.devicecontrol.Logger;
import org.namelessrom.devicecontrol.actions.ActionProcessor;
import org.namelessrom.devicecontrol.actions.BaseAction;
import org.namelessrom.devicecontrol.database.DataItem;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.Constants;

public class ReadAheadAction extends BaseAction {

    public static final String NAME = "read_ahead";

    public int     id      = -1;
    public String  trigger = "";
    public String  value   = "";
    public boolean bootup  = false;

    public ReadAheadAction(final String value, final boolean bootup) {
        super();
        this.value = value;
        this.bootup = bootup;
    }

    @Override public String getName() { return NAME; }

    @Override public String getCategory() { return ActionProcessor.CATEGORY_GPU; }

    @Override public String getTrigger() { return trigger; }

    @Override public String getValue() { return value; }

    @Override public boolean getBootup() { return bootup; }

    @Override protected void setupAction() {
        // TODO: what?
    }

    @Override public void triggerAction() {
        if (TextUtils.isEmpty(value)) {
            Logger.wtf(this, "No value for action!");
            return;
        }

        final StringBuilder sb = new StringBuilder();
        int c = 0;
        for (final String readAheadPath : Constants.READ_AHEAD_PATH) {
            sb.append(Utils.getWriteCommand(readAheadPath, value));
            if (bootup) {
                PreferenceHelper.setBootup(new DataItem(DatabaseHandler.CATEGORY_EXTRAS,
                        "readahead" + (c++), readAheadPath, value));
            }
        }

        Utils.runRootCommand(sb.toString());
    }

}
