package com.lt.hm.wovideo.base;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.util.Util;
import com.google.gson.Gson;
import com.lt.hm.wovideo.R;
import com.lt.hm.wovideo.handler.UnLoginHandler;
import com.lt.hm.wovideo.http.HttpApis;
import com.lt.hm.wovideo.http.HttpCallback;
import com.lt.hm.wovideo.http.NetUtils;
import com.lt.hm.wovideo.http.ResponseCode;
import com.lt.hm.wovideo.model.NetUsage;
import com.lt.hm.wovideo.model.UserModel;
import com.lt.hm.wovideo.model.response.ResponseBullet;
import com.lt.hm.wovideo.utils.ScreenUtils;
import com.lt.hm.wovideo.utils.SharedPrefsUtils;
import com.lt.hm.wovideo.utils.TLog;
import com.lt.hm.wovideo.utils.UT;
import com.lt.hm.wovideo.utils.UserMgr;
import com.lt.hm.wovideo.video.model.Bullet;
import com.lt.hm.wovideo.video.model.VideoModel;
import com.lt.hm.wovideo.video.player.AVController;
import com.lt.hm.wovideo.video.player.AVPlayer;
import com.lt.hm.wovideo.video.player.BulletSendDialog;
import com.lt.hm.wovideo.video.player.EventLogger;
import com.lt.hm.wovideo.video.player.HlsRendererBuilder;
import com.lt.hm.wovideo.video.player.WoDanmakuParser;
import com.lt.hm.wovideo.video.sensor.ScreenSwitchUtils;
import com.victor.loading.rotate.RotateLoading;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.BaseCacheStuffer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.parser.android.BiliDanmukuParser;
import master.flame.danmaku.danmaku.util.IOUtils;

import static com.lt.hm.wovideo.video.NewVideoPage.CONTENT_ID_EXTRA;
import static com.lt.hm.wovideo.video.NewVideoPage.CONTENT_TYPE_EXTRA;
import static com.lt.hm.wovideo.video.NewVideoPage.PROVIDER_EXTRA;

/**
 * As a base class for all activity which needs to play a video.
 * Contains all the video control and bullet screen control methods in base activity for easy to maintain.
 * FIXME: Known issue, current version is messed up by the screen orientation logical, need to clean it.
 *
 * @author KECB
 *         Created by KECB on 7/19/16.
 * @version 1.0
 */

public class BaseVideoActivity extends BaseActivity implements SurfaceHolder.Callback, AVPlayer.Listener, AVPlayer.CaptionListener, AVPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener, AVController.OnInterfaceInteract {

    // Video thing
    // For use when launching the demo app using adb.
    private static final String CONTENT_EXT_EXTRA = "type";
    private static final int MENU_GROUP_TRACKS = 1;
    private static final int ID_OFFSET = 2;
    private static final CookieManager defaultCookieManager;
    private ScreenSwitchUtils instance;

