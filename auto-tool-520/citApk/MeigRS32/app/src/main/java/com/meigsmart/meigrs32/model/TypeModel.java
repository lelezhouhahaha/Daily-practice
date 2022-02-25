package com.meigsmart.meigrs32.model;

import android.content.ComponentName;

/**
 * Created by chenMeng on 2018/4/24.
 */
public class TypeModel {
    private int id;
    private String name;
    private int type;//0 unselected ; 1 pass ; 2 failure
    private Class cls;
    private int startType;  //0: local   1:package/class name
    private String packageName;
    private String className;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Class getCls() {
        return cls;
    }

    public void setCls(Class cls) {
        this.cls = cls;
    }

    public void setStartType(int type)
    {
        startType = type;
    }

    public int getStartType()
    {
        return startType;
    }

    public void setPackageName(String pkgname)
    {
        packageName = pkgname;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public void setClassName(String clsname)
    {
        className = clsname;
    }

    public String getClassName()
    {
        return className;
    }
}
