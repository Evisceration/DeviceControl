/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2013-2014 Alexander "Evisceration" Martinz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.namelessrom.devicecontrol.utils;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;

import com.stericson.roottools.RootTools;
import com.stericson.roottools.execution.CommandCapture;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.events.ShellOutputEvent;
import org.namelessrom.devicecontrol.services.TaskerService;
import org.namelessrom.devicecontrol.utils.cmdprocessor.CMDProcessor;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.constants.FileConstants;
import org.namelessrom.devicecontrol.utils.providers.BusProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.namelessrom.devicecontrol.Application.logDebug;

public class Utils implements DeviceConstants, FileConstants {

    public static boolean isNameless() {
        return existsInFile(Scripts.BUILD_PROP, "ro.nameless.version");
    }

    public static boolean isGmsInstalled() { return isPackageInstalled("com.android.vending"); }

    public static boolean existsInFile(final String file, final String prop) {
        return !findPropValue(file, prop).isEmpty();
    }

    public static String findPropValue(final String file, final String prop) {
        String value;
        try {
            value = findPropValueOf(file, prop);
        } catch (Exception e) { value = ""; }

        return value;
    }

    private static String findPropValueOf(final String file, final String prop)
            throws Exception {
        final File f = new File(file);
        if (f.exists() && f.canRead()) {
            final BufferedReader br =
                    new BufferedReader(new InputStreamReader(new FileInputStream(f)));

            String s;
            while ((s = br.readLine()) != null) {
                if (s.contains(prop)) return s.replace(prop + '=', "");
            }

            br.close();
        }

        return "";
    }

    /**
     * Reads a single line from a file.
     *
     * @param sFile The files to read from.
     * @return The read string OR null if not existing.
     */
    public static String readOneLine(String sFile) {
        String sLine = null;
        if (fileExists(sFile)) {
            BufferedReader brBuffer;
            try {
                brBuffer = new BufferedReader(new FileReader(sFile), 512);
                try {
                    sLine = brBuffer.readLine();
                } finally {
                    brBuffer.close();
                }
            } catch (Exception e) {
                return readFileViaShell(sFile);
            }
        }
        return sLine;
    }

    /**
     * Fallback if everything fails
     *
     * @param filePath The file to read
     * @return The file's content
     */
    public static String readFileViaShell(String filePath) {
        return readFileViaShell(filePath, true);
    }

    public static String readFileViaShell(String filePath, boolean useSu) {
        final String command = "cat " + filePath;
        return useSu ? CMDProcessor.runSuCommand(command).getStdout()
                : CMDProcessor.runShellCommand(command).getStdout();
    }

    /**
     * Write a string value to the specified file.
     *
     * @param filename The filename
     * @param value    The value
     */
    public static void writeValue(final String filename, final String value) {
        if (fileExists(filename)) {
            try {
                final FileWriter fw = new FileWriter(filename);
                try {
                    fw.write(value);
                } finally {
                    fw.close();
                }
            } catch (IOException ignored) { writeValueViaShell(filename, value); }
        }
    }

    /**
     * Fallback if everything fails
     *
     * @param filename The file to write
     * @param value    The value to write
     */
    private static void writeValueViaShell(final String filename, final String value) {
        runRootCommand(Utils.getWriteCommand(filename, value));
    }

    /**
     * Check if the specified file exists.
     *
     * @param filename The filename
     * @return Whether the file exists or not
     */
    public static boolean fileExists(final String filename) {
        return new File(filename).exists();
    }

    /**
     * Checks if the given paths in a string array are existing and returns the existing path.
     *
     * @param paths The string array, containing the file paths
     * @return The path of the existing file as string
     */
    public static String checkPaths(final String[] paths) {
        for (final String s : paths) {
            if (fileExists(s)) { return s; }
        }
        return "";
    }

    /**
     * Setup the directories for Device Control
     */
    public static void setupDirectories() {
        final String basePath = Application.getFilesDirectory();
        final String logDir = basePath + DC_LOG_DIR;
        final String backupDir = basePath + DC_BACKUP_DIR;
        final String[] dirList = new String[]{logDir, backupDir};
        File dir;
        for (String s : dirList) {
            dir = new File(s);
            if (!dir.exists()) {
                logDebug("setupDirectories: creating " + s);
                final boolean isSuccess = dir.mkdirs();
                logDebug("setupDirectories: " + (isSuccess ? "true" : "false"));
            }
        }
    }

