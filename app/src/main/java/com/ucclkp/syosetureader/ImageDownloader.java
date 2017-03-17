package com.ucclkp.syosetureader;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ImageDownloader
{
    private Handler mHandler;
    private ExecutorService mThreadPool;
    private SyosetuCacheManager mCacheMgr;

    private ArrayList<Future<?>> mFutureList;
    private ArrayList<OnDownloadListener> mDownloadListenerList;


    public static class ImageResult
    {
        public String pageId = "";
        public String imageUrl = "";

        public Bitmap bitmap = null;
    }


    private final static int MSG_IMAGE_DOWNLOAD_COMPLETE = 0;


    public ImageDownloader(SyosetuCacheManager cacheManager)
    {
        mCacheMgr = cacheManager;

        mFutureList = new ArrayList<>();
        mDownloadListenerList = new ArrayList<>();

        mThreadPool = Executors.newFixedThreadPool(2);
        mHandler = new Handler(mHandlerCallback);
    }


    public void download(String pageId, String imgUrl)
    {
        ImageTask task = new ImageTask(pageId, imgUrl, mHandler, mCacheMgr);

        Future<?> future = mThreadPool.submit(task);
        mFutureList.add(future);
    }

    public void cancel()
    {
        mThreadPool.shutdownNow();
    }


    public void addOnDownloadListener(OnDownloadListener l)
    {
        if (!mDownloadListenerList.contains(l))
            mDownloadListenerList.add(l);
    }

    public void removeOnDownloadListener(OnDownloadListener l)
    {
        mDownloadListenerList.remove(l);
    }

    public void removeAllOnDownloadListener()
    {
        mDownloadListenerList.clear();
    }


    private Handler.Callback mHandlerCallback = new Handler.Callback()
    {
        @Override
        public boolean handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_IMAGE_DOWNLOAD_COMPLETE:
                {
                    ImageResult result = (ImageResult) msg.obj;

                    for (int i = 0; i < mDownloadListenerList.size(); ++i)
                        mDownloadListenerList.get(i).onDownloadComplete(result.pageId, result);

                    return true;
                }
            }

            return false;
        }
    };


    private static class ImageTask implements Runnable
    {
        private String mPageId;
        private String mImageUrl;

        private Handler mHandler;
        private SyosetuCacheManager mCacheMgr;


        ImageTask(String pageId, String imgUrl, Handler handler,
                  SyosetuCacheManager cacheManager)
        {
            mPageId = pageId;
            mImageUrl = imgUrl;

            mHandler = handler;
            mCacheMgr = cacheManager;
        }

        @Override
        public void run()
        {
            ImageResult result = new ImageResult();

            result.pageId = mPageId;
            result.imageUrl = mImageUrl;
            result.bitmap = HtmlUtility.getBitmapFromUrl(mImageUrl);

            mCacheMgr.putBitmap(mImageUrl, result.bitmap);

            Message msg = new Message();
            msg.obj = result;
            msg.what = MSG_IMAGE_DOWNLOAD_COMPLETE;

            mHandler.sendMessage(msg);
        }
    }


    public interface OnDownloadListener
    {
        void onDownloadComplete(String pageId, ImageResult result);
    }
}
