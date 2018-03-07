package com.wangguangjie.hit;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.wangguangjie.hit.controller.InformationAdapter;
import com.wangguangjie.hit.model.NewItem;
import com.wangguangjie.hit.utils.StoreInformation;
import com.wangguangjie.hit.view.RefreshLinearLayout;

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
    //封装信息;
    private StoreInformation store_lists;
    //显示信息列表
    private ListView mListView;
    //下拉刷新GroupView
    private RefreshLinearLayout mRefreshLinearLayout;

    //pages;
    private int pages = 0;
    //各个信息的url;
    private static final String HIT1 = "http://www.hitsz.edu.cn/article/id-74.html";
    private static final String HIT2 = "http://www.hitsz.edu.cn/article/id-75.html";
    private static final String HIT3 = "http://www.hitsz.edu.cn/article/id-77.html";
    private static final String HIT4 = "http://www.hitsz.edu.cn/article/id-78.html";
    private static final String HIT5 = "http://www.hitsz.edu.cn/article/id-80.html";
    final static String mString="?maxPageItems=10&keywords=&pager.offset=";
    private static String url1 = HIT1 + mString;
    private static String url2 = HIT2 + mString;
    private static String url3 = HIT3 + mString;
    private static String url4 = HIT4 + mString;
    private static String url5 = HIT5 + mString;

    //当前url;
    private String url = url1;
    //页码数;初始页码为0;
    private int page_number = 0;
    //当前url页码的url;
    private String page_url=url+page_number;
    //
    final private String HIT = "http://www.hitsz.edu.cn";
    //
    private InformationAdapter adapter;
    //
    private boolean first = true;
    private boolean isFirst;
    SpinnerAdapter spinnerAdapter;
    ActionBar.OnNavigationListener navigationListener;
    ActionBar actionBar;
    //主线程执行信息显示,如果出现异常情况通知用户;
    private  Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //更改解析出信息,更新界面;
            if (msg.what == 0x123) {
                updateUI();
            }
            //如果无更多页面不许进行加载更多;
            else if (msg.what == 0x125) {
                Toast.makeText(MainActivity.this, "无更多信息!", Toast.LENGTH_LONG).show();
            }
            //恢复本地保存数据.
            else if (msg.what == 0x126) {
                adapter = new InformationAdapter(MainActivity.this, store_lists.getLists());
                mListView.setAdapter(adapter);
                mListView.deferNotifyDataSetChanged();
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

    //异步加载缓存数据;
    class RecoveryThread implements Runnable {
        @Override
        public void run() {
            store_lists.recoveryData();
            Message msg = new Message();
            msg.what = 0x126;
            handler.sendMessage(msg);
        }
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        this.setContentView(R.layout.activity_main);
        //初始化组件;
        initView();
        //子线程获取缓存数据
        store_lists = new StoreInformation(getSharedPreferences("hit1", MODE_PRIVATE));
        new Thread(new RecoveryThread()).start();
    }

    @Override
    public void onResume(){
        super.onResume();
        //用户初次则自动进行首次数据获取;
        if(store_lists.getLists().size()==0){
            new Thread(new getThread()).start();
        }
    }
    //加载菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       // getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //选择菜单项回调;
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
                    page_number = 0;
                    page_url = url + page_number;
                    page_number+=10;
                    new Thread(new getThread()).start();
                }
                isFirst=false;
                return true;
            }
        };
        //设置actionbar
        actionBar=getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(spinnerAdapter,
                navigationListener);
        actionBar.setTitle(getResources().getString(R.string.title));
        actionBar.setIcon(R.mipmap.hit);
        //注册ListView监听
        mListView=findViewById(R.id.listview);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
                Log.d("mytest","1");
                if (store_lists.getLists()!=null&&store_lists.getLists().size() > 0) {
                    Intent intent1 = new Intent(MainActivity.this, WebInformation.class);
                    Bundle bundle = new Bundle();
                    NewItem item = store_lists.getLists().get(position - 1);
                    bundle.putString("url", item.getUrl());
                    intent1.putExtras(bundle);
                    startActivity(intent1);
                }
            }
        });
        //注册RefreshLinearLayout的下拉刷新监听器
        mRefreshLinearLayout=findViewById(R.id.refresh_linear_layout);
        mRefreshLinearLayout.setOnRefreshingListener(new RefreshLinearLayout.RefreshingListener() {
            @Override
            public void onRefresh() {
                first=true;
                page_number=0;
                page_url=url+page_number;
                page_number+=10;
                getMessage();
                store_lists.storeData();
            }
        });
        //注册RefreshLinearLayout的加载更多监听器;
        mRefreshLinearLayout.setOnGetMoreListener(new RefreshLinearLayout.GetMoreListener() {
            @Override
            public void onGetMore() {
                if(page_number/10<=pages)
                {
                    first=false;
                    page_url=url+page_number;
                    page_number+=10;
                    getMessage();
                    store_lists.storeData();
                }
                else{
                    //无更多内容
                    Toast.makeText(MainActivity.this,"无更多内容",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //刷新界面;
    private void updateUI()
    {
        //列表刷新
        adapter.notifyDataSetChanged();
        //设置刷新动画
        View view1 = mListView;
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
            analyzeHtml();
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
    public void  analyzeHtml()
    {
        Connection connect = Jsoup.connect(page_url);
        //伪装成浏览器对url进行访问,防止无法获取某些网站的document;
        connect.header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/    20100101 Firefox/32.0");
        try
        {
            Document doc = connect.get();
            //解析出body 标签下的div标签;
            Elements elements = doc.select("body ul");
            //下拉刷新或者第一次刷新,清除数据;
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
            //获取相关信息
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
