package org.namelessrom.devicecontrol.fragments.performance.sub;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.negusoft.holoaccent.dialog.AccentAlertDialog;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.database.DataItem;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.events.SectionAttachedEvent;
import org.namelessrom.devicecontrol.utils.CpuUtils;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.constants.PerformanceConstants;
import org.namelessrom.devicecontrol.utils.providers.BusProvider;
import org.namelessrom.devicecontrol.views.AttachListFragment;

import java.util.List;

import static butterknife.ButterKnife.findById;

public class LowMemoryKillerFragment extends AttachListFragment implements DeviceConstants {

    private LmkAdapter adapter;

    @Override public void onAttach(final Activity activity) {
        super.onAttach(activity, ID_LOWMEMORYKILLER);
    }

    @Override public void onDestroy() {
        BusProvider.getBus().post(new SectionAttachedEvent(ID_RESTORE_FROM_SUB));
        super.onDestroy();
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Spinner spinner = new Spinner(getActivity());
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_spinner_dropdown_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final String[] presets = Application.getStringArray(R.array.minfree_entries);
        final String[] values = Application.getStringArray(R.array.minfree_values);
        spinnerAdapter.addAll(presets);

        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(0);
        getListView().addHeaderView(spinner);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(final AdapterView<?> adapterView, final View view,
                    final int position, final long id) {
                editDialog(position - 1);
            }
        });

        spinner.post(new Runnable() {
            @Override public void run() {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    private boolean initialized = false;

                    @Override public void onItemSelected(final AdapterView<?> adapterView,
                            final View view, final int position, final long id) {
                        // because onItemSelected gets executed prematurely we need to hack...
                        if (!initialized) {
                            initialized = true;
                            return;
                        }

                        if (TextUtils.isEmpty(values[position])) return;

                        adapter = new LmkAdapter(getActivity(), values[position].split(","));
                        applyMinFree();
                    }

                    @Override public void onNothingSelected(final AdapterView<?> adapterView) { }
                });
            }
        });

        new LmkTask().execute();
    }

    private void applyMinFree() {
        final StringBuilder sb = new StringBuilder();
        for (final String s : adapter.values) {
            sb.append(String.format("%s,", s));
        }
        applyMinFree(sb.toString());
    }

    private void applyMinFree(String value) {
        if (value.endsWith(",")) { value = value.substring(0, value.length() - 1); }
        Utils.runRootCommand(Utils.getWriteCommand(PerformanceConstants.FILE_MINFREE, value));

        PreferenceHelper.setBootup(new DataItem(DatabaseHandler.CATEGORY_LMK, "lmk",
                PerformanceConstants.FILE_MINFREE, value));

        adapter = new LmkAdapter(getActivity(), value.trim().split(","));
        setListAdapter(adapter);
    }

    private void editDialog(final int position) {
        final Activity activity = getActivity();
        if (activity == null) return;

        final View editDialog = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_prop, null);
        final EditText tv = findById(editDialog, R.id.prop_value);
        final TextView tn = findById(editDialog, R.id.prop_name_tv);

        tv.setText(adapter.values[position]);
        tn.setText(LmkAdapter.TYPES[position]);

        new AccentAlertDialog.Builder(activity)
                .setTitle(getString(R.string.edit_property))
                .setView(editDialog)
                .setNegativeButton(getString(android.R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) { }
                        }
                )
                .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        adapter.values[position] = tv.getText().toString().trim();
                        applyMinFree();
                    }
                }).show();
    }

    private class LmkTask extends AsyncTask<Void, Void, String[]> {
        @Override protected String[] doInBackground(Void... voids) {
            final String values = Utils.readFile(PerformanceConstants.FILE_MINFREE);
            if (values != null) {
                return values.trim().replace("\n", "").split(",");
            }
            return new String[0];
        }

        @Override protected void onPostExecute(final String[] strings) {
            adapter = new LmkAdapter(getActivity(), strings);
            setListAdapter(adapter);
        }
    }

    private static class LmkAdapter extends ArrayAdapter<String> {
        private final Context  context;
        public final  String[] values;

        private static final String[] TYPES = {
                Application.getStr(R.string.foreground_applications),
                Application.getStr(R.string.visible_applications),
                Application.getStr(R.string.secondary_server),
                Application.getStr(R.string.hidden_applications),
                Application.getStr(R.string.content_providers),
                Application.getStr(R.string.empty_applications)
        };

        public LmkAdapter(final Context context, final String[] values) {
            super(context, R.layout.list_item_prop, values);
            this.context = context;
            this.values = values;
        }

        private final class ViewHolder {
            private final TextView pp;
            private final TextView pv;

            private ViewHolder(final View rootView) {
                pp = findById(rootView, R.id.prop);
                pv = findById(rootView, R.id.pval);
            }
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (convertView == null) {
                convertView = ((LayoutInflater)
                        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.list_item_prop, parent, false);
                viewHolder = new ViewHolder(convertView);
                assert (convertView != null);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            if (viewHolder.pp != null) {
                viewHolder.pp.setText(TYPES[position]);
            }
            if (viewHolder.pv != null) {
                viewHolder.pv.setText(values[position]);
            }

            return convertView;
        }
    }

    public static String restore() {
        final StringBuilder sbCmd = new StringBuilder();

        final List<DataItem> items = DatabaseHandler.getInstance().getAllItems(
                DatabaseHandler.TABLE_BOOTUP, DatabaseHandler.CATEGORY_LMK);

        for (final DataItem item : items) {
            sbCmd.append(Utils.getWriteCommand(item.getFileName(), item.getValue()));
        }

        return sbCmd.toString();
    }
}
