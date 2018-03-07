package com.wangguangjie.hit;

import android.animation.Animator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Bitmap;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.ProgressBar;


/**
 * Created by wangguangjie on 16/9/26.
 */

public class WebInformation extends Activity {
    WebView mWebview;
    WebSettings mWebSettings;
    String url;
    ProgressBar mProgressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.web_information);
        Bundle bundle=getIntent().getExtras();
        url=bundle.getString("url");

        mWebview = (WebView) findViewById(R.id.webView1);
        mProgressBar=(ProgressBar)findViewById(R.id.load_progress);
        mWebSettings = mWebview.getSettings();
        //可以与JavaScript交互.
        mWebSettings.setJavaScriptEnabled(true);
        //支持查件;
        //mWebSettings.setPluginsEnabled(true);
        //适应屏幕大小;
        mWebSettings.setUseWideViewPort(true);//图片缩放至webview大小.
        mWebSettings.setLoadWithOverviewMode(true);//缩放至屏幕大小.
        mWebSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        mWebSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        mWebSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件
        mWebSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //关闭webview中缓存
        mWebSettings.setAllowFileAccess(true); //设置可以访问文件
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        mWebSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        mWebSettings.setDefaultTextEncodingName("utf-8");//设置编码格式
        //优先使用缓存:
        mWebSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        //缓存模式如下：
        //LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
        //LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
        //LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
        //LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
        mWebview.loadUrl(url);

        //设置不用系统浏览器打开,直接显示在当前Webview
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                System.out.println("开始加载了");
                mProgressBar.setVisibility(View.VISIBLE);
                //mWebview.setVisibility(View.GONE);

            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                {
                    mProgressBar.setVisibility(View.GONE);
                    //加载动画;
                    View view1 = view;
                    int[] location = {0, 0};
                    view1.getLocationOnScreen(location);
                    int cx = location[0] + view1.getWidth() / 2;
                    int cy = view1.getHeight() / 2;
                    int radix = (int) Math.hypot(view1.getWidth() / 2, view1.getHeight() / 2);
                    Animator animator = ViewAnimationUtils.createCircularReveal(view1, cx, cy, 0, radix);
                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                    animator.setDuration(750);
                    animator.start();
                    mWebview.setVisibility(View.VISIBLE);
                }
            }
        });

        //设置WebChromeClient类
        mWebview.setWebChromeClient(new WebChromeClient() {

            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
            }

            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgressBar.setProgress(newProgress);
            }
        });
    }

    //点击返回上一页面而不是退出浏览器
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebview.canGoBack()) {
            mWebview.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    //
    @Override
    protected void onDestroy() {
        if (mWebview != null) {
            mWebview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebview.clearHistory();

            ((ViewGroup) mWebview.getParent()).removeView(mWebview);
            mWebview.destroy();
            mWebview = null;
        }
        super.onDestroy();
    }
}