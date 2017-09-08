package com.wangguangjie.hit;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * Created by wangguangjie on 16/9/26.
 * 本程序实现爬取哈工大教务处的信息,并做分类处理,用户可根据需要分类查看;
 * 本程序主要有三个亮点:1.使用Jsoup实现爬去网络信息(掌握了使用Jsoup爬取信息)。
 *                  2.自定义ListViw,实现下拉刷新,上拉获取更多信心,并记录上次刷新时间。
 *                  3.使用缓存对每一次刷新的信息进行存储,让用户下次使用程序时候不用等待很长的时间就可进行查询信息。
 *                  4.定义菜单栏,进行个性化设置和功能扩展。
 */

public class MainActivity extends Activity {
    //列表信息;
    // private ArrayList<NewItem> lists=new ArrayList<>();
    private StoreInformation store_lists;
    //pages;
    private int pages = 0;
    //url;
    private final String HIT1 = "http://www.hitsz.edu.cn/article/id-74.html";
    private final String HIT2 = "http://www.hitsz.edu.cn/article/id-75.html";
    private final String HIT3 = "http://www.hitsz.edu.cn/article/id-77.html";
    private final String HIT4 = "http://www.hitsz.edu.cn/article/id-78.html";
    private final String HIT5 = "http://www.hitsz.edu.cn/article/id-80.html";

    private String url1 = HIT1 + "?maxPageItems=10&keywords=&pager.offset=";
    private String url2 = HIT2 + "?maxPageItems=10&keywords=&pager.offset=";
    private String url3 = HIT3 + "?maxPageItems=10&keywords=&pager.offset=";
    private String url4 = HIT4 + "?maxPageItems=10&keywords=&pager.offset=";
    private String url5 = HIT5 + "?maxPageItems=10&keywords=&pager.offset=";

