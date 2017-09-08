package com.wangguangjie.hit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by wangguangjie on 16/9/26.
 */

public class InformationAdapter extends BaseAdapter {
    ArrayList<NewItem> lists=new ArrayList<>();
    LayoutInflater layoutInflater;
    public InformationAdapter(Context context, ArrayList<NewItem> ls)
    {
        this.lists=ls;
        layoutInflater= LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return lists.size();
    }

    @Override
    public Object getItem(int i) {
        return lists.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout line;
        TextView title;
        TextView time;
        TextView visitCount;
        if(view==null)
        {
            line=(LinearLayout)layoutInflater.inflate(R.layout.application4_item,null);

        }
        else
        {
            line=(LinearLayout)view;
        }
        title=(TextView)line.findViewById(R.id.item_title);
        time=(TextView)line.findViewById(R.id.item_time);
        visitCount=(TextView)line.findViewById(R.id.item_visitCount);
        //Map<String ,Object> map=lists.get(i);
        NewItem item=lists.get(i);
        title.setText(item.getTitle());
        time.setText(item.getTime());
        visitCount.setText(item.getVisitCount());
        return line;
    }
}
