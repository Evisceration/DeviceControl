package org.namelessrom.devicecontrol.fragments.tools;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.activities.FilePickerActivity;
import org.namelessrom.devicecontrol.events.RefreshEvent;
import org.namelessrom.devicecontrol.events.SectionAttachedEvent;
import org.namelessrom.devicecontrol.fragments.filepicker.FilePickerFragment;
import org.namelessrom.devicecontrol.objects.FlashItem;
import org.namelessrom.devicecontrol.utils.FlashUtils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.providers.BusProvider;
import org.namelessrom.devicecontrol.widgets.AttachFragment;
import org.namelessrom.devicecontrol.widgets.adapters.FlashListAdapter;

import java.util.ArrayList;
import java.util.List;

import static butterknife.ButterKnife.findById;
import static org.namelessrom.devicecontrol.Application.logDebug;

/**
 * Created by alex on 22.06.14.
 */
public class FlasherFragment extends AttachFragment implements DeviceConstants,
        View.OnClickListener {

    private static final int    REQUEST_CODE_FILE = 100;
    public static final  String EXTRA_FLASHITEM   = "extra_flashitem";

    private FlashListAdapter mAdapter;

    private LinearLayout mContainer;
    private ListView     mFlashList;

    private TextView mEmptyView;

    @Override
    public void onAttach(final Activity activity) { super.onAttach(activity, ID_TOOLS_FLASHER); }

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

    @Override public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View v = inflater.inflate(R.layout.fragment_flasher, container, false);

        mContainer = findById(v, R.id.container);
        mFlashList = findById(v, R.id.flash_list);
        mEmptyView = findById(v, android.R.id.empty);

        final Button mCancel = findById(v, R.id.btn_cancel);
        mCancel.setOnClickListener(this);
        final Button mApply = findById(v, R.id.btn_apply);
        mApply.setOnClickListener(this);

        mAdapter = new FlashListAdapter();

        return v;
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFlashList.setAdapter(mAdapter);

        checkAdapter();
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_flasher, menu);
    }

    @Override public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.onBackPressed();
                }
                return true;
            }
            case R.id.action_task_add: {
                final Intent i = new Intent(getActivity(), FilePickerActivity.class);
                i.putExtra(FilePickerFragment.ARG_FILE_TYPE, "zip");
                startActivityForResult(i, REQUEST_CODE_FILE);
                return true;
            }
            default: {
                return false;
            }
        }
    }

    private void checkAdapter() {
        if (mAdapter != null && mContainer != null && mEmptyView != null) {
            if (mAdapter.getCount() != 0) {
                mContainer.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.INVISIBLE);
            } else {
                mContainer.setVisibility(View.INVISIBLE);
                mEmptyView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Subscribe public void onRefreshEvent(final RefreshEvent event) {
        if (event == null) return;
        checkAdapter();
    }

    @Override public void onActivityResult(final int req, final int res, final Intent data) {
        logDebug("onActivityResult(%s, %s, %s)", req, res, data);
        if (req == REQUEST_CODE_FILE && res == Activity.RESULT_OK) {
            final FlashItem item = (FlashItem) data.getExtras().getSerializable(EXTRA_FLASHITEM);
            logDebug("onActivityResult(): item is " + (item == null ? "null" : "not null"));
            if (item == null) return;

            logDebug(String.format("onActivityResult(%s)", item.getPath()));
            final List<FlashItem> flashItemList = new ArrayList<FlashItem>();
            flashItemList.addAll(((FlashListAdapter) mFlashList.getAdapter()).getFlashItemList());
            flashItemList.add(item);
            mAdapter = new FlashListAdapter(flashItemList);
            mFlashList.setAdapter(mAdapter);
            checkAdapter();
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override public void onClick(final View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.btn_apply: {
                // TODO: dialog, if accepted, not closeable as we are waiting for the reboot
                final List<FlashItem> flashItemList = mAdapter.getFlashItemList();
                final List<String> fileList = new ArrayList<String>(flashItemList.size());
                for (final FlashItem item : flashItemList) {
                    fileList.add(item.getPath());
                }
                FlashUtils.triggerFlash(fileList);
                break;
            }
            case R.id.btn_cancel: {
                // TODO: dialog
                mAdapter = new FlashListAdapter();
                mFlashList.setAdapter(mAdapter);
                checkAdapter();
                break;
            }
        }
    }
}
