package com.ucclkp.syosetureader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Html;

import com.ucclkp.syosetureader.novel.NovelFragment;
import com.ucclkp.syosetureader.novel.NovelParser;
import com.ucclkp.syosetureader.novel.NovelSectionParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NovelDownloadService extends Service
{
    private int mNotificationId;
    private Handler mWorkHandler;
    private List<DownloadBlock> mDownloadQueue;
    private List<OnDownloadEventListener> mListenerList;

    private SyosetuBooks mSyosetuBooks;
    private DownloadTask mDownloadTask;
    private ControlBridge mController;
    private NotificationManager mNotifitionMgr;

    private Notification.Builder mMsgNotifyBuilder;
    private Notification.Builder mProNotifyBuilder;

    private NovelParser mNovelParser;
    private NovelSectionParser mNovelSectionParser;

    private final Object mWorkSync = new Object();


    public final static int STATE_WAITING = 0;
    public final static int STATE_FETCHING_INDEX = 1;
    public final static int STATE_DOWNLOADING = 2;
    public final static int STATE_PAUSED = 3;
    public final static int STATE_FAILED = 4;
    public final static int STATE_COMPLETED = 5;

    private final static int MSG_TASK_ADDED = 0;
    private final static int MSG_TASK_STATE_CHANGED = 1;


    public static class DownloadBlock
    {
        public int state;
        public String novelUrl = "";
        public String novelCode = "";
        public String novelTitle = "";
        public boolean isShortNovel = false;
        public SyosetuUtility.SyosetuSite novelSite;
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return mController;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        mNotificationId = 1;

        mWorkHandler = new Handler(mWorkCallback);
        mSyosetuBooks = ((UApplication) getApplication()).getSyosetuBooks();

        mMsgNotifyBuilder = new Notification.Builder(getApplicationContext());
        mMsgNotifyBuilder.setSmallIcon(R.mipmap.ic_book_black_24dp);

        mProNotifyBuilder = new Notification.Builder(getApplicationContext());
        mProNotifyBuilder.setSmallIcon(R.mipmap.ic_book_black_24dp);

        mNovelParser = new NovelParser(null);
        mNovelSectionParser = new NovelSectionParser(null);

        mListenerList = new ArrayList<>();
        mDownloadQueue = Collections.synchronizedList(new ArrayList<DownloadBlock>());

        mController = new ControlBridge();
        mNotifitionMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        mListenerList.clear();
        mDownloadQueue.clear();

        if (mDownloadTask != null)
            mDownloadTask.cancel();
        stopForeground(true);
    }


    private void sendMessage(int what, Object obj)
    {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = -1;
        msg.arg2 = -1;
        mWorkHandler.sendMessage(msg);
    }

    private void sendMessage(int what, Object obj, int have, int total)
    {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = have;
        msg.arg2 = total;
        mWorkHandler.sendMessage(msg);
    }

    public void processNovelPage(NovelParser.NovelData data, DownloadBlock block)
    {
        if (data != null)
        {
            if (block.isShortNovel)
            {
                mSyosetuBooks.updateBook(
                        block.novelCode, block.novelUrl,
                        data.headTitle, data.headAuthor, data.headAuthorUrl,
                        data.novelInfoUrl, data.novelFeelUrl, data.novelReviewUrl,
                        Html.toHtml(data.headSummary), data.headAttention,
                        block.novelSite.name(), getApplicationContext().getString(R.string.type_short),
                        null, data.length, STATE_COMPLETED, -1, -1);

                block.state = STATE_COMPLETED;
                block.novelTitle = data.headTitle;
                sendMessage(MSG_TASK_STATE_CHANGED, block);

                mMsgNotifyBuilder.setWhen(System.currentTimeMillis())
                        .setContentTitle("下载完成: " + data.headTitle)
                        .setContentText(block.novelCode);
                mNotifitionMgr.notify(mNotificationId, mMsgNotifyBuilder.build());
                stopForeground(false);
            }
            else
            {
                int sectionCount = 0;
                for (int i = 0; i < data.chOrSeList.size(); ++i)
                {
                    if (data.chOrSeList.get(i).type == NovelParser.NT_SECTION)
                        ++sectionCount;
                }

                mSyosetuBooks.updateBook(
                        block.novelCode, block.novelUrl,
                        data.headTitle, data.headAuthor, data.headAuthorUrl,
                        data.novelInfoUrl, data.novelFeelUrl, data.novelReviewUrl,
                        Html.toHtml(data.headSummary), data.headAttention,
                        block.novelSite.name(), getApplicationContext().getString(R.string.type_series),
                        NovelFragment.saveListToJSON(data.chOrSeList), -1,
                        STATE_DOWNLOADING, 0, sectionCount);

                block.state = STATE_DOWNLOADING;
                block.novelTitle = data.headTitle;
                sendMessage(MSG_TASK_STATE_CHANGED, block);

                int counter = 0;
                boolean failed = false;
                for (int i = 0; i < data.chOrSeList.size(); ++i)
                {
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                        break;
                    }

                    NovelParser.NovelChOrSeData chOrSeData = data.chOrSeList.get(i);
                    if (chOrSeData.type == NovelParser.NT_SECTION)
                    {
                        mProNotifyBuilder.setProgress(data.chOrSeList.size(), i, false)
                                .setContentTitle("正在下载: " + data.headTitle)
                                .setContentText(counter + "/" + sectionCount);
                        mNotifitionMgr.notify(mNotificationId, mProNotifyBuilder.build());

                        NovelSectionParser.SectionData sectionData =
                                mNovelSectionParser.enterSync(chOrSeData.sectionUrl);
                        if (sectionData != null)
                            processSectionPage(sectionData, block);
                        else
                        {
                            failed = true;
                            break;
                        }

                        ++counter;
                        mSyosetuBooks.updateBookState(
                                block.novelCode, STATE_DOWNLOADING, counter, sectionCount);
                        sendMessage(MSG_TASK_STATE_CHANGED, block, counter, sectionCount);
                    }
                }

                if (!failed)
                {
                    mSyosetuBooks.updateBookState(block.novelCode, STATE_COMPLETED);

                    block.state = STATE_COMPLETED;
                    sendMessage(MSG_TASK_STATE_CHANGED, block);

                    mMsgNotifyBuilder.setWhen(System.currentTimeMillis())
                            .setContentTitle("下载完成: " + data.headTitle)
                            .setContentText(block.novelCode);
                }
                else
                {
                    mSyosetuBooks.updateBookState(block.novelCode, STATE_FAILED);

                    block.state = STATE_FAILED;
                    sendMessage(MSG_TASK_STATE_CHANGED, block);

                    mMsgNotifyBuilder.setWhen(System.currentTimeMillis())
                            .setContentTitle("下载失败: " + data.headTitle)
                            .setContentText("请检查网络连接");
                }
                mNotifitionMgr.notify(mNotificationId, mMsgNotifyBuilder.build());
                stopForeground(false);
            }
        }
        else
        {
            mSyosetuBooks.updateBookState(block.novelCode, STATE_FAILED);

            block.state = STATE_FAILED;
            sendMessage(MSG_TASK_STATE_CHANGED, block);

            mMsgNotifyBuilder.setWhen(System.currentTimeMillis())
                    .setContentTitle("下载失败: " + block.novelCode)
                    .setContentText("请检查网络连接");
            mNotifitionMgr.notify(mNotificationId, mMsgNotifyBuilder.build());
            stopForeground(false);
        }
    }

    public boolean processSectionPage(NovelSectionParser.SectionData data, DownloadBlock block)
    {
        if (data != null)
        {
            String sectionUrl;
            if (mNovelSectionParser.getHtmlData().redirection)
                sectionUrl = mNovelSectionParser.getHtmlData().location;
            else
                sectionUrl = mNovelSectionParser.getHtmlData().url;

            mSyosetuBooks.insertSectionUni(
                    block.novelCode, SyosetuUtility.constructSectionId(block.novelCode, sectionUrl),
                    sectionUrl, data.prevUrl, data.nextUrl, data.title, block.novelSite.name(), data.number,
                    Html.toHtml(data.sectionContent), data.length);

            return true;
        }

        return false;
    }

    public boolean initialDownload(DownloadBlock block)
    {
        mProNotifyBuilder.setWhen(System.currentTimeMillis())
                .setProgress(100, 1, true)
                .setContentTitle("正在准备");
        startForeground(
                mNotificationId,
                mProNotifyBuilder.build());

        if (block.isShortNovel)
        {
            Cursor cursor = ((UApplication) getApplication())
                    .getSyosetuBooks().getBook(block.novelCode);
            if (cursor != null)
            {
                int state = cursor.getInt(cursor.getColumnIndex(
                        SyosetuBooks.COLUMN_STATE));
                cursor.close();

                if (state != STATE_FAILED)
                {
                    mMsgNotifyBuilder.setWhen(System.currentTimeMillis())
                            .setContentTitle("该小说已下载")
                            .setContentText(block.novelCode);
                    mNotifitionMgr.notify(
                            mNotificationId,
                            mMsgNotifyBuilder.build());
                    stopForeground(false);
                    return false;
                }
            }

            mProNotifyBuilder.setWhen(System.currentTimeMillis())
                    .setProgress(100, 1, true)
                    .setContentTitle("正在下载短篇")
                    .setContentText(block.novelCode);
            mNotifitionMgr.notify(
                    mNotificationId,
                    mProNotifyBuilder.build());

            block.state = STATE_DOWNLOADING;
            sendMessage(MSG_TASK_STATE_CHANGED, block, -2, -2);

            mSyosetuBooks.insertBookUni(
                    block.novelCode, block.novelUrl,
                    null, null, null, null, null, null, null, null,
                    block.novelSite.name(), getApplicationContext().getString(R.string.type_short),
                    null, -1, STATE_DOWNLOADING, -2, -2);
        }
        else
        {
            Cursor cursor = ((UApplication) getApplication())
                    .getSyosetuBooks().getBook(block.novelCode);
            if (cursor != null)
            {
                int state = cursor.getInt(cursor.getColumnIndex(
                        SyosetuBooks.COLUMN_STATE));
                cursor.close();

                if (state != STATE_FAILED)
                {
                    mMsgNotifyBuilder.setWhen(System.currentTimeMillis())
                            .setContentTitle("该小说已下载")
                            .setContentText(block.novelCode);
                    mNotifitionMgr.notify(
                            mNotificationId,
                            mMsgNotifyBuilder.build());
                    stopForeground(false);
                    return false;
                }
            }

            mProNotifyBuilder.setWhen(System.currentTimeMillis())
                    .setProgress(100, 1, true)
                    .setContentTitle("正在获取目录")
                    .setContentText(block.novelCode);
            mNotifitionMgr.notify(
                    mNotificationId,
                    mProNotifyBuilder.build());

            block.state = STATE_FETCHING_INDEX;
            sendMessage(MSG_TASK_STATE_CHANGED, block, -2, -2);

            mSyosetuBooks.insertBookUni(
                    block.novelCode, block.novelUrl,
                    null, null, null, null, null, null, null, null,
                    block.novelSite.name(), getApplicationContext().getString(R.string.type_series),
                    null, -1, STATE_FETCHING_INDEX, -2, -2);
        }

        return true;
    }


    public class ControlBridge extends Binder
    {
        public boolean startDownload(String novelUrl, boolean isShort)
        {
            //已经在下载队列中。
            for (int i = 0; i < mDownloadQueue.size(); ++i)
            {
                if (mDownloadQueue.get(i).novelCode
                        .equals(HtmlUtility.getUrlRear(novelUrl)))
                    return false;
            }

            DownloadBlock block = new DownloadBlock();
            block.state = STATE_WAITING;
            block.novelUrl = novelUrl;
            block.isShortNovel = isShort;
            block.novelCode = HtmlUtility.getUrlRear(novelUrl);
            block.novelSite = SyosetuUtility.getSiteFromNovelUrl(novelUrl);

            block.state = STATE_WAITING;
            sendMessage(MSG_TASK_ADDED, block);

            synchronized (mWorkSync)
            {
                mDownloadQueue.add(block);
                mWorkSync.notify();
            }

            if (mDownloadTask == null)
            {
                mDownloadTask = new DownloadTask();
                mDownloadTask.start();
            }

            return true;
        }

        public void addDownloadEventListener(OnDownloadEventListener l)
        {
            mListenerList.add(l);

            int taskCount = mDownloadQueue.size();
            for (int i = 1; i < taskCount; ++i)
                l.onTaskAdded(mDownloadQueue.get(i));
        }

        public void removeDownloadEventListener(OnDownloadEventListener l)
        {
            mListenerList.remove(l);
        }

        public void removeAllDownloadEventListener()
        {
            mListenerList.clear();
        }
    }


    private class DownloadTask extends Thread
    {
        private final AtomicBoolean mCancelled
                = new AtomicBoolean();

        @Override
        public void run()
        {
            super.run();

            while (!mCancelled.get())
            {
                synchronized (mWorkSync)
                {
                    if (mDownloadQueue.size() == 0)
                    {
                        try
                        {
                            mWorkSync.wait();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            break;
                        }
                    }
                }

                if (mCancelled.get()) break;

                if (mDownloadQueue.size() > 0)
                {
                    DownloadBlock block = mDownloadQueue.get(0);

                    if (initialDownload(block))
                    {
                        NovelParser.NovelData novelData
                                = mNovelParser.enterSync(block.novelUrl);
                        if (mNovelParser.getHtmlData().redirection)
                            block.novelUrl = mNovelParser.getHtmlData().location;
                        processNovelPage(novelData, block);
                    }

                    mDownloadQueue.remove(0);
                }

                ++mNotificationId;
            }
        }

        public void cancel()
        {
            mCancelled.set(true);
            interrupt();
        }
    }


    private Handler.Callback mWorkCallback = new Handler.Callback()
    {
        @Override
        public boolean handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_TASK_ADDED:
                    for (int i = 0; i < mListenerList.size(); ++i)
                        mListenerList.get(i).onTaskAdded((DownloadBlock) msg.obj);
                    return true;

                case MSG_TASK_STATE_CHANGED:
                    for (int i = 0; i < mListenerList.size(); ++i)
                        mListenerList.get(i).onTaskStateChanged((DownloadBlock) msg.obj, msg.arg1, msg.arg2);
                    return true;
            }

            return false;
        }
    };


    public interface OnDownloadEventListener
    {
        void onTaskAdded(DownloadBlock block);

        void onTaskStateChanged(DownloadBlock block, int have, int total);
    }
}