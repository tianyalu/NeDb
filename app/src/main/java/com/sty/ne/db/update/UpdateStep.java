package com.sty.ne.db.update;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: tian
 * @UpdateDate: 2020/11/24 8:44 PM
 */
public class UpdateStep {
    private String versionFrom;
    private String versionTo;
    private List<UpdateDb> updateDbs;

    public UpdateStep(Element ele) {
        versionFrom = ele.getAttribute("versionFrom");
        versionTo = ele.getAttribute("versionTo");
        this.updateDbs = new ArrayList<>();
        NodeList dbs = ele.getElementsByTagName("updateDb");
        for (int i = 0; i < dbs.getLength(); i++) {
            Element db = (Element) dbs.item(i);
            UpdateDb updateDb = new UpdateDb(db);
            this.updateDbs.add(updateDb);
        }
    }

    public String getVersionFrom() {
        return versionFrom;
    }

    public String getVersionTo() {
        return versionTo;
    }

    public List<UpdateDb> getUpdateDbs() {
        return updateDbs;
    }
}
