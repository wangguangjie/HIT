package com.wangguangjie.hit;

/**
 * Created by wangguangjie on 16/9/28.
 */

public class NewItem {
    public String url;
    public String title;
    public String visitCount;
    public String time;
    public NewItem(String title, String time, String visitCount,String url)
    {
        this.title=title;
        this.time=time;
        this.visitCount=visitCount;
        this.url=url;
    }
    public void setUrl(String url)
    {
        this.url=url;
    }
    public String getUrl()
    {
        return url;
    }
    public String getVisitCount(){
        return visitCount;
    }
    public void setTitle(String title)
    {
        this.title=title;
    }
    public String getTitle()
    {
        return title;
    }
    public void setTime(String t)
    {
        this.time=t;
    }
    public String getTime()
    {
        return time;
    }
}