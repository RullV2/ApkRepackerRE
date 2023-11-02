package com.masrull.update;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.widget.Toast;
import com.masrull.update.R;

/* loaded from: classes.dex */
public class UpdaterAssistant {
    protected String Updates = "https://raw.githubusercontent.com/cumaRull/ApkRepacker/new-bugs/Release/Update-From-Json.json";
   
	Context c;
    boolean showToast;

    public UpdaterAssistant(Context c, boolean showToast) {
        this.c = c;
        this.showToast = showToast;
    }

    public void checkForUpdates() {
        if (isNetworkAvailable()) {
            String data = Updates;
            try {
                String preferences = new String(data);
                new UpdaterEngine(this.c, getCurrentChannel(), this.showToast).execute(preferences);
            } catch (Exception e) {
            }
        } else if (this.showToast) {
            Toast.makeText(this.c, (int) R.string.no_internet, 0).show();
        }
		}
		
	

    public String getCurrentChannel() {
        SharedPreferences prefs = this.c.getSharedPreferences("com.mrikso.apkrepacker_preferences", 0);
        return prefs.getString("channel", null);
    }
	
	

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) this.c.getSystemService("connectivity");
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        boolean haveConnectedMobile = false;
        boolean haveConnectedWifi = false;
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI") && ni.isConnected()) {
                haveConnectedWifi = true;
            }
            if (ni.getTypeName().equalsIgnoreCase("MOBILE") && ni.isConnected()) {
                haveConnectedMobile = true;
            }
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}
