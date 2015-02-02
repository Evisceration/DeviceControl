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
package org.namelessrom.devicecontrol.ui.fragments.tools;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.base.R;
import org.namelessrom.devicecontrol.objects.AppItem;
import org.namelessrom.devicecontrol.ui.adapters.AppListAdapter;
import org.namelessrom.devicecontrol.ui.views.AttachFragment;
import org.namelessrom.devicecontrol.utils.SortHelper;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppListFragment extends AttachFragment implements DeviceConstants {
    private RecyclerView mRecyclerView;
    private LinearLayout mProgressContainer;

    @Override protected String getFragmentId() { return ID_TOOLS_APP_MANAGER; }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_refresh, menu);
    }

    @Override public boolean onOptionsItemSelected(final MenuItem item) {
        // get the id of our item
        final int id = item.getItemId();

        // if the user hit refresh
        if (id == R.id.menu_action_refresh) {
            new LoadApps().execute();
            return true;
        }

        return false;
    }

    @Override public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_app_list, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mProgressContainer = (LinearLayout) rootView.findViewById(R.id.progressContainer);
        return rootView;
    }

    @Override public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        mRecyclerView.setHasFixedSize(true);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
    }

    @Override public void onResume() {
        super.onResume();
        new LoadApps().execute();
    }

    private void invalidateOptionsMenu() {
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private class LoadApps extends AsyncTask<Void, Void, List<AppItem>> {
        @Override protected void onPreExecute() {
            if (mProgressContainer != null) {
                mProgressContainer.setVisibility(View.VISIBLE);
            }
        }

        @Override protected List<AppItem> doInBackground(Void... params) {
            final PackageManager pm = Application.get().getPackageManager();
            final List<AppItem> appList = new ArrayList<>();
            final List<PackageInfo> pkgInfos = pm.getInstalledPackages(0);

            for (final PackageInfo pkgInfo : pkgInfos) {
                if (pkgInfo.applicationInfo == null) {
                    continue;
                }
                appList.add(new AppItem(pkgInfo,
                        String.valueOf(pkgInfo.applicationInfo.loadLabel(pm)),
                        pkgInfo.applicationInfo.loadIcon(pm)));
            }
            Collections.sort(appList, SortHelper.sAppComparator);

            return appList;
        }

        @Override protected void onPostExecute(final List<AppItem> appItems) {
            if (appItems != null && isAdded()) {
                if (mProgressContainer != null) {
                    mProgressContainer.setVisibility(View.GONE);
                }
                final AppListAdapter adapter = new AppListAdapter(getActivity(), appItems);
                mRecyclerView.setAdapter(adapter);
            }
            invalidateOptionsMenu();
        }
    }

}
