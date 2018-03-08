package com.wangguangjie.hit;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;


import com.wangguangjie.hit.controller.TableFragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;
import android.support.design.widget.TabLayout;

/**
 * Created by wangguangjie on 16/9/26.
 * 本程序实现爬取哈工大教务处的信息,并做分类处理,用户可根据需要分类查看;
 * 本程序主要有三个亮点:1.使用Jsoup实现爬去网络信息(掌握了使用Jsoup爬取信息)。
 *                  2.自定义ListViw,实现下拉刷新,上拉获取更多信心,并记录上次刷新时间。
 *                  3.使用缓存对每一次刷新的信息进行存储,让用户下次使用程序时候不用等待很长的时间就可进行查询信息。
 *                  4.定义菜单栏,进行个性化设置和功能扩展。
 */

public class MainActivity extends AppCompatActivity {
    //各项urls
    private String[] urls;

    private List<ItemFragment> mItemFragments=new ArrayList<>();
    private ViewPager mViewPager;
    private  TabLayout mTableLayout;
    public static String PREFERENCES="PERFERENCESNAME";


    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        this.setContentView(R.layout.activity_main);
        urls=getResources().getStringArray(R.array.items_urls);
        mViewPager=(ViewPager)findViewById(R.id.view_pager);
        for(int i=0;i<urls.length;i++){
            ItemFragment fragment=new ItemFragment();
            Bundle bundle=new Bundle();
            bundle.putString(PREFERENCES,"hit"+i);
            bundle.putString("url",urls[i]);
            fragment.setArguments(bundle);
            mItemFragments.add(fragment);
        }
        mViewPager.setAdapter(new TableFragmentPagerAdapter(getSupportFragmentManager(),
                mItemFragments,getResources().getStringArray(R.array.classifies)));
        mTableLayout=(android.support.design.widget.TabLayout)findViewById(R.id.table_layout);
        mTableLayout.setupWithViewPager(mViewPager);
    }

}
