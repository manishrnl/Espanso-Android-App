package com.manishrnl.espansoandroid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExcludedAppsAdapter extends BaseAdapter {
    public interface OnDeleteClickListener {
        void onDeleteClick(ExcludedAppItem item);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final List<ExcludedAppItem> allItems = new ArrayList<>();
    private final List<ExcludedAppItem> filteredItems = new ArrayList<>();
    private OnDeleteClickListener deleteClickListener;
    private String currentQuery = "";

    public ExcludedAppsAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setData(List<ExcludedAppItem> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        applyFilter(currentQuery);
    }

    public void filter(String query) {
        this.currentQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        applyFilter(currentQuery);
    }

    private void applyFilter(String query) {
        filteredItems.clear();
        if (query.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            for (ExcludedAppItem item : allItems) {
                if (item.appName.toLowerCase(Locale.ROOT).contains(query)
                        || item.packageName.toLowerCase(Locale.ROOT).contains(query)) {
                    filteredItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Override
    public ExcludedAppItem getItem(int position) {
        return filteredItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).packageName.hashCode();
    }

    @Override
    public View getView(int position, View reusableView, ViewGroup parent) {
        View view = reusableView;
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.row_excluded_app, parent, false);
            holder = new ViewHolder(
                    view.findViewById(R.id.excludedAppIcon),
                    view.findViewById(R.id.excludedAppName),
                    view.findViewById(R.id.excludedAppPackage),
                    view.findViewById(R.id.excludedAppBadge),
                    view.findViewById(R.id.excludedAppDeleteButton)
            );
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        ExcludedAppItem item = getItem(position);
        holder.name.setText(item.appName);
        holder.packageName.setText(item.packageName);

        if (item.icon != null) {
            holder.icon.setImageDrawable(item.icon);
            holder.icon.setImageTintList(null);
        } else {
            holder.icon.setImageResource(R.drawable.ic_shield);
            holder.icon.setColorFilter(context.getColor(R.color.accent));
        }

        if (item.isDefaultBanking) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(R.string.badge_default);
        } else if (item.isInstalled) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(R.string.badge_installed);
        } else {
            holder.badge.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(item);
            }
        });

        return view;
    }

    public static final class ExcludedAppItem {
        public final String packageName;
        public final String appName;
        public final Drawable icon;
        public final boolean isInstalled;
        public final boolean isDefaultBanking;

        public ExcludedAppItem(
                String packageName,
                String appName,
                Drawable icon,
                boolean isInstalled,
                boolean isDefaultBanking
        ) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
            this.isInstalled = isInstalled;
            this.isDefaultBanking = isDefaultBanking;
        }
    }

    private static final class ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final TextView badge;
        final ImageButton deleteButton;

        ViewHolder(
                ImageView icon,
                TextView name,
                TextView packageName,
                TextView badge,
                ImageButton deleteButton
        ) {
            this.icon = icon;
            this.name = name;
            this.packageName = packageName;
            this.badge = badge;
            this.deleteButton = deleteButton;
        }
    }
}
