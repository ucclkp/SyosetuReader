package com.ucclkp.syosetureader;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class SyosetuCacheManager
{
    private Context mContext;
    private DiskLruCache mDiskCache;
    private LruCache<String, Bitmap> mBmpMemCache;
    private LruCache<String, String> mTextMemCache;

    private ArrayList<OnCacheChangedListener> mListenerList;


    public final static int ACTION_ADD = 0;
    public final static int ACTION_REMOVE = 1;


    public SyosetuCacheManager(Context context)
    {
        mContext = context;
        mListenerList = new ArrayList<>();

        int cacheSize = getMemoryCacheSize();
        mBmpMemCache = new LruCache<String, Bitmap>(cacheSize)
        {
            @Override
            protected int sizeOf(String key, Bitmap value)
            {
                return value.getByteCount();
            }
        };

        mTextMemCache = new LruCache<>(cacheSize);

        open();
    }


    public void addOnCacheChangedListener(OnCacheChangedListener l)
    {
        mListenerList.add(l);
    }

    public void removeOnCacheChangedListener(OnCacheChangedListener l)
    {
        mListenerList.remove(l);
    }


    public synchronized void open()
    {
        if (mDiskCache != null)
        {
            if (!mDiskCache.isClosed())
                return;
        }

        try
        {
            mDiskCache = DiskLruCache.open(
                    new File(mContext.getCacheDir(), "dataCache"),
                    getApplicationVersion(mContext),
                    1, 64 * 1024 * 1024);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public synchronized boolean putText(String key, String text)
    {
        boolean succeed = true;
        String keyInternal = hashUp(key);

        mTextMemCache.put(keyInternal, text);

        DiskLruCache.Editor editor = null;
        try
        {
            editor = mDiskCache.edit(keyInternal);
            editor.set(0, text);

            editor.commit();
            mDiskCache.flush();
        }
        catch (IOException e)
        {
            try
            {
                if (editor != null)
                    editor.abort();
            }
            catch (IOException e1)
            {
                succeed = false;
                e1.printStackTrace();
            }

            succeed = false;
            e.printStackTrace();
        }

        for (int i = 0; i < mListenerList.size(); ++i)
        {
            mListenerList.get(i).onCacheChanged(key, ACTION_ADD);
        }

        return succeed;
    }

    public synchronized boolean putBitmap(String key, Bitmap bmp)
    {
        boolean succeed = true;
        String keyInternal = hashUp(key);

        mBmpMemCache.put(keyInternal, bmp);

        DiskLruCache.Editor editor = null;
        BufferedOutputStream out = null;
        try
        {
            editor = mDiskCache.edit(keyInternal);
            OutputStream outputStream = editor.newOutputStream(0);
            out = new BufferedOutputStream(outputStream, 1024 * 8);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);

            out.flush();
            editor.commit();
            mDiskCache.flush();
        }
        catch (IOException e)
        {
            try
            {
                if (editor != null)
                    editor.abort();
            }
            catch (IOException e1)
            {
                succeed = false;
                e1.printStackTrace();
            }

            succeed = false;
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (out != null)
                    out.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < mListenerList.size(); ++i)
        {
            mListenerList.get(i).onCacheChanged(key, ACTION_ADD);
        }

        return succeed;
    }

    public synchronized String getText(String key)
    {
        String keyInternal = hashUp(key);

        String text = mTextMemCache.get(keyInternal);
        if (text != null)
            return text;

        try
        {
            DiskLruCache.Snapshot snapshot = mDiskCache.get(keyInternal);
            if (snapshot != null)
            {
                String data = snapshot.getString(0);
                mTextMemCache.put(keyInternal, data);
                return data;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public synchronized Bitmap getBitmap(String key)
    {
        String keyInternal = hashUp(key);

        Bitmap bmp = mBmpMemCache.get(keyInternal);
        if (bmp != null)
            return bmp;

        try
        {
            DiskLruCache.Snapshot snapshot = mDiskCache.get(keyInternal);
            if (snapshot != null)
            {
                InputStream inputStream = snapshot.getInputStream(0);
                Bitmap data = BitmapFactory.decodeStream(inputStream);

                mBmpMemCache.put(keyInternal, data);
                return data;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public synchronized boolean removeText(String key, boolean memory, boolean disk)
    {
        String keyInternal = hashUp(key);

        if (memory)
            mTextMemCache.remove(keyInternal);

        if (disk)
        {
            try
            {
                mDiskCache.remove(keyInternal);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return false;
            }
        }

        if (memory || disk)
        {
            for (int i = 0; i < mListenerList.size(); ++i)
            {
                mListenerList.get(i).onCacheChanged(key, ACTION_REMOVE);
            }
        }

        return true;
    }

    public synchronized boolean removeBitmap(String key, boolean memory, boolean disk)
    {
        String keyInternal = hashUp(key);

        if (memory)
            mBmpMemCache.remove(keyInternal);

        if (disk)
        {
            try
            {
                mDiskCache.remove(keyInternal);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return false;
            }
        }

        if (memory || disk)
        {
            for (int i = 0; i < mListenerList.size(); ++i)
            {
                mListenerList.get(i).onCacheChanged(key, ACTION_REMOVE);
            }
        }

        return true;
    }

    public synchronized boolean delete()
    {
        try
        {
            mBmpMemCache.evictAll();
            mTextMemCache.evictAll();
            mDiskCache.delete();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public synchronized boolean flush()
    {
        try
        {
            mDiskCache.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public synchronized boolean close()
    {
        try
        {
            mDiskCache.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    private int getMemoryCacheSize()
    {
        Long maxMemory = Runtime.getRuntime().maxMemory() / 8;
        return maxMemory.intValue();
    }

    private int getApplicationVersion(Context context)
    {
        try
        {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        return 1;
    }

    private String hashUp(String string)
    {
        byte[] hash;

        try
        {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return null;
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash)
        {
            if ((b & 0xFF) < 0x10)
                hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString();
    }


    public interface OnCacheChangedListener
    {
        void onCacheChanged(String key, int action);
    }
}