    static {
        defaultCookieManager = new CookieManager();
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private String mVideoId;

    private EventLogger mEventLogger;
    protected AVController mMediaController;
    private AspectRatioFrameLayout mVideoFrame;
    private View mShutterView;
    private RotateLoading mRotateLoading;
    private SurfaceView mSurfaceView;

    private AVPlayer mPlayer;
    private boolean mPlayerNeedsPrepare;
    protected long mPlayerPosition;
    private boolean mEnableBackgroundAduio = false;

    private Uri mContentUri;
    private int mContentType;
    private String mContentId;
    private String mProvider;

    private AudioCapabilitiesReceiver mAudioCapabilitiesReceiver;
    private WoDanmakuParser mParser;
    private IDanmakuView mDanmakuView;
    private DanmakuContext mContext;

    private long mLoadedBytes;
    private int statueHight;//5.0以上的状态栏高度

    /**
     * Makes a best guess to infer the type from a media {@link Uri} and an optional overriding file
     * extension.
     *
     * @param uri           The {@link Uri} of the media.
     * @param fileExtension An overriding file extension.
     * @return The inferred type.
     */
    private static int inferContentType(Uri uri, String fileExtension) {
        String lastPathSegment = !TextUtils.isEmpty(fileExtension) ? "." + fileExtension
                : uri.getLastPathSegment();
        return Util.inferContentType(lastPathSegment);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mMediaController.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private BaseCacheStuffer.Proxy mCacheStufferAdapter = new BaseCacheStuffer.Proxy() {

        private Drawable mDrawable;

        @Override
        public void prepareDrawing(final BaseDanmaku danmaku, boolean fromWorkerThread) {
            if (danmaku.text instanceof Spanned) { // 根据你的条件检查是否需要需要更新弹幕
                // FIXME 这里只是简单启个线程来加载远程url图片，请使用你自己的异步线程池，最好加上你的缓存池
                new Thread() {

                    @Override
                    public void run() {
                        String url = "http://www.bilibili.com/favicon.ico";
                        InputStream inputStream = null;
                        Drawable drawable = mDrawable;
                        if (drawable == null) {
                            try {
                                URLConnection urlConnection = new URL(url).openConnection();
                                inputStream = urlConnection.getInputStream();
                                drawable = BitmapDrawable.createFromStream(inputStream, "bitmap");
                                mDrawable = drawable;
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                IOUtils.closeQuietly(inputStream);
                            }
                        }
                        if (drawable != null) {
                            drawable.setBounds(0, 0, 100, 100);
                            SpannableStringBuilder spannable = createSpannable(drawable);
                            danmaku.text = spannable;
                            if (mDanmakuView != null) {
                                mDanmakuView.invalidateDanmaku(danmaku, false);
                            }
                            return;
                        }
                    }
                }.start();
            }
        }

        @Override
        public void releaseResource(BaseDanmaku danmaku) {
            // TODO 重要:清理含有ImageSpan的text中的一些占用内存的资源 例如drawable
        }
    };

    private SpannableStringBuilder createSpannable(Drawable drawable) {
        String text = "bitmap";
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
        ImageSpan span = new ImageSpan(drawable);//ImageSpan.ALIGN_BOTTOM);
        spannableStringBuilder.setSpan(span, 0, text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableStringBuilder.append("图文混排");
        spannableStringBuilder.setSpan(new BackgroundColorSpan(Color.parseColor("#8A2233B1")), 0, spannableStringBuilder.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableStringBuilder;
    }

    // Activity lifecycle

    private int width, height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View root = findViewById(R.id.video_root);
        instance = ScreenSwitchUtils.init(this.getApplicationContext());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            statueHight = ScreenUtils.getStatusHeight(getApplicationContext());

            WindowManager manager = this.getWindowManager();
            DisplayMetrics outMetrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(outMetrics);
            width = outMetrics.widthPixels;
            height = outMetrics.heightPixels;

            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            setSystemUiVisibility();
        }

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //toggleControlsVisibility();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return false;
            }
        });
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK
                        || keyCode == KeyEvent.KEYCODE_ESCAPE
                        || keyCode == KeyEvent.KEYCODE_MENU) {
                    return false;
                }
                return mMediaController.dispatchKeyEvent(event);
            }
        });

        mRotateLoading = (RotateLoading) findViewById(R.id.loading);
        mShutterView = findViewById(R.id.shutter);
        mDanmakuView = (IDanmakuView) findViewById(R.id.sv_danmaku);

        mVideoFrame = (AspectRatioFrameLayout) findViewById(R.id.video_frame);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(this);

//        mMediaController = new KeyCompatibleMediaController(this);
        mMediaController = new KeyCompatibleMediaController(this, mDanmakuView);

        mMediaController.setAnchorView((FrameLayout) findViewById(R.id.video_frame));
        mMediaController.setGestureListener(this);

        CookieHandler currentHanlder = CookieHandler.getDefault();
        if (currentHanlder != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager);
        }

        mAudioCapabilitiesReceiver = new AudioCapabilitiesReceiver(this, this);
        mAudioCapabilitiesReceiver.register();


        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5); // 滚动弹幕最大显示5行
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        mContext = DanmakuContext.create();
        mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3).setDuplicateMergingEnabled(false).setScrollSpeedFactor(1.2f).setScaleTextSize(1.2f)
                .setCacheStuffer(new SpannedCacheStuffer(), mCacheStufferAdapter) // 图文混排使用SpannedCacheStuffer
