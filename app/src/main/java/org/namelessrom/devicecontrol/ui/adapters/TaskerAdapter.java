package org.namelessrom.devicecontrol.ui.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.actions.ActionProcessor;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.database.TaskerItem;
import org.namelessrom.devicecontrol.utils.DrawableHelper;
import org.namelessrom.devicecontrol.wizard.AddTaskActivity;

import java.util.List;

public class TaskerAdapter extends RecyclerView.Adapter<TaskerAdapter.TaskerViewHolder> {
    private Activity mActivity;
    private List<TaskerItem> mTasks;

    public TaskerAdapter(final Activity activity, final List<TaskerItem> tasks) {
        mActivity = activity;
        mTasks = tasks;
    }

    @Override public int getItemCount() {
        return mTasks == null ? 0 : mTasks.size();
    }

    @Override public TaskerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final int resId;
        if (Application.get().isDarkTheme()) {
            resId = R.layout.card_tasker_dark;
        } else {
            resId = R.layout.card_tasker_light;
        }
        final View v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
        return new TaskerViewHolder(v);
    }

    @Override public void onBindViewHolder(TaskerViewHolder holder, int position) {
        final TaskerItem item = mTasks.get(position);

        holder.image.setImageDrawable(ActionProcessor.getImageForCategory(item.category));
        holder.trigger.setText(item.trigger);
        holder.action.setText(item.name);
        holder.value.setText(item.value);
        holder.enabled.setChecked(item.enabled);
        holder.enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.enabled = isChecked;
                DatabaseHandler.getInstance().updateOrInsertTaskerItem(item);
            }
        });

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                final Intent intent = new Intent(mActivity, AddTaskActivity.class);
                intent.putExtra(AddTaskActivity.ARG_ITEM, item);
                mActivity.startActivity(intent);
            }
        });

        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setIcon(
                        DrawableHelper.applyAccentColorFilter(R.drawable.ic_general_trash));
                alert.setTitle(R.string.delete_task);
                alert.setMessage(mActivity.getString(R.string.delete_task_question));
                alert.setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int b) {
                                d.dismiss();
                            }
                        });
                alert.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int b) {
                                DatabaseHandler.getInstance().deleteTaskerItem(item);
                                mTasks.remove(item);
                                d.dismiss();
                                notifyDataSetChanged();
                            }
                        });
                alert.show();
                return true;
            }
        });
    }

    public static class TaskerViewHolder extends RecyclerView.ViewHolder {
        public final CardView cardView;
        public final ImageView image;
        public final TextView trigger;
        public final TextView action;
        public final TextView value;
        public final Switch enabled;

        public TaskerViewHolder(final View view) {
            super(view);
            cardView = (CardView) view;
            image = (ImageView) cardView.findViewById(R.id.task_image);
            trigger = (TextView) cardView.findViewById(R.id.trigger);
            action = (TextView) cardView.findViewById(R.id.action);
            value = (TextView) cardView.findViewById(R.id.value);
            enabled = (Switch) cardView.findViewById(R.id.enabled);
        }
    }

}
