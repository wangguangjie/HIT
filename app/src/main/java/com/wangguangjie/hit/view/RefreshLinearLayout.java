package com.wangguangjie.hit.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wangguangjie.hit.R;

import org.jetbrains.annotations.Nullable;

/**
 * Created by wangguangjie on 2018/2/14.
 * 自主开发的下拉刷新，支持下拉进行刷新和上拉获取更多。
 * 功能：
 *     1.下拉刷新
 *     2.上拉加载更多
 * 特色：
 *    1.界面简单，可靠，代码健壮，支持扩展.
 * 开发难点：
 *    1.首先需要判断是否可以下拉刷新和上拉加载更多，判断方法分别为通过ListView可见第一项是否为0和顶端距离是否为0（以便第一项全部出现），
 *    和判断List最后一项低端距离是否为ListVIew的高度进行判断.
 *    2.在判断是否可以下拉刷新和上拉加载更多的时候，防止多线程范围共同变量造成程序错误，在正在刷新时候不进行上拉加载更多，正在上拉加载更多时候不进行
 *    下拉刷新，通知避免通多个下拉刷新和多个上拉加载更多，也就是在进行刷新时无法下拉刷新，在加载更多的时候无法上拉加载更多。
 *    3.下拉刷新释放时采用异步任务动态改变刷新头的状态
 *    4.释放刷新时释放进行刷新采用异步任务进行动态改变刷新头的状态，通知当刷新头上侧与父视图上侧对齐时进行刷新任务，刷新任务结束后与下拉刷新一样的
 *    5.上拉获取更多信息时同时采用异步任务获取更多信息，上拉获取更多和获取结束通知用户具有更强的用户交互性。
 * 使用：
 *    1.RefreshLinearlayout继续Linearlayout，拥有LinearLayout的全部功能，在使用上和LinearLayout区别不大.
 *    2.在调用下拉刷新和上拉加载更多功能时候，只需要实现RefreshingListener和GetMoreListener监听器即可，并注册监听器。
 * 扩展：
 *    以后有需求，可以添加功能更强的下拉刷新，同时设计更美观的动画或者更好的布局，最基本的保证是足够健壮性.
 * Bug修复：
 *    1.在下拉又上滑后出现header无法完全隐藏，修复：在退出触摸时间时，重新设置header的位置；
 *    2.判断ListView是否滑动到低端时判断出错。
 *    3.点击操作出现错误，应该屏蔽点击操作.
 */

public class RefreshLinearLayout extends LinearLayout implements View.OnTouchListener{


    /**
     * refresh 四种状态
     */
    private int REFRESH_COMPLETED=0;
    private int PULL_TO_REFRESH=1;
    private int RELEASE_TO_REFRESH=2;
    private int REFRESHING=3;
    private int PUSH_GET_MORE=4;
    private int GETING_MORE=5;

    //当前状态;
    private int mCurrentState=REFRESH_COMPLETED;
    //上次状态，用于避免重复操作;
    private int mLastState;
    //刷新头
    private LinearLayout mHeader;
    //进度条
    private ProgressBar mProgressBar;
    //
    private ImageView mImageView;
    //状态描述
    private TextView mDescriptionTextView;
    //上次刷新时间
    private TextView mUpdateTimeTextView;
    //rooter
    private LinearLayout mRooter;
    //
    private TextView mRootTextView;

    //在被认为滚动操作之前的最大触摸距离;
    private int mTouchSlop;

    //
    private SharedPreferences mSharedPreferences;
    //
    private ListView mListView;
    //判断是或否已经加载
    private boolean hasLoaded;
    //按下纵方向位置
    private float mYDown;
    //
    private int mRatio=-10;
    //
    private MarginLayoutParams mHeaderMarginLayoutParams;
    private MarginLayoutParams mRooterMarginLayoutParams;
    //刷新监听器;
    private RefreshingListener mRefreshingListener;
    //
    private GetMoreListener mGetMoreListener;

    //
    final static private String LAST_UPDATE_TIME="last_update_time";

