package com.meigsmart.meigrs32.db;

/**
 * Created by chenMeng on 2018/4/26.
 */
public class FunctionBean {
    private int id;
    private String fatherName;
    private String subclassName;
    private int results;//0 unTest ; 1  failure ; 2 success
    private String reason;//test reason

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getSubclassName() {
        return subclassName;
    }

    public void setSubclassName(String subclassName) {
        this.subclassName = subclassName;
    }

    public int getResults() {
        return results;
    }

    public void setResults(int results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "{\nid:"+getId()+"\nfatherName:"+getFatherName()+"\nsubName:"+getSubclassName()+"\nresults:"+getResults()+"\n}";
    }
}
