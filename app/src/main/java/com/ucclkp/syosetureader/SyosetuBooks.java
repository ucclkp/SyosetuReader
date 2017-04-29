package com.ucclkp.syosetureader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SyosetuBooks
{
    private SQLiteDatabase mDataBase;

    private final static String TABLE_SYOSETU = "Syosetu";
    private final static String TABLE_SYOSETU_SECTION = "SyosetuSection";

    public final static String COLUMN_NCODE = "Ncode";
    public final static String COLUMN_URL = "Url";
    public final static String COLUMN_NAME = "Name";
    public final static String COLUMN_AUTHOR = "Author";
    public final static String COLUMN_AUTHOR_URL = "AuthorUrl";
    public final static String COLUMN_SUMMARY = "Summary";
    public final static String COLUMN_ATTENTION = "Attention";
    public final static String COLUMN_SITE = "Site";
    public final static String COLUMN_TYPE = "Type";
    public final static String COLUMN_LIST = "List";
    public final static String COLUMN_LENGTH = "Length";
    public final static String COLUMN_INFO_URL = "InfoUrl";
    public final static String COLUMN_FEEL_URL = "FeelUrl";
    public final static String COLUMN_REVIEW_URL = "ReviewUrl";

    public final static String COLUMN_STATE = "State";
    public final static String COLUMN_HAVE = "DledCount";
    public final static String COLUMN_TOTAL = "TotalCount";

    public final static String COLUMN_SECTION_ID = "SectionId";
    public final static String COLUMN_PREV_URL = "PrevUrl";
    public final static String COLUMN_NEXT_URL = "NextUrl";
    public final static String COLUMN_CONTENT = "Content";
    public final static String COLUMN_NUMBER = "Number";


    public SyosetuBooks(Context context)
    {
        openLibrary(context);
    }


    private void openLibrary(Context context)
    {
        mDataBase = context.openOrCreateDatabase("SyosetuBooks.db", Context.MODE_PRIVATE, null);
        //mDataBase.execSQL("DROP TABLE " + TABLE_SYOSETU + ";");
        //mDataBase.execSQL("DROP TABLE " + TABLE_SYOSETU_SECTION + ";");

        mDataBase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SYOSETU + "("
                + "Ncode VARCHAR PRIMARY KEY,"
                + "Url TEXT UNIQUE NOT NULL,"
                + "Name NTEXT,"
                + "Author NTEXT,"
                + "AuthorUrl TEXT,"
                + "InfoUrl TEXT,"
                + "FeelUrl TEXT,"
                + "ReviewUrl TEXT,"
                + "Summary NTEXT,"
                + "Attention NTEXT,"
                + "Site NVARCHAR,"
                + "Type NVARCHAR,"
                + "List NTEXT,"
                + "Length INTEGER,"
                + "State INTEGER,"
                + "DledCount INTEGER,"
                + "TotalCount INTEGER);");

        mDataBase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SYOSETU_SECTION + "("
                + "Ncode VARCHAR NOT NULL,"
                + "SectionId VARCHAR PRIMARY KEY,"
                + "Url TEXT NOT NULL,"
                + "PrevUrl TEXT,"
                + "NextUrl TEXT,"
                + "Name NTEXT,"
                + "Site NVARCHAR,"
                + "Number VARCHAR,"
                + "Content NTEXT,"
                + "Length INTEGER);");
    }


    public synchronized void begin()
    {
        mDataBase.beginTransaction();
    }

    public synchronized void end()
    {
        mDataBase.endTransaction();
    }

    public synchronized void successful()
    {
        mDataBase.setTransactionSuccessful();
    }


    public synchronized void insertBook(
            String ncode, String url, String name,
            String author, String authorUrl,
            String infoUrl, String feelUrl, String reviewUrl,
            String summary, String attention, String site,
            String type, String list, int length,
            int state, int have, int all)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NCODE, ncode);
        values.put(COLUMN_URL, url);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_AUTHOR, author);
        values.put(COLUMN_AUTHOR_URL, authorUrl);
        values.put(COLUMN_INFO_URL, infoUrl);
        values.put(COLUMN_FEEL_URL, feelUrl);
        values.put(COLUMN_REVIEW_URL, reviewUrl);
        values.put(COLUMN_SUMMARY, summary);
        values.put(COLUMN_ATTENTION, attention);
        values.put(COLUMN_SITE, site);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_LIST, list);
        values.put(COLUMN_LENGTH, length);
        values.put(COLUMN_STATE, state);
        values.put(COLUMN_HAVE, have);
        values.put(COLUMN_TOTAL, all);

        mDataBase.insert(TABLE_SYOSETU, null, values);
    }

    public synchronized void insertBookUni(
            String ncode, String url, String name,
            String author, String authorUrl,
            String infoUrl, String feelUrl, String reviewUrl,
            String summary, String attention, String site,
            String type, String list, int length,
            int state, int have, int all)
    {
        mDataBase.beginTransaction();

        try
        {
            if (hasBook(ncode))
                updateBook(ncode, url, name, author, authorUrl,
                        infoUrl, feelUrl, reviewUrl, summary,
                        attention, site, type, list, length,
                        state, have, all);
            else
                insertBook(ncode, url, name, author, authorUrl,
                        infoUrl, feelUrl, reviewUrl, summary,
                        attention, site, type, list, length,
                        state, have, all);

            mDataBase.setTransactionSuccessful();
        } finally
        {
            mDataBase.endTransaction();
        }
    }

    public synchronized void updateBook(
            String ncode, String url, String name,
            String author, String authorUrl,
            String infoUrl, String feelUrl, String reviewUrl,
            String summary, String attention, String site,
            String type, String list, int length,
            int state, int have, int all)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NCODE, ncode);
        values.put(COLUMN_URL, url);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_AUTHOR, author);
        values.put(COLUMN_AUTHOR_URL, authorUrl);
        values.put(COLUMN_INFO_URL, infoUrl);
        values.put(COLUMN_FEEL_URL, feelUrl);
        values.put(COLUMN_REVIEW_URL, reviewUrl);
        values.put(COLUMN_SUMMARY, summary);
        values.put(COLUMN_ATTENTION, attention);
        values.put(COLUMN_SITE, site);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_LIST, list);
        values.put(COLUMN_LENGTH, length);
        values.put(COLUMN_STATE, state);
        values.put(COLUMN_HAVE, have);
        values.put(COLUMN_TOTAL, all);

        mDataBase.update(TABLE_SYOSETU, values, "Ncode=?", new String[]{ncode});
    }

    public synchronized void updateBookState(String ncode, int state)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATE, state);

        mDataBase.update(TABLE_SYOSETU, values, "Ncode=?", new String[]{ncode});
    }

    public synchronized void updateBookState(String ncode, int state, int have, int all)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATE, state);
        values.put(COLUMN_HAVE, have);
        values.put(COLUMN_TOTAL, all);

        mDataBase.update(TABLE_SYOSETU, values, "Ncode=?", new String[]{ncode});
    }

    public synchronized boolean hasBook(String ncode)
    {
        boolean result;
        Cursor cursor = mDataBase.query(
                TABLE_SYOSETU, null, "Ncode=?",
                new String[]{ncode}, null, null, null);
        result = cursor.moveToFirst();
        cursor.close();

        return result;
    }

    public synchronized boolean hasBook(String ncode, int[] position)
    {
        boolean result = false;
        Cursor cursor = mDataBase.query(
                TABLE_SYOSETU, null, null,
                null, null, null, null);
        if (cursor.moveToFirst())
        {
            do
            {
                if (cursor.getString(cursor.getColumnIndex("Ncode"))
                        .equals(ncode))
                {
                    result = true;
                    position[0] = cursor.getPosition();
                    position[1] = cursor.getCount();
                    break;
                }
            }
            while (cursor.moveToNext());
        }

        cursor.close();

        return result;
    }

    public synchronized Cursor getBook()
    {
        Cursor cursor = mDataBase.query(
                TABLE_SYOSETU, null, null,
                null, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public synchronized Cursor getBook(String ncode)
    {
        Cursor cursor = mDataBase.query(
                TABLE_SYOSETU, null, "Ncode=?",
                new String[]{ncode}, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public synchronized void deleteBook()
    {
        mDataBase.delete(TABLE_SYOSETU, null, null);
    }

    public synchronized void deleteBook(String ncode)
    {
        mDataBase.delete(TABLE_SYOSETU, "Ncode=?", new String[]{ncode});
    }


    public synchronized void insertSection(
            String ncode, String sectionId, String url, String prevUrl, String nextUrl,
            String name, String site, String number, String content, int length)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NCODE, ncode);
        values.put(COLUMN_URL, url);
        values.put(COLUMN_PREV_URL, prevUrl);
        values.put(COLUMN_NEXT_URL, nextUrl);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_NUMBER, number);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_LENGTH, length);
        values.put(COLUMN_SECTION_ID, sectionId);
        values.put(COLUMN_SITE, site);

        mDataBase.insert(TABLE_SYOSETU_SECTION, null, values);
    }

    public synchronized void insertSectionUni(
            String ncode, String sectionId, String url, String prevUrl, String nextUrl,
            String name, String site, String number, String content, int length)
    {
        mDataBase.beginTransaction();

        try
        {
            if (hasSection(ncode, sectionId))
                updateSection(
                        ncode, sectionId, url, prevUrl, nextUrl,
                        name, site, number, content, length);
            else
                insertSection(
                        ncode, sectionId, url, prevUrl, nextUrl,
                        name, site, number, content, length);

            mDataBase.setTransactionSuccessful();
        } finally
        {
            mDataBase.endTransaction();
        }
    }

    public synchronized void updateSection(
            String ncode, String sectionId, String url, String prevUrl, String nextUrl,
            String name, String site, String number, String content, int length)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NCODE, ncode);
        values.put(COLUMN_URL, url);
        values.put(COLUMN_PREV_URL, prevUrl);
        values.put(COLUMN_NEXT_URL, nextUrl);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_NUMBER, number);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_LENGTH, length);
        values.put(COLUMN_SECTION_ID, sectionId);
        values.put(COLUMN_SITE, site);

        mDataBase.update(
                TABLE_SYOSETU_SECTION,
                values,
                "Ncode=? AND SectionId=?",
                new String[]{ncode, sectionId});
    }

    public synchronized boolean hasSection(String ncode, String sectionId)
    {
        boolean result;
        Cursor cursor = mDataBase.query(
                TABLE_SYOSETU_SECTION, null, "Ncode=? AND SectionId=?",
                new String[]{ncode, sectionId}, null, null, null);
        result = cursor.moveToFirst();
        cursor.close();

        return result;
    }

    public synchronized Cursor getSection()
    {
        Cursor cursor = mDataBase.query(
                TABLE_SYOSETU_SECTION, null,
                null, null, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public synchronized Cursor getSection(String ncode)
    {
        Cursor cursor = mDataBase.query(
                TABLE_SYOSETU_SECTION, null, "Ncode=?",
                new String[]{ncode}, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public synchronized Cursor getSection(String ncode, String sectionId)
    {
        Cursor cursor = mDataBase.query(
                TABLE_SYOSETU_SECTION, null, "Ncode=? AND SectionId=?",
                new String[]{ncode, sectionId}, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public synchronized void deleteSection()
    {
        mDataBase.delete(TABLE_SYOSETU_SECTION, null, null);
    }

    public synchronized void deleteSection(String ncode)
    {
        mDataBase.delete(TABLE_SYOSETU_SECTION, "Ncode=?", new String[]{ncode});
    }
}
