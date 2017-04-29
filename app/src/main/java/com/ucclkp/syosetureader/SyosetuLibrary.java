package com.ucclkp.syosetureader;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SyosetuLibrary
{
    private SQLiteDatabase mDataBase;


    private final static String TABLE_HISTORY = "History";
    private final static String TABLE_FAVORITE = "Favorite";

    public final static String COLUMN_NCODE = "Ncode";
    public final static String COLUMN_URL = "Url";
    public final static String COLUMN_NAME = "Name";
    public final static String COLUMN_TIME = "Time";
    public final static String COLUMN_SITE = "Site";
    public final static String COLUMN_TYPE = "Type";
    public final static String COLUMN_VIEWED = "Viewed";
    public final static String COLUMN_CURRENT = "Current";
    public final static String COLUMN_OFFSET = "Offset";


    public SyosetuLibrary(Context context)
    {
        openLibrary(context);
    }


    private void openLibrary(Context context)
    {
        mDataBase = context.openOrCreateDatabase("SyosetuLibrary.db", Context.MODE_PRIVATE, null);
        mDataBase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_HISTORY + "("
                + "Ncode VARCHAR PRIMARY KEY,"
                + "Url TEXT UNIQUE NOT NULL,"
                + "Name NTEXT,"
                + "Time VARCHAR,"
                + "Site NVARCHAR,"
                + "Type NVARCHAR,"
                + "Viewed TEXT,"
                + "Current VARCHAR,"
                + "Offset INTEGER);");

        mDataBase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITE + "("
                + "Ncode VARCHAR PRIMARY KEY,"
                + "Url TEXT UNIQUE NOT NULL,"
                + "Name NTEXT,"
                + "Time VARCHAR,"
                + "Site NVARCHAR,"
                + "Type NVARCHAR);");
    }


    public void begin()
    {
        mDataBase.beginTransaction();
    }

    public void end()
    {
        mDataBase.endTransaction();
    }

    public void successful()
    {
        mDataBase.setTransactionSuccessful();
    }


    public void insertHis(
            String ncode, String url, String name, String time, String site,
            String type, String viewed, String current, int offset)
    {
        ContentValues values = new ContentValues();
        values.put("Ncode", ncode);
        values.put("Url", url);
        values.put("Name", name);
        values.put("Time", time);
        values.put("Site", site);
        values.put("Type", type);
        values.put("Viewed", viewed);
        values.put("Current", current);
        values.put("Offset", offset);

        mDataBase.insert(TABLE_HISTORY, null, values);
    }

    public void insertHisUni(
            String ncode, String url, String name, String time, String site,
            String type, String viewed, String current, int offset)
    {
        mDataBase.beginTransaction();

        try
        {
            if (hasHis(ncode))
                updateHis(ncode, url, name, time, site, type, viewed, current, offset);
            else
                insertHis(ncode, url, name, time, site, type, viewed, current, offset);

            mDataBase.setTransactionSuccessful();
        } finally
        {
            mDataBase.endTransaction();
        }
    }

    public boolean insertHisLast(
            String ncode, String url, String name, String time, String site,
            String type, String viewed, String current, int offset)
    {
        boolean result = false;
        mDataBase.beginTransaction();

        try
        {
            int[] position = new int[2];

            if (hasHis(ncode, position))
            {
                if (position[0] == position[1] - 1)
                {
                    updateHis(ncode, url, name, time, site, type, viewed, current, offset);
                    result = false;
                } else
                {
                    deleteHis(ncode);
                    insertHis(ncode, url, name, time, site, type, viewed, current, offset);
                    result = true;

                }
            } else
            {
                insertHis(ncode, url, name, time, site, type, viewed, current, offset);
                result = true;
            }

            mDataBase.setTransactionSuccessful();
        } finally
        {
            mDataBase.endTransaction();
        }

        return result;
    }

    public void updateHis(
            String ncode, String url, String name, String time, String site,
            String type, String viewed, String current, int offset)
    {
        ContentValues values = new ContentValues();
        values.put("Ncode", ncode);
        values.put("Url", url);
        values.put("Name", name);
        values.put("Time", time);
        values.put("Site", site);
        values.put("Type", type);
        values.put("Viewed", viewed);
        values.put("Current", current);
        values.put("Offset", offset);

        mDataBase.update(TABLE_HISTORY, values, "Ncode=?", new String[]{ncode});
    }

    public boolean hasHis(String ncode)
    {
        boolean result;
        Cursor cursor = mDataBase.query(TABLE_HISTORY, null, "Ncode=?",
                new String[]{ncode}, null, null, null);
        result = cursor.moveToFirst();
        cursor.close();

        return result;
    }

    public boolean hasHis(String ncode, int[] position)
    {
        boolean result = false;
        Cursor cursor = mDataBase.query(TABLE_HISTORY, null, null,
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

    public Cursor getHis()
    {
        Cursor cursor = mDataBase.query(
                TABLE_HISTORY, null, null, null, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public Cursor getHis(String ncode)
    {
        Cursor cursor = mDataBase.query(TABLE_HISTORY, null, "Ncode=?",
                new String[]{ncode}, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public void deleteHis()
    {
        mDataBase.delete(TABLE_HISTORY, null, null);
    }

    public void deleteHis(String ncode)
    {
        mDataBase.delete(TABLE_HISTORY, "Ncode=?", new String[]{ncode});
    }


    public void insertFav(
            String ncode, String url, String name, String time, String site, String type)
    {
        ContentValues values = new ContentValues();
        values.put("Ncode", ncode);
        values.put("Url", url);
        values.put("Name", name);
        values.put("Time", time);
        values.put("Site", site);
        values.put("Type", type);

        mDataBase.insert(TABLE_FAVORITE, null, values);
    }

    public void insertFavUni(
            String ncode, String url, String name, String time, String site, String type)
    {
        mDataBase.beginTransaction();

        try
        {
            if (hasFav(ncode))
                updateFav(ncode, url, name, time, site, type);
            else
                insertFav(ncode, url, name, time, site, type);

            mDataBase.setTransactionSuccessful();
        } finally
        {
            mDataBase.endTransaction();
        }
    }

    public void updateFav(
            String ncode, String url, String name, String time, String site, String type)
    {
        ContentValues values = new ContentValues();
        values.put("Ncode", ncode);
        values.put("Url", url);
        values.put("Name", name);
        values.put("Time", time);
        values.put("Site", site);
        values.put("Type", type);

        mDataBase.update(TABLE_FAVORITE, values, "Ncode=?", new String[]{ncode});
    }

    public boolean hasFav(String ncode)
    {
        boolean result;
        Cursor cursor = mDataBase.query(TABLE_FAVORITE, null, "Ncode=?",
                new String[]{ncode}, null, null, null);
        result = cursor.moveToFirst();
        cursor.close();

        return result;
    }

    public boolean hasFav(String ncode, int[] position)
    {
        boolean result = false;
        Cursor cursor = mDataBase.query(TABLE_FAVORITE, null, null,
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

    public Cursor getFav()
    {
        Cursor cursor = mDataBase.query(
                TABLE_FAVORITE, null, null, null, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public Cursor getFav(String ncode)
    {
        Cursor cursor = mDataBase.query(TABLE_FAVORITE, null, "Ncode=?",
                new String[]{ncode}, null, null, null);

        if (cursor.moveToFirst())
            return cursor;
        else
            cursor.close();

        return null;
    }

    public void deleteFav()
    {
        mDataBase.delete(TABLE_FAVORITE, null, null);
    }

    public void deleteFav(String ncode)
    {
        mDataBase.delete(TABLE_FAVORITE, "Ncode=?", new String[]{ncode});
    }
}
