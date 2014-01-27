/*
 * Copyright (C) 2014 Alexander "Evisceration" Martinz
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses
 */

package org.namelessrom.devicecontrol.fragments.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.utils.CMDProcessor;
import org.namelessrom.devicecontrol.utils.adapters.PackAdapter;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;


public class ToolsFreezer extends Fragment
        implements DeviceConstants, AdapterView.OnItemClickListener {

    private static final String ARG_FREEZER = "arg_freezer";
    private LinearLayout linlaHeaderProgress;
    private LinearLayout linNopack, llist;
    private Switch mSwitch;
    private String pmList[];
    private PackageManager packageManager;
    private ListView packList;
    private PackAdapter adapter;
    private int curpos;
    private Boolean freeze;
    private String packs;
    private String pn;
    private String titlu;

    public static ToolsFreezer newInstance(final int freezer) {
        Bundle b = new Bundle();
        b.putBoolean(ToolsFreezer.ARG_FREEZER, (freezer == 0));
        ToolsFreezer fragment = new ToolsFreezer();
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View v = layoutInflater.inflate(R.layout.tools_freezer_list, viewGroup, false);


        freeze = getArguments().getBoolean(ToolsFreezer.ARG_FREEZER, false);
        packs = "sys"; // XXX getArguments().getString("packs", "usr");

        pmList = new String[]{};
        packageManager = getActivity().getPackageManager();

        linlaHeaderProgress = (LinearLayout) v.findViewById(R.id.linlaHeaderProgress);
        linNopack = (LinearLayout) v.findViewById(R.id.noproc);

        llist = (LinearLayout) v.findViewById(R.id.llist);
        mSwitch = (Switch) v.findViewById(R.id.tools_freezer_toggle);

        packList = (ListView) v.findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        if (freeze) {
            titlu = getString(R.string.pt_freeze);
        } else {
            titlu = getString(R.string.pt_unfreeze);
        }
        new GetPacksOperation().execute();

        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long row) {
        pn = (String) parent.getItemAtPosition(position);
        curpos = position;
        if (freeze) {
            makedialog(titlu, getString(R.string.freeze_msg, pn));
        } else {
            makedialog(titlu, getString(R.string.unfreeze_msg, pn));
        }

    }

    private class GetPacksOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            CMDProcessor.CommandResult cr;
            if (!freeze) {
                cr = new CMDProcessor()
                        .sh.runWaitFor("busybox echo `pm list packages -d | cut -d':' -f2`");
            } else {
                if (packs.equals("sys")) {
                    cr = new CMDProcessor()
                            .sh.runWaitFor("busybox echo `pm list packages -s -e | cut -d':' -f2`");
                } else {
                    cr = new CMDProcessor()
                            .sh.runWaitFor("busybox echo `pm list packages -3 -e | cut -d':' -f2`");
                }
            }
            if (cr.success() && !cr.stdout.equals(""))
                return cr.stdout;
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                pmList = result.split(" ");
            }
            linlaHeaderProgress.setVisibility(View.GONE);
            if (pmList.length > 0) {
                adapter = new PackAdapter(getActivity(), pmList, packageManager);
                packList.setAdapter(adapter);
                linNopack.setVisibility(View.GONE);
                llist.setVisibility(LinearLayout.VISIBLE);
                if (!freeze) {
                    // TODO
                } else {
                    mSwitch.setChecked(packs.equals("sys"));
                }
            } else {
                linNopack.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPreExecute() {
            linlaHeaderProgress.setVisibility(View.VISIBLE);
            linNopack.setVisibility(View.GONE);
            llist.setVisibility(LinearLayout.GONE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    private void makedialog(String titlu, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titlu)
                .setMessage(msg)
                .setNegativeButton(getString(R.string.dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                //finish();
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
        //alertDialog.setCancelable(false);
        Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (theButton != null) {
            theButton.setOnClickListener(new FreezeListener(alertDialog));
        }
    }

    class FreezeListener implements View.OnClickListener {
        private final Dialog dialog;

        public FreezeListener(Dialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {
            dialog.cancel();
            new FreezeOperation().execute();
        }
    }

    private class FreezeOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            CMDProcessor.CommandResult cr;
            if (freeze) {
                cr = new CMDProcessor().su.runWaitFor("pm disable " + pn + " 2> /dev/null");
            } else {
                cr = new CMDProcessor().su.runWaitFor("pm enable " + pn + " 2> /dev/null");
            }
            if (cr.success()) {
                return "ok";
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals("ok")) {
                adapter.delItem(curpos);
                adapter.notifyDataSetChanged();
                if (adapter.isEmpty()) {
                    llist.setVisibility(LinearLayout.GONE);
                    linNopack.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
