package com.example.imageclassificationdemo;

public class ScanResult {
    String classType;
    float classPercent;
    String remarks;
    String disease;

    public ScanResult() {
    }

    public ScanResult(String classType, float classPercent, String remarks, String disease) {
        this.classType = classType;
        this.classPercent = classPercent;
        this.remarks = remarks;
        this.disease = disease;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public float getClassPercent() {
        return classPercent;
    }

    public void setClassPercent(float classPercent) {
        this.classPercent = classPercent;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }
}
