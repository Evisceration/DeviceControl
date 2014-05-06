package org.namelessrom.devicecontrol.fragments.tools;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.events.SubFragmentEvent;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.providers.BusProvider;
import org.namelessrom.devicecontrol.widgets.AttachPreferenceFragment;

public class ToolsMoreFragment extends AttachPreferenceFragment implements DeviceConstants,
        MediaScannerConnection.MediaScannerConnectionClient {

    private MediaScannerConnection mMediaScannerConnection;
    private PreferenceScreen       mMediaScan;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity, ID_TOOLS_MORE);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tools_more);

        mMediaScan = (PreferenceScreen) findPreference("media_scan");
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (key == null || key.isEmpty()) return false;

        if (key.equals("media_scan")) {
            startMediaScan();
        } else if (key.equals("freezer")) {
            BusProvider.getBus().post(new SubFragmentEvent(ID_TOOLS_FREEZER));
        } else if (key.equals("editors")) {
            BusProvider.getBus().post(new SubFragmentEvent(ID_TOOLS_EDITORS));
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void startMediaScan() {
        if (mMediaScannerConnection != null) {
            mMediaScannerConnection.disconnect();
        }
        mMediaScannerConnection = new MediaScannerConnection(Application.applicationContext, this);
        mMediaScannerConnection.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        try {
            final String path = Environment.getExternalStorageDirectory().getPath();
            mMediaScannerConnection.scanFile(path, null);
            Application.HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaScan != null) {
                        mMediaScan.setSummary("Media Scanner started scanning " + path);
                    }
                }
            });
        } catch (Exception ignored) { /* ignored */ }
    }

    @Override
    public void onScanCompleted(final String path, final Uri uri) {
        Application.HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (mMediaScan != null) {
                    mMediaScan.setSummary("Media Scanner finished scanning " + path);
                }
            }
        });
        if (mMediaScannerConnection.isConnected()) {
            mMediaScannerConnection.disconnect();
        }
    }
}
