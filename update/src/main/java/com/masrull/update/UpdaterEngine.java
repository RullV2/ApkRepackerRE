package com.masrull.update;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import com.masrull.update.R;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class UpdaterEngine extends AsyncTask<String, Void, JSONObject> {
    WeakReference<Context> c;
    boolean showToast;
    String update_channel;
	Context saaa;

	private JSONObject UpdaterEngine;

    public UpdaterEngine(Context c, String update_channel, boolean showToast) {
        this.c = new WeakReference<>(c);
        this.update_channel = update_channel;
        this.showToast = showToast;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.os.AsyncTask
    public JSONObject doInBackground(String... params) {
        try {
            URL Url = new URL(params[0]);
            HttpURLConnection connection = (HttpURLConnection) Url.openConnection();
            InputStream is = connection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = br.readLine();
                if (line != null) {
                    sb.append(line);
                } else {
                    String line2 = sb.toString();
                    connection.disconnect();
                    is.close();
                    sb.delete(0, sb.length());
                    return (JSONObject) new JSONObject(line2).get(this.update_channel);
                }
            }
        } catch (Exception e) {
            Log.w("UpdaterEngine", e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.os.AsyncTask
    public void onPostExecute(JSONObject result) {
        super.onPostExecute(UpdaterEngine);
        try {
            if (result != null) {
                int version = result.getInt("current_version");
                final String linkApk = result.getString("download_url");
                String changelog = result.getString("changelog");
				
				
                int verCode = -1;
                
                try {
                    PackageInfo pInfo = this.c.get().getPackageManager().getPackageInfo(this.c.get().getPackageName(), 0);
                    verCode = pInfo.versionCode;
                    
          
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                if (version > verCode) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(this.c.get());
                    builder1.setTitle(R.string.update_title);
                    builder1.setMessage(changelog);
                    builder1.setCancelable(true);
                    builder1.setPositiveButton(R.string.update_download, new DialogInterface.OnClickListener() { // from class: com.alensw.updater.UpdaterEngine.1
							@Override // android.content.DialogInterface.OnClickListener
							public void onClick(DialogInterface dialog, int id) {
								new DownloaderManager(UpdaterEngine.this.c.get(), linkApk).startDownload();
							}
						});
                    builder1.setNegativeButton(R.string.update_close, new DialogInterface.OnClickListener() { // from class: com.alensw.updater.UpdaterEngine.2
							@Override // android.content.DialogInterface.OnClickListener
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
					
                } else {
                    noUpdatesToast();
                }
                return;
            }
            noUpdatesToast();
        } catch (Exception e2) {
            Log.w("UpdaterEngine", e2);
            noUpdatesToast();
        }
    }

    public void noUpdatesToast() {
        if (this.showToast) {
            Toast.makeText(this.c.get(), (int) R.string.update_not_found, 0).show();
        }
    }
	
	
}
