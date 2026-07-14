/*
 *  Copyright (C) 2011 Leonel Hernández Sandoval.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 */

package org.kde.necessitas.mucephi.android_xcas;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Dialog listing {@link Bookmarks}. Mirrors {@link HistoryDialog} so the two
 * entry points behave consistently.
 */
public class BookmarksDialog {

    public interface Listener {
        void onSelected(String input);
    }

    public static void show(Context context, final Bookmarks bookmarks, final Listener listener) {
        final ListView listView = new ListView(context);
        listView.setBackgroundColor(0xFFFFFFFF);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_list_item_1, bookmarks.snapshot()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(0xFF000000);
                text.setTextSize(16f);
                text.setSingleLine(false);
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String value = adapter.getItem(position);
                bookmarks.remove(value);
                adapter.remove(value);
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.action_bookmarks)
                .setView(listView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.bookmarks_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bookmarks.clear();
                    }
                });

        final AlertDialog dialog = builder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String value = adapter.getItem(position);
                if (listener != null) {
                    listener.onSelected(value);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