//        .setCacheStuffer(new BackgroundCacheStuffer())  // 绘制背景使用BackgroundCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);
        if (mDanmakuView != null) {
//            mParser = createParser(this.getResources().openRawResource(R.raw.comments));
            mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
//                    Log.d("DFM", "danmakuShown(): text=" + danmaku.text);
                }

                @Override
                public void prepared() {
                    mDanmakuView.start();
                }
            });
//            mDanmakuView.setOnDanmakuClickListener(new IDanmakuView.OnDanmakuClickListener() {
//                @Override
//                public void onDanmakuClick(BaseDanmaku latest) {
//                    Log.d("DFM", "onDanmakuClick text:" + latest.text);
//                }
//
//                @Override
//                public void onDanmakuClick(IDanmakus danmakus) {
//                    Log.d("DFM", "onDanmakuClick danmakus size:" + danmakus.size());
//                }
//            });
//            mDanmakuView.prepare(mParser, mContext);
//            mDanmakuView.showFPS(false);
//            mDanmakuView.enableDanmakuDrawingCache(true);
            mDanmakuView.hide();
//            ((View) mDanmakuView).setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View view) {
//                    mMediaController.setVisibility(View.VISIBLE);
        }
//            });
        ((View) mDanmakuView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //toggleControlsVisibility();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return false;
            }
        });

    }


    //TODO using real data
    private BaseDanmakuParser createParser(InputStream stream) {
        if (stream == null) {
            return new BaseDanmakuParser() {

                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }

        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);

        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        BaseDanmakuParser parser = new BiliDanmukuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        return parser;

    }

    protected Intent onUrlGot(VideoModel video) {
        Intent mpdIntent = new Intent(this, BaseVideoActivity.class)
                .setData(Uri.parse(video.getmPlayUrl().getFormatUrl()))
                .putExtra(CONTENT_ID_EXTRA, video.getmVideoName())
                .putExtra(CONTENT_TYPE_EXTRA, Util.TYPE_HLS)
                .putExtra(PROVIDER_EXTRA, "");
        return mpdIntent;
    }

    @Override
    public void onNewIntent(Intent intent) {
        releasePlayer();
        mPlayerPosition = 0;
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            onShown();
        }
        if (instance != null) {
            instance.start(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || mPlayer == null) {
            onShown();
        }
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    protected void onShown() {
        try {
            Intent intent = getIntent();
            mContentUri = intent.getData();
            mContentType = intent.getIntExtra(CONTENT_TYPE_EXTRA,
                    inferContentType(mContentUri, intent.getStringExtra(CONTENT_EXT_EXTRA)));
            mContentId = intent.getStringExtra(CONTENT_ID_EXTRA);
            mProvider = intent.getStringExtra(PROVIDER_EXTRA);
            if (mPlayer == null) {
                if (!maybeRequestPermission()) {
                    preparePlayer(true);
                }
            } else {
                mPlayer.setBackgrounded(false);
            }
            mShutterView.setVisibility(View.INVISIBLE);
        } catch (NullPointerException ex) {
            //TODO deal with this if needed
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer == null) return;
        mLoadedBytes = mPlayer.getLoadedBytes();
        if (Util.SDK_INT <= 23) {
            onHidden();
        }
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            onHidden();
        }
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            Log.d("Bullet", "onStop: danmu prepared and gonna release");
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }
        if (instance != null) {
            instance.stop();
        }

    }

    private void onHidden() {
        if (!mEnableBackgroundAduio) {
            releasePlayer();
        } else {
            mPlayer.setBackgrounded(true);
        }
        mShutterView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NetUsage usage = new NetUsage();
        usage.setUserId("");
        usage.setVideoId(mVideoId);
        usage.setCreateTime(System.currentTimeMillis() + "");
        usage.setBytes(String.valueOf(mLoadedBytes));
        netUsageDatabase.insert(usage);

        mAudioCapabilitiesReceiver.unregister();
        releasePlayer();
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            Log.d("Bullet", "onDestroy: danmu prepared and gonna release");
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        TLog.error("sensor--999>");
        //Check the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mMediaController.hide();
            mMediaController.setAnchorView((FrameLayout) findViewById(R.id.video_root));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            );

            //TODO set title
