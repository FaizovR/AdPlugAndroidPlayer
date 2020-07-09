package com.example.adplugplayer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

interface IMountCallback {
    void onMediaChanged(int count);
}

public class FileManager extends BroadcastReceiver {
    public static final int STORAGE_ILLEGAL = -1;
    public static final int STORAGE_INTERNAL = 0;
    public static final int STORAGE_EXTERNAL_1 = 1;
    public static final int STORAGE_EXTERNAL_2 = 2;
    private static final String TAG = "FileManager";
    private final Context mContext;
    private IMountCallback mCallback;
    private int mStorage;
    private File mCurrentDir;

    public FileManager(Context context, int storage) {
        mContext = context;
        mStorage = storage;
        mCurrentDir = getTopDir();
    }

    public FileManager(Context context) {
        this(context, STORAGE_INTERNAL);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int count = getStorageCount(false);
        if (mCallback == null || action == null) {
            return;
        }
        switch (action) {
            case Intent.ACTION_MEDIA_EJECT:
            case Intent.ACTION_MEDIA_MOUNTED:
            case Intent.ACTION_MEDIA_UNMOUNTED:
            case Intent.ACTION_MEDIA_REMOVED:
                mCallback.onMediaChanged(count);
                break;
            default:
                break;
        }
    }

    public void setCallback(IMountCallback callback) {
        mCallback = callback;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        mContext.registerReceiver(this, filter);
    }

    public void unsetCallback() {
        mContext.unregisterReceiver(this);
        mCallback = null;
    }

