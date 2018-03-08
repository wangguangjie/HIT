package com.wangguangjie.hit.controller;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.wangguangjie.hit.ItemFragment;

import java.util.List;


/**
 * Created by wangguangjie on 2018/3/7.
 */

public class TableFragmentPagerAdapter extends FragmentPagerAdapter {

    List<ItemFragment> mFragments;
    String[] items;

    public TableFragmentPagerAdapter(FragmentManager manager, List<ItemFragment> list, String[] strings){
        super(manager);
        mFragments=list;
        items=strings;
    }
    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position){
        return items[position];
    }
}