//            mMediaController.setTitle(videoName.getText().toString());
            mVideoFrame.setLayoutParams(lp);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mVideoFrame.setAspectRatio((float) height / (width + statueHight));
            } else {
                mVideoFrame.requestLayout();
            }

            mMediaController.setBulletScreen(true);


//            //hit status bar
//            getWindow().getDecorView().setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            //      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                            //       | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            //       | View.SYSTEM_UI_FLAG_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            //show danmu
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //show status bar
            //  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            mMediaController.setBulletScreen(false);
            mMediaController.hide();
            mMediaController.setAnchorView((FrameLayout) findViewById(R.id.video_frame));
            // when in portrait screen, turn off bullet screen.
            if (mDanmakuView != null) {
                mDanmakuView.hide();
            }
        }
        setSystemUiVisibility();
    }

    private void setSystemUiVisibility() {
        //hit status bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        //      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //       | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        //       | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }


    // Surface Life cycle
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing...
    }

    // Internal methods

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.blockingClearSurface();
        }
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {

    }

    @Override
    public void onCues(List<Cue> cues) {

    }

    @Override
    public void onId3Metadata(List<Id3Frame> id3Frames) {

    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == AVPlayer.STATE_READY || playbackState == AVPlayer.STATE_ENDED) {
            mRotateLoading.stop();
            //TODO play next if exist.
            mDanmakuView.show();
            mDanmakuView.seekTo(mPlayer.getCurrentPosition());
        } else {
            mRotateLoading.start();
            if (mDanmakuView == null) return;
            mDanmakuView.hide();
        }
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        mShutterView.setVisibility(View.GONE);
        mVideoFrame.setAspectRatio(
                height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
    }

    private AVPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(this, "AVPlayer");
        switch (mContentType) {
            case Util.TYPE_HLS:
                return new HlsRendererBuilder(this, userAgent, mContentUri.toString());
            default:
                throw new IllegalStateException("Unsupported type:" + mContentType);
        }
    }

    private void preparePlayer(boolean playWhenReady) {
        if (mPlayer == null) {
            mPlayer = new AVPlayer(getRendererBuilder(), this);
            mPlayer.addListener(this);
            mPlayer.setCaptionListener(this);
            mPlayer.setMetadataListener(this);
            mPlayer.seekTo(mPlayerPosition);
            mPlayerNeedsPrepare = true;
            mMediaController.setMediaPlayer(mPlayer.getPlayerControl());
            mMediaController.setEnabled(true);
            mMediaController.setScreenSwitchUtils(instance);
            mMediaController.setInterfaceListener(this);
            mEventLogger = new EventLogger();
            mEventLogger.startSession();
            mPlayer.addListener(mEventLogger);
            mPlayer.setInfoListener(mEventLogger);
            mPlayer.setInternalErrorListener(mEventLogger);
        }
        if (mPlayerNeedsPrepare) {
            mPlayer.prepare();
            mPlayerNeedsPrepare = false;
        }
        mPlayer.setSurface(mSurfaceView.getHolder().getSurface());
        mPlayer.setPlayWhenReady(playWhenReady);
    }

    protected void releasePlayer() {
        if (mPlayer != null) {
            mPlayerPosition = mPlayer.getCurrentPosition();
            mPlayer.release();
            mPlayer = null;
            if (mEventLogger == null) return;
            mEventLogger.endSession();
            mEventLogger = null;
        }
    }

    /**
     * 获取弹幕
     */
    protected void getBullets() {
        TLog.log("Bullet", "get bullets" + mVideoId);
        if (mDanmakuView != null) {
            mDanmakuView.release();
        }
        NetUtils.getBullets(mVideoId, this);
    }

    @Override
    public <T> void onSuccess(T value, int flag) {
        super.onSuccess(value, flag);
        switch (flag) {
            case HttpApis.http_bullet:
                ResponseBullet res = (ResponseBullet) value;
                mParser = new WoDanmakuParser();
                TLog.log("getBullet" + mParser);
                mParser.setmDanmuListData(res.getBody());
                if (mContext == null || mParser == null || mDanmakuView == null)
                    return;
                mDanmakuView.prepare(mParser, mContext);
                mDanmakuView.showFPS(false);
                mDanmakuView.enableDanmakuDrawingCache(true);
                if (mMediaController.getBulletScreen()) {
                    mDanmakuView.hide();
                } else {
                    mDanmakuView.show();
                }
                break;
            case HttpApis.http_add_bullet:
                String str = (String) value;
                if (TextUtils.isEmpty(str) || !str.equals(ResponseCode.Success)) return;
                UT.showNormal("弹幕成功");
                addDanmaku(bullet);
                break;
        }
    }

    @Override
    public void onFail(String error, int flag) {
        super.onFail(error, flag);
        switch (flag) {
            case HttpApis.http_add_bullet:
                UT.showNormal(error);
                break;

        }
    }

    private Bullet bullet;

    /**
     * 添加弹幕
     *
     * @param bullet {@link Bullet}
     */
    protected void addBullet(Bullet bullet) {
        if (UserMgr.isLogin()) {
            HashMap<String, Object> map = new HashMap<>();
            UserModel model = UserMgr.getUseInfo();
            //FIXME user id is incorrect!
            map.put("userId", model.getId());
            map.put("vfPlayId", mVideoId);
            map.put("time", mPlayer.getCurrentPosition() / 1000);
//            map.put("time", bullet.getTime());
            map.put("context", bullet.getContent());
            map.put("fontColor", bullet.getFontColor());
            map.put("fontSize", bullet.getFontSize());
            TLog.log("Bullet", "userId: " + model.getId()
                    + "; videoId: " + mVideoId + "; time: " + mPlayer.getCurrentPosition() / 1000
                    + "; content: " + bullet.getContent() + "; fontColor: " + bullet.getFontColor()
                    + "; fontSize: " + bullet.getFontSize());

            HttpApis.addBullet(map, HttpApis.http_add_bullet, new HttpCallback<>(String.class, this));
        } else {
            UnLoginHandler.unLogin(this);
        }
    }

    private void addDanmaku(Bullet bullet) {
        BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL, mContext);

        if (danmaku == null || mDanmakuView == null || mParser == null) {
            return;
        }

        danmaku.text = bullet.getContent();
        danmaku.padding = 5;
        danmaku.isLive = false; //是否是直播弹幕
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.time = mDanmakuView.getCurrentTime() + 1200;
        danmaku.textSize = 22f * (mParser.getDisplayer().getDensity() - 0.6f);
