package com.example.adrian.mob.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UploadInfo {

    public String name;
    public String url;

    public UploadInfo() {
    }

    public UploadInfo(String name, String url) {
        this.name = name;
        this.url= url;
    }
}