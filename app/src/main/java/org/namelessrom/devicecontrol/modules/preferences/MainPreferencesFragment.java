/*
 *  Copyright (C) 2013 - 2015 Alexander "Evisceration" Martinz
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
package org.namelessrom.devicecontrol.modules.preferences;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.pollfish.main.PollFish;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.MainActivity;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.configuration.DeviceConfiguration;
import org.namelessrom.devicecontrol.theme.AppResources;
import org.namelessrom.devicecontrol.utils.Utils;

import alexander.martinz.libs.materialpreferences.MaterialPreference;
import alexander.martinz.libs.materialpreferences.MaterialPreferenceCategory;
import alexander.martinz.libs.materialpreferences.MaterialSupportPreferenceFragment;
import alexander.martinz.libs.materialpreferences.MaterialSwitchPreference;

public class MainPreferencesFragment extends MaterialSupportPreferenceFragment implements MaterialPreference.MaterialPreferenceChangeListener, MaterialPreference.MaterialPreferenceClickListener {
    private MaterialPreference mSetOnBoot;

    private MaterialSwitchPreference mSwipeOnContent;
    // TODO: more customization
    private MaterialSwitchPreference mDarkTheme;

    private MaterialSwitchPreference mShowPollfish;

    @Override protected int getLayoutResourceId() {
        return R.layout.preferences_app_device_control_main;
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final DeviceConfiguration configuration = DeviceConfiguration.get(getActivity());

        MaterialPreferenceCategory category =
                (MaterialPreferenceCategory) view.findViewById(R.id.cat_prefs_general);
        category.getCardView().setBackgroundColor(AppResources.get().getCardBackgroundColor());

        mSetOnBoot = (MaterialPreference) view.findViewById(R.id.prefs_set_on_boot);
        mSetOnBoot.setOnPreferenceClickListener(this);

        category = (MaterialPreferenceCategory) view.findViewById(R.id.cat_prefs_interface);
        category.getCardView().setBackgroundColor(AppResources.get().getCardBackgroundColor());

        mDarkTheme = (MaterialSwitchPreference) view.findViewById(R.id.prefs_dark_theme);
        mDarkTheme.setChecked(AppResources.get().isDarkTheme());
        mDarkTheme.setOnPreferenceChangeListener(this);

        mSwipeOnContent = (MaterialSwitchPreference) view.findViewById(R.id.prefs_swipe_on_content);
        mSwipeOnContent.setChecked(configuration.swipeOnContent);
        mSwipeOnContent.setOnPreferenceChangeListener(this);

        category = (MaterialPreferenceCategory) view.findViewById(R.id.cat_prefs_support);
        category.getCardView().setBackgroundColor(AppResources.get().getCardBackgroundColor());

        mShowPollfish = (MaterialSwitchPreference) view.findViewById(R.id.prefs_show_pollfish);
        mShowPollfish.setChecked(configuration.showPollfish);
        mShowPollfish.setOnPreferenceChangeListener(this);

        setupVersionPreference(view);
    }

    private void setupVersionPreference(View view) {
        MaterialPreference version = (MaterialPreference) view.findViewById(R.id.prefs_version);
        if (version != null) {
            version.getCardView().setBackgroundColor(AppResources.get().getCardBackgroundColor());

            String title;
            String summary;
            try {
                final PackageManager pm = Application.get().getPackageManager();
                if (pm != null) {
                    final PackageInfo pInfo = pm.getPackageInfo(getActivity().getPackageName(), 0);
                    title = getString(R.string.app_version_name, pInfo.versionName);
                    summary = getString(R.string.app_version_code, pInfo.versionCode);
                } else {
                    throw new Exception("pm is null");
                }
            } catch (Exception ignored) {
                title = getString(R.string.app_version_name, getString(R.string.unknown));
                summary = getString(R.string.app_version_code, getString(R.string.unknown));
            }
            version.setTitle(title);
            version.setSummary(summary);
        }
    }

    @Override public boolean onPreferenceChanged(MaterialPreference preference, Object newValue) {
        if (mShowPollfish == preference) {
            final boolean value = (Boolean) newValue;

            DeviceConfiguration.get(getActivity()).showPollfish = value;
            DeviceConfiguration.get(getActivity()).saveConfiguration(getActivity());

            if (value) {
                PollFish.show();
            } else {
                PollFish.hide();
            }
            mShowPollfish.setChecked(value);
            return true;
        } else if (mSwipeOnContent == preference) {
            final boolean value = (Boolean) newValue;

            DeviceConfiguration.get(getActivity()).swipeOnContent = value;
            DeviceConfiguration.get(getActivity()).saveConfiguration(getActivity());

            mSwipeOnContent.setChecked(value);

            // update the menu
            MainActivity.setSwipeOnContent(value);
            return true;
        } else if (mDarkTheme == preference) {
            final boolean isDark = (Boolean) newValue;
            AppResources.get().setDarkTheme(isDark);
            mDarkTheme.setChecked(isDark);

            if (isDark) {
                AppResources.get().setAccentColor(getResources().getColor(R.color.accent));
            } else {
                AppResources.get().setAccentColor(getResources().getColor(R.color.accent_light));
            }

            // restart the activity to apply new theme
            Utils.restartActivity(getActivity());
            return true;
        }

        return false;
    }

    @Override public boolean onPreferenceClicked(MaterialPreference preference) {
        if (mSetOnBoot == preference) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            new SobDialogFragment().show(fragmentManager, "sob_dialog_fragment");
            return true;
        }

        return false;
    }
}
