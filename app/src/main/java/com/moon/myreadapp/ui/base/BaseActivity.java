package com.moon.myreadapp.ui.base;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.moon.appframework.action.RouterAction;
import com.moon.appframework.common.log.XLog;
import com.moon.appframework.core.XActivity;
import com.moon.appframework.core.XDispatcher;
import com.moon.myreadapp.R;
import com.moon.myreadapp.common.event.UpdateUIEvent;
import com.moon.myreadapp.constants.Constants;
import com.moon.myreadapp.ui.WelcomeActivity;
import com.moon.myreadapp.util.PreferenceUtils;
import com.moon.myreadapp.util.ScreenUtils;
import com.moon.myreadapp.util.ThemeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import de.halfbit.tinybus.Subscribe;


/**
 * Created by moon on 15/12/30.
 *
 *
 * 附带左滑返回的baseActivity.采用了SlidingPaneLayout来实现
 *
 *
 */
public abstract class BaseActivity extends XActivity{




    private final static String TAG = BaseActivity.class.getSimpleName();
    private final static String WINDOWBITMAP = "screenshots.jpg";
    private File mFileTemp;
    private SlidingPaneLayout slidingPaneLayout;
    private ImageView behindImageView;
    private int defaultTranslationX = 100;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        //配置主题
        initTheme();

        super.onCreate(savedInstanceState);

