package com.wangguangjie.hit;

import android.content.SharedPreferences;

import java.util.ArrayList;

/**
 * Created by wangguangjie on 16/10/1.
 */

public class StoreInformation {
    //存储列表数据;
    private ArrayList<NewItem> lists=new ArrayList<>();
    //private Context context;
    //用于保存列表数据;
    public final static String LIST_SIZE="SIZE";
    private SharedPreferences preferences;

    public StoreInformation(SharedPreferences p)
    {
        //preferences=new MainActivity().preferences;
        preferences=p;
        //preferences= PreferenceManager.getDefaultSharedPreferences(c);
    }

    public void addItem(NewItem item)
    {
        lists.add(item);
    }

    public ArrayList<NewItem> getLists()
    {
        return lists;
    }
    public void clear()
    {
        lists.clear();
    }
    public void storeData()
    {
        SharedPreferences.Editor editor=preferences.edit();
        editor.clear();

        for(int i=0;i<lists.size();i++)
        {
            editor.putString("title"+i,lists.get(i).title);
            editor.putString("time"+i,lists.get(i).time);
            editor.putString("visitCount"+i,lists.get(i).visitCount);
            editor.putString("url"+i,lists.get(i).url);
        }
        editor.putInt(LIST_SIZE,lists.size());
        editor.commit();
    }
    public void recoveryData()
    {
        //ArrayList<NewItem> p_lists=new ArrayList<>();
        //SharedPreferences.Editor editor=preferences.edit();
        for(int i=0;i<preferences.getInt(LIST_SIZE,0);i++)
        {
            String title,time,url,visitCount;
            title=preferences.getString("title"+i,"");
            time=preferences.getString("time"+i,"");
            visitCount=preferences.getString("visitCount"+i,"");
            url=preferences.getString("url"+i,"");
            NewItem item=new NewItem(title,time,visitCount,url);
            lists.add(item);
        }
    }
}
