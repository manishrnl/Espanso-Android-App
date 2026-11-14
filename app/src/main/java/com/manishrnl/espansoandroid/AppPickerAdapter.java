package com.manishrnl.espansoandroid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AppPickerAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<PickerAppItem> allItems = new ArrayList<>();
    private final List<PickerAppItem> filteredItems = new ArrayList<>();

    public AppPickerAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setData(List<PickerAppItem> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        applyFilter("");
    }

    public void filter(String query) {
        applyFilter(query != null ? query.trim().toLowerCase(Locale.ROOT) : "");
    }

    private void applyFilter(String query) {
        filteredItems.clear();
        if (query.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            for (PickerAppItem item : allItems) {
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
    public PickerAppItem getItem(int position) {
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
            view = inflater.inflate(R.layout.row_app_picker, parent, false);
            holder = new ViewHolder(
                    view.findViewById(R.id.pickerAppIcon),
                    view.findViewById(R.id.pickerAppName),
                    view.findViewById(R.id.pickerAppPackage),
                    view.findViewById(R.id.pickerAppStatus)
            );
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        PickerAppItem item = getItem(position);
        holder.name.setText(item.appName);
        holder.packageName.setText(item.packageName);

        if (item.icon != null) {
            holder.icon.setImageDrawable(item.icon);
        } else {
            holder.icon.setImageResource(R.drawable.ic_shield);
        }

        holder.status.setVisibility(item.isAlreadyAdded ? View.VISIBLE : View.GONE);
        view.setAlpha(item.isAlreadyAdded ? 0.6f : 1.0f);

        return view;
    }

    public static final class PickerAppItem {
        public final String packageName;
        public final String appName;
        public final Drawable icon;
        public boolean isAlreadyAdded;

        public PickerAppItem(
                String packageName,
                String appName,
                Drawable icon,
                boolean isAlreadyAdded
        ) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
            this.isAlreadyAdded = isAlreadyAdded;
        }
    }

    private static final class ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final TextView status;

        ViewHolder(ImageView icon, TextView name, TextView packageName, TextView status) {
            this.icon = icon;
            this.name = name;
            this.packageName = packageName;
            this.status = status;
        }
    }
}
