package com.sty.ne.db.update;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * xml 文件的根节点
 * @Author: tian
 * @UpdateDate: 2020/11/24 8:42 PM
 */
public class UpdateXml {
    private List<UpdateStep> updateSteps;

    public UpdateXml(Document document) {
        //获取升级的脚本，解析根节点
        NodeList updateSteps = document.getElementsByTagName("updateStep");
        this.updateSteps = new ArrayList<>();
        for (int i = 0; i < updateSteps.getLength(); i++) {
            Element ele = (Element) updateSteps.item(i);
            UpdateStep step = new UpdateStep(ele);
            this.updateSteps.add(step);
        }
    }

    public List<UpdateStep> getUpdateSteps() {
        return updateSteps;
    }
}