    //
    private boolean isGetMore=false;

    private Context mContext;
    //
    private boolean canRefresh;
    private boolean canGetMore;

    public RefreshLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //初始化值;
        mContext=context;
        mHeader=(LinearLayout) LayoutInflater.from(context).inflate(R.layout.refresh_head,null);
        mProgressBar=(ProgressBar)mHeader.findViewById(R.id.progressbar);
        mImageView=(ImageView)mHeader.findViewById(R.id.arrow);
        mDescriptionTextView=(TextView)mHeader.findViewById(R.id.description);
        mUpdateTimeTextView=(TextView)mHeader.findViewById(R.id.update_time);
        mTouchSlop= ViewConfiguration.get(context).getScaledTouchSlop();
        mSharedPreferences= PreferenceManager.getDefaultSharedPreferences(context);
        hasLoaded=false;
        //是指为垂直布局;
        setOrientation(VERTICAL);
        //添加header
        addView(mHeader,0);

    }

    /**
     * LinearLayout 回调函数
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed,int l,int t, int r, int b){
        super.onLayout(changed,l,t,r,b);
        //避免重复加载;
        if(changed&&!hasLoaded){
            //设置header隐藏;
            mHeaderMarginLayoutParams=(MarginLayoutParams)mHeader.getLayoutParams();
            mHeaderMarginLayoutParams.topMargin=-mHeader.getHeight();
            mHeader.setLayoutParams(mHeaderMarginLayoutParams);

            mListView = (ListView) this.getChildAt(1);

            if(mListView!=null)
               mListView.setOnTouchListener(this);
            hasLoaded=true;
        }
    }

    private void setHeaderDescription(){
        String description="";
        if(mCurrentState==PULL_TO_REFRESH){
            description=getResources().getString(R.string.pull_to_refresh);
        }else if(mCurrentState==RELEASE_TO_REFRESH){
            description=getResources().getString(R.string.release_to_refresh);
        }else{
            description=getResources().getString(R.string.refreshing);
        }
        mDescriptionTextView.setText(description);
    }

    private void setHeaderUpdateTime(){
        long currentTime= System.currentTimeMillis();
        long lastTime=mSharedPreferences.getLong(LAST_UPDATE_TIME,-1);
        String description="";
        Long time=(currentTime-lastTime)/1000;
        if(lastTime==-1){
            description=getResources().getString(R.string.not_updated_yet);
        }else if(time<60){
            description=getResources().getString(R.string.updated_just_now);
        }else if(time<60*60){
            description=getResources().getString(R.string.updated_at,time/60+"分钟");
        }else if(time<60*60*24){
            description=getResources().getString(R.string.updated_at,time/60/60+"小时");
        }else if(time<60*60*24*30){
            description=getResources().getString(R.string.updated_at,time/60/60/24+"天");
        }else if(time<60*60*24*30*12){
            description=getResources().getString(R.string.updated_at,time/60/60/24/30+"天");
        }else{
            description=getResources().getString(R.string.updated_at,time/60/60/24/30/12+"年");
        }
        mUpdateTimeTextView.setText(description);
    }

    //
    private void setAnimation(){
        float centerX=mImageView.getWidth()/2;
        float centerY=mImageView.getHeight()/2;
        float fromDegree=0;
        float toDegree=0;
        if(mCurrentState!=mLastState) {
            if (mCurrentState == PULL_TO_REFRESH) {
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                fromDegree = 180;
                toDegree = 360;
                RotateAnimation animation = new RotateAnimation(fromDegree, toDegree, centerX, centerY);
                animation.setDuration(100);
                animation.setFillAfter(true);
                mImageView.startAnimation(animation);
            } else if (mCurrentState == RELEASE_TO_REFRESH) {
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                fromDegree = 0;
                toDegree = 180;
                RotateAnimation animation = new RotateAnimation(fromDegree, toDegree, centerX, centerY);
                animation.setDuration(100);
                animation.setFillAfter(true);
                mImageView.startAnimation(animation);
            } else {
                mImageView.clearAnimation();
                mImageView.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    //ListView触摸事件;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //只有刚按下去的时候才进行判断是否能够进行下拉刷新或者上拉加载更多
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            canRefresh=canRefresh();
            canGetMore=canGetMore();
        }
        if(canRefresh||canGetMore)
        {
            switch (event.getAction()) {
                //获取按下的的y坐标;
                case MotionEvent.ACTION_DOWN: {
                    mYDown = event.getRawY();
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    float currentY = event.getRawY();
                    float distance = currentY - mYDown;
                    //此时可以进行下拉，并且是下拉操作;
                    if(distance>=0&&canRefresh) {
                        //距离小于阈值时候不进行上拉操作;
                        if (distance <= mTouchSlop){
                            //小bug：特别注意可能由于distance太小导致退出触摸时间，从而distance无法为0，导致header无法复原.
                            mHeaderMarginLayoutParams.topMargin=(- mHeader.getHeight());
                            mHeader.setLayoutParams(mHeaderMarginLayoutParams);
                            return false;
                        }
                        if(mHeaderMarginLayoutParams.topMargin<-mHeader.getHeight())
                            return false;
                        //控制下拉操作的范围,在下拉范围内进行正常的下拉操作;
                        if (distance <= 600) {
                            //根据用户移动的距离计算下拉距离;
                            mHeaderMarginLayoutParams.topMargin = (int) distance / 2 - mHeader.getHeight();
                            mHeader.setLayoutParams(mHeaderMarginLayoutParams);
                            mLastState = mCurrentState;
                            //根据此时header上边缘的距离判断此时状态;
                            if (mHeaderMarginLayoutParams.topMargin < 0) {
                                mCurrentState = PULL_TO_REFRESH;
                            } else {
                                mCurrentState = RELEASE_TO_REFRESH;
                            }
                            //根据状态调整动画
                            setAnimation();
                            //根据状态设置描述信息
                            setHeaderDescription();
                        }
                    }else if(distance<0&&canGetMore){
                            //上拉
                            distance=mYDown-currentY;
                            if(distance<mTouchSlop){
                                mHeaderMarginLayoutParams.topMargin=(- mHeader.getHeight());
                                mHeader.setLayoutParams(mHeaderMarginLayoutParams);
                                return false;
                            }
                            mCurrentState=PUSH_GET_MORE;
                        }
                        //除了够进行下拉操作并且下拉刷新进行处理，和能够进行上拉操作并进行上拉操作进行处理之外，其余操作屏蔽
                    else{
                        return false;
                      }
                    }
                    break;
                case MotionEvent.ACTION_UP: {
                    if (mCurrentState == PULL_TO_REFRESH) {
                        //如果释放时是下拉刷新，则不刷新并隐藏header;
                        new HideHeaderTask().execute();
                    } else if (mCurrentState == RELEASE_TO_REFRESH) {
                        //如果释放时是释放刷新操作,则进行刷新操作;
                        mLastState=mCurrentState;
                        mCurrentState=REFRESHING;
                        new RefreshingTask().execute();
                    } else if(mCurrentState==PUSH_GET_MORE){
                        mCurrentState=GETING_MORE;
                        Toast.makeText(mContext,"正在获取更多信息", Toast.LENGTH_SHORT).show();
                        canGetMore=false;
                        new GetMoreTask().execute();
                    }else{
                        return false;
                    }
                    break;
                }
                default:break;
            }
            //设置上次更新时间;
            setHeaderUpdateTime();
            //根据状态调整动画
            setAnimation();
            //根据状态设置描述信息
            setHeaderDescription();
            return true;
        }
       else{
            return false;
        }
    }
    //判断是否可以进行下拉操作;
    private boolean canRefresh(){
        //只有当前一次刷新操作完毕才进行下次下拉操作
        if(mCurrentState==REFRESH_COMPLETED) {
            View firstChild = mListView.getChildAt(0);
            //如果ListView不为空,则只有当其第一项在最顶端且此时顶端距离父组件为0才允许进行下拉操作;
            if (firstChild != null) {
                int firstVisiblePos = mListView.getFirstVisiblePosition();
                if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
                    return true;
                } else {
                    return false;
                }
            }
            //如果ListView为空时不允许下拉操作;
            else {
                return true;
            }
        }else {
            return false;
        }
    }

    //首先判断可见的最下面的View是否是最后一个，如果是则说明已经滑到最后一项了，再判断此时最后一个View视图的底部和父控件的顶部边缘的距离，如果恰好是屏幕大小则说明无法再继续滑到.
    private boolean canGetMore(){
        //首先判断最下面的选项时候是ListView的最后一个选项(但ListView为空);
        if(mListView.getLastVisiblePosition()==(mListView.getCount()-1)){
            View bottomView=mListView.getChildAt(mListView.getLastVisiblePosition()-
                    mListView.getFirstVisiblePosition());
            //判断ListView是否为空;
            if(bottomView!=null)
            {
                //如果是最后一个选项，再判断是否完全滑到底端;
                return (mListView.getHeight()==bottomView.getBottom());
            }else{
                //列表选项无法进行上拉获取更多;
                return false;
            }
        }else{
            return false;
        }
    }

    private class GetMoreTask extends AsyncTask<Void,Integer,Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            //
            if(mGetMoreListener!=null) {
                mGetMoreListener.onGetMore();
            }
            return null;
        }
        @Override
        protected  void onPostExecute(Integer res){
            //mListView.refreshDrawableState();
            Toast.makeText(mContext,"获取完毕", Toast.LENGTH_SHORT).show();
            mCurrentState=REFRESH_COMPLETED;
        }
    }
    private class HideHeaderTask extends AsyncTask<Void,Integer,Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            int marginY=mHeaderMarginLayoutParams.topMargin;
            while(true){
                marginY+=mRatio;
                if(marginY<=(-mHeader.getHeight())){
                    marginY=-mHeader.getHeight();
                    publishProgress(marginY);
                    break;
                }
                publishProgress(marginY);
                try{
                    Thread.sleep(20);
                }catch (InterruptedException ie){
                    ie.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... topMargin){
            mHeaderMarginLayoutParams.topMargin=topMargin[0];
            mHeader.setLayoutParams(mHeaderMarginLayoutParams);
        }
        @Override
        protected void onPostExecute(Integer res){
            refreshCompleted();
        }
    }

    private class RefreshingTask extends AsyncTask<Void,Integer,Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            if(true){
                int marginY=mHeaderMarginLayoutParams.topMargin;
                while(true){
                    marginY+=mRatio;
                    if(marginY<=0){
                        marginY=0;
                        publishProgress(marginY);
                        break;
                    }
                    publishProgress(marginY);
                    try{
                        Thread.sleep(20);
                    }catch (InterruptedException ie){
                        ie.printStackTrace();
                    }
                }
                if(mRefreshingListener!=null)
                    mRefreshingListener.onRefresh();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... topMargin){
            mHeaderMarginLayoutParams.topMargin=topMargin[0];
            mHeader.setLayoutParams(mHeaderMarginLayoutParams);
        }

        @Override
        protected void onPostExecute(Integer res){
            mSharedPreferences.edit().putLong(LAST_UPDATE_TIME, System.currentTimeMillis()).commit();
            refreshCompleted();
            setAnimation();
        }
    }

    //刷新完毕;
    private void refreshCompleted(){
        mCurrentState=REFRESH_COMPLETED;
        mHeaderMarginLayoutParams.topMargin=-mHeader.getHeight();
        mHeader.setLayoutParams(mHeaderMarginLayoutParams);
    }

    public void setOnRefreshingListener(RefreshingListener refreshingListener){
        mRefreshingListener=refreshingListener;
    }
    public interface RefreshingListener{
        void onRefresh();
    }

   public  interface GetMoreListener{
        void onGetMore();
    }

    public void setOnGetMoreListener(GetMoreListener getMoreListener){
        mGetMoreListener=getMoreListener;
    }

}
