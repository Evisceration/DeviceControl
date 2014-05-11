package org.namelessrom.devicecontrol.fragments.performance.sub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.text.InputType;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.squareup.otto.Subscribe;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.database.DataItem;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.events.GovernorEvent;
import org.namelessrom.devicecontrol.events.SectionAttachedEvent;
import org.namelessrom.devicecontrol.utils.CpuUtils;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.providers.BusProvider;
import org.namelessrom.devicecontrol.widgets.AttachPreferenceFragment;
import org.namelessrom.devicecontrol.widgets.preferences.CustomPreference;

import java.io.File;

public class GovernorFragment extends AttachPreferenceFragment implements DeviceConstants {

    private static PreferenceCategory mCategory;
    private static Context            mContext;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity, ID_GOVERNOR_TUNABLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getBus().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getBus().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BusProvider.getBus().post(new SectionAttachedEvent(ID_RESTORE_FROM_SUB));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.governor);

        setHasOptionsMenu(true);

        mCategory = (PreferenceCategory) findPreference("key_gov_category");
        mContext = getActivity();

        CpuUtils.getGovernorEvent();
    }

    @Subscribe
    public void onGovernor(final GovernorEvent event) {
        if (event == null) return;

        final String curGov = event.getCurrentGovernor();
        if (new File("/sys/devices/system/cpu/cpufreq/" + curGov).exists()) {
            mCategory.setTitle(curGov + " Tweakable values");
            new addPreferences().execute(curGov);
        } else {
            getPreferenceScreen().removeAll();
        }

        isSupported(getPreferenceScreen(), mContext, R.string.no_gov_tweaks_message);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.onBackPressed();
                }
                return true;
            default:
                break;
        }

        return false;
    }

    class addPreferences extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            if (mCategory.getPreferenceCount() != 0) {
                mCategory.removeAll();
            }
            final String currentGovernor = params[0];
            final File f = new File("/sys/devices/system/cpu/cpufreq/" + currentGovernor);
            if (f.exists()) {
                final File[] files = f.listFiles();
                for (final File file : files) {
                    final String fileName = file.getName();

                    // Do not try to read boostpulse
                    if ("boostpulse".equals(fileName)) {
                        continue;
                    }

                    final String filePath = file.getAbsolutePath();
                    final String fileContent = Utils.readOneLine(filePath).trim()
                            .replaceAll("\n", "");
                    final CustomPreference pref = new CustomPreference(mContext);
                    pref.setTitle(fileName);
                    pref.setSummary(fileContent);
                    pref.setKey(filePath);
                    mCategory.addPreference(pref);
                    pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                        @Override
                        public boolean onPreferenceClick(final Preference p) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            LinearLayout ll = new LinearLayout(mContext);
                            ll.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT));
                            final EditText et = new EditText(mContext);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(40, 40, 40, 40);
                            params.gravity = Gravity.CENTER;
                            String val = p.getSummary().toString();
                            et.setLayoutParams(params);
                            et.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                            et.setGravity(Gravity.CENTER_HORIZONTAL);
                            et.setText(val);
                            ll.addView(et);
                            builder.setView(ll);
                            builder.setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String value = et.getText().toString();
                                            p.setSummary(value);
                                            Utils.writeValue(p.getKey(), value);
                                            updateBootupListDb(p, value);
                                        }
                                    }
                            );
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            Window window = dialog.getWindow();
                            window.setLayout(800, LayoutParams.WRAP_CONTENT);
                            return true;
                        }

                    });
                }
            }

            return null;
        }

    }

    private static void updateBootupListDb(final Preference p, final String value) {

        class updateListDb extends AsyncTask<String, Void, Void> {

            @Override
            protected Void doInBackground(String... params) {
                final String name = p.getTitle().toString();
                final String key = p.getKey();
                PreferenceHelper.setBootup(new DataItem(
                        DatabaseHandler.CATEGORY_CPU, name, key, value));

                return null;
            }

        }
        new updateListDb().execute();
    }

}