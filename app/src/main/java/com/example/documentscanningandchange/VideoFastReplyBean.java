package com.example.documentscanningandchange;

import androidx.annotation.NonNull;

/**
 * @description: 语音回复
 * @author: admin
 * @date: 2023/1/17
 * @email: 1145338587@qq.com
 */
public class VideoFastReplyBean {

    String id;
    String path;
    String remarks;
    long duration;
    String userid;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }
}
