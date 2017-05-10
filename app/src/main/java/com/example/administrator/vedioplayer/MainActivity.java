package com.example.administrator.vedioplayer;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;



import java.util.logging.LogRecord;


public class MainActivity extends Activity {

    private VideoView videoView;
    private LinearLayout controller_layout;
    private ImageView play_controller_img,screen_img,volume_img,exit;
    private TextView time_current_tv,time_total_tv;
    private SeekBar volume_seek,play_seek;
    public static final int UPDATE_UI = 1;
    private int screen_width,screen_height;
    private RelativeLayout videoLayout;
    private AudioManager audioManager;
    private boolean isFullScreen = false;
    private boolean isAdjust = false;
    private int threshold = 54;
    private float mBrightness;
    float lastX,lastY;
    private ImageView operation_bg,operation_percent;
    private FrameLayout progress_layout;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        initUI();
        setPlayerEvent();

        //本地视频播放
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/22.mp4";
        Log.e("Main","path: "+path);
        videoView.setVideoPath(path);

        //网络视频播放
//        videoView.setVideoURI(Uri.parse("http://www.tangoh.cn/22.mp4"));

        UIHandler.sendEmptyMessage(UPDATE_UI);
        videoView.start();

    }

    /**
     * 播放时间格式化
     * @param textView
     * @param millisecond
     */
    private void updateTextViewWithTimeFormat(TextView textView,int millisecond) {
        int second = millisecond / 1000;
        int hh = second / 3600;
        int mm = second % 3600 / 60;
        int ss = second % 60;
        String str = null;
        if (hh != 0) {
            str = String.format("%02d:%02d:%02d", hh, mm, ss);

        } else {
            str = String.format("%02d:%02d", mm, ss);
        }
        textView.setText(str);
    }


    /**
     * 进度刷新
     */
    private Handler UIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI) {
                //获取视频当前的播放时间
                int currentPosition = videoView.getCurrentPosition();
                //获取视频播放总时间
                int totalduration = videoView.getDuration();
                //格式化视频播放时间
                updateTextViewWithTimeFormat(time_current_tv, currentPosition);
                updateTextViewWithTimeFormat(time_total_tv, totalduration);

                play_seek.setMax(totalduration);
                play_seek.setProgress(currentPosition);
                UIHandler.sendEmptyMessageDelayed(UPDATE_UI, 500);
            }
        }
    };


    /**
     * 暂停播放时停止刷新
     */
    @Override
    protected void onPause() {
        super.onPause();
        UIHandler.removeMessages(UPDATE_UI);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void setPlayerEvent() {

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        /**
         * 暂停开始按钮控制
         */
        play_controller_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(videoView.isPlaying()){
                    play_controller_img.setImageResource(R.drawable.start_video_df);
                    //暂停播放
                    videoView.pause();
                    UIHandler.removeMessages(UPDATE_UI);
                }
                else
                {
                    play_controller_img.setImageResource(R.drawable.pause_video_df);
                   //继续播放
                    videoView.start();
                    UIHandler.sendEmptyMessage(UPDATE_UI);
                }
            }
        });

        /**
         * 进度条控制播放进度
         */
        play_seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            /**
             * 时间与进度条同步
             */
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTextViewWithTimeFormat(time_current_tv,progress);
            }

            @Override
            /**
             * 停止刷新
             */
            public void onStartTrackingTouch(SeekBar seekBar) {
                UIHandler.removeMessages(UPDATE_UI);
            }

            @Override
            /**
             * 跳转进度，继续刷新
             */
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                //令视频播放进度遵循seekbar停止拖动的这一刻的进度
                videoView.seekTo(progress);
                UIHandler.sendEmptyMessage(UPDATE_UI);
            }
        });

        /**
         * 音量控制进度条
         */
        volume_seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        /**
         * 横竖屏控制按钮
         */
        screen_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFullScreen){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                else{
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        });

        /**
         * 控制VideoView的手势事件
         */
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();

                switch (event.getAction()){
                    /**
                     * 手指落下屏幕的那一刻（只会调用一次）
                     */
                    case MotionEvent.ACTION_DOWN: {
                        controller_layout.setVisibility(View.VISIBLE);
                        exit.setVisibility(View.VISIBLE);
                        lastX = x;
                        lastY = y;
                        Log.e("Main", "x" + lastX);
                        Log.e("Main", "y" + lastY);
                        break;
                    }

                    /**
                     * 手指在屏幕上移动（调用多次）
                     */
                    case MotionEvent.ACTION_MOVE: {
                        float detlaX = x - lastX;
                        float detlaY = y - lastY;
                        Log.e("Main", "detlaX " + detlaX);
                        Log.e("Main", "detlaY " + detlaY);
                        float absdetlaX = Math.abs(detlaX);

                        float absdetlaY = Math.abs(detlaY);
                        if (absdetlaX > threshold && absdetlaY > threshold) {
                            if (absdetlaX < absdetlaY) {
                                isAdjust = true;
                            } else {
                                isAdjust = false;
                            }
                        } else if (absdetlaX < threshold && absdetlaY > threshold) {
                            isAdjust = true;
                        } else if (absdetlaX > threshold && absdetlaY < threshold) {
                            isAdjust = false;
                        }

                        Log.e("Main", "手势是否合法" + isAdjust);
                        if (isAdjust) {
                            /**
                             * 在判断好当前手势事件已经合法的前提下，去区分此时手势应该调节亮度还是调节声音
                             */
                            if (x < screen_width / 2) {
                                if (detlaY > 0) {
                                    Log.e("Main", "降低亮度" + detlaY);
                                } else {
                                    Log.e("Main", "升高亮度" + detlaY);
                                }

                                changeBrightness(-detlaY);
                            } else {
                                /**
                                 * 调节声音
                                 */
                                if (detlaY > 0) {
                                    Log.e("Main", "减小声音" + detlaY);
                                } else {
                                    Log.e("Main", "增大声音" + detlaY);
                                }
                                Log.e("Main", "detlaY " + detlaY);
                                changeVolume(-detlaY);
                            }
                        }
                        lastX = x;
                        lastY = y;
                        Log.e("Main", "lastx " + lastX);
                        Log.e("Main", "lasty " + lastY);
                        break;
                    }

                    /**
                     * 手指离开屏幕的那一刻(只调用一次)
                     */
                    case MotionEvent.ACTION_UP: {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                controller_layout.setVisibility(View.GONE);
                                exit.setVisibility(View.GONE);
                            }
                        },6000);

                        progress_layout.setVisibility(View.GONE);
                        break;
                    }
                }
                return true;
            }
        });
    }

    private void changeVolume(float detlaY){
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int index = (int)(detlaY/screen_height*max*3);
        int volume = Math.max(current+index,0);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);
        if(progress_layout.getVisibility()==View.GONE){
            progress_layout.setVisibility(View.VISIBLE);
        }
        operation_bg.setImageResource(R.mipmap.video_voice_bg);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width = (int)(DensityUtils.dp2px(this,94)*(float)volume/max);
        operation_percent.setLayoutParams(layoutParams);
        volume_seek.setProgress(volume);
    }

    private void changeBrightness(float detlaY){
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        mBrightness = attributes.screenBrightness;
        float index = detlaY/screen_height;
        mBrightness += index;
        if(mBrightness > 1.0f){
            mBrightness = 1.0f;
        }
        if(mBrightness < 0.01f){
            mBrightness = 0.01f;
        }
        if(progress_layout.getVisibility()==View.GONE){
            progress_layout.setVisibility(View.VISIBLE);
        }
        operation_bg.setImageResource(R.mipmap.video_brightness_bg);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width = (int)(DensityUtils.dp2px(this,94)*mBrightness);
        operation_percent.setLayoutParams(layoutParams);
        attributes.screenBrightness = mBrightness;
        getWindow().setAttributes(attributes);
    }



    /**
     * 监听到屏幕方向的改变
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /**
         * 横屏
         */
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            volume_img.setVisibility(View.VISIBLE);
            volume_seek.setVisibility(View.VISIBLE);
            isFullScreen = true;
            getWindow().clearFlags((WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        }
        /**
         * 竖屏
         */
        else{
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,DensityUtils.dp2px(this,screen_height));
            volume_img.setVisibility(View.GONE);
            volume_seek.setVisibility(View.GONE);
            isFullScreen = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }

    }

    private void setVideoViewScale(int width,int height){
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        videoView.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams layoutParams1 = videoLayout.getLayoutParams();
        layoutParams1.width = width;
        layoutParams1.height = height;
        videoLayout.setLayoutParams(layoutParams1);
    }

    /**
     * 初始化UI布局
     */
    private void initUI() {
        videoLayout = (RelativeLayout) findViewById(R.id.videoLayout);
        controller_layout = (LinearLayout) findViewById(R.id.contrallerbar_layout);
        videoView = (VideoView) findViewById(R.id.videobView);
        exit = (ImageView) findViewById(R.id.exit);
        play_controller_img = (ImageView) findViewById(R.id.pause_img);
        play_seek = (SeekBar) findViewById(R.id.play_seek);
        time_current_tv = (TextView) findViewById(R.id.time_current_tv);
        time_total_tv = (TextView) findViewById(R.id.time_total_tv);
        volume_img = (ImageView) findViewById(R.id.volume_img);
        volume_seek = (SeekBar) findViewById(R.id.volume_seek);
        screen_img = (ImageView) findViewById(R.id.screen_img);
        screen_width = getResources().getDisplayMetrics().widthPixels;
        screen_height = getResources().getDisplayMetrics().heightPixels;
        progress_layout = (FrameLayout) findViewById(R.id.progerss_layout);
        operation_bg = (ImageView) findViewById(R.id.opertion_bg);
        operation_percent = (ImageView) findViewById(R.id.operation_percent);


        /**
         * 当前设备的最大音量
         */
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        /**
         * 获取设备当前的音量
         */
        int streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume_seek.setMax(streamMaxVolume);
        volume_seek.setProgress(streamVolume);


    }
}
