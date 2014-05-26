package org.namelessrom.devicecontrol.fragments.performance.sub;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.events.SectionAttachedEvent;
import org.namelessrom.devicecontrol.utils.ActionProcessor;
import org.namelessrom.devicecontrol.utils.DialogHelper;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.constants.FileConstants;
import org.namelessrom.devicecontrol.utils.constants.PerformanceConstants;
import org.namelessrom.devicecontrol.utils.providers.BusProvider;
import org.namelessrom.devicecontrol.widgets.AttachPreferenceFragment;
import org.namelessrom.devicecontrol.widgets.preferences.CustomCheckBoxPreference;
import org.namelessrom.devicecontrol.widgets.preferences.CustomPreference;

import java.util.ArrayList;
import java.util.List;

import static org.namelessrom.devicecontrol.Application.logDebug;

public class KsmFragment extends AttachPreferenceFragment
        implements DeviceConstants, FileConstants, PerformanceConstants,
        Preference.OnPreferenceChangeListener {

    //----------------------------------------------------------------------------------------------
    private PreferenceScreen         mRoot;
    //----------------------------------------------------------------------------------------------
    private CustomPreference         mFullScans;
    private CustomPreference         mPagesShared;
    private CustomPreference         mPagesSharing;
    private CustomPreference         mPagesUnshared;
    private CustomPreference         mPagesVolatile;
    //----------------------------------------------------------------------------------------------
    private CustomCheckBoxPreference mEnable;
    private CustomCheckBoxPreference mDefer;
    private CustomPreference         mPagesToScan;
    private CustomPreference         mSleep;

    @Override public void onAttach(final Activity activity) { super.onAttach(activity, ID_KSM); }

    @Override public void onDestroy() {
        super.onDestroy();
        BusProvider.getBus().post(new SectionAttachedEvent(ID_RESTORE_FROM_SUB));
    }

    @Override public void onResume() {
        super.onResume();
        BusProvider.getBus().register(this);
    }

    @Override public void onPause() {
        super.onPause();
        BusProvider.getBus().unregister(this);
    }

    @Override public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.extras_ksm);
        setHasOptionsMenu(true);

        mRoot = getPreferenceScreen();
        PreferenceCategory category;
        String tmpString;

        //------------------------------------------------------------------------------------------
        // KSM-Informations
        //------------------------------------------------------------------------------------------
        category = (PreferenceCategory) findPreference("ksm_info");
        if (category != null) {
            mFullScans = (CustomPreference) findPreference("ksm_full_scans");
            if (mFullScans != null) {
                if (!Utils.fileExists(KSM_FULL_SCANS)) {
                    category.removePreference(mFullScans);
                }
            }

            mPagesShared = (CustomPreference) findPreference("ksm_pages_shared");
            if (mPagesShared != null) {
                if (!Utils.fileExists(KSM_PAGES_SHARED)) {
                    category.removePreference(mPagesShared);
                }
            }

            mPagesSharing = (CustomPreference) findPreference("ksm_pages_sharing");
            if (mPagesSharing != null) {
                if (!Utils.fileExists(KSM_PAGES_SHARING)) {
                    category.removePreference(mPagesSharing);
                }
            }

            mPagesUnshared = (CustomPreference) findPreference("ksm_pages_unshared");
            if (mPagesUnshared != null) {
                if (!Utils.fileExists(KSM_PAGES_UNSHARED)) {
                    category.removePreference(mPagesUnshared);
                }
            }

            mPagesVolatile = (CustomPreference) findPreference("ksm_pages_volatile");
            if (mPagesVolatile != null) {
                if (!Utils.fileExists(KSM_PAGES_VOLATILE)) {
                    category.removePreference(mPagesVolatile);
                }
            }

            new RefreshTask().execute();
        }

        removeIfEmpty(category);

        //------------------------------------------------------------------------------------------
        // KSM-Tweakables
        //------------------------------------------------------------------------------------------
        category = (PreferenceCategory) findPreference("ksm_settings");
        if (category != null) {
            mEnable = (CustomCheckBoxPreference) findPreference("ksm_run");
            if (mEnable != null) {
                if (Utils.fileExists(KSM_RUN)) {
                    tmpString = Utils.readOneLine(KSM_RUN);
                    mEnable.setChecked(Utils.isEnabled(tmpString));
                    mEnable.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mEnable);
                }
            }

            mDefer = (CustomCheckBoxPreference) findPreference("ksm_deferred");
            if (mDefer != null) {
                if (Utils.fileExists(KSM_DEFERRED)) {
                    tmpString = Utils.readOneLine(KSM_DEFERRED);
                    mDefer.setChecked(Utils.isEnabled(tmpString));
                    mDefer.setOnPreferenceChangeListener(this);
                } else {
                    category.removePreference(mDefer);
                }
            }

            mPagesToScan = (CustomPreference) findPreference("ksm_pages_to_scan");
            if (mPagesToScan != null) {
                if (Utils.fileExists(KSM_PAGES_TO_SCAN)) {
                    tmpString = Utils.readOneLine(KSM_PAGES_TO_SCAN);
                    mPagesToScan.setSummary(tmpString);
                } else {
                    category.removePreference(mPagesToScan);
                }
            }

            mSleep = (CustomPreference) findPreference("ksm_sleep");
            if (mSleep != null) {
                if (Utils.fileExists(KSM_SLEEP)) {
                    tmpString = Utils.readOneLine(KSM_SLEEP);
                    mSleep.setSummary(tmpString);
                } else {
                    category.removePreference(mSleep);
                }
            }
        }

        removeIfEmpty(category);

        isSupported(mRoot, getActivity());
    }

    private void removeIfEmpty(final PreferenceCategory preferenceCategory) {
        if (mRoot != null && preferenceCategory.getPreferenceCount() == 0) {
            mRoot.removePreference(preferenceCategory);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (mPagesToScan == preference) {
            final String title = String.valueOf(mPagesToScan.getTitle());
            final int currentProgress = Integer.parseInt(Utils.readOneLine(KSM_PAGES_TO_SCAN));
            DialogHelper.openSeekbarDialog(getActivity(), currentProgress, title, 1,
                    1024, preference, KSM_PAGES_TO_SCAN, DatabaseHandler.CATEGORY_EXTRAS);
            return true;
        } else if (mSleep == preference) {
            final String title = String.valueOf(mSleep.getTitle());
            final int currentProgress = Integer.parseInt(Utils.readOneLine(KSM_SLEEP));
            DialogHelper.openSeekbarDialog(getActivity(), currentProgress, title, 50,
                    5000, preference, KSM_SLEEP, DatabaseHandler.CATEGORY_EXTRAS);
            return true;
        }

        return false;
    }

    @Override public boolean onPreferenceChange(final Preference preference, final Object o) {
        if (mEnable == preference) {
            ActionProcessor.processAction(ActionProcessor.ACTION_KSM_ENABLED,
                    (((Boolean) o) ? "1" : "0"));
            return true;
        } else if (mDefer==preference) {
            ActionProcessor.processAction(ActionProcessor.ACTION_KSM_DEFERRED,
                    (((Boolean) o) ? "1" : "0"));
            return true;
        }

        return false;
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_refresh, menu);
    }

    @Override public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.onBackPressed();
                }
                return true;
            case R.id.menu_action_refresh:
                new RefreshTask().execute();
            default:
                break;
        }

        return false;
    }

    private class RefreshTask extends AsyncTask<Void, Void, List<String>> {

        @Override protected List<String> doInBackground(Void... params) {
            final ArrayList<String> list = new ArrayList<String>();

            list.add(Utils.readOneLine(KSM_FULL_SCANS));     // 0
            list.add(Utils.readOneLine(KSM_PAGES_SHARED));   // 1
            list.add(Utils.readOneLine(KSM_PAGES_SHARING));  // 2
            list.add(Utils.readOneLine(KSM_PAGES_UNSHARED)); // 3
            list.add(Utils.readOneLine(KSM_PAGES_VOLATILE)); // 4

            return list;
        }

        @Override protected void onPostExecute(final List<String> strings) {
            if (isAdded()) {
                String tmp;
                if (mFullScans != null) {
                    tmp = strings.get(0);
                    logDebug("strings.get(0): " + tmp);
                    mFullScans.setSummary(tmp);
                }
                if (mPagesShared != null) {
                    tmp = strings.get(1);
                    logDebug("strings.get(1): " + tmp);
                    mPagesShared.setSummary(tmp);
                }
                if (mPagesSharing != null) {
                    tmp = strings.get(2);
                    logDebug("strings.get(2): " + tmp);
                    mPagesSharing.setSummary(tmp);
                }
                if (mPagesUnshared != null) {
                    tmp = strings.get(3);
                    logDebug("strings.get(3): " + tmp);
                    mPagesUnshared.setSummary(tmp);
                }
                if (mPagesVolatile != null) {
                    tmp = strings.get(4);
                    logDebug("strings.get(4): " + tmp);
                    mPagesVolatile.setSummary(tmp);
                }
            }
        }
    }
}


