package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NextPopupBean implements Serializable {
    private boolean visible;
    private List<LinkBean> linkBeans;
    private String title;
    private String message;

    public NextPopupBean() {
        reset();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<LinkBean> getLinkBeans() {
        return linkBeans;
    }

    public void setLinkBeans(List<LinkBean> linkBeans) {
        this.linkBeans = linkBeans;
    }

    public static NextPopupBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        NextPopupBean npb = (NextPopupBean)mbr.resolve("nextPopupBean");
        return npb;
    }

    public void reset() {
        visible = false;
        linkBeans = new ArrayList<LinkBean>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
