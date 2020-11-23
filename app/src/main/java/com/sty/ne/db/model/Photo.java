package com.sty.ne.db.model;

import com.sty.ne.db.annotation.DbTable;

/**
 * @Author: tian
 * @UpdateDate: 2020/11/23 10:41 PM
 */
@DbTable("tb_photo")
public class Photo {
    private String time;
    private String path;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
