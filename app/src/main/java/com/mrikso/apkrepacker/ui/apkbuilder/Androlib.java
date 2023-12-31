/**
 *  Copyright (C) 2019 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2019 Connor Tumbleson <connor.tumbleson@gmail.com>
 *  Copyright (C) 2020 Mr Ikso <mriko821@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mrikso.apkrepacker.ui.apkbuilder;

import android.content.Context;

import com.mrikso.apkrepacker.utils.common.DLog;
import com.mrikso.apktool.R;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import brut.androlib.ApkOptions;
import brut.androlib.ApktoolProperties;
import brut.androlib.meta.MetaInfo;
import brut.androlib.meta.UsesFramework;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResUnknownFiles;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.androlib.src.SmaliDecoder;
import brut.common.BrutException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.BrutIO;
import brut.util.Logger;
import brut.util.OS;

//import java.util.logging.Logger;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Androlib implements ISmaliCallback {

    private final String TAG = "Androlib";

    private final AndrolibResources mAndRes;
    protected final ResUnknownFiles mResUnknownFiles = new ResUnknownFiles();
    public ApkOptions apkOptions;
    private int mMinSdkVersion = 0;
    private TaskStepInfo mStepInfo = new TaskStepInfo();
    private IBuilderCallback mBuilderCallback;
    private Context mContext;

    public Androlib(ApkOptions apkOptions, Logger LOGGER) {
        this.LOGGER = LOGGER;
        mAndRes = new AndrolibResources(LOGGER);
        this.apkOptions = apkOptions;
        mAndRes.apkOptions = apkOptions;
    }

    public void setCallback(IBuilderCallback callback){
        mBuilderCallback = callback;
    }

    public void setContext(Context context){
        mContext = context;
    }

    public ResTable getResTable(ExtFile apkFile)
            throws AndrolibException {
        return mAndRes.getResTable(apkFile, true);
    }

    public ResTable getResTable(ExtFile apkFile, boolean loadMainPkg)
            throws AndrolibException {
        return mAndRes.getResTable(apkFile, loadMainPkg);
    }

//decode methods
    public void decodeSourcesRaw(ExtFile apkFile, File outDir, String filename)
            throws AndrolibException {
        try {
            LOGGER.info(R.string.log_build_copy_file,"Copying raw " + filename + " file...");
            apkFile.getDirectory().copyToDir(outDir, filename);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeSourcesSmali(File apkFile, File outDir, String filename, boolean bakdeb, int api)
            throws AndrolibException {
        try {
            File smaliDir;
            if (filename.equalsIgnoreCase("classes.dex")) {
                smaliDir = new File(outDir, SMALI_DIRNAME);
            } else {
                smaliDir = new File(outDir, SMALI_DIRNAME + "_" + filename.substring(0, filename.indexOf(".")));
            }
            OS.rmdir(smaliDir);
            smaliDir.mkdirs();
            LOGGER.info(R.string.log_text,"Baksmaling " + filename + "...");
            SmaliDecoder.decode(apkFile, smaliDir, filename, bakdeb, api);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeManifestRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        try {
            LOGGER.info(R.string.log_text,"Copying raw manifest...");
            apkFile.getDirectory().copyToDir(outDir, APK_MANIFEST_FILENAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeManifestFull(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decodeManifest(resTable, apkFile, outDir);
    }

    public void decodeResourcesRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        try {
            LOGGER.info(R.string.log_text,"Copying raw resources...");
            apkFile.getDirectory().copyToDir(outDir, APK_RESOURCES_FILENAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeResourcesFull(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decode(resTable, apkFile, outDir);
    }

    public void decodeManifestWithResources(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decodeManifestWithResources(resTable, apkFile, outDir);
    }

    public void decodeRawFiles(ExtFile apkFile, File outDir, short decodeAssetMode)
            throws AndrolibException {
        LOGGER.info(R.string.log_text,"Copying assets and libs...");
        try {
            Directory in = apkFile.getDirectory();

            if (decodeAssetMode == ApkDecoder.DECODE_ASSETS_FULL) {
                if (in.containsDir("assets")) {
                    in.copyToDir(outDir, "assets");
                }
            }
            if (in.containsDir("lib")) {
                in.copyToDir(outDir, "lib");
            }
            if (in.containsDir("libs")) {
                in.copyToDir(outDir, "libs");
            }
            if (in.containsDir("kotlin")) {
                in.copyToDir(outDir, "kotlin");
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void recordUncompressedFiles(ExtFile apkFile, Collection<String> uncompressedFilesOrExts) throws AndrolibException {
        try {
            Directory unk = apkFile.getDirectory();
            Set<String> files = unk.getFiles(true);
            String ext;

            for (String file : files) {
                if (isAPKFileNames(file) && unk.getCompressionLevel(file) == 0 && unk.getSize(file) != 0) {
                    ext = FilenameUtils.getExtension(file);

                    if (ext.isEmpty() || !NO_COMPRESS_PATTERN.matcher(ext).find()) {
                        ext = file;
                    }
                    if (!uncompressedFilesOrExts.contains(ext)) {
                        uncompressedFilesOrExts.add(ext);
                    }
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean isAPKFileNames(String file) {
        for (String apkFile : APK_STANDARD_ALL_FILENAMES) {
            if (apkFile.equals(file) || file.startsWith(apkFile + "/")) {
                return true;
            }
        }
        return false;
    }

    public void decodeUnknownFiles(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        LOGGER.info(R.string.log_text,"Copying unknown files...");
        File unknownOut = new File(outDir, UNK_DIRNAME);
        try {
            Directory unk = apkFile.getDirectory();

            // loop all items in container recursively, ignoring any that are pre-defined by aapt
            Set<String> files = unk.getFiles(true);
            for (String file : files) {
                if (!isAPKFileNames(file) && !file.endsWith(".dex")) {

                    // copy file out of archive into special "unknown" folder
                    unk.copyToDir(unknownOut, file);
                    // lets record the name of the file, and its compression type
                    // so that we may re-include it the same way
                    mResUnknownFiles.addUnknownFileInfo(file, String.valueOf(unk.getCompressionLevel(file)));
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void writeOriginalFiles(ExtFile apkFile, File outDir)
            throws AndrolibException {
        LOGGER.info(R.string.log_text,"Copying original files...");
        File originalDir = new File(outDir, "original");
        if (!originalDir.exists()) {
            originalDir.mkdirs();
        }

        try {
            Directory in = apkFile.getDirectory();
            if (in.containsFile("AndroidManifest.xml")) {
                in.copyToDir(originalDir, "AndroidManifest.xml");
            }
            if (in.containsDir("META-INF")) {
                in.copyToDir(originalDir, "META-INF");

                if (in.containsDir("META-INF/services")) {
                    // If the original APK contains the folder META-INF/services folder
                    // that is used for service locators (like coroutines on android),
                    // copy it to the destination folder so it does not get dropped.
                    LOGGER.info(R.string.log_text,"Copying META-INF/services directory");
                    in.copyToDir(outDir, "META-INF/services");
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void writeMetaFile(File mOutDir, MetaInfo meta)
            throws AndrolibException {
        try {
            meta.save(new File(mOutDir, "apktool.json"));
        } catch (IOException|JSONException ex) {
            throw new AndrolibException(ex);
        }
    }

    public MetaInfo readMetaFile(ExtFile appDir)
            throws AndrolibException {
        try {
            InputStream in = appDir.getDirectory().getFileInput("apktool.json");
            MetaInfo meta = MetaInfo.load(in);
            in.close();
            return meta;
        } catch (DirectoryException | IOException |JSONException ex) {
            throw new AndrolibException(ex);
        }
    }

    //build methods

    public MetaInfo build(File appDir, File outFile) throws BrutException {
        return build(new ExtFile(appDir), outFile);
    }

    public MetaInfo build(ExtFile appDir, File outFile)
            throws BrutException {
        LOGGER.info(R.string.log_text,"Using Apktool " + Androlib.getVersion());

        MetaInfo meta = readMetaFile(appDir);
        apkOptions.isFramework = meta.isFrameworkApk;
        apkOptions.resourcesAreCompressed = meta.compressionType;
        apkOptions.doNotCompress = meta.doNotCompress;

        mAndRes.setSdkInfo(meta.sdkInfo);
        mAndRes.setPackageId(meta.packageInfo);
        mAndRes.setPackageRenamed(meta.packageInfo);
        mAndRes.setVersionInfo(meta.versionInfo);
        mAndRes.setSharedLibrary(meta.sharedLibrary);
        mAndRes.setSparseResources(meta.sparseResources);

        if (meta.sdkInfo != null && meta.sdkInfo.get("minSdkVersion") != null) {
            String minSdkVersion = meta.sdkInfo.get("minSdkVersion");
            mMinSdkVersion = mAndRes.getMinSdkVersionFromAndroidCodename(meta, minSdkVersion);
        }

        if (outFile == null) {
            String outFileName = meta.apkFileName;
            outFile = new File(appDir, "dist" + File.separator + (outFileName == null ? "out.apk" : outFileName));
        }

        new File(appDir, APK_DIRNAME).mkdirs();
        File manifest = new File(appDir, "AndroidManifest.xml");
        File manifestOriginal = new File(appDir, "AndroidManifest.xml.orig");

        mStepInfo.stepTotal = 0;
        mStepInfo.stepTotal++;
        buildSources(appDir);

        buildNonDefaultSources(appDir);
        buildManifestFile(appDir, manifest, manifestOriginal);
        buildResources(appDir, meta.usesFramework);
        buildLibs(appDir);
        buildCopyOriginalFiles(appDir);
        mStepInfo.stepTotal++;
        buildApk(appDir, outFile);

        // we must go after the Apk is built, and copy the files in via Zip
        // this is because Aapt won't add files it doesn't know (ex unknown files)
        buildUnknownFiles(appDir, outFile, meta);

        // we copied the AndroidManifest.xml to AndroidManifest.xml.orig so we can edit it
        // lets restore the unedited one, to not change the original
        if (manifest.isFile() && manifest.exists() && manifestOriginal.isFile()) {
            try {
                if (new File(appDir, "AndroidManifest.xml").delete()) {
                    FileUtils.moveFile(manifestOriginal, manifest);
                }
            } catch (IOException ex) {
                throw new AndrolibException(ex.getMessage());
            }
        }
        // final
        LOGGER.info(R.string.log_text,"Built apk...");
        setNextStep("Building apk");
        return meta;
    }

    private void buildManifestFile(File appDir, File manifest, File manifestOriginal)
            throws AndrolibException {

        // If we decoded in "raw", we cannot patch AndroidManifest
        if (new File(appDir, "resources.arsc").exists()) {
            return;
        }
        if (manifest.isFile() && manifest.exists()) {
            try {
                if (manifestOriginal.exists()) {
                    manifestOriginal.delete();
                }
                FileUtils.copyFile(manifest, manifestOriginal);
                ResXmlPatcher.fixingPublicAttrsInProviderAttributes(manifest, LOGGER);
            } catch (IOException ex) {
                throw new AndrolibException(ex.getMessage());
            }
        }
    }

    public void buildSources(File appDir)
            throws AndrolibException {
        if (!buildSourcesRaw(appDir, "classes.dex") && !buildSourcesSmali(appDir, "smali", "classes.dex")) {
            LOGGER.warning(R.string.log_text,"Could not find sources");
        }
    }

    public void buildNonDefaultSources(ExtFile appDir)
            throws AndrolibException {
        try {
            // loop through any smali_ directories for multi-dex apks
            Map<String, Directory> dirs = appDir.getDirectory().getDirs();
            for (Map.Entry<String, Directory> directory : dirs.entrySet()) {
                String name = directory.getKey();
                if (name.startsWith("smali_")) {
                    mStepInfo.stepTotal++;
                    String filename = name.substring(name.indexOf("_") + 1) + ".dex";

                    if (!buildSourcesRaw(appDir, filename) && !buildSourcesSmali(appDir, name, filename)) {
                        LOGGER.warning(R.string.log_text,"Could not find sources");
                    }
                }
            }

            // loop through any classes#.dex files for multi-dex apks
            File[] dexFiles = appDir.listFiles();
            if (dexFiles != null) {
                for (File dex : dexFiles) {

                    // skip classes.dex because we have handled it in buildSources()
                    if (dex.getName().endsWith(".dex") && ! dex.getName().equalsIgnoreCase("classes.dex")) {
                        buildSourcesRaw(appDir, dex.getName());
                    }
                }
            }
        } catch(DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildSourcesRaw(File appDir, String filename)
            throws AndrolibException {
        File working = new File(appDir, filename);
        if (!working.exists()) {
            return false;
        }
        File stored = new File(appDir, APK_DIRNAME + "/" + filename);
        if (apkOptions.forceBuildAll || isModified(working, stored)) {
            LOGGER.info(R.string.log_text,"Copying " + appDir.toString() + " " + filename + " file...");
            try {
                BrutIO.copyAndClose(new FileInputStream(working), new FileOutputStream(stored));
                return true;
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
        }
        return true;
    }

    public boolean buildSourcesSmali(File appDir, String folder, String filename)
            throws AndrolibException {
        ExtFile smaliDir = new ExtFile(appDir, folder);
        if (!smaliDir.exists()) {
            return false;
        }
        File dex = new File(appDir, APK_DIRNAME + "/" + filename);
        if (! apkOptions.forceBuildAll) {
            LOGGER.info(R.string.log_text,"Checking whether sources has changed...");
        }
        if (apkOptions.forceBuildAll || isModified(smaliDir, dex)) {
            setNextStep("Building dex");
            LOGGER.info(R.string.log_text,"Smaling " + folder + " folder into " + filename + "...");
            dex.delete();
            SmaliBuilder smaliBuilder = new SmaliBuilder(smaliDir, dex, apkOptions.forceApi > 0 ? apkOptions.forceApi : mMinSdkVersion);
            smaliBuilder.setCallback(this);
            smaliBuilder.build();
        }
        return true;
    }

    public void buildResources(ExtFile appDir, UsesFramework usesFramework)
            throws BrutException {
        long start = System.currentTimeMillis();
        if (!buildResourcesRaw(appDir) && !buildResourcesFull(appDir, usesFramework)
                && !buildManifest(appDir, usesFramework)) {
            LOGGER.warning(R.string.log_text,"Could not find resources");
        }
        DLog.d(TAG, String.format(Locale.ENGLISH, "Buildet resources in %d ms", (System.currentTimeMillis() - start)));
    }

    public boolean buildResourcesRaw(ExtFile appDir)
            throws AndrolibException {
        try {
            if (!new File(appDir, "resources.arsc").exists()) {
                return false;
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (! apkOptions.forceBuildAll) {
                LOGGER.info(R.string.log_text,"Checking whether resources has changed...");
            }
            if (apkOptions.forceBuildAll || isModified(newFiles(APK_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
                LOGGER.info(R.string.log_text,"Copying raw resources...");
                appDir.getDirectory().copyToDir(apkDir, APK_RESOURCES_FILENAMES);
            }
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildResourcesFull(File appDir, UsesFramework usesFramework)
            throws AndrolibException {
        try {
            mStepInfo.stepTotal++;
            if (!new File(appDir, "res").exists()) {
                return false;
            }
            if (! apkOptions.forceBuildAll) {
                LOGGER.info(R.string.log_text,"Checking whether resources has changed...");
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            File resourceFile = new File(apkDir.getParent(), "resources.zip");

            if (apkOptions.forceBuildAll || isModified(newFiles(APP_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir)) || (apkOptions.isAapt2() && !isFile(resourceFile))) {
                LOGGER.info(R.string.log_text,"Building resources...");
                setNextStep("Building resources");
                if (apkOptions.debugMode) {
                    ResXmlPatcher.removeApplicationDebugTag(new File(appDir, "AndroidManifest.xml"),LOGGER);
                }

                File apkFile = File.createTempFile("APKTOOL", null);
                apkFile.delete();
                resourceFile.delete();

                File ninePatch = new File(appDir, "9patch");
                if (!ninePatch.exists()) {
                    ninePatch = null;
                }
                mAndRes.aaptPackage(apkFile, new File(appDir,
                                "AndroidManifest.xml"), new File(appDir, "res"),
                        ninePatch, null, parseUsesFramework(usesFramework));
              //  mBuilderCallback.taskFailed(mAndRes.getAaptError());
                Directory tmpDir = new ExtFile(apkFile).getDirectory();

                // Sometimes an application is built with a resources.arsc file with no resources,
                // Apktool assumes it will have a rebuilt arsc file, when it doesn't. So if we
                // encounter a copy error, move to a warning and continue on. (#1730)
                try {
                    tmpDir.copyToDir(apkDir,
                            tmpDir.containsDir("res") ? APK_RESOURCES_FILENAMES
                                    : APK_RESOURCES_WITHOUT_RES_FILENAMES);
                } catch (DirectoryException ex) {
                    mBuilderCallback.taskFailed(ex.getMessage());
                    LOGGER.warning(R.string.log_text,ex.getMessage());
                }

                // delete tmpDir
                apkFile.delete();
            }
            return true;
        } catch (IOException | BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildManifestRaw(ExtFile appDir)
            throws AndrolibException {
        try {
            File apkDir = new File(appDir, APK_DIRNAME);
            LOGGER.info(R.string.log_text,"Copying raw AndroidManifest.xml...");
            appDir.getDirectory().copyToDir(apkDir, APK_MANIFEST_FILENAMES);
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildManifest(ExtFile appDir, UsesFramework usesFramework)
            throws BrutException {
        try {
            if (!new File(appDir, "AndroidManifest.xml").exists()) {
                return false;
            }
            if (! apkOptions.forceBuildAll) {
                LOGGER.info(R.string.log_text,"Checking whether resources has changed...");
            }

            File apkDir = new File(appDir, APK_DIRNAME);

            if (apkOptions.forceBuildAll || isModified(newFiles(APK_MANIFEST_FILENAMES, appDir),
                    newFiles(APK_MANIFEST_FILENAMES, apkDir))) {
                LOGGER.info(R.string.log_text,"Building AndroidManifest.xml...");

                File apkFile = File.createTempFile("APKTOOL", null);
                apkFile.delete();

                File ninePatch = new File(appDir, "9patch");
                if (!ninePatch.exists()) {
                    ninePatch = null;
                }

                mAndRes.aaptPackage(apkFile, new File(appDir,
                                "AndroidManifest.xml"), null, ninePatch, null,
                        parseUsesFramework(usesFramework));

                Directory tmpDir = new ExtFile(apkFile).getDirectory();
                tmpDir.copyToDir(apkDir, APK_MANIFEST_FILENAMES);
            }
            return true;
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException(ex);
        } catch (AndrolibException ex) {
            LOGGER.warning(R.string.log_text,"Parse AndroidManifest.xml failed, treat it as raw file.");
            return buildManifestRaw(appDir);
        }
    }

    public void buildLibs(File appDir) throws AndrolibException {
        buildLibrary(appDir, "lib");
        buildLibrary(appDir, "libs");
        buildLibrary(appDir, "kotlin");
        buildLibrary(appDir, "META-INF/services");
    }

    public void buildLibrary(File appDir, String folder) throws AndrolibException {
        File working = new File(appDir, folder);

        if (! working.exists()) {
            return;
        }

        File stored = new File(appDir, APK_DIRNAME + "/" + folder);
        if (apkOptions.forceBuildAll || isModified(working, stored)) {
            LOGGER.info(R.string.log_text,"Copying libs... (/" + folder + ")");
            try {
                OS.rmdir(stored);
                OS.cpdir(working, stored);
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

    public void buildCopyOriginalFiles(File appDir)
            throws AndrolibException {
        if (apkOptions.copyOriginalFiles) {
            File originalDir = new File(appDir, "original");
            if (originalDir.exists()) {
                try {
                    LOGGER.info(R.string.log_text,"Copy original files...");
                    Directory in = (new ExtFile(originalDir)).getDirectory();
                    if (in.containsFile("AndroidManifest.xml")) {
                        LOGGER.info(R.string.log_text,"Copy AndroidManifest.xml...");
                        in.copyToDir(new File(appDir, APK_DIRNAME), "AndroidManifest.xml");
                    }
                    if (in.containsDir("META-INF")) {
                        LOGGER.info(R.string.log_text,"Copy META-INF...");
                        in.copyToDir(new File(appDir, APK_DIRNAME), "META-INF");
                    }
                } catch (DirectoryException ex) {
                    throw new AndrolibException(ex);
                }
            }
        }
    }

    public void buildUnknownFiles(File appDir, File outFile, MetaInfo meta)
            throws AndrolibException {
        if (meta.unknownFiles != null) {
            mStepInfo.stepTotal++;

            setNextStep("Copying unknown files/dir");
            LOGGER.info(R.string.log_text,"Copying unknown files/dir...");

            Map<String, String> files = meta.unknownFiles;
            File tempFile = new File(outFile.getParent(), outFile.getName() + ".apktool_temp");
            boolean renamed = outFile.renameTo(tempFile);
            if (!renamed) {
                throw new AndrolibException("Unable to rename temporary file");
            }

            try {
                ZipFile inputFile = new ZipFile(tempFile);
                ZipOutputStream actualOutput = new ZipOutputStream(new FileOutputStream(outFile));
                copyExistingFiles(inputFile, actualOutput);
                copyUnknownFiles(appDir, actualOutput, files);
                inputFile.close();
                actualOutput.close();
            } catch (IOException | BrutException ex) {
                throw new AndrolibException(ex);
            }

            // Remove our temporary file.
            tempFile.delete();
        }
    }

    private void copyExistingFiles(ZipFile inputFile, ZipOutputStream outputFile) throws IOException {
        // First, copy the contents from the existing outFile:
        Enumeration<? extends ZipEntry> entries = inputFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = new ZipEntry(entries.nextElement());

            // We can't reuse the compressed size because it depends on compression sizes.
            entry.setCompressedSize(-1);
            outputFile.putNextEntry(entry);

            // No need to create directory entries in the final apk
            if (! entry.isDirectory()) {
                BrutIO.copy(inputFile, outputFile, entry);
            }

            outputFile.closeEntry();
        }
    }

    private void copyUnknownFiles(File appDir, ZipOutputStream outputFile, Map<String, String> files)
            throws BrutException, IOException {
        long start = System.currentTimeMillis();

        File unknownFileDir = new File(appDir, UNK_DIRNAME);

        // loop through unknown files
        for (Map.Entry<String,String> unknownFileInfo : files.entrySet()) {
            File inputFile = new File(unknownFileDir, BrutIO.sanitizeUnknownFile(unknownFileDir, unknownFileInfo.getKey()));
            if (inputFile.isDirectory()) {
                continue;
            }

            ZipEntry newEntry = new ZipEntry(unknownFileInfo.getKey());
            int method = Integer.parseInt(unknownFileInfo.getValue());
            String log = String.format("Copying unknown file %s with method %d", unknownFileInfo.getKey(), method);
           // setNextStep(log);
            LOGGER.fine(R.string.log_text,log);
            if (method == ZipEntry.STORED) {
                newEntry.setMethod(ZipEntry.STORED);
                newEntry.setSize(inputFile.length());
                newEntry.setCompressedSize(-1);
                BufferedInputStream unknownFile = new BufferedInputStream(new FileInputStream(inputFile));
                CRC32 crc = BrutIO.calculateCrc(unknownFile);
                newEntry.setCrc(crc.getValue());
            } else {
                newEntry.setMethod(ZipEntry.DEFLATED);
            }
            outputFile.putNextEntry(newEntry);

            BrutIO.copy(inputFile, outputFile);
            outputFile.closeEntry();
            DLog.d(TAG, String.format(Locale.ENGLISH, "Copied unknown resources in %d ms", (System.currentTimeMillis() - start)));
        }
    }

    public void buildApk(File appDir, File outApk) throws AndrolibException {
        long start = System.currentTimeMillis();

        mStepInfo.stepTotal++;
        setNextStep("Building apk file");
        LOGGER.info(R.string.log_text,"Building apk file...");
        if (outApk.exists()) {
            outApk.delete();
        } else {
            File outDir = outApk.getParentFile();
            if (outDir != null && !outDir.exists()) {
                outDir.mkdirs();
            }
        }
        File assetDir = new File(appDir, "assets");
        if (!assetDir.exists()) {
            assetDir = null;
        }
        mAndRes.zipPackage(outApk, new File(appDir, APK_DIRNAME), assetDir);
        DLog.d(TAG, String.format(Locale.ENGLISH, "Buildet apk(zipping) in %d ms", (System.currentTimeMillis() - start)));
    }

    public void publicizeResources(File arscFile) throws AndrolibException {
        mAndRes.publicizeResources(arscFile);
    }

    public void installFramework(File frameFile)
            throws AndrolibException {
        mAndRes.installFramework(frameFile);
    }

    public void emptyFrameworkDirectory() throws AndrolibException {
        mAndRes.emptyFrameworkDirectory();
    }

    public boolean isFrameworkApk(ResTable resTable) {
        for (ResPackage pkg : resTable.listMainPackages()) {
            if (pkg.getId() < 64) {
                return true;
            }
        }
        return false;
    }

    public static String getVersion() {
        return ApktoolProperties.get("application.version");
    }

    private File[] parseUsesFramework(UsesFramework usesFramework)
            throws AndrolibException {
        if (usesFramework == null) {
            return null;
        }

        List<Integer> ids = usesFramework.ids;
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        String tag = usesFramework.tag;
        File[] files = new File[ids.size()];
        int i = 0;
        for (int id : ids) {
            files[i++] = mAndRes.getFrameworkApk(id, tag);
        }
        return files;
    }

    private boolean isModified(File working, File stored) {
        return ! stored.exists() || BrutIO.recursiveModifiedTime(working) > BrutIO .recursiveModifiedTime(stored);
    }

    private boolean isFile(File working) {
        return working.exists();
    }

    private boolean isModified(File[] working, File[] stored) {
        for (File file : stored) {
            if (!file.exists()) {
                return true;
            }
        }
        return BrutIO.recursiveModifiedTime(working) > BrutIO.recursiveModifiedTime(stored);
    }

    private File[] newFiles(String[] names, File dir) {
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(dir, names[i]);
        }
        return files;
    }

    public void close() throws IOException {
        mAndRes.close();
    }

    //set next steps
    private void setNextStep(String description) {
        mStepInfo.stepIndex++;
        mStepInfo.stepDescription = description;
        mBuilderCallback.setTaskStepInfo(mStepInfo);
    }

    @Override
    public void updateAssembledFiles(int assembledFiles, int totalFiles) {
        //if (System.currentTimeMillis() > this.lastUpdateAssembleTime + 500) {
            String fmt = mContext.getString(R.string.assemble_dex_detail);
            mStepInfo.stepDescription = String.format(fmt, assembledFiles, totalFiles);
            mBuilderCallback.setTaskStepInfo(mStepInfo);
            //  }

    }

    private final Logger LOGGER;

    private final static String SMALI_DIRNAME = "smali";
    private final static String APK_DIRNAME = "build/apk";
    private final static String UNK_DIRNAME = "unknown";
    private final static String[] APK_RESOURCES_FILENAMES = new String[] {
            "resources.arsc", "AndroidManifest.xml", "res" };
    private final static String[] APK_RESOURCES_WITHOUT_RES_FILENAMES = new String[] {
            "resources.arsc", "AndroidManifest.xml" };
    private final static String[] APP_RESOURCES_FILENAMES = new String[] {
            "AndroidManifest.xml", "res" };
    private final static String[] APK_MANIFEST_FILENAMES = new String[] {
            "AndroidManifest.xml" };
    private final static String[] APK_STANDARD_ALL_FILENAMES = new String[] {
            "classes.dex", "AndroidManifest.xml", "resources.arsc", "res", "r", "R",
            "lib", "libs", "assets", "META-INF", "kotlin" };
    private final static Pattern NO_COMPRESS_PATTERN = Pattern.compile("(" +
            "jpg|jpeg|png|gif|wav|mp2|mp3|ogg|aac|mpg|mpeg|mid|midi|smf|jet|rtttl|imy|xmf|mp4|" +
            "m4a|m4v|3gp|3gpp|3g2|3gpp2|amr|awb|wma|wmv|webm|mkv|so)$");

}
