package com.masrull.update;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import com.masrull.update.R;

public class DownloadTask extends AsyncTask<String, Integer, String> {
    private String filepath;
    private final ProgressDialog mPDialog;
    private PowerManager.WakeLock mWakeLock;
    private final WeakReference<Context> weakContext;

    public DownloadTask(Context context) {
        this.weakContext = new WeakReference<>(context);
        this.mPDialog = new ProgressDialog(context);
        this.mPDialog.setMessage(this.weakContext.get().getResources().getString(R.string.update_downloading));
        this.mPDialog.setIndeterminate(true);
        this.mPDialog.setProgressStyle(1);
        this.mPDialog.setCancelable(false);
        this.mPDialog.setOnCancelListener(new sa(this, this));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Removed duplicated region for block: B:63:0x011a  */
    /* JADX WARN: Removed duplicated region for block: B:73:0x012e  */
    @Override // android.os.AsyncTask
    /*
	 Code decompiled incorrectly, please refer to instructions dump.
	 */
    public String doInBackground(String... sUrl)  {
        byte[] data;
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            try {
                URL url = new URL(sUrl[0]);
                HttpURLConnection connection2 = (HttpURLConnection) url.openConnection();
                connection2.connect();
                if (connection2.getResponseCode() != 200) {
                    String str = "Server returned HTTP " + connection2.getResponseCode() + " " + connection2.getResponseMessage();
                    if (0 != 0) {
                        try {
                            output.close();
                        } catch (IOException e) {
                        }
                    }
                    if (0 != 0) {
                        input.close();
                    }
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    return str;
                }
                int fileLength = connection2.getContentLength();
                int i = 1;
                String filename = sUrl[0].endsWith(".apk") ? sUrl[0].substring(sUrl[0].lastIndexOf("/") + 1) : "apkeditor.apk";
                this.filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename;
                InputStream input2 = connection2.getInputStream();
                OutputStream output2 = new FileOutputStream(this.filepath, false);
                byte[] data2 = new byte[4096];
                long total = 0;
                while (true) {
                    int count = input2.read(data2);
                    if (count == -1) {
                        try {
                            output2.close();
                            if (input2 != null) {
                                input2.close();
                            }
                        } catch (IOException e2) {
                        }
                        if (connection2 != null) {
                            connection2.disconnect();
                        }
                        return null;
                    } else if (isCancelled()) {
                        input2.close();
                        try {
                            output2.close();
                            if (input2 != null) {
                                input2.close();
                            }
                        } catch (IOException e3) {
                        }
                        if (connection2 != null) {
                            connection2.disconnect();
                        }
                        return null;
                    } else {
                        total += count;
                        if (fileLength > 0) {
                            Integer[] numArr = new Integer[i];
                            data = data2;
                            numArr[0] = Integer.valueOf((int) ((100 * total) / fileLength));
                            publishProgress(numArr);
                        } else {
                            data = data2;
                        }
                        byte[] data3 = data;
                        output2.write(data3, 0, count);
                        data2 = data3;
                        i = 1;
                    }
                }
            } catch (Exception e4) {
                String exc = e4.toString();
                if (0 != 0) {
                    try {
                        output.close();
                    } catch (IOException e5) {
                        if (0 != 0) {
                            connection.disconnect();
                        }
                        return exc;
                    }
                }
                if (0 != 0) {
                    input.close();
                }
                if (0 != 0) {
                }
                return exc;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    output.close();
                } catch (IOException e6) {
                    if (0 != 0) {
                        connection.disconnect();
                    }
                    try {
						throw th;
					} catch (Throwable e) {}
                }
            }
            if (0 != 0) {
                try {
					input.close();
				} catch (IOException e) {}
            }
            if (0 != 0) {
            }
            try {
				throw th;
			} catch (Throwable e) {}
        }
		return null;
	}

    @Override // android.os.AsyncTask
    protected void onPreExecute() {
        super.onPreExecute();
        PowerManager pm = (PowerManager) this.weakContext.get().getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, getClass().getName());
        this.mWakeLock.acquire();
        this.mPDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.os.AsyncTask
    public void onProgressUpdate(Integer... progress) {
        
        this.mPDialog.setIndeterminate(false);
        mPDialog.setMax(100);
        
        this.mPDialog.setProgress(progress[0].intValue());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.os.AsyncTask
    public void onPostExecute(String result) {
        this.mWakeLock.release();
        this.mPDialog.dismiss();
        if (result != null) {
            Toast.makeText(this.weakContext.get(), R.string.update_not_found + result, 1).show();
            return;
        }
        ApkInstaller.installApplication(this.weakContext.get(), this.filepath);
    }
}
