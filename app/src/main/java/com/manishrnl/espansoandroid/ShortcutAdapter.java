package com.manishrnl.espansoandroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class ShortcutAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<Shortcut> items = new ArrayList<>();

    public ShortcutAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setItems(List<Shortcut> shortcuts) {
        items.clear();
        items.addAll(shortcuts);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Shortcut getItem(int position) {
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
            view = inflater.inflate(R.layout.row_shortcut, parent, false);
            holder = new ViewHolder(
                    view.findViewById(R.id.keywordText),
                    view.findViewById(R.id.replacementText),
                    view.findViewById(R.id.folderText),
                    view.findViewById(R.id.modeText)
            );
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Shortcut shortcut = getItem(position);
        holder.keyword.setText(shortcut.getKeyword());
        holder.replacement.setText(shortcut.getText());
        holder.folder.setText(shortcut.getFolder());
        holder.folder.setVisibility(
                shortcut.getFolder().trim().isEmpty() ? View.GONE : View.VISIBLE
        );
        holder.mode.setText(
                shortcut.isReplaceAfterSpace() ? R.string.after_space : R.string.instant
        );
        return view;
    }

    private static final class ViewHolder {
        final TextView keyword;
        final TextView replacement;
        final TextView folder;
        final TextView mode;

        ViewHolder(
                TextView keyword,
                TextView replacement,
                TextView folder,
                TextView mode
        ) {
            this.keyword = keyword;
            this.replacement = replacement;
            this.folder = folder;
            this.mode = mode;
        }
    }
}