        setContentViewAndBindVm(savedInstanceState);
    }


    private void initLayout(){

        //这个文件需要创建出来,截图使用
        mFileTemp = new File(getCacheDir(), WINDOWBITMAP);

        //如果不是栈底则注入
        if(!isTaskRoot()){
            //通过反射来改变SlidingPanelayout的值
            try {
                slidingPaneLayout = new SlidingPaneLayout(this){

                    /**
                     * 需要处理一下页面内的viewpager
                     */
                    private List<ViewPager> mViewPagers = new LinkedList<ViewPager>();

                    @Override
                    public boolean onInterceptTouchEvent(MotionEvent ev) {
                        //处理ViewPager冲突问题
                        ViewPager mViewPager = getTouchViewPager(mViewPagers, ev);
                        // Log.i(TAG, "mViewPager = " + mViewPager);

                        if(mViewPager != null && mViewPager.getCurrentItem() != 0){
                            //如果有viewpager,而且不是第一页,就给viewpager代理.
                            //TODO 更好的逻辑应该是判断一下滑动速度.快滑 返回;慢滑 切换viewpager
                            return false;
                        }
                        return super.onInterceptTouchEvent(ev);
                    }
                    /**
                     * 返回我们touch的ViewPager
                     * @param mViewPagers
                     * @param ev
                     * @return
                     */
                    private ViewPager getTouchViewPager(List<ViewPager> mViewPagers, MotionEvent ev){
                        if(mViewPagers == null || mViewPagers.size() == 0){
                            return null;
                        }
                        Rect mRect = new Rect();
                        for(ViewPager v : mViewPagers){
                            v.getHitRect(mRect);

                            if(mRect.contains((int)ev.getX(), (int)ev.getY())){
                                return v;
                            }
                        }
                        return null;
                    }
                    @Override
                    protected void onLayout(boolean changed, int l, int t, int r, int b) {
                        super.onLayout(changed, l, t, r, b);
                        if (changed) {
                            getAlLViewPager(mViewPagers, this);
                            //Log.i(TAG, "ViewPager size = " + mViewPagers.size());
                        }
                    }
                    /**
                     * 获取SwipeBackLayout里面的ViewPager的集合
                     * @param mViewPagers
                     * @param parent
                     */
                    private void getAlLViewPager(List<ViewPager> mViewPagers, ViewGroup parent){
                        int childCount = parent.getChildCount();
                        for(int i=0; i<childCount; i++){
                            View child = parent.getChildAt(i);
                            if(child instanceof ViewPager){
                                mViewPagers.add((ViewPager)child);
                            }else if(child instanceof ViewGroup){
                                getAlLViewPager(mViewPagers, (ViewGroup)child);
                            }
                        }
                    }

                };


                //改变 mOverhangSize
                Field f_overHang = SlidingPaneLayout.class.getDeclaredField("mOverhangSize");
                f_overHang.setAccessible(true);
                f_overHang.set(slidingPaneLayout, 0);
                slidingPaneLayout.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {
                    @Override
                    public void onPanelClosed(View view) {

                    }

                    @Override
                    public void onPanelOpened(View view) {
                        finish();
                        overridePendingTransition(0, 0);
                    }

                    @Override
                    public void onPanelSlide(View view, float v) {
                        Log.e(TAG, "onPanelSlide ：" + v);
                        //duang duang duang 你可以在这里加入很多特效
                        behindImageView.setTranslationX(v * defaultTranslationX - defaultTranslationX);

                    }
                });
               slidingPaneLayout.setSliderFadeColor(getResources().getColor(android.R.color.transparent));
            } catch (Exception e) {
                e.printStackTrace();
            }

            defaultTranslationX = ScreenUtils.dpToPx(defaultTranslationX);

            //behindframeLayout
            FrameLayout behindframeLayout = new FrameLayout(this);
            behindImageView = new ImageView(this);
            behindImageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            behindframeLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            behindframeLayout.addView(behindImageView, 0);

            //containerLayout
//            LinearLayout containerLayout = new LinearLayout(this);
//            containerLayout.setOrientation(LinearLayout.HORIZONTAL);
//            containerLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
//            containerLayout.setLayoutParams(new ViewGroup.LayoutParams(getWindowManager().getDefaultDisplay().getWidth() + shadowWidth, ViewGroup.LayoutParams.MATCH_PARENT));
//            //you view container
//            frameLayout = new FrameLayout(this);
//            frameLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
            //frameLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            //add shadow
//            shadowImageView = new ImageView(this);
//            shadowImageView.setBackgroundResource(R.drawable.shadow_left);
//            shadowImageView.setLayoutParams(new LinearLayout.LayoutParams(shadowWidth, LinearLayout.LayoutParams.MATCH_PARENT));
//            containerLayout.addView(shadowImageView);
            //containerLayout.addView(frameLayout);
            //containerLayout.setTranslationX(-shadowWidth);
            //添加两个view
            slidingPaneLayout.addView(behindframeLayout, 0);
            //slidingPaneLayout.addView(containerLayout, 1);


            //替换decorview
            ViewGroup decor = (ViewGroup) getWindow().getDecorView();
            ViewGroup decorChild = (ViewGroup) decor.getChildAt(0);
            decorChild.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            decor.removeView(decorChild);
//            frameLayout.removeAllViews();
//            frameLayout.addView(decorChild);

            slidingPaneLayout.addView(decorChild, 1);
            decor.addView(slidingPaneLayout);



            //设置图片 这里设置原图大小比较好.像如果出现输入法,可能会改变容器的大小
            behindImageView.setScaleType(ImageView.ScaleType.MATRIX);
            behindImageView.setImageBitmap(getBitmap());
        }
    }

    protected abstract void setContentViewAndBindVm(Bundle savedInstanceState);

    @Override
    public void startActivity(Intent intent, Bundle options) {
        screenshots(false);
        super.startActivity(intent, options);
        XLog.d("startActivity");
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        injectView(this);
        initLayout();
        //event bus init
        XDispatcher.register(this);
        checkEvent();
    }

    private void initTheme() {
        ThemeUtils.Theme theme = ThemeUtils.getCurrentTheme(this);
        ThemeUtils.changTheme(this, theme);
    }

    /**
     * 该方法提供统一设置toolbar的方式;  可以继承 ToolBarExpandActivity,可以直接再顶部视图中设置一个toolbar
     *
     * @param toolbar
     */
    protected void initToolBar(Toolbar toolbar) {
        if (toolbar == null) return;
        setSupportActionBar(toolbar);
        toolbar.collapseActionView();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setContentInsetsRelative(0, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XDispatcher.unregister(this);
    }


    protected abstract Toolbar getToolBar();

    protected abstract
    @LayoutRes
    int getLayoutView();

    @Subscribe
    public void onUpdateEvent(UpdateUIEvent event) {

        if (event.getStatus() == UpdateUIEvent.THEME_CHANGE) {
            recreate();
        }
    }

    protected int getScreenHeight() {
        return findViewById(android.R.id.content).getHeight();
    }
    protected int getScreenWidth() {
        return findViewById(android.R.id.content).getWidth();
    }

    protected void checkEvent(){
        boolean isFirstUse = PreferenceUtils.getInstance(this).getBooleanParam(Constants.APP_IS_FIRST_USE,true);
        //第一次进入
        //isFirstUse = true;
        if (isFirstUse){
            XLog.d("checkEvent: first");
            XDispatcher.from(this).dispatch(new RouterAction(WelcomeActivity.class, true));
            PreferenceUtils.getInstance(this).saveParam(Constants.APP_IS_FIRST_USE, false);
        }else {
            XLog.d("checkEvent: not first");
        }
    }


    /**
     * 取得视觉差背景图
     *
     * @return
     */
    public Bitmap getBitmap() {
        return BitmapFactory.decodeFile(mFileTemp.getAbsolutePath());
    }



    /**
     * 对当前界面进行截图保存,这个动作应该在start另外一个act之前做好
     * @param isFullScreen
     */
    public void screenshots(boolean isFullScreen) {
        try {
            //View是你需要截图的View
            View decorView = getWindow().getDecorView();
            decorView.setDrawingCacheEnabled(true);
            decorView.buildDrawingCache();
            Bitmap b1 = decorView.getDrawingCache();
            // 获取状态栏高度 /
            Rect frame = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
            int statusBarHeight = frame.top;
            //Log.e(TAG, "statusBarHeight:" + statusBarHeight);
            // 获取屏幕长和高 Get screen width and height
            int width = getWindowManager().getDefaultDisplay().getWidth();
            int height = getWindowManager().getDefaultDisplay().getHeight();
            // 去掉标题栏 Remove the statusBar Height
            Bitmap bitmap;
            if (isFullScreen) {
                bitmap = Bitmap.createBitmap(b1, 0, 0, width, height);
            } else {
                bitmap = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height - statusBarHeight);
            }
            decorView.destroyDrawingCache();
            FileOutputStream out = new FileOutputStream(mFileTemp);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//            Bitmap big = getBitmap();
//            if (big == null){
//                Log.e(TAG, "statusBarHeight:big null" );
//            } else {
//                Log.e(TAG, "statusBarHeight:big " + big.toString());
//            }
        } catch (Exception e) {
            Log.e(TAG, "statusBarHeight:" + e.toString());

        }
    }
}
