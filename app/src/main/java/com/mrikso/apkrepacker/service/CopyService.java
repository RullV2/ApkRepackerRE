/*
 * Copyright (C) 2018 George Venios
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mrikso.apkrepacker.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.StatFs;

import com.mrikso.apkrepacker.fragment.base.BaseFilesFragment;
import com.mrikso.apkrepacker.ui.filemanager.holder.FileHolder;
import com.mrikso.apkrepacker.ui.filemanager.storage.operation.CopyOperation;
import com.mrikso.apkrepacker.ui.filemanager.storage.operation.FileOperationRunnerInjector;
import com.mrikso.apkrepacker.ui.filemanager.storage.operation.MoveOperation;
import com.mrikso.apkrepacker.ui.filemanager.storage.operation.argument.CopyArguments;
import com.mrikso.apkrepacker.ui.filemanager.storage.operation.argument.MoveArguments;
import com.mrikso.apkrepacker.ui.filemanager.storage.operation.ui.OperationStatusDisplayerInjector;
import com.mrikso.apkrepacker.ui.filemanager.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * To use, call the copyTo and moveTo static methods with the appropriate parameters.
 * <br/><br/><br/>
 * Internal usage instructions: <br/>
 * <ol>
 * <li>Pass the files to be copied/moved as a list of FileHolders on EXTRA_FILES.</li>
 * <li>Pass the path to copy/move to as the data string of the intent.</li>
 * <li>Choose between copy or move by using ACTION_COPY or ACTION_MOVE respectively.</li>
 * </ol>
 */
public class CopyService extends IntentService {
    private static final String ACTION_COPY = "com.mrikso.apkrepacker.action.COPY";
    private static final String ACTION_MOVE = "com.mrikso.apkrepacker.action.MOVE";
    private static final String EXTRA_FILES = "com.mrikso.apkrepacker.action.FILES";
    private static BaseFilesFragment mFilesFragment;

    public CopyService() {
        super(CopyService.class.getName());
    }

    public static void setFilesFragment(BaseFilesFragment filesFragment){
        mFilesFragment = filesFragment;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getData() == null) return;

        List<FileHolder> files = intent.getParcelableArrayListExtra(EXTRA_FILES);
        File to = new File(intent.getData().getPath());
        long remSpace;

        if (ACTION_COPY.equals(intent.getAction())) {
            remSpace = spaceRemainingAfterCopy(files, to);
            if (remSpace > 0) {
                copy(files, to);
            }
        } else if (ACTION_MOVE.equals(intent.getAction())) {
            remSpace = spaceRemainingAfterMove(files, to);
            if (remSpace > 0) {
                move(files, to);
            }
        } else {
            return;
        }

        if (remSpace > 0) {
            mFilesFragment.refresh(this, to);
        } else {
            //showNotEnoughSpaceNotification(-remSpace, files, to.getPath(), this);
        }
    }

    private void copy(final List<FileHolder> files, final File to) {
        try {
            FileOperationRunnerInjector.operationRunner(this).run(new CopyOperation(this, OperationStatusDisplayerInjector.operationStatusDisplayer(this)), CopyArguments.copyArgs(files, to, mFilesFragment));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void move(List<FileHolder> files, File to) {
        try {
            FileOperationRunnerInjector.operationRunner(this).run(new MoveOperation(this, OperationStatusDisplayerInjector.operationStatusDisplayer(this)), MoveArguments.moveArgs(files, to, mFilesFragment));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long spaceRemainingAfterCopy(List<FileHolder> of, File on) {
        long needed = 0;

        for (FileHolder f : of) {
            needed += (f.getFile().isDirectory() ? FileUtils.folderSize(f.getFile()) : f.getFile().length());
        }

        return on.getUsableSpace() - needed;
    }

    private static long spaceRemainingAfterMove(List<FileHolder> of, File on) {
        long needed = 0;
        // We know all clipboard files are on the same directory.
        boolean onSameStorage = onSameStorage(of.get(0).getFile(), on);

        for (FileHolder f : of) {
            if (!onSameStorage) {
                needed += (f.getFile().isDirectory() ? FileUtils.folderSize(f.getFile()) : f.getFile().length());
            }
        }

        return on.getUsableSpace() - needed;
    }

    /**
     * Bad but good enough for the common case. Someone, please write something better :)
     *
     * @return Whether these two files are on the same storage card/disk.
     * May return false positives but never false negatives.
     */
    private static boolean onSameStorage(File file1, File file2) {
        StatFs fs1 = new StatFs(file1.getAbsolutePath());
        StatFs fs2 = new StatFs(file2.getAbsolutePath());
        return fs1.getAvailableBlocksLong() == fs2.getAvailableBlocksLong()
                && fs1.getBlockCountLong() == fs2.getBlockCountLong()
                && fs1.getFreeBlocksLong() == fs2.getFreeBlocksLong()
                && fs1.getBlockSizeLong() == fs2.getBlockSizeLong();
    }

    public static void copyTo(Context c, List<FileHolder> mClipboard, File copyTo) {
        Intent i = new Intent(ACTION_COPY);
        i.setClassName(c, CopyService.class.getName());
        i.setData(Uri.fromFile(copyTo));
        i.putParcelableArrayListExtra(EXTRA_FILES,
                mClipboard instanceof ArrayList
                ? (ArrayList<FileHolder>) mClipboard
                : new ArrayList<>(mClipboard));
        c.startService(i);
    }

    public static void moveTo(Context c, List<FileHolder> mClipboard, File moveTo) {
        Intent i = new Intent(ACTION_MOVE);
        i.setClassName(c, CopyService.class.getName());
        i.setData(Uri.fromFile(moveTo));
        i.putParcelableArrayListExtra(EXTRA_FILES, mClipboard instanceof ArrayList
                ? (ArrayList<FileHolder>) mClipboard
                : new ArrayList<>(mClipboard));
        c.startService(i);
    }
}
