/*
 * Copyright (C) 2013 The CyanogenMod Project
 * Modifications Copyright (C) 2013-2014 Alexander "Evisceration" Martinz
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

package org.namelessrom.devicecontrol.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.database.DataItem;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.constants.FileConstants;

/**
 * Special preference type that allows configuration of
 * vibrator intensity settings on Samsung devices
 */
public class VibratorTuningPreference extends DialogPreference
        implements SeekBar.OnSeekBarChangeListener, DeviceConstants, FileConstants {
    public final static String   FILE_VIBRATOR = Utils.checkPaths(FILES_VIBRATOR);
    private final       Vibrator vib           =
            (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
    private SeekBar             mSeekBar;
    private TextView            mValue;
    private String              mOriginalValue;
    private Drawable            mProgressDrawable;
    private Drawable            mProgressThumb;
    private LightingColorFilter mRedFilter;

    private String color = "#FFFFFF";

    public VibratorTuningPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_dialog_vibrator_tuning);
    }

    public void setTitleColor(String color) {
        this.color = color;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final TextView mTitle = (TextView) view.findViewById(android.R.id.title);
        mTitle.setTextColor(Color.parseColor(color));
        mTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

        final TextView mSummary = (TextView) view.findViewById(android.R.id.summary);
        mSummary.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.defaults_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar) view.findViewById(R.id.vibrator_seekbar);
        mValue = (TextView) view.findViewById(R.id.vibrator_value);
        final TextView mWarning = (TextView) view.findViewById(R.id.textWarn);

        final String strWarnMsg = getContext().getResources().getString(
                R.string.vibrator_warning
                , strengthToPercent(VIBRATOR_INTENSITY_WARNING_THRESHOLD) - 1);
        mWarning.setText(strWarnMsg);

        final Drawable progressDrawable = mSeekBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) progressDrawable;
            mProgressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        }
        mProgressThumb = mSeekBar.getThumb();
        mRedFilter = new LightingColorFilter(Color.BLACK,
                getContext().getResources().getColor(android.R.color.holo_red_light));

        // Read the current value from sysfs in case user wants to dismiss his changes
        mOriginalValue = Utils.readOneLine(FILE_VIBRATOR);

        // Restore percent value from SharedPreferences object
        final String value = PreferenceHelper.getBootupValue(KEY_VIBRATOR_TUNING);
        final int percent = strengthToPercent(value != null
                ? Integer.parseInt(value)
                : VIBRATOR_INTENSITY_DEFAULT_VALUE);

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setProgress(percent);

        final Button testVibration = (Button) view.findViewById(R.id.vibrator_test);
        testVibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vib != null) {
                    vib.cancel();
                    vib.vibrate(250);
                }
            }
        });
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            final Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
            if (defaultsButton != null) {
                defaultsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int progress = strengthToPercent(VIBRATOR_INTENSITY_DEFAULT_VALUE);
                        mSeekBar.setProgress(progress);

                        final String value = String.valueOf(percentToStrength(progress));
                        Utils.runRootCommand(Utils.getWriteCommand(FILE_VIBRATOR, value));
                    }
                });
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            PreferenceHelper.setBootup(new DataItem(
                    DatabaseHandler.CATEGORY_DEVICE, KEY_VIBRATOR_TUNING,
                    FILE_VIBRATOR, String.valueOf(mSeekBar.getProgress())));
        } else {
            Utils.runRootCommand(
                    Utils.getWriteCommand(FILE_VIBRATOR, String.valueOf(mOriginalValue))
            );
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        final boolean shouldWarn =
                progress >= strengthToPercent(VIBRATOR_INTENSITY_WARNING_THRESHOLD);
        if (mProgressDrawable != null) {
            mProgressDrawable.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        if (mProgressThumb != null) {
            mProgressThumb.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        mValue.setText(String.format("%d%%", progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        final String value = String.valueOf(percentToStrength(seekBar.getProgress()));
        Utils.runRootCommand(Utils.getWriteCommand(FILE_VIBRATOR, value));
    }

    /**
     * Convert vibrator strength to percent
     */
    public static int strengthToPercent(int strength) {
        final double maxValue = VIBRATOR_INTENSITY_MAX;
        final double minValue = VIBRATOR_INTENSITY_MIN;

        double percent = (strength - minValue) * (100 / (maxValue - minValue));

        if (percent > 100) { percent = 100; } else if (percent < 0) { percent = 0; }

        return (int) percent;
    }

    /**
     * Convert percent to vibrator strength
     */
    public static int percentToStrength(int percent) {
        int strength = Math.round(
                (((VIBRATOR_INTENSITY_MAX - VIBRATOR_INTENSITY_MIN) * percent) / 100)
                        + VIBRATOR_INTENSITY_MIN
        );

        if (strength > VIBRATOR_INTENSITY_MAX) {
            strength = VIBRATOR_INTENSITY_MAX;
        } else if (strength < VIBRATOR_INTENSITY_MIN) { strength = VIBRATOR_INTENSITY_MIN; }

        return strength;
    }

    public static boolean isSupported() {
        return (!FILE_VIBRATOR.isEmpty());
    }

}
