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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import org.namelessrom.devicecontrol.Logger;
import org.namelessrom.devicecontrol.MainActivity;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.database.DataItem;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.events.ShellOutputEvent;
import org.namelessrom.devicecontrol.listeners.OnShellOutputListener;
import org.namelessrom.devicecontrol.preferences.AwesomeCheckBoxPreference;
import org.namelessrom.devicecontrol.preferences.AwesomeListPreference;
import org.namelessrom.devicecontrol.preferences.CustomCheckBoxPreference;
import org.namelessrom.devicecontrol.preferences.CustomPreference;
import org.namelessrom.devicecontrol.preferences.VibratorTuningPreference;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.constants.FileConstants;
import org.namelessrom.devicecontrol.views.AttachPreferenceFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class DeviceFragment extends AttachPreferenceFragment
        implements DeviceConstants, FileConstants, Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener, OnShellOutputListener {

    private static final String FC_PATH = "/sys/kernel/fast_charge";

    //==============================================================================================
    // Input
    //==============================================================================================
    private CustomCheckBoxPreference  mGloveMode;
    private AwesomeCheckBoxPreference mAwesomeGloveMode;
    private AwesomeCheckBoxPreference mKnockOn;

    //==============================================================================================
    // Lights
    //==============================================================================================
    private AwesomeCheckBoxPreference mBacklightKey;
    private AwesomeCheckBoxPreference mBacklightNotification;
    private AwesomeCheckBoxPreference mKeyboardBacklight;

    //==============================================================================================
    // Display
    //==============================================================================================
    private AwesomeCheckBoxPreference mLcdPowerReduce;
    private AwesomeCheckBoxPreference mLcdSunlightEnhancement;
    private AwesomeCheckBoxPreference mLcdColorEnhancement;
    //----------------------------------------------------------------------------------------------
    private AwesomeListPreference mPanelColor;

    //==============================================================================================
    // Extras
    //==============================================================================================
    private AwesomeCheckBoxPreference mLoggerMode;
    private CustomPreference          mFastCharge;

    //==============================================================================================
    // Overridden Methods
    //==============================================================================================

    @Override protected int getFragmentId() { return ID_FEATURES; }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.device_features);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        PreferenceCategory category = (PreferenceCategory) findPreference("input_gestures");
        if (category != null) {
            mKnockOn = (AwesomeCheckBoxPreference) findPreference("knockon_gesture_enable");
            if (mKnockOn != null) {
                if (mKnockOn.isSupported()) {
                    try {
                        mKnockOn.initValue();
                    } catch (Exception ignored) { }
                    mKnockOn.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mKnockOn);
                }
            }

            if (category.getPreferenceCount() == 0) {
                preferenceScreen.removePreference(category);
            }
        }

        category = (PreferenceCategory) findPreference("input_others");
        if (category != null) {
            final VibratorTuningPreference pref =
                    (VibratorTuningPreference) findPreference("vibrator_tuning");
            if (pref != null && !VibratorTuningPreference.isSupported()) {
                category.removePreference(pref);
            }

            mAwesomeGloveMode = (AwesomeCheckBoxPreference) findPreference("input_glove_mode_aw");
            if (mAwesomeGloveMode.isSupported()) {
                mAwesomeGloveMode.initValue();
                mAwesomeGloveMode.setOnPreferenceChangeListener(this);
            } else {
                category.removePreference(mAwesomeGloveMode);
                mAwesomeGloveMode = null;
            }

            mGloveMode = (CustomCheckBoxPreference) findPreference("input_glove_mode");
            if (mGloveMode != null) {
                try {
                    // if we have already added a glove mode preference, remove it too
                    if (mAwesomeGloveMode == null || !isHtsSupported()) {
                        category.removePreference(mGloveMode);
                    } else {
                        final String value = DatabaseHandler.getInstance()
                                .getValueByName(mGloveMode.getKey(), DatabaseHandler.TABLE_BOOTUP);
                        final boolean enableGlove = (value != null && value.equals("1"));

                        enableHts(enableGlove);
                        mGloveMode.setOnPreferenceChangeListener(this);
                    }
                } catch (Exception exc) { category.removePreference(mGloveMode); }
            }

            if (category.getPreferenceCount() == 0) {
                preferenceScreen.removePreference(category);
            }
        }

        // LIGHTS

        category = (PreferenceCategory) findPreference("touchkey");
        if (category != null) {
            mBacklightKey = (AwesomeCheckBoxPreference) findPreference("touchkey_light");
            if (mBacklightKey != null) {
                if (mBacklightKey.isSupported()) {
                    mBacklightKey.initValue();
                    mBacklightKey.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mBacklightKey);
                }
            }

            mBacklightNotification = (AwesomeCheckBoxPreference) findPreference("touchkey_bln");
            if (mBacklightNotification != null) {
                if (mBacklightNotification.isSupported()) {
                    mBacklightNotification.initValue();
                    mBacklightNotification.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mBacklightNotification);
                }
            }

            mKeyboardBacklight = (AwesomeCheckBoxPreference) findPreference("keyboard_light");
            if (mKeyboardBacklight != null) {
                if (mKeyboardBacklight.isSupported()) {
                    mKeyboardBacklight.initValue();
                    mKeyboardBacklight.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mKeyboardBacklight);
                }
            }

            if (category.getPreferenceCount() == 0) {
                preferenceScreen.removePreference(category);
            }
        }

        // Display

        category = (PreferenceCategory) findPreference("graphics");
        if (category != null) {
            mPanelColor = (AwesomeListPreference) findPreference("panel_color_temperature");
            if (mPanelColor != null) {
                if (mPanelColor.isSupported()) {
                    mPanelColor.initValue();
                    mPanelColor.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mPanelColor);
                }
            }

            mLcdPowerReduce = (AwesomeCheckBoxPreference) findPreference("lcd_power_reduce");
            if (mLcdPowerReduce != null) {
                if (mLcdPowerReduce.isSupported()) {
                    mLcdPowerReduce.initValue();
                    mLcdPowerReduce.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mLcdPowerReduce);
                }
            }

            mLcdSunlightEnhancement =
                    (AwesomeCheckBoxPreference) findPreference("lcd_sunlight_enhancement");
            if (mLcdSunlightEnhancement != null) {
                if (mLcdSunlightEnhancement.isSupported()) {
                    mLcdSunlightEnhancement.initValue();
                    mLcdSunlightEnhancement.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mLcdSunlightEnhancement);
                }
            }

            mLcdColorEnhancement =
                    (AwesomeCheckBoxPreference) findPreference("lcd_color_enhancement");
            if (mLcdColorEnhancement != null) {
                if (mLcdColorEnhancement.isSupported()) {
                    mLcdColorEnhancement.initValue();
                    mLcdColorEnhancement.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mLcdColorEnhancement);
                }
            }

            if (category.getPreferenceCount() == 0) {
                preferenceScreen.removePreference(category);
            }
        }

        category = (PreferenceCategory) findPreference("extras");
        if (category != null) {
            mLoggerMode = (AwesomeCheckBoxPreference) findPreference("logger_mode");
            if (mLoggerMode != null) {
                if (mLoggerMode.isSupported()) {
                    mLoggerMode.initValue(true);
                    mLoggerMode.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mLoggerMode);
                }
            }

            if (category.getPreferenceCount() == 0) {
                preferenceScreen.removePreference(category);
            }
        }

        mFastCharge = (CustomPreference) findPreference("fast_charge");
        if (mFastCharge != null) {
            if (Utils.fileExists(FC_PATH)) {
                mFastCharge.setOnPreferenceClickListener(this);
            } else {
                preferenceScreen.removePreference(mFastCharge);
            }
        }

        isSupported(preferenceScreen, getActivity());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mGloveMode && mGloveMode.isEnabled()) {
            final boolean value = (Boolean) o;
            enableHts(value);
            PreferenceHelper.setBootup(
                    new DataItem(DatabaseHandler.CATEGORY_DEVICE, mGloveMode.getKey(),
                            mGloveMode.getKey(), (value ? "1" : "0"))
            );
            return true;
        } else if (preference == mAwesomeGloveMode) {
            mAwesomeGloveMode.writeValue((Boolean) o);
            return true;
        } else if (preference == mKnockOn) {
            mKnockOn.writeValue((Boolean) o);
            return true;
        } else if (preference == mBacklightKey) {
            mBacklightKey.writeValue((Boolean) o);
            return true;
        } else if (preference == mBacklightNotification) {
            mBacklightNotification.writeValue((Boolean) o);
            return true;
        } else if (preference == mKeyboardBacklight) {
            mKeyboardBacklight.writeValue((Boolean) o);
            return true;
        } else if (preference == mPanelColor) {
            mPanelColor.writeValue(String.valueOf(o));
            return true;
        } else if (preference == mLcdPowerReduce) {
            mLcdPowerReduce.writeValue((Boolean) o);
            return true;
        } else if (preference == mLcdSunlightEnhancement) {
            mLcdSunlightEnhancement.writeValue((Boolean) o);
            return true;
        } else if (preference == mLcdColorEnhancement) {
            mLcdColorEnhancement.writeValue((Boolean) o);
            return true;
        } else if (mLoggerMode == preference) {
            mLoggerMode.writeValue((Boolean) o);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mFastCharge == preference) {
            MainActivity.loadFragment(getActivity(), ID_FAST_CHARGE);
            return true;
        }

        return false;
    }

    //==============================================================================================
    // Methods
    //==============================================================================================

    public static String restore() {
        final StringBuilder sbCmd = new StringBuilder();

        final List<DataItem> items = DatabaseHandler.getInstance()
                .getAllItems(DatabaseHandler.TABLE_BOOTUP, DatabaseHandler.CATEGORY_DEVICE);

        String filename, value;
        for (final DataItem item : items) {
            filename = item.getFileName();
            value = item.getValue();
            if ("input_glove_mode".equals(filename)) {
                final String mode = ("1".equals(value) ? GLOVE_MODE_ENABLE : GLOVE_MODE_DISABLE);
                sbCmd.append(Utils.getWriteCommand(COMMAND_PATH, mode));
            } else {
                sbCmd.append(Utils.getWriteCommand(filename, value));
            }
        }

        return sbCmd.toString();
    }

    private static final String COMMAND_PATH       = "/sys/class/sec/tsp/cmd";
    private static final String GLOVE_MODE         = "glove_mode";
    private static final String GLOVE_MODE_ENABLE  = GLOVE_MODE + ",1";
    private static final String GLOVE_MODE_DISABLE = GLOVE_MODE + ",0";

    public void enableHts(final boolean enable) {
        if (mGloveMode != null) mGloveMode.setEnabled(false);
        final String mode = (enable ? GLOVE_MODE_ENABLE : GLOVE_MODE_DISABLE);
        Utils.getCommandResult(this, Utils.getWriteCommand(COMMAND_PATH, mode) +
                Utils.getReadCommand("/sys/class/sec/tsp/cmd_result"));
    }

    public void onShellOutput(final ShellOutputEvent event) {
        if (event == null) return;

        final String output = event.getOutput();

        if (output == null || mGloveMode == null) return;

        mGloveMode.setChecked(output.contains(GLOVE_MODE_ENABLE));
        mGloveMode.setEnabled(true);
    }

    /**
     * Whether device supports high touch sensitivity.
     *
     * @return boolean Supported devices must return always true
     */
    private boolean isHtsSupported() {
        final File f = new File(COMMAND_PATH);

        // Check to make sure that the kernel supports glove mode
        if (f.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader("/sys/class/sec/tsp/cmd_list"));
                String currentLine;
                while ((currentLine = reader.readLine()) != null) {
                    if (currentLine.equals(GLOVE_MODE)) {
                        Logger.v(DeviceInformationFragment.class,
                                "Glove mode / high touch sensitivity supported");
                        return true;
                    }
                }
            } catch (IOException ignored) {
                // ignored
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException ignored) {
                    // ignored
                }
            }
        }

        Logger.v(DeviceInformationFragment.class,
                "Glove mode / high touch sensitivity NOT supported");

        return false;
    }

}
