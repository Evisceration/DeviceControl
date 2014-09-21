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
package org.namelessrom.devicecontrol;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;
import com.stericson.roottools.RootTools;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Scripts;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.wizard.AddTaskActivity;

import java.io.File;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formKey = "",
        formUri = "https://reports.nameless-rom.org" +
                "/acra-devicecontrol/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "namelessreporter",
        formUriBasicAuthPassword = "weareopentoeveryone",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogOkToast = R.string.crash_dialog_ok_toast)
public class Application extends android.app.Application implements DeviceConstants {

    public static final Handler HANDLER = new Handler();

    public static boolean IS_NAMELESS = false;

    private static Application sInstance;

    @Override public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        Application.sInstance = this;

        DatabaseHandler.getInstance();
        Logger.setEnabled(PreferenceHelper.getBoolean(EXTENSIVE_LOGGING, false));

        if (Utils.existsInFile(Scripts.BUILD_PROP, "ro.nameless.debug=1")) {
            // setup thread policy
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .detectCustomSlowCalls()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());

            // setup vm policy
            final StrictMode.VmPolicy.Builder vmpolicy = new StrictMode.VmPolicy.Builder();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                vmpolicy.detectLeakedRegistrationObjects();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    vmpolicy.detectFileUriExposure();
                }
            }
            vmpolicy
                    .detectAll()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .setClassInstanceLimit(AddTaskActivity.class, 100)
                    .penaltyLog();
            StrictMode.setVmPolicy(vmpolicy.build());

            // enable debug mode at root tools
            RootTools.debugMode = true;
        }

        IS_NAMELESS = Utils.isNameless();
        Logger.v(this, String.format("is nameless: %s", IS_NAMELESS));

        final boolean showLauncher =
                PreferenceHelper.getBoolean(SHOW_LAUNCHER, true) || !Application.IS_NAMELESS;
        toggleLauncherIcon(showLauncher);

        try {
            DB snappydb = DBFactory
                    .open(this); //create or open an existing databse using the default name

            snappydb.put("name", "Jack Reacher");
            snappydb.putInt("age", 42);
            snappydb.putBoolean("single", true);
            snappydb.put("books", new String[]{"One Shot", "Tripwire", "61 Hours"});

            String name = snappydb.get("name");
            int age = snappydb.getInt("age");
            boolean single = snappydb.getBoolean("single");
            String[] books = snappydb.getArray("books", String.class);// get array of string

            Logger.i(this, String.format("name: %s, age: %s, single: %s", name, age, single));
            for (final String s : books) {
                Logger.i(this, s);
            }

            snappydb.close();
        } catch (SnappydbException snappyExc) {
            Logger.e(this, "snappyExc", snappyExc);
        }
    }


    @Override public void onTerminate() {
        // do some placebo :P
        DatabaseHandler.tearDown();
        super.onTerminate();
    }

    public static Application get() { return Application.sInstance; }

    @SuppressLint("SdCardPath") public String getFilesDirectory() {
        final File tmp = getFilesDir();
        if (tmp != null && tmp.isDirectory()) {
            return tmp.getPath();
        } else {
            return "/data/data/" + Application.get().getPackageName();
        }
    }

    public void toggleLauncherIcon(final boolean showLauncher) {
        if (Application.IS_NAMELESS) {
            final String pkg = "com.android.settings";
            final Resources res;
            try {
                res = getPackageManager().getResourcesForApplication(pkg);
            } catch (PackageManager.NameNotFoundException exc) {
                Logger.e(this, "You dont have settings? That's weird.", exc);
                Utils.enableComponent(getPackageName(), DummyLauncher.class.getName());
                return;
            }

            if (!showLauncher && res != null
                    && res.getIdentifier("device_control", "string", pkg) > 0) {
                Logger.v(this, "Implemented into system and showLauncher is not set!");
                Utils.disableComponent(getPackageName(), DummyLauncher.class.getName());
            } else {
                Logger.v(this, "Implemented into system and showLauncher is set!");
                Utils.enableComponent(getPackageName(), DummyLauncher.class.getName());
            }
        } else {
            Logger.v(this, "Not implemented into system!");
            Utils.enableComponent(getPackageName(), DummyLauncher.class.getName());
        }
    }

    public int getColor(final int resId) {
        return getResources().getColor(resId);
    }

    public String[] getStringArray(final int resId) {
        return getResources().getStringArray(resId);
    }
}
