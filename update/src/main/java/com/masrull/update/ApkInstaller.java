package com.masrull.update;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import java.io.File;
import androidx.core.content.FileProvider;

/* loaded from: classes.dex */
public class ApkInstaller {
    public static void installApplication(Context context, String filePath) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(uriFromFile(context, new File(filePath)), "application/vnd.android.package-archive");
        intent.addFlags(268435456);
        intent.addFlags(1);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("ApkInstaller", "Error in opening the file!");
        }
    }

    private static Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(context, "com.mrikso.apkrepacker.provider", file);
        }
        return Uri.fromFile(file);
    }
}
