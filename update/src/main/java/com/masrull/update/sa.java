package com.masrull.update;

import android.content.DialogInterface;

class sa implements DialogInterface.OnCancelListener {
    final /* synthetic */ DownloadTask this$0;
    final /* synthetic */ DownloadTask val$me;

    sa(DownloadTask this$0, DownloadTask downloadTask) {
        this.this$0 = this$0;
        this.val$me = downloadTask;
    }

    @Override // android.content.DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        this.val$me.cancel(true);
    }
}
