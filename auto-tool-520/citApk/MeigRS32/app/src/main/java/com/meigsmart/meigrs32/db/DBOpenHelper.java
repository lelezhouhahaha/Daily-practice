package com.meigsmart.meigrs32.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "MeiGRS51_01.db"; //数据库名称
    private static final int DB_VERSION = 1;//数据库版本,大于0

    private static final String CREATE_FUNCTION = "create table " + FunctionDao.TABLE + " ("
            + FunctionDao.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + FunctionDao.FATHER_NAME + " TEXT, "
            + FunctionDao.SUB_NAME + " TEXT, "
            + FunctionDao.REASON + " TEXT, "
            + FunctionDao.RESULTS + " int)";

    public DBOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FUNCTION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