    public int getStorageCount(boolean notify) {
        int count = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externals = mContext.getExternalFilesDirs(null);
            if (externals != null) {
                if (externals.length > 0 && externals[0] != null) {
                    count++;
                    if (externals.length > 1 && externals[1] != null) {
                        count++;
                    }
                }
            }
        } else {
            File external = mContext.getExternalFilesDir(null);
            if (external != null) {
                count++;
            }
        }
        if (mStorage >= count) {
            Log.w(TAG, "checkStorage: media removed " + mStorage);
            mStorage = STORAGE_INTERNAL;
            mCurrentDir = getTopDir();
            if (notify && mCallback != null) {
                mCallback.onMediaChanged(count);
            }
        }
        return count;
    }

    public int getStorageCount() {
        return getStorageCount(true);
    }

    public File createNewFile(String name) {
        File file = getFile(name);
        if (file != null) {
            try {
                if (!file.createNewFile()) {
                    Log.e(TAG, "Failed to create file " + mCurrentDir.getPath() + File.separator + name);
                    file = null;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return file;
    }

    public boolean delete(String name) {
        File file = getFile(name);
        if (file != null) {
            return file.delete();
        } else {
            return false;
        }
    }

    public String[] list() {
        String[] dirs = null;
        if (isValidDir(mCurrentDir)) {
            dirs = mCurrentDir.list();
        }
        return dirs;
    }

    public File[] listFiles() {
        File[] dirFiles = null;
        if (isValidDir(mCurrentDir)) {
            dirFiles = mCurrentDir.listFiles();
        }
        return dirFiles;
    }

    public File mkdir(String dir) {
        File newDir = getFile(dir);
        if ((newDir != null) && newDir.mkdir()) {
            return newDir;
        } else {
            Log.e(TAG, "mkdir: failed " + newDir);
            return null;
        }
    }

    public boolean renameTo(File file, String name) {
        if (!isValidFile(file)) {
            Log.w(TAG, "renameTo: Illegal file " + file);
            return false;
        }
        if (!isValidName(name)) {
            Log.w(TAG, "renameTo: Illegal dir " + name);
            return false;
        }
        String path = file.getAbsolutePath();
        File dest = new File(path, name);
        return file.renameTo(dest);
    }

    public File getDir() {
        return mCurrentDir;
    }

    public File changeDir(File dir) {
        if (!isValidDir(dir)) {
            Log.w(TAG, "changeDir: Illegal dir " + dir);
        } else {
            mCurrentDir = dir;
        }
        return mCurrentDir;
    }

    public File changeDir(String dir) {
        File file = new File(mCurrentDir, dir);
        if (!isValidDir(file)) {
            Log.w(TAG, "changeDir: Illegal dir " + file);
        } else {
            mCurrentDir = file;
        }
        return mCurrentDir;
    }

    public File changeDirTop(int storage) {
        File dir = null;
        int count = getStorageCount();
        if (storage < count) {
            mStorage = storage;
            dir = changeDir(getTopDir());
        } else {
            Log.w(TAG, "changeDirTop: illegal storage " + storage);
        }
        return dir;
    }

    public File changeDirTop() {
        return changeDir(getTopDir());
    }

    public File changeDirUp() {
        if (isTopDir()) {
            Log.w(TAG, "changeDirUp: Already at top dir " + mCurrentDir);
            return mCurrentDir;
        }
        String path = mCurrentDir.getAbsolutePath();
        int endIndex = path.lastIndexOf(File.separatorChar);
        if (endIndex == -1) {
            Log.w(TAG, "changeDirUp: No higher dir " + mCurrentDir);
            return mCurrentDir;
        }
        String upDir = path.substring(0, endIndex);
        File dir = new File(upDir);
        return changeDir(dir);
    }

    public boolean isTopDir() {
        File dir = getTopDir();
        if (dir == null) {
            return false;
        }
        return dir.equals(getDir());
    }

    public File getFile(String name) {
        if (!isValidDir(mCurrentDir)) {
            Log.w(TAG, "getFile: No current dir " + mCurrentDir);
            return null;
        }
        if (!isValidName(name)) {
            Log.w(TAG, "getFile: Illegal file " + name);
            return null;
        }
        return new File(mCurrentDir, name);
    }

    public int getStorage() {
        return mStorage;
    }

    public int inStorage(String name) {
        int storage = STORAGE_ILLEGAL;
        if (!isValidName(name)) {
            Log.w(TAG, "inStorage: Illegal file " + name);
            return storage;
        }
        File dir = mContext.getFilesDir();
        if (dir != null) {
            if (name.startsWith(dir.getAbsolutePath())) {
                storage = STORAGE_INTERNAL;
            }
        }
        dir = mContext.getExternalFilesDir(null);
        if (storage == STORAGE_ILLEGAL && dir != null) {
            if (name.startsWith(dir.getAbsolutePath())) {
                storage = STORAGE_EXTERNAL_1;
            }
        }
        if (storage == STORAGE_ILLEGAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externals = mContext.getExternalFilesDirs(null);
            if (externals != null && externals.length > 1 && externals[1] != null) {
                dir = externals[1];
                if (name.startsWith(dir.getAbsolutePath())) {
                    storage = STORAGE_EXTERNAL_2;
                }
            }
        }
        return storage;
    }

    public String getFileName(String name) {
        String dirName = null;
        if (!isValidName(name)) {
            Log.w(TAG, "getFileName: Illegal file " + name);
            return null;
        }
        int storage = inStorage(name);
        File dir = getTopDir(storage);
        if (dir != null) {
            dirName = dir.getAbsolutePath();
        }
        if (dirName == null || !name.startsWith(dirName)) {
            Log.w(TAG, "getFileName: Illegal path " + name);
            return null;
        }
        name = name.replaceFirst(dirName, "");
        if (name.startsWith(File.separator)) {
            name = name.replaceFirst(File.separator, "");
        }
        return name;
    }

    public File getTopDir(int storage) {
        getStorageCount();
        File dir = null;
        switch (storage) {
            case STORAGE_INTERNAL:
                dir = mContext.getFilesDir();
                break;
            case STORAGE_EXTERNAL_1:
                dir = mContext.getExternalFilesDir(null);
                break;
            case STORAGE_EXTERNAL_2:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    File[] externals = mContext.getExternalFilesDirs(null);
                    if (externals != null && externals.length > 1 && externals[1] != null) {
                        dir = externals[1];
                    } else {
                        Log.w(TAG, "getTopDir: storage not available " + externals);
                    }
                } else {
                    Log.w(TAG, "getTopDir: Not supported in SDK version " + Build.VERSION.SDK_INT);
                }
                break;
            default:
                Log.w(TAG, "getTopDir: storage not supported " + storage);
                break;
        }
        return dir;
    }

    public File getTopDir() {
        return getTopDir(mStorage);
    }

    public boolean startsWith(File file) {
        boolean startsWith = false;
        String name = file.getAbsolutePath();
        if (name == null) {
            return startsWith;
        }
        File dir = mContext.getFilesDir();
        if (dir != null) {
            startsWith = name.startsWith(dir.getAbsolutePath());
        }
        dir = mContext.getExternalFilesDir(null);
        if (!startsWith && dir != null) {
            startsWith = name.startsWith(dir.getAbsolutePath());
        }
        if (!startsWith && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externals = mContext.getExternalFilesDirs(null);
            if (externals != null && externals.length > 1 && externals[1] != null) {
                dir = externals[1];
                startsWith = name.startsWith(dir.getAbsolutePath());
            }
        }
        return startsWith;
    }

    private boolean isValidFile(File file) {
        return (file != null) && file.exists() && !file.isDirectory();
    }

    private boolean isValidDir(File dir) {
        return (dir != null) && dir.exists() && dir.isDirectory();
    }

    private boolean isValidName(String name) {
        return (name != null) && !name.isEmpty();
    }
}
