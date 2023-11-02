package com.masrull.update;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import java.io.File;

/* loaded from: classes.dex */
public class DownloaderManager {
  public Context c;
  public String url;
	protected String username = "cumaRull";

    public void startDownload() {
        startDownload__$prependPatch();
        startDownload__$prependSource();
    }

    public DownloaderManager(Context c, String url) {
        this.c = c;
        this.url = url;
    }

    private void startDownload__$prependPatch() {
        try {
            File f = new File(Environment.getExternalStorageDirectory() + "/Download");
            if (!f.isDirectory()) {
                f.mkdirs();
            }
        } catch (Exception e) {
        }
    }

    private void startDownload__$prependSource() {
        String data = username;
        try {
            String updt = new String(data);
            if (this.url.contains(updt)) {
            DownloadTask downloadTask = new DownloadTask(this.c);
            downloadTask.execute(this.url);
            }
        } catch (Exception e) {
        }
    }
}