//            danMaKu.textColor = Color.parseColor(bullet.getFontColor());
        danmaku.textShadowColor = Color.WHITE;
//        danmaku.underlineColor = Color.GREEN;
//        danmaku.borderColor = Color.GREEN;
        mDanmakuView.addDanmaku(danmaku);
//            getBullets();

    }

    protected long getCurrentPosition() {
        if (mPlayer != null && mPlayer.getPlaybackState() != AVPlayer.STATE_PREPARING) {
            return mPlayer.getCurrentPosition() / 1000;
        }
        return 0;
    }

    protected void setVideoTitle(String title) {
        mMediaController.setTitle(title);
    }

    protected void setVideoModel(VideoModel model) {
        mMediaController.setVideoModel(model);
    }

    protected void setQualityListener(AVController.OnQualitySelected listener) {
        mMediaController.setListener(listener);
    }

    protected void setQualitySwitchText(String name) {
        mMediaController.setmQualitySwitch(name);
    }

    protected void setVideoId(String videoId) {
        mVideoId = videoId;
    }

    /**
     * Seek to certain position, unit is millisecond.
     *
     * @param positionMs
     */
    protected void seekTo(long positionMs) {
        if (mPlayer != null && mPlayer.getPlaybackState() != AVPlayer.STATE_PREPARING) {
            mPlayer.seekTo(positionMs);
        }
        if (mDanmakuView != null) {
            mDanmakuView.seekTo(positionMs);
        }
    }

    @Override
    protected void init(Bundle savedInstanceState) {

    }

    @Override
    public void initViews() {

    }

    @Override
    public void initDatas() {

    }

    @Override
    public void onOpenBulletEditor() {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment bulletDialog = BulletSendDialog.newInstance(this);
        bulletDialog.show(ft, "dialog");
    }

    @Override
    public void onSendBulletClick(Bullet bullet) {
        if (mMediaController.getBulletScreen()) {
            if (TextUtils.isEmpty(bullet.getContent().trim())) {
                UT.showNormal("弹幕不能为空");
                return;
            }
            if (!TextUtils.isEmpty(bullet.getContent().trim()) && bullet.getContent().length() > 50) {
                UT.showNormal("弹幕仅限50字");
                return;
            }
            this.bullet = bullet;
            addBullet(bullet);
        } else {
            UT.showNormal("请先开启弹幕功能");
        }
    }

    @Override
    public void onBulletSwitchCheck(boolean isChecked) {
        if (isChecked) {
            mMediaController.setBulletScreen(true);
            mDanmakuView.show();
        } else {
            mMediaController.setBulletScreen(false);
            mDanmakuView.hide();
        }
    }

    private static final class KeyCompatibleMediaController extends AVController {

        private MediaPlayerControl playerControl;
        private IDanmakuView mDanmakuView;

        public KeyCompatibleMediaController(Context context, IDanmakuView danmakuView) {
            super(context);
            mDanmakuView = danmakuView;

        }

        @Override
        public void setMediaPlayer(MediaPlayerControl playerControl) {
            super.setMediaPlayer(playerControl);
            this.playerControl = playerControl;
        }

        @Override
        public void toggleBulletScreen(boolean isShow) {
            super.toggleBulletScreen(isShow);
            if (isShow) {
                TLog.log("toggleBullet_status" + isShow);
                if (playerControl == null) mDanmakuView.hide();
                mDanmakuView.seekTo(playerControl.getCurrentPosition());
                mDanmakuView.show();
            } else {
                TLog.log("toggleBullet_status" + isShow);
                mDanmakuView.hide();
            }
        }

        @Override
        public void show() {
            super.show();
        }

        @Override
        public void setTitle(String titleName) {
            super.setTitle(titleName);
        }


        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            int keyCode = event.getKeyCode();
            if (playerControl.canSeekForward() && (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    playerControl.seekTo(playerControl.getCurrentPosition() + 15000); // milliseconds
                    show();
                }
                return true;
            } else if (playerControl.canSeekBackward() && (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    playerControl.seekTo(playerControl.getCurrentPosition() - 5000); // milliseconds
                    show();
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }

    /**
     * Checks whether it is necessary to ask for permission to read storage. If necessary, it also
     * requests permission.
     *
     * @return true if a permission request is made. False if it is not necessary.
     */
    @TargetApi(23)
    private boolean maybeRequestPermission() {
        if (requiresPermission(mContentUri)) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            return true;
        } else {
            return false;
        }
    }

    @TargetApi(23)
    private boolean requiresPermission(Uri uri) {
        return Util.SDK_INT >= 23
                && Util.isLocalFileUri(uri)
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mMediaController != null) {
                mMediaController.doBackClick();
            }
        }
        return false;
    }
}
