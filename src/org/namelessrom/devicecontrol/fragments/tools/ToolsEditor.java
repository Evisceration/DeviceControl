/*
 *  Copyright (C) 2013 h0rn3t
 *  Modifications Copyright (C) 2013-2014 Alexander "Evisceration" Martinz
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
package org.namelessrom.devicecontrol.fragments.tools;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.threads.FireAndGet;
import org.namelessrom.devicecontrol.threads.ReadValue;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.adapters.PropAdapter;
import org.namelessrom.devicecontrol.utils.classes.Prop;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.constants.FileConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class ToolsEditor extends Fragment
        implements DeviceConstants, FileConstants, AdapterView.OnItemClickListener {

    private static final String ARG_EDITOR = "arg_editor";
    private static final int HANDLER_DELAY = 250;

    private ListView packList;
    private LinearLayout linlaHeaderProgress;
    private LinearLayout nofiles;
    private RelativeLayout tools;
    private PropAdapter adapter = null;
    private EditText filterText = null;
    private final List<Prop> props = new ArrayList<Prop>();
    private final String dn = DC_BACKUP_DIR;

    private final String syspath = "/system/etc/";
    private String mod = "sysctl";
    private String sob = SYSCTL_SOB;
    private Boolean isdyn = false;
    private int mEditorType;
    private String mBuildName = "build";
    private String[] oggs = {};

    public static ToolsEditor newInstance(final int editor) {
        Bundle b = new Bundle();
        b.putInt(ToolsEditor.ARG_EDITOR, editor);
        ToolsEditor fragment = new ToolsEditor();
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEditorType = getArguments().getInt(ARG_EDITOR);

        switch (mEditorType) {
            default:
            case 0:
                mod = "vm";
                sob = VM_SOB;
                break;
            case 1:
                mod = "sysctl";
                sob = SYSCTL_SOB;
                break;
            case 2:
                mod = "buildprop";
                break;
        }

        View view = inflater.inflate((mEditorType == 2)
                ? R.layout.build_prop_view
                : R.layout.prop_view, container, false);

        packList = (ListView) view.findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        packList.setFastScrollEnabled(true);
        packList.setFastScrollAlwaysVisible(true);
        if (mEditorType == 2) {
            packList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    final Prop p = adapter.getItem(i);
                    if (!p.getName().contains("fingerprint")) {
                        makeDialog(getString(R.string.etc_del_prop_title)
                                , getString(R.string.etc_del_prop_msg, p.getName())
                                , (byte) 1, p);
                    }
                    return true;
                }
            });
        }
        linlaHeaderProgress = (LinearLayout) view.findViewById(R.id.linlaHeaderProgress);
        nofiles = (LinearLayout) view.findViewById(R.id.nofiles);
        tools = (RelativeLayout) view.findViewById(R.id.tools);
        filterText = (EditText) view.findViewById(R.id.filtru);
        filterText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable s) {
            }

            @Override
            public void beforeTextChanged(final CharSequence s, final int start,
                                          final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
                if (adapter != null) {
                    adapter.getFilter().filter(filterText.getText().toString());
                }
            }
        });
        if (mEditorType == 2) {
            Button addButton = (Button) view.findViewById(R.id.addBtn);
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    editBuildPropDialog(null);
                }
            });
            Button restoreButton = (Button) view.findViewById(R.id.restoreBtn);
            restoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    makeDialog(getString(R.string.etc_prop_restore_title),
                            getString(R.string.etc_prop_restore_msg), (byte) 0, null);
                }
            });
        } else {
            Button applyBtn = (Button) view.findViewById(R.id.applyBtn);
            applyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View arg0) {
                    Shell.SU.run("busybox mount -o remount,rw /system" + ";"
                            + "busybox cp " + dn + "/" + mod + ".conf"
                            + " " + syspath + mod + ".conf;"
                            + "busybox chmod 644 " + syspath + mod + ".conf" + ";"
                            + "busybox mount -o remount,ro /system" + ";"
                            + "busybox sysctl -p " + syspath + mod + ".conf" + ";");
                }
            });
            final Switch setOnBoot = (Switch) view.findViewById(R.id.applyAtBoot);
            setOnBoot.setChecked(PreferenceHelper.getBoolean(sob, false));
            setOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    PreferenceHelper.setBoolean(sob, isChecked);
                }
            });
        }
        tools.setVisibility(View.GONE);
        isdyn = (new File(DYNAMIC_DIRTY_WRITEBACK_PATH).exists());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mEditorType == 2) {
                    new GetBuildPropOperation().execute();
                } else {
                    new GetPropOperation().execute();
                }
            }
        }, HANDLER_DELAY);

        return view;
    }

    private class GetPropOperation extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(final String... params) {

            StringBuilder sb = new StringBuilder();
            sb.append("busybox mkdir -p ").append(dn).append(";\n");

            if (new File(syspath + mod + ".conf").exists()) {
                sb.append("busybox cp " + syspath).append(mod).append(".conf").append(" ")
                        .append(dn).append("/").append(mod).append(".conf;\n");
            } else {
                sb.append("busybox echo \"# created by DeviceControl\n\" > ")
                        .append(dn).append("/").append(mod).append(".conf;\n");
            }

            switch (mEditorType) {
                default:
                case 0:
                    sb.append("busybox echo `busybox find /proc/sys/vm/* -type f ")
                            .append("-prune -perm -644`;\n");
                    break;
                case 1:
                    sb.append("busybox echo `busybox find /proc/sys/* -type f -perm -644 |")
                            .append(" grep -v \"vm.\"`;\n");
                    break;
            }

            new FireAndGet(sb.toString(), readHandler).run();

            return null;
        }

        @Override
        protected void onPreExecute() {
            linlaHeaderProgress.setVisibility(View.VISIBLE);
            nofiles.setVisibility(View.GONE);
            tools.setVisibility(View.GONE);
        }

    }

    Handler readHandler = new Handler() {
        public void handleMessage(Message msg) {
            final int action = msg.getData().getInt(READ_VALUE_ACTION);
            final String text = msg.getData().getString(READ_VALUE_TEXT);

            switch (action) {
                case READ_VALUE_ACTION_RESULT:
                    if (mEditorType == 2) {
                        loadBuildProp(text);
                    } else {
                        loadProp(text);
                    }
                    break;
            }
        }
    };

    void loadProp(final String result) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if ((result != null) && (!result.isEmpty())) {
                    props.clear();
                    String[] p = result.split(" ");
                    for (String aP : p) {
                        if (aP.trim().length() > 0 && aP != null) {
                            final String pv = Utils.readOneLine(aP).trim();
                            final String pn = aP.trim().replace("/", ".").substring(10, aP.length());
                            props.add(new Prop(pn, pv));
                        }
                    }
                    Collections.sort(props);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            linlaHeaderProgress.setVisibility(View.GONE);
                            if (props.isEmpty()) {
                                nofiles.setVisibility(View.VISIBLE);
                            } else {
                                nofiles.setVisibility(View.GONE);
                                tools.setVisibility(View.VISIBLE);
                                adapter = new PropAdapter(getActivity(), props);
                                packList.setAdapter(adapter);
                            }
                        }
                    });
                }
            }
        }).run();
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view,
                            final int position, final long row) {
        final Prop p = adapter.getItem(position);
        if (p != null) {
            if (mEditorType == 2) {
                if (!p.getName().contains("fingerprint")) {
                    editBuildPropDialog(p);
                }
            } else {
                editPropDialog(p);
            }
        }
    }

    private void editPropDialog(final Prop p) {
        String title;

        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View editDialog = factory.inflate(R.layout.prop_edit_dialog, null);
        final EditText tv = (EditText) editDialog.findViewById(R.id.vprop);
        final TextView tn = (TextView) editDialog.findViewById(R.id.nprop);

        if (p != null) {
            tv.setText(p.getVal());
            tn.setText(p.getName());
            title = getString(R.string.etc_edit_prop_title);
        } else {
            title = getString(R.string.etc_add_prop_title);
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(editDialog)
                .setNegativeButton(getString(R.string.etc_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                            }
                        })
                .setPositiveButton(
                        getString(R.string.etc_save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (p != null) {
                            if (tv.getText().toString() != null) {
                                p.setVal(tv.getText().toString().trim());
                                String cmd = getActivity().getFilesDir() + "/utils -setprop \""
                                        + p.getName() + "=" + p.getVal() + "\" " + dn
                                        + "/" + mod + ".conf";
                                Shell.SU.run(cmd);
                                Log.e("BUILD", "cmd: " + cmd);
                            }
                        } else {
                            if (tv.getText().toString() != null
                                    && tn.getText().toString() != null
                                    && tn.getText().toString().trim().length() > 0) {
                                props.add(new Prop(tn.getText().toString().trim(),
                                        tv.getText().toString().trim()));
                                Shell.SU.run(getActivity().getFilesDir() + "/utils -setprop \""
                                        + tn.getText().toString().trim() + "="
                                        + tv.getText().toString().trim() + "\" " + dn + "/"
                                        + mod + ".conf");
                            }
                        }
                        Collections.sort(props);
                        adapter.notifyDataSetChanged();
                    }
                }).create().show();
    }

    private void makeDialog(String t, String m, byte op, Prop p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(t)
                .setMessage(m)
                .setNegativeButton(getString(R.string.dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .setPositiveButton(getString(R.string.dialog_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (theButton != null) {
            theButton.setOnClickListener(new CustomListener(alertDialog, op, p));
        }
    }

    void loadBuildProp(final String s) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                props.clear();
                String p[] = s.split("\n");
                for (String aP : p) {
                    if (!aP.contains("#") && aP.trim().length() > 0 && aP != null && aP.contains("=")) {
                        aP = aP.replace("[", "").replace("]", "");
                        String pp[] = aP.split("=");
                        if (pp.length >= 2) {
                            String r = "";
                            for (int i = 2; i < pp.length; i++) {
                                r = r + "=" + pp[i];
                            }
                            props.add(new Prop(pp[0].trim(), pp[1].trim() + r));
                        } else {
                            props.add(new Prop(pp[0].trim(), ""));
                        }
                    }
                }
                Collections.sort(props);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        linlaHeaderProgress.setVisibility(View.GONE);
                        if (props.isEmpty()) {
                            nofiles.setVisibility(View.VISIBLE);
                        } else {
                            nofiles.setVisibility(View.GONE);
                            tools.setVisibility(View.VISIBLE);
                            adapter = new PropAdapter(getActivity(), props);
                            packList.setAdapter(adapter);
                        }
                    }
                });
            }
        }).run();
    }


    private class GetBuildPropOperation extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {

            mBuildName = "build";
            mBuildName = (Build.DISPLAY.equals("") || Build.DISPLAY == null)
                    ? mBuildName + ".prop"
                    : mBuildName + "-" + Build.DISPLAY.replace(" ", "_") + ".prop";
            if (!new File(dn + "/" + mBuildName).exists()) {
                Shell.SH.run("busybox cp /system/build.prop " + dn + "/" + mBuildName);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(),
                                getString(R.string.etc_prop_backup, dn + "/" + mBuildName),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            List<String> result = Shell.SU.run("busybox find /system -type f -name "
                    + "\"*.ogg\" -print0");
            String resultString = "";
            for (String s : result) {
                resultString += s;
            }
            oggs = resultString.split("\0");
            new ReadValue("/system/build.prop", readHandler, true).run();

            return null;
        }

        @Override
        protected void onPreExecute() {
            linlaHeaderProgress.setVisibility(View.VISIBLE);
        }
    }

    private void editBuildPropDialog(final Prop p) {
        String title;

        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View editDialog = factory.inflate(R.layout.prop_build_prop_dialog, null);
        final EditText tv = (EditText) editDialog.findViewById(R.id.vprop);
        final EditText tn = (EditText) editDialog.findViewById(R.id.nprop);
        final TextView tt = (TextView) editDialog.findViewById(R.id.text1);
        final Spinner sp = (Spinner) editDialog.findViewById(R.id.spinner);
        final LinearLayout lpresets = (LinearLayout) editDialog.findViewById(R.id.lpresets);
        ArrayAdapter<CharSequence> vAdapter =
                new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item);
        vAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vAdapter.clear();


        if (p != null) {
            final String v = p.getVal();

            lpresets.setVisibility(LinearLayout.GONE);
            if (v.equals("0")) {
                vAdapter.add("0");
                vAdapter.add("1");
                lpresets.setVisibility(LinearLayout.VISIBLE);
                sp.setAdapter(vAdapter);
            } else if (v.equals("1")) {
                vAdapter.add("1");
                vAdapter.add("0");
                lpresets.setVisibility(LinearLayout.VISIBLE);
                sp.setAdapter(vAdapter);
            } else if (v.equalsIgnoreCase("true")) {
                vAdapter.add("true");
                vAdapter.add("false");
                lpresets.setVisibility(LinearLayout.VISIBLE);
                sp.setAdapter(vAdapter);
            } else if (v.equalsIgnoreCase("false")) {
                vAdapter.add("false");
                vAdapter.add("true");
                lpresets.setVisibility(LinearLayout.VISIBLE);
                sp.setAdapter(vAdapter);
            } else if (v.contains(".ogg")) {
                if (oggs.length > 0) {
                    vAdapter.add(v);
                    for (String ogg : oggs) {
                        File f = new File(ogg);
                        if (!f.getName().equalsIgnoreCase(v))
                            vAdapter.add(f.getName());
                    }
                    lpresets.setVisibility(LinearLayout.VISIBLE);
                    sp.setAdapter(vAdapter);
                }
            }

            tv.setText(p.getVal());
            tn.setText(p.getName());
            tn.setVisibility(EditText.GONE);
            tt.setText(p.getName());
            title = getString(R.string.etc_edit_prop_title);

        } else {//add
            title = getString(R.string.etc_add_prop_title);
            vAdapter.add("");
            vAdapter.add("0");
            vAdapter.add("1");
            vAdapter.add("true");
            vAdapter.add("false");
            sp.setAdapter(vAdapter);
            lpresets.setVisibility(LinearLayout.VISIBLE);
            tt.setText(getString(R.string.etc_prop_name));
            tn.setVisibility(EditText.VISIBLE);

        }
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                tv.setText(sp.getSelectedItem().toString().trim());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(editDialog)
                .setNegativeButton(getString(R.string.dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setPositiveButton(getString(R.string.etc_save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (p != null) {
                            if (tv.getText().toString() != null) {
                                p.setVal(tv.getText().toString().trim());
                                Shell.SU.run(getActivity().getFilesDir() + "/utils -setprop \""
                                        + p.getName() + "=" + p.getVal() + "\"");
                            }
                        } else {
                            if (tv.getText().toString() != null
                                    && tn.getText().toString() != null
                                    && tn.getText().toString().trim().length() > 0) {
                                props.add(new Prop(tn.getText().toString().trim(),
                                        tv.getText().toString().trim()));
                                Shell.SU.run(getActivity().getFilesDir() + "/utils -setprop \""
                                        + tn.getText().toString().trim() + "="
                                        + tv.getText().toString().trim() + "\"");
                            }
                        }

                        Collections.sort(props);
                        adapter.notifyDataSetChanged();
                    }
                }).create().show();
    }

    class CustomListener implements View.OnClickListener {
        private final Dialog dialog;
        private final byte op;
        private final Prop p;

        public CustomListener(Dialog dialog, byte op, Prop p) {
            this.dialog = dialog;
            this.op = op;
            this.p = p;
        }

        @Override
        public void onClick(View v) {
            dialog.cancel();
            switch (op) {
                case 0:
                    if (new File(dn + "/" + mBuildName).exists()) {
                        Shell.SU.run("mount -o rw,remount /system;\n" + "busybox cp " + dn + "/"
                                + mBuildName + " /system/build.prop;\n" + "busybox chmod 644 "
                                + "/system/build.prop;\n" + "mount -o ro,remount /system;\n");
                        new GetBuildPropOperation().execute();
                    } else {
                        Toast.makeText(getActivity(),
                                getString(R.string.etc_prop_no_backup), Toast.LENGTH_LONG).show();
                    }
                    break;
                case 1:
                    Shell.SU.run("mount -o rw,remount /system;\n" + "busybox sed -i '/"
                            + p.getName().replace(".", "\\.") + "/d' " + "/system/build.prop;\n"
                            + "mount -o ro,remount /system;\n");
                    adapter.remove(p);
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    }
}
