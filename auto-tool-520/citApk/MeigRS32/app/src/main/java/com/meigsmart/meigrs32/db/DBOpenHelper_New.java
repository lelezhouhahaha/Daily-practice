package com.meigsmart.meigrs32.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper_New extends SQLiteOpenHelper {
    private static final String DB_NAME = "MeiGRS51_02.db"; //数据库名称
    private static final int DB_VERSION = 1;//数据库版本,大于0

    private static final String CREATE_FUNCTION = "create table " + FunctionDao_New.TABLE + " ("
            + FunctionDao_New.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + FunctionDao_New.FATHER_NAME + " TEXT, "
            + FunctionDao_New.SUB_NAME + " TEXT, "
            + FunctionDao_New.REASON + " TEXT, "
            + FunctionDao_New.RESULTS + " int)";

    public DBOpenHelper_New(Context context) {
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