    private String className1="announcement";
    private String className2="newsletters";
    private String className3="";
    private String className4="";
    private String className5="";
    private String className6="";
    //
    private String url = url1;
    private String page_url;
    //页码数;初始页码为1;
    private int page_number = 0;
    //
    final private String HIT = "http://www.hitsz.edu.cn";
    //
    private PullListView listView;
    //
    private InformationAdapter adapter;
    //
    private boolean first = true;
    //
    int select;
    private boolean isFirst;
    //主线程执行信息显示,如果出现异常情况通知用户;
    SpinnerAdapter spinnerAdapter;
    ActionBar.OnNavigationListener navigationListener;
    ActionBar actionBar;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //更改解析出信息,更新界面;
            if (msg.what == 0x123) {
                showInfo();
                //缓存数据;
                new Thread(){
                    public void run(){
                        store_lists.storeData();
                    }
                }.start();
            }
            //通过actionbar的选择进行解析数据
            else if (msg.what == 0x124) {
                first = true;
                page_number = 0;
                page_url = url + page_number;
                page_number+=10;
                new Thread(new getThread()).start();
            }
            //如果无更多页面不许进行加载更多;
            else if (msg.what == 0x125) {
                Toast.makeText(MainActivity.this, "无更多信息!", Toast.LENGTH_LONG).show();
                listView.getMoreComplete();
            } else if (msg.what == 0x126) {
                adapter = new InformationAdapter(MainActivity.this, store_lists.getLists());
                listView.setAdapter(adapter);
                listView.deferNotifyDataSetChanged();
            }
            //处理异常信息;
            else if (msg.what == 0x111) {
                Toast.makeText(MainActivity.this, "无法获取信息", Toast.LENGTH_LONG).show();
            }
            //获取信息失败;
            else if (msg.what == 0x222) {
                Toast.makeText(MainActivity.this, "信息获取失败,请重新尝试!", Toast.LENGTH_LONG).show();
            }
            //无法连接网络;
            else if (msg.what == 0x333) {
                Toast.makeText(MainActivity.this, "无法连接网络,请重新尝试!", Toast.LENGTH_LONG).show();
            }
        }
    };

    //子线程执行网络信息的获取任务;
    class getThread implements Runnable {

        @Override
        public void run() {
            getMessage();
        }
    }

    class WastTime implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(2000);
                Message message = new Message();
                message.what = 0x125;
                handler.sendMessage(message);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    //刚启动程序时,为防止用户长时间的等待,次线程加载缓存数据;
    class RecoveryThread implements Runnable {

        @Override
        public void run() {
            Message msg = new Message();
            msg.what = 0x126;
            handler.sendMessage(msg);
        }
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        this.setContentView(R.layout.application4_start);
        //初始化组件;
        initView();
        store_lists = new StoreInformation(getSharedPreferences("hit1", MODE_PRIVATE));
        store_lists.recoveryData();
        if (store_lists.getLists().size() > 0) {
            new Thread(new RecoveryThread()).start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        new Thread(new getThread()).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.application4_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //菜单操作响应;
    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        switch (menu.getItemId()) {
            case R.id.item_search:
                //业务代码;
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(menu);
    }

    //初始化界面;
    private void initView()
    {
        isFirst=true;
        //actionBar的相关设置;
        spinnerAdapter= ArrayAdapter.createFromResource(this,R.array.classifies,android.R.layout.simple_spinner_dropdown_item);
        navigationListener=new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int position, long l) {
                switch (position)
                {
                    case 0:
                        url=url1;
                        break;
                    case 1:
                        url=url2;
                        break;
                    case 2:
                        url=url3;
                        break;
                    case 3:
                        url=url4;
                        break;
                    case 4:
                        url=url5;
                        break;
                    default:
                        break;
                }
                if(!isFirst) {
                    Message msg = new Message();
                    msg.what = 0x124;
                    handler.sendMessage(msg);
                }
                isFirst=false;
                return true;
            }
        };
        actionBar=getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(spinnerAdapter,
                navigationListener);
        actionBar.setTitle("HIT消息");
        actionBar.setIcon(R.mipmap.hit);

        //获取每个页面的url;
        page_url=url+page_number;
        page_number+=10;
        //对listview设置下拉刷新,底部刷新和点击操作监听;
        listView=(PullListView)findViewById(R.id.pulllist);
        listView.addSharePreference(getSharedPreferences("pull_listview",MODE_PRIVATE));
        //根据用户选择不同的打开方式;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
                final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,AlertDialog.THEME_HOLO_DARK);
                builder.setTitle("提示");
                //builder.setMessage("需要使用浏览器打开吗?");
                builder.setSingleChoiceItems(new String[]{"本地打开查看", "浏览器打开"}, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        select=i;
                    }
                });
                builder.setPositiveButton("确定",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (select)
                        {
                            case 1:
                                Intent intent=new Intent();
                                intent.setAction("android.intent.action.VIEW");
                                Uri uri= Uri.parse(store_lists.getLists().get(position-1).getUrl());
                                intent.setData(uri);
                                // intent.setClassName("com.android.browser","om.android.browser.BrowserActivity");
                                startActivity(intent);
                                builder.create().dismiss();
                                break;
                            case 0:
                                Intent intent1=new Intent(MainActivity.this,WebInformation.class);
                                Bundle bundle=new Bundle();
                                NewItem item=store_lists.getLists().get(position-1);
                                bundle.putString("url",item.getUrl());
                                intent1.putExtras(bundle);
                                startActivity(intent1);
                                builder.create().dismiss();
                                break;
                            default:break;
                        }
                        select=0;
                    }
                });
                builder.setNegativeButton("取消",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        //finish();
                    }
                });
                builder.create().show();
            }
        });
        //刷新;
        listView.setOnRefreshListener(new PullListView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                first=true;
                page_number=0;
                page_url=url+page_number;
                page_number+=10;
                //page_number++;
                //page_url+=page_number;
                //pages=0;
                //lists.clear();
                new Thread(new getThread()).start();
            }
        },1);
        //加载更多;
        listView.setOnGetMoreListener(new PullListView.OnGetMoreListener() {
            @Override
            public void onGetMore() {
                first=false;
                if(page_number/10<=pages)
                {
                    page_url=url+page_number;
                    page_number+=10;
                    new Thread(new getThread()).start();
                }
                else
                {
                    new Thread(new WastTime()).start();
                }

            }
        });
    }

    //主线程显示信息;
    private void showInfo()
    {
        //判断是否是第一次刷新;
        if(first) {
            adapter = new InformationAdapter(this, store_lists.getLists());
            listView.setAdapter(adapter);
        }
        else
        {
            adapter.notifyDataSetChanged();
        }
        //信息获取完毕,技术刷新操作;
        listView.refreshComplete();
        listView.getMoreComplete();
        View view1 = listView;
        int[] location = {0, 0};
        view1.getLocationOnScreen(location);
        int cx = location[0] + view1.getWidth() / 2;
        int cy = view1.getHeight() / 2;
        int radix = (int) Math.hypot(view1.getWidth() / 2, view1.getHeight() / 2);
        Animator animator = ViewAnimationUtils.createCircularReveal(view1, cx, cy, 0, radix);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(750);
        animator.start();

    }
    //获取网络信息;
    private void getMessage()
    {
        if(isNetWorkAvailable())
        {
            analyHtml();
            Message msg = new Message();
            msg.what = 0x123;
            handler.sendMessage(msg);
        }
        else
        {
            Message message=new Message();
            message.what=0x333;
            handler.sendMessage(message);
        }
    }

    //直接从获取页码的document进行解析;
    //本程序采用这种方法解析html;
    public void  analyHtml()
    {
        Connection connect = Jsoup.connect(page_url);
        //伪装成浏览器对url进行访问,防止无法获取某些网站的document;
        connect.header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/    20100101 Firefox/32.0");
        try
        {
            Document doc = connect.get();

            //解析出body 标签下的div标签;
            Elements elements = doc.select("body ul");
            Log.d("mylogcat2","2");
            //上拉刷新或者第一次刷新,清除数据;
            if(first)
            {
                store_lists.clear();
                Elements elements1=doc.select("body div");
                for(Element element:elements1){
                    if(element.className().equals("page_num")){
                        pages=element.getElementsByTag("a").size();
                    }
                }
            }

            //获取相关信息:Jsoup;
            for(Element el1:elements)
            {
                if(el1.className().equals("announcement"))
                {
                    analyze1(el1);
                    break;
                }
                else if(el1.className().equals("newsletters"))
                {
                    analyze2(el1);
                    break;
                }
                else if(el1.className().equals("lecture_n"))
                {
                    analyze3(el1);
                    break;
                }

            }
        }
        catch (Exception ie)
        {
            Log.d("mylogcat0",ie.toString());
            //无法获取document时候提醒用户;
            Message message=new Message();
            message.what=0x222;
            handler.sendMessage(message);
            ie.printStackTrace();
        }
    }

    private void analyze1(Element el1){
        String title;
        String url;
        String time;
        String visitCount;
        String top;
        Elements el0=el1.getElementsByTag("li");
        for(Element ell:el0)
        {
            title=ell.getElementsByTag("a").text();
            url=HIT+ell.getElementsByTag("a").attr("href");
            Elements ss=ell.getElementsByTag("span");
            if(ss.size()==3){
                top=ss.get(0).text();
                title=title.substring(0,title.length()-2)+"[置顶]";
                time=ss.get(1).text();
                visitCount=ss.get(2).text();
            }else
            {
                time=ss.get(0).text();
                visitCount=ss.get(1).text();
            }
            //title=ss.get(1).text();
            store_lists.addItem(new NewItem(title,time,visitCount,url));
        }
    }
    private void analyze2(Element el1){
        String title;
        String url;
        String time;
        String visitCount;
        String top;
        Elements el0=el1.getElementsByTag("li");
        for(Element ell:el0){
            title=ell.getElementsByTag("a").text();
            url=HIT+ell.getElementsByTag("a").get(0).attr("href");
            Elements ss=ell.getElementsByTag("span");
            time=ss.get(0).text();
            visitCount=ss.get(1).text();
            store_lists.addItem(new NewItem(title,time,visitCount,url));
        }
    }
    private void analyze3(Element el1){
        String title="";
        String url;
        String time="";
        String visitCount="";
        String top;
        Elements el0=el1.getElementsByTag("li");
        for(Element ell:el0){
            Elements s=ell.getElementsByTag("div");
            title=s.get(0).getElementsByTag("a").text();
            url=HIT+s.get(0).getElementsByTag("a").attr("href");
            time=s.get(2).getElementsByTag("span").get(0).text();
            visitCount=s.get(2).getElementsByTag("span").get(1).text();
            store_lists.addItem(new NewItem(title,time,visitCount,url));
        }
    }
    //判断是否有网络连接;
    public boolean isNetWorkAvailable()
    {
        Context context=MainActivity.this.getApplicationContext();
        ConnectivityManager connectmanger=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //NetworkInfo network=connectmanger.getActiveNetworkInfo();
        if(connectmanger!=null)
        {
            NetworkInfo ninfo=connectmanger.getActiveNetworkInfo();
            return ninfo != null && ninfo.isConnected();
        }
        else{
            return false;
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation==Configuration.ORIENTATION_PORTRAIT){

        }
    }
}