    /**
     * Reads string array from file
     *
     * @param path File to read from
     * @return string array
     */
    public static String[] readStringArray(final String path) {
        final String line = readOneLine(path);
        if (line != null) {
            return line.split(" ");
        }
        return null;
    }

    public static boolean getCommandResult(final String command) {
        return new CMDProcessor().su.runWaitFor(command).success();
    }

    public static void runRootCommand(final String command) {
        final CommandCapture comm = new CommandCapture(0, false, command);
        try {
            RootTools.getShell(true).add(comm);
        } catch (Exception e) {
            logDebug("runRootCommand: " + e.getMessage());
        }
    }

    public static void getCommandResult(final int ID, final String COMMAND) {
        getCommandResult(ID, COMMAND, null, false);
    }

    public static void getCommandResult(final int ID, final String COMMAND, final String EXTRAS) {
        getCommandResult(ID, COMMAND, EXTRAS, false);
    }

    public static void getCommandResult(final int ID, final String COMMAND, final String EXTRAS,
            final boolean NEWLINE) {
        final StringBuilder sb = new StringBuilder();
        final CommandCapture comm = new CommandCapture(0, false, COMMAND) {
            @Override
            public void commandOutput(int id, String line) {
                sb.append(line);
                if (NEWLINE) {
                    sb.append('\n');
                }
            }

            @Override
            public void commandCompleted(int id, int exitcode) {
                final String result = sb.toString();
                logDebug(String.format("Generic Output for %s: %s", String.valueOf(ID), result));
                Application.HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        BusProvider.getBus().post(new ShellOutputEvent(ID, result, EXTRAS));
                    }
                });
            }
        };
        try {
            RootTools.getShell(true).add(comm);
        } catch (Exception e) {
            logDebug("runRootCommand: " + e.getMessage());
        }
    }

    public static String getReadCommand(final String path) {
        return String.format("cat %s 2> /dev/null", path);
    }

    public static String getWriteCommand(final String path, final String value) {
        return String.format("chmod 644 %s;\n", path) +
                String.format("busybox echo \"%s\" > %s;\n", value, path);
    }

    public static boolean isPackageInstalled(final String packageName) {
        try {
            final PackageManager pm = Application.applicationContext.getPackageManager();
            if (pm != null) {
                pm.getPackageInfo(packageName, 0);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static void disableComponent(final String packageName, final String componentName) {
        toggleComponent(packageName, componentName, true);
    }

    public static void enableComponent(final String packageName, final String componentName) {
        toggleComponent(packageName, componentName, false);
    }

    private static void toggleComponent(final String packageName, final String componentName,
            boolean disable) {
        final ComponentName component = new ComponentName(packageName,
                packageName + componentName);
        final PackageManager pm = Application.applicationContext.getPackageManager();
        if (pm != null) {
            pm.setComponentEnabledSetting(component,
                    (disable
                            ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_ENABLED),
                    PackageManager.DONT_KILL_APP
            );
        }
    }

    public static void startTaskerService() {
        final String packageName = Application.applicationContext.getPackageName();
        final String componentName = TaskerService.class.getName().replace(packageName, "");
        final DatabaseHandler db = DatabaseHandler.getInstance(Application.applicationContext);
        if (db.getTableCount(DatabaseHandler.TABLE_TASKER) > 0) {
            enableComponent(packageName, componentName);
            final Intent tasker = new Intent(Application.applicationContext, TaskerService.class);
            tasker.setAction(TaskerService.ACTION_START);
            Application.applicationContext.startService(tasker);
            logDebug("Service Started: " + componentName);
        } else {
            final Intent tasker = new Intent(Application.applicationContext, TaskerService.class);
            tasker.setAction(TaskerService.ACTION_STOP);
            Application.applicationContext.startService(tasker);
            disableComponent(packageName, componentName);
            logDebug("Service NOT Started: " + componentName);
        }
    }

    public static boolean isEnabled(String s) {
        if (s != null) {
            s = s.trim().toUpperCase();
            return s.equals("Y") || s.equals("1");
        } else { return false; }
    }

    public static String getBatteryHealth(final int healthInt) {
        String health;

        switch (healthInt) {
            case BatteryManager.BATTERY_HEALTH_COLD:
                health = "cold";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                health = "good";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                health = "dead";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                health = "overvoltage";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                health = "overheat";
                break;
            default:
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                health = "unknown";
                break;
        }

        return health;
    }

    public static void remount(final String path, final String mode) {
        try {
            RootTools.remount(path, mode);
        } catch (Exception e) {
            logDebug(String.format("Could not remount %s with options \"%s\", error: %s",
                    path, mode, e));
        }
    }

}
