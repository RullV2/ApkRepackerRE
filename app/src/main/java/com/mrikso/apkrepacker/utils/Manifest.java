package com.mrikso.apkrepacker.utils;

import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Manifest {
    private static File apkFile;

    public Manifest(File apk) {
        this.apkFile = apk;
    }

    public Manifest(String apk) {
        this.apkFile = new File(apk);
    }

    public static boolean isSplitRequired() throws IOException, XmlPullParserException {
        XmlResourceParser parser = getParserForManifest(apkFile);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("application")) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (parser.getAttributeName(i).equals("isSplitRequired")) {
                        return parser.getAttributeBooleanValue(i, true);
                    }
                }
            }
        }
        return false;
    }

    public static int getTargetSdkVersion() throws IOException, XmlPullParserException {
        XmlResourceParser parser = getParserForManifest(apkFile);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("uses-sdk")) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (parser.getAttributeName(i).equals("targetSdkVersion")) {
                        return parser.getAttributeIntValue(i, -1);
                    }
                }
            }
        }
        return -1;
    }

    public static String getPackageName() throws IOException, XmlPullParserException {
        XmlResourceParser parser = getParserForManifest(apkFile);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("manifest")) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (parser.getAttributeName(i).equals("package")) {
                        return parser.getAttributeValue(i);
                    }
                }
            }
        }
        return "";
    }

    public static String getVersionName() throws IOException, XmlPullParserException {
        XmlResourceParser parser = getParserForManifest(apkFile);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("manifest")) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (parser.getAttributeName(i).equals("versionName")) {
                        return parser.getAttributeValue(i);
                    }
                }
            }
        }
        return "";
    }

    public static int getVersionCode() throws IOException, XmlPullParserException {
        XmlResourceParser parser = getParserForManifest(apkFile);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("manifest")) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (parser.getAttributeName(i).equals("versionCode")) {
                        return parser.getAttributeIntValue(i, 0);
                    }
                }
            }
        }
        return 0;
    }

    public static String getPackageLabel() throws IOException, XmlPullParserException {
        XmlResourceParser parser = getParserForManifest(apkFile);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("application")) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (parser.getAttributeName(i).equals("label")) {
                        return parser.getAttributeValue(i);
                    }
                }
            }
        }
        return "";
    }

    private static XmlResourceParser getParserForManifest(final File apkFile) throws IOException {
        final Object assetManagerInstance = getAssetManager();
        final int cookie = addAssets(apkFile, assetManagerInstance);
        return ((AssetManager) assetManagerInstance).openXmlResourceParser(cookie, "AndroidManifest.xml");
    }

    private static int addAssets(final File apkFile, final Object assetManagerInstance) {
        try {
            Method addAssetPath = assetManagerInstance.getClass().getMethod("addAssetPath", String.class);
            return (Integer) addAssetPath.invoke(assetManagerInstance, apkFile.getAbsolutePath());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static Object getAssetManager() {
        Class assetManagerClass = null;
        try {
            assetManagerClass = Class.forName("android.content.res.AssetManager");
			return assetManagerClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getMinSdkVersion() throws IOException, XmlPullParserException {
        XmlResourceParser parser = getParserForManifest(apkFile);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("uses-sdk")) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (parser.getAttributeName(i).equals("minSdkVersion")) {
                        return parser.getAttributeIntValue(i, -1);
                    }
                }
            }
        }
        return -1;
    }
}
