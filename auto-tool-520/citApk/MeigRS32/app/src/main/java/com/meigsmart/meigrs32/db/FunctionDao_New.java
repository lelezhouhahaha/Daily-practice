package com.meigsmart.meigrs32.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenMeng on 2017/11/1.
 */

public class FunctionDao_New {
    private DBOpenHelper_New locationHelper;
    private SQLiteDatabase locationDb;
    public static String TABLE = "function";
    public static String ID = "id";
    public static String FATHER_NAME = "fatherName";
    public static String SUB_NAME = "subclassName";
    public static String RESULTS = "results";
    public static String REASON = "reason";

    public FunctionDao_New(Context context) {
        locationHelper = new DBOpenHelper_New(context);
    }

    /**
     * 添加一条新记录
     *
     * @param bean
     */
    public void addData(FunctionBean bean) {
        locationDb = locationHelper.getReadableDatabase();

        ContentValues values = new ContentValues();
        values.put(FATHER_NAME, bean.getFatherName());
        values.put(SUB_NAME, bean.getSubclassName());
        values.put(RESULTS, bean.getResults());
        values.put(REASON,bean.getReason());

        locationDb.insert(TABLE, null, values);
        locationDb.close();
    }

    /**
     * 删除指定记录
     */
    public void delete(String fatherName, String subName) {
        locationDb = locationHelper.getReadableDatabase();
        if (locationDb.isOpen())
            locationDb.delete(TABLE, FATHER_NAME + "=? and " + SUB_NAME + "=? ", new String[]{fatherName, subName});
        locationDb.close();
    }

    /**
     * 删除记录
     */
    public void deleteAll() {
        locationDb = locationHelper.getReadableDatabase();
        if (locationDb.isOpen())
            locationDb.delete(TABLE, null, null);
        locationDb.close();
    }

    /**
     * 更新数据
     */
    public void update(String fatherName, String subName, int result,String reason) {
        locationDb = locationHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(RESULTS, result);
        values.put(REASON,reason);
        if (locationDb.isOpen())
            locationDb.update(TABLE, values, FATHER_NAME + "=? and " + SUB_NAME + "=? ", new String[]{fatherName, subName});
        locationDb.close();
    }

    /**
     * 查询记录  subName
     *
     * @return
     */
    public FunctionBean getSubData(String fatherName, String subName) {
        FunctionBean bean = new FunctionBean();

        String sql = "select * from " + TABLE + " where "+ FATHER_NAME + "=? and " + SUB_NAME + "=? ";
        locationDb = locationHelper.getReadableDatabase();
        Cursor cursor = locationDb.rawQuery(sql, new String[]{fatherName , subName});

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(ID));
            String fName = cursor.getString(cursor.getColumnIndex(FATHER_NAME));
            String sName = cursor.getString(cursor.getColumnIndex(SUB_NAME));
            int result = cursor.getInt(cursor.getColumnIndex(RESULTS));
            String reason = cursor.getString(cursor.getColumnIndex(REASON));

            bean.setId(id);
            bean.setFatherName(fName);
            bean.setSubclassName(sName);
            bean.setResults(result);
            bean.setReason(reason);

        }
        cursor.close();
        //关闭数据库
        locationDb.close();
        return bean;
    }

    /**
     * 查询记录  fatherName
     *
     * @return
     */
    public List<FunctionBean> getFatherData(String fatherName) {
        List<FunctionBean> dataList = new ArrayList<>();

        String sql = "select * from " + TABLE + " where "+ FATHER_NAME + "=? ";
        locationDb = locationHelper.getReadableDatabase();
        Cursor cursor = locationDb.rawQuery(sql, new String[]{fatherName});

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(ID));
            String fName = cursor.getString(cursor.getColumnIndex(FATHER_NAME));
            String sName = cursor.getString(cursor.getColumnIndex(SUB_NAME));
            int result = cursor.getInt(cursor.getColumnIndex(RESULTS));
            String reason = cursor.getString(cursor.getColumnIndex(REASON));

            FunctionBean bean = new FunctionBean();
            bean.setId(id);
            bean.setFatherName(fName);
            bean.setSubclassName(sName);
            bean.setResults(result);
            bean.setReason(reason);

            dataList.add(bean);
        }
        cursor.close();
        locationDb.close();
        return dataList;
    }

    /**
     * 查询所有的记录
     *
     * @return
     */
    public List<FunctionBean> getAllData() {
        List<FunctionBean> dataList = new ArrayList<>();

        String sql = "select * from " + TABLE;
        locationDb = locationHelper.getReadableDatabase();
        Cursor cursor = locationDb.rawQuery(sql, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(ID));
            String fName = cursor.getString(cursor.getColumnIndex(FATHER_NAME));
            String sName = cursor.getString(cursor.getColumnIndex(SUB_NAME));
            int result = cursor.getInt(cursor.getColumnIndex(RESULTS));
            String reason = cursor.getString(cursor.getColumnIndex(REASON));

            FunctionBean bean = new FunctionBean();
            bean.setId(id);
            bean.setFatherName(fName);
            bean.setSubclassName(sName);
            bean.setResults(result);
            bean.setReason(reason);

            dataList.add(bean);
        }
        cursor.close();
        locationDb.close();
        return dataList;
    }

}
