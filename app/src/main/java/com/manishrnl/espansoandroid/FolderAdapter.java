package com.manishrnl.espansoandroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FolderAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<FolderItem> items = new ArrayList<>();

    public FolderAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public void setShortcuts(List<Shortcut> shortcuts) {
        Map<String, Integer> counts = new HashMap<>();
        for (Shortcut shortcut : shortcuts) {
            String folder = shortcut.getFolder().trim();
            counts.put(folder, counts.getOrDefault(folder, 0) + 1);
        }

        items.clear();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            items.add(new FolderItem(entry.getKey(), entry.getValue()));
        }
        items.sort((left, right) -> {
            String leftName = left.displayName(context).toLowerCase(Locale.ROOT);
            String rightName = right.displayName(context).toLowerCase(Locale.ROOT);
            int comparison = leftName.compareTo(rightName);
            if (comparison != 0) {
                return comparison;
            }
            return left.displayName(context).compareTo(right.displayName(context));
        });
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public FolderItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getFolder().hashCode();
    }

    @Override
    public View getView(int position, View reusableView, ViewGroup parent) {
        View view = reusableView;
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.row_folder, parent, false);
            holder = new ViewHolder(
                    view.findViewById(R.id.folderHeaderText),
                    view.findViewById(R.id.folderCountText)
            );
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        FolderItem item = getItem(position);
        holder.name.setText(item.displayName(context).replace("\\", " / "));
        holder.count.setText(context.getResources().getQuantityString(
                R.plurals.shortcut_count,
                item.getCount(),
                item.getCount()
        ));
        return view;
    }

    public static final class FolderItem {
        private final String folder;
        private final int count;

        FolderItem(String folder, int count) {
            this.folder = folder;
            this.count = count;
        }

        public String getFolder() {
            return folder;
        }

        public int getCount() {
            return count;
        }

        String displayName(Context context) {
            return folder.isEmpty() ? context.getString(R.string.unfiled) : folder;
        }
    }

    private static final class ViewHolder {
        final TextView name;
        final TextView count;

        ViewHolder(TextView name, TextView count) {
            this.name = name;
            this.count = count;
        }
    }
}
