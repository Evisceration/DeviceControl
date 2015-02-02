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
package org.namelessrom.devicecontrol.ui.fragments.about;

import android.support.v4.app.Fragment;

import org.namelessrom.devicecontrol.base.R;
import org.namelessrom.devicecontrol.ui.views.AttachViewPagerFragment;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;

import java.util.ArrayList;

public class AboutFragment extends AttachViewPagerFragment {

    @Override protected String getFragmentId() { return DeviceConstants.ID_ABOUT; }

    @Override public ViewPagerAdapter getPagerAdapter() {
        final ArrayList<Fragment> fragments = new ArrayList<>(3);
        final ArrayList<CharSequence> titles = new ArrayList<>(3);

        fragments.add(new WelcomeFragment());
        titles.add(getString(R.string.about));
        fragments.add(new SupportFragment());
        titles.add(getString(R.string.support));
        fragments.add(new LicenseFragment());
        titles.add(getString(R.string.licenses));
        fragments.add(new PrivacyFragment());
        titles.add(getString(R.string.privacy));

        return new ViewPagerAdapter(getChildFragmentManager(), fragments, titles);
    }

}
