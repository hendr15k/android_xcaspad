/*
 *  Copyright (C) 2011 Leonel Hernández Sandoval.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.kde.necessitas.mucephi.android_xcas;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.appcompat.widget.SearchView;

import org.giac.xcaspad.Calculator;
import org.kde.necessitas.mucephi.android_xcas.adapteroperations.AdapterOperations;
import org.kde.necessitas.mucephi.android_xcas.adapteroperations.HolderOperation;
import org.kde.necessitas.mucephi.android_xcas.adapteroperations.TouchCallback;
import org.kde.necessitas.mucephi.android_xcas.aidehelp.AideParser;
import org.kde.necessitas.mucephi.android_xcas.aidehelp.HelpActivity;
import org.kde.necessitas.mucephi.android_xcas.History;
import org.kde.necessitas.mucephi.android_xcas.HistoryDialog;

import java.util.ArrayList;
import java.util.List;


public class XcasPadActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static int ACTIVITY_HELP = 0;
    private final static int ACTIVITY_SETTINGS = 1;
    private final static int ACTIVITY_ZOOMIN = 2;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL = 0;

    private RecyclerView mRecyclerView;
    private AdapterOperations mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<HolderOperation> operations = new ArrayList<HolderOperation>();
    private HolderOperation contextHolderOperation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xcas_pad);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final EditText txtInputOperation = findViewById(R.id.txt_input);

        mRecyclerView = findViewById(R.id.recycler_outputs);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        setupQuickKeys(txtInputOperation);

        /*  For every operation we have two components: entry to do calculus and his result.
            This adapter perform the contextual operations like to retrieve the text result into the input operation text box
         */

        AdapterOperations.InputListener inputListener = new AdapterOperations.InputListener() {
            @Override
            public void onInputClick(String result) {
                txtInputOperation.setText(result);
            }

            @Override
            public void onOutputClick(String result) {
                if (result != null && !result.trim().isEmpty()) {
                    showOutputDialog(result);
                } else {
                    txtInputOperation.setText(result);
                }
            }

            @Override
            public void onInputLongClick(View v, int position) {
                contextHolderOperation = mAdapter.getItem(position);
                v.showContextMenu();
            }

            @Override
            public void onOutputLongClick(View v, int position) {
                contextHolderOperation = mAdapter.getItem(position);
                v.showContextMenu();
            }
        };

        /*  This adapter perform the contextual operations that appears on a long click input or output
         */

        OperationContextMenu operationContextMenu = new OperationContextMenu(this, new OperationContextMenu.ContextMenuListener() {

            Bitmap img;
            String operation;

            @Override
            public void setHolderOperation(boolean isInput) {

                img = contextHolderOperation.getBmpOutput();
                operation = contextHolderOperation.getStrOutput();

                if(isInput){
                    img = contextHolderOperation.getBmpInput();
                    operation = contextHolderOperation.getStrInput();
                }
            }

            @Override
            public void contextZoomIn() {
                Intent intentZoomIn = new Intent(getApplicationContext(), ZoomInActivity.class);
                intentZoomIn.putExtra("operation", operation);
                startActivityForResult(intentZoomIn, ACTIVITY_ZOOMIN);
            }

            @Override
            public void contextCopyText() {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("xcas pad", operation);
                clipboard.setPrimaryClip(clip);
            }

            @Override
            public void contextAction(String action) {
                performOperation(String.format("%s(%s)",action, operation));
            }

            @Override
            public void contextBookmark() {
                Bookmarks bookmarks = Bookmarks.get(XcasPadActivity.this);
                boolean nowBookmarked = bookmarks.toggle(operation);
                int msgRes = nowBookmarked ? R.string.bookmark_added : R.string.bookmark_removed;
                showSnack(getString(msgRes));
                Haptics.success(XcasPadActivity.this);
            }

            @Override
            public void contextShareImage() {
                if (img == null) {
                    showSnack(getString(R.string.share_image_unavailable));
                    return;
                }
                shareBitmap(img, operation);
            }
        });

        /* The adapter that handles the list of input and their output operations.
         */

        mAdapter = new AdapterOperations(operations, inputListener, operationContextMenu);
        mAdapter.setChangeListener(new AdapterOperations.ChangeListener() {
            @Override
            public void onDatasetChanged() {
                SessionPersistence.get(XcasPadActivity.this).save(operations);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        new ItemTouchHelper(new TouchCallback(mAdapter, new TouchCallback.OnRemoveListener() {
            @Override
            public void onItemRemoved(final int position, final HolderOperation removed) {
                if (removed == null) {
                    return;
                }
                Snackbar.make(mRecyclerView, R.string.op_removed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mAdapter.insert(position, removed);
                                mRecyclerView.scrollToPosition(position);
                                Haptics.success(XcasPadActivity.this);
                            }
                        })
                        .show();
            }
        })).attachToRecyclerView(mRecyclerView);

        /*This function loads the session from one file .cas */

        SessionFromSender.load(this, new SessionFromSender.OnLoadFromSender() {
            @Override
            public void loadInBackground(List<String> lists) {
                for(String op: lists){
                    operations.add(Calculator.prettyPrint(op));
                }
            }

            @Override
            public void onFinishLoading() {
                restoreSession();
                mAdapter.notifyDataSetChanged();
            }
        });

        /*This is the button that perform the operations.*/

        findViewById(R.id.btn_doit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                evaluateCurrentInput();
            }
        });

        txtInputOperation.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(android.widget.TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    evaluateCurrentInput();
                    return true;
                }
                return false;
            }
        });

        checkForUpdates(false);
    }

    private void evaluateCurrentInput() {
        final EditText txtInputOperation = findViewById(R.id.txt_input);
        if (txtInputOperation == null) {
            return;
        }
        String input = txtInputOperation.getText().toString();
        if (input.equals("some_tests")) {
            performOperationsBatch(TestsOperations.operations);
        } else {
            performOperation(input);
            txtInputOperation.setText("");
        }
    }

    /* This function batches the UI updates for tests */
    private void performOperationsBatch(String[] inputs){
        boolean hasFailed = false;
        for (String input : inputs) {
            HolderOperation operation = Calculator.prettyPrint(input);
            operations.add(operation);
            History.get(this).add(input);

            String output = operation.getStrOutput();
            boolean failed = output == null || output.trim().isEmpty();
            if (failed) {
                hasFailed = true;
            }
        }

        mAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(mAdapter.getItemCount()-1);
        SessionPersistence.get(this).save(operations);

        if (hasFailed) {
            Haptics.error(this);
            showSnack(getString(R.string.op_error));
        } else {
            Haptics.success(this);
        }
    }

    /* This is the main function that reads the input an makes the operation through the callings to the JNI
     */
    private void performOperation(String input){
        HolderOperation operation = Calculator.prettyPrint(input);
        operations.add(operation);
        mAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(mAdapter.getItemCount()-1);
        History.get(this).add(input);
        SessionPersistence.get(this).save(operations);

        String output = operation.getStrOutput();
        boolean failed = output == null || output.trim().isEmpty();
        if (failed) {
            Haptics.error(this);
            showSnack(getString(R.string.op_error));
        } else {
            Haptics.success(this);
        }
    }

    private void showSnack(String message) {
        View anchor = findViewById(R.id.btn_doit);
        if (anchor == null) {
            anchor = findViewById(R.id.recycler_outputs);
        }
        Snackbar.make(anchor != null ? anchor : new android.widget.TextView(this),
                message, Snackbar.LENGTH_LONG).show();
    }

    private void shareBitmap(Bitmap bitmap, String caption) {
        if (bitmap == null) {
            showSnack(getString(R.string.share_image_unavailable));
            return;
        }
        ImageShareHelper.share(this, bitmap, caption);
    }

    private void showOutputDialog(String text) {
        final EditText textView = new EditText(this);
        textView.setText(text);
        textView.setTextIsSelectable(true);
        textView.setSingleLine(false);
        textView.setHorizontalScrollBarEnabled(true);
        textView.setVerticalScrollBarEnabled(true);
        textView.setTextColor(getResources().getColor(R.color.primaryText));
        textView.setBackgroundColor(getResources().getColor(R.color.inputBackground));
        textView.setPadding(32, 32, 32, 32);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(getResources().getColor(R.color.windowBackground));
        scroll.addView(textView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.output_dialog_title)
                .setView(scroll)
                .setNegativeButton(R.string.output_dialog_dismiss, null)
                .setNeutralButton(R.string.output_dialog_copy, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("xcas pad", text);
                        clipboard.setPrimaryClip(clip);
                        showSnack(getString(R.string.zoom_copied));
                        Haptics.success(XcasPadActivity.this);
                    }
                })
                .setPositiveButton(R.string.output_dialog_use, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        EditText input = findViewById(R.id.txt_input);
                        input.setText(text);
                        input.setSelection(text.length());
                    }
                })
                .create();
        dialog.show();
    }

    private void restoreSession() {
        if (!operations.isEmpty()) {
            return;
        }
        List<SessionPersistence.Entry> entries = SessionPersistence.get(this).restore();
        if (entries == null) {
            return;
        }
        for (SessionPersistence.Entry e : entries) {
            HolderOperation op = new HolderOperation();
            op.setStrInput(e.input);
            op.setStrOutput(e.output);
            if (e.bitmap != null) {
                op.setBmpInput(android.graphics.BitmapFactory.decodeByteArray(
                        e.bitmap, 0, e.bitmap.length));
            }
            operations.add(op);
        }
        if (mAdapter.getItemCount() > 0) {
            mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.xcas_pad, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                showSearchResults(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    private void showSearchResults(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        List<String> sessionMatches = new ArrayList<>();
        for (HolderOperation op : operations) {
            if (op.getStrInput() != null && op.getStrInput().contains(query)) {
                sessionMatches.add(op.getStrInput());
            }
        }
        List<String> historyMatches = History.get(this).snapshot();
        List<String> finalMatches = new ArrayList<>();
        for (String h : historyMatches) {
            if (h.contains(query) && !sessionMatches.contains(h)) {
                finalMatches.add(h);
            }
        }
        sessionMatches.addAll(finalMatches);

        if (sessionMatches.isEmpty()) {
            showSnack(getString(R.string.search_no_results));
            return;
        }

        HistoryDialog.show(this, History.get(this), new HistoryDialog.Listener() {
            @Override
            public void onSelected(String selected) {
                EditText input = findViewById(R.id.txt_input);
                input.setText(selected);
                input.setSelection(selected.length());
            }
        });
        // We'll just toast the count of matches for now since HistoryDialog only shows history snapshot.
        // To show actual search results, a custom dialog would be better, but let's keep it simple.
        showSnack(getString(R.string.search_results_found, sessionMatches.size()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intentSettings = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(intentSettings, ACTIVITY_SETTINGS);
            return true;
        }
        else if (id == R.id.action_save_session){
            if(requestWritePermission())
                SaveSession.download(this, operations);
        }
        else if (id == R.id.action_share_session){
            if (operations == null || operations.isEmpty()) {
                showSnack(getString(R.string.share_session_empty));
            } else {
                SessionShareHelper.share(this, operations);
                showSnack(getString(R.string.share_session_title));
            }
        }
        else if (id == R.id.action_export_latex){
            if (operations == null || operations.isEmpty()) {
                showSnack(getString(R.string.export_latex_empty));
            } else {
                LatexExportHelper.share(this, operations);
                showSnack(getString(R.string.export_latex_title));
            }
        }
        else if(id == R.id.action_clear_session){
            operations.clear();
            mAdapter.notifyDataSetChanged();
            SessionPersistence.get(this).clear();
        }
        else if(id == R.id.action_check_update){
            checkForUpdates(true);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_help) {

            Intent intentHelp = new Intent(getApplicationContext(), HelpActivity.class);
            startActivityForResult(intentHelp, ACTIVITY_HELP);

        } else if (id == R.id.nav_history) {
            HistoryDialog.show(this, History.get(this), new HistoryDialog.Listener() {
                @Override
                public void onSelected(String input) {
                    EditText txtInput = findViewById(R.id.txt_input);
                    txtInput.setText(input);
                    txtInput.setSelection(input.length());
                }
            });
        } else if (id == R.id.nav_bookmarks) {
            BookmarksDialog.show(this, Bookmarks.get(this), new BookmarksDialog.Listener() {
                @Override
                public void onSelected(String input) {
                    EditText txtInput = findViewById(R.id.txt_input);
                    txtInput.setText(input);
                    txtInput.setSelection(input.length());
                }
            });
        } else if (id == R.id.nav_settings) {
            Intent intentSettings = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(intentSettings, ACTIVITY_SETTINGS);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == ACTIVITY_HELP){
            if(resultCode == RESULT_OK && data != null){
                ((EditText)findViewById(R.id.txt_input)).setText(data.getStringExtra("function"));
            }
        }
        else if(requestCode == ACTIVITY_SETTINGS){
            if(resultCode == RESULT_OK && data != null){
                String changed = data.getStringExtra("changed");
                if("lang".equals(changed)){
                    AideParser.reset();
                }
            }
        }
    }

    private boolean requestWritePermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL);
            }

            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    SaveSession.download(this, operations);

                } else {

                }
                return;
            }
        }
    }

    private void checkForUpdates(final boolean showFeedback) {
        String currentVersion;
        try {
            currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            currentVersion = "0.0.0";
        }

        UpdateChecker.checkForUpdate(this, currentVersion, new UpdateChecker.UpdateCallback() {
            @Override
            public void onUpdateAvailable(String versionName, String releaseNotes, final String apkUrl) {
                String currentVer;
                try {
                    currentVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (Exception e) {
                    currentVer = "?";
                }

                String message = getString(R.string.update_available_message, versionName, currentVer, releaseNotes);
                if (message.length() > 500) {
                    message = message.substring(0, 500) + "…";
                }

                new AlertDialog.Builder(XcasPadActivity.this)
                        .setTitle(R.string.update_available_title)
                        .setMessage(message)
                        .setPositiveButton(R.string.update_download, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                showSnack(getString(R.string.update_downloading));
                                long downloadId = UpdateChecker.downloadApk(XcasPadActivity.this, apkUrl);
                                UpdateChecker.installOnDownloadComplete(XcasPadActivity.this, downloadId);
                            }
                        })
                        .setNegativeButton(R.string.update_later, null)
                        .show();
            }

            @Override
            public void onNoUpdate() {
                if (showFeedback) {
                    showSnack(getString(R.string.update_up_to_date));
                }
            }

            @Override
            public void onError(String message) {
                if (showFeedback) {
                    showSnack(getString(R.string.update_check_failed));
                }
            }
        });
    }

    private void showMessage(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(XcasPadActivity.this);

        builder.setMessage(message)
                .setTitle(R.string.app_name);

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void setupQuickKeys(final EditText input) {
        LinearLayout container = findViewById(R.id.quick_keys_container);
        if (container == null) {
            return;
        }
        String[] keys = {"(", ")", "[", "]", "{", "}", "^", "=", ",", ":", "pi", "e", "sqrt(", "abs(", "sin(", "cos(", "tan(", "log(", "ln("};
        for (final String key : keys) {
            Button btn = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(4, 0, 4, 0);
            btn.setLayoutParams(params);
            btn.setText(key);
            btn.setPadding(8, 4, 8, 4);
            btn.setMinWidth(0);
            btn.setMinHeight(0);
            btn.setTextSize(14f);
            btn.setAllCaps(false);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int start = input.getSelectionStart();
                    int end = input.getSelectionEnd();
                    if (start < 0) {
                        input.append(key);
                    } else {
                        String text = input.getText().toString();
                        input.setText(text.substring(0, start) + key + text.substring(end));
                        input.setSelection(start + key.length());
                    }
                }
            });
            container.addView(btn);
        }
    }
}
