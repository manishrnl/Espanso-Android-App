package com.manishrnl.espansoandroid;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class TrashAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<TrashEntry> items = new ArrayList<>();

    public TrashAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public void setItems(List<TrashEntry> entries) {
        items.clear();
        items.addAll(entries);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public TrashEntry getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View reusableView, ViewGroup parent) {
        View view = reusableView;
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.row_trash, parent, false);
            holder = new ViewHolder(
                    view.findViewById(R.id.trashIcon),
                    view.findViewById(R.id.trashKindText),
                    view.findViewById(R.id.trashTitleText),
                    view.findViewById(R.id.trashDetailText),
                    view.findViewById(R.id.trashDateText)
            );
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        TrashEntry entry = getItem(position);
        holder.title.setText(entry.getTitle().replace("\\", " / "));
        holder.date.setText(formatDeletionDetails(context, entry.getDeletedAt()));
        if (entry.isFolder()) {
            holder.icon.setImageResource(R.drawable.ic_folder);
            holder.kind.setText(R.string.folder);
            holder.detail.setText(context.getResources().getQuantityString(
                    R.plurals.shortcut_count,
                    entry.getChildCount(),
                    entry.getChildCount()
            ));
        } else {
            holder.icon.setImageResource(R.drawable.ic_shortcut);
            holder.kind.setText(R.string.shortcut);
            String folder = entry.getFolder().trim();
            String location = folder.isEmpty()
                    ? context.getString(R.string.unfiled)
                    : folder.replace("\\", " / ");
            String replacement = TextUtils.isEmpty(entry.getDetail())
                    ? context.getString(R.string.empty_replacement)
                    : entry.getDetail();
            holder.detail.setText(context.getString(
                    R.string.trash_shortcut_detail,
                    replacement,
                    location
            ));
        }
        return view;
    }

    public static String formatDeletionDetails(Context context, long deletedAt) {
        DateFormat dateTimeFormat = new SimpleDateFormat("EEE, MMM d, yyyy 'at' HH:mm", Locale.getDefault());
        String formattedDate = dateTimeFormat.format(new Date(deletedAt));

        long expiresAt = deletedAt + ShortcutDatabase.TRASH_RETENTION_MILLIS;
        long diffMillis = expiresAt - System.currentTimeMillis();

        String autoDeleteText;
        if (diffMillis <= 0) {
            autoDeleteText = context.getString(R.string.auto_delete_today);
        } else {
            long daysLeft = (long) Math.ceil((double) diffMillis / (24L * 60L * 60L * 1000L));
            autoDeleteText = context.getResources().getQuantityString(
                    R.plurals.auto_delete_days,
                    (int) daysLeft,
                    (int) daysLeft
            );
        }

        return context.getString(R.string.trash_deletion_details, formattedDate, autoDeleteText);
    }

    private static final class ViewHolder {
        final ImageView icon;
        final TextView kind;
        final TextView title;
        final TextView detail;
        final TextView date;

        ViewHolder(
                ImageView icon,
                TextView kind,
                TextView title,
                TextView detail,
                TextView date
        ) {
            this.icon = icon;
            this.kind = kind;
            this.title = title;
            this.detail = detail;
            this.date = date;
        }
    }
}
