package org.kde.necessitas.mucephi.android_xcas;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private static final String REPO = "hendr15k/android_xcaspad";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";

    public interface UpdateCallback {
        void onUpdateAvailable(String versionName, String releaseNotes, String apkUrl);
        void onNoUpdate();
        void onError(String message);
    }

    public static void checkForUpdate(final Context context, final String currentVersion, final UpdateCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int code = conn.getResponseCode();
                    if (code != 200) {
                        postError(callback, "GitHub API returned " + code);
                        return;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    JSONObject json = new JSONObject(sb.toString());
                    String tagName = json.getString("tag_name");
                    String body = json.optString("body", "");

                    String remoteVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                    String localVersion = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;

                    if (compareVersions(remoteVersion, localVersion) > 0) {
                        String apkUrl = findApkUrl(json);
                        if (apkUrl != null) {
                            final String finalRemote = tagName;
                            final String finalBody = body;
                            final String finalApk = apkUrl;
                            postUpdate(callback, finalRemote, finalBody, finalApk);
                        } else {
                            postError(callback, "No APK found in release");
                        }
                    } else {
                        postNoUpdate(callback);
                    }

                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private static String findApkUrl(JSONObject release) {
        try {
            JSONArray assets = release.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.getString("name");
                if (name.endsWith(".apk")) {
                    return asset.getString("browser_download_url");
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < parts1.length ? parseNum(parts1[i]) : 0;
            int n2 = i < parts2.length ? parseNum(parts2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private static int parseNum(String s) {
        try {
            StringBuilder digits = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (Character.isDigit(c)) digits.append(c);
                else break;
            }
            return digits.length() > 0 ? Integer.parseInt(digits.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long downloadApk(Context context, String apkUrl) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(apkUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("Xcas Pad Update");
        request.setDescription("Downloading update…");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "xcaspad-update.apk");
        request.setMimeType("application/vnd.android.package-archive");
        return dm.enqueue(request);
    }

    public static void installApk(Context context) {
        File apk = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "xcaspad-update.apk");
        if (!apk.exists()) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apk);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
        }

        context.startActivity(intent);
    }

    public static BroadcastReceiver installOnDownloadComplete(final Context context, final long downloadId) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    installApk(context);
                }
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        return receiver;
    }

    private static void postUpdate(final UpdateCallback cb, final String ver, final String notes, final String url) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                cb.onUpdateAvailable(ver, notes, url);
            }
        });
    }

    private static void postNoUpdate(final UpdateCallback cb) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                cb.onNoUpdate();
            }
        });
    }

    private static void postError(final UpdateCallback cb, final String msg) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                cb.onError(msg);
            }
        });
    }
}
