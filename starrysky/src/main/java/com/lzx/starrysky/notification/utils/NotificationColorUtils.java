package com.lzx.starrysky.notification.utils;

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;

import androidx.core.graphics.ColorUtils;

/**
 * 通知栏颜色工具类，主要是判断通知栏主题是白色还是黑色
 */
public class NotificationColorUtils {
    private static String NOTIFICATION_TITLE = "notification_music_title";
    private static String NOTIFICATION_CONTENT = "notification_music_content";

    private static int COLOR_UNDEF = 987654321;
    private static final double COLOR_THRESHOLD = 180.0D;
    private static int NOTIFICATION_TITLE_COLOR = Color.parseColor("#de000000");
    private static int NOTIFICATION_LINE2_COLOR = Color.parseColor("#8a000000");
    private static int EVENTCONTENT_TITLE_COLOR = -1;
    private static int EVENTCONTENT_COLOR = Color.parseColor("#b3ffffff");
    private TextView titleView = null;
    private TextView contentView = null;
    private NotificationColorModel mNotificationColorModel;

    public NotificationColorUtils() {
    }

    public void setTitleTextColor(Context context, RemoteViews remoteView, int viewId, Notification notification) {
        if (mNotificationColorModel == null) {
            isDarkNotificationBar(context, notification);
        }

        if (mNotificationColorModel.getTitleColor() == COLOR_UNDEF && Build.VERSION.SDK_INT >= 21) {
            if (mNotificationColorModel.isDarkNotificationBg()) {
                mNotificationColorModel.setTitleColor(EVENTCONTENT_TITLE_COLOR);
            } else {
                mNotificationColorModel.setTitleColor(NOTIFICATION_TITLE_COLOR);
            }
        }

        remoteView.setTextColor(viewId, mNotificationColorModel.getTitleColor());
    }

    public void setContentTextColor(Context context, RemoteViews remoteView, int viewId, Notification notification) {
        if (mNotificationColorModel == null) {
            isDarkNotificationBar(context, notification);
        }

        if (mNotificationColorModel.getContentColor() == COLOR_UNDEF && Build.VERSION.SDK_INT >= 21) {
            if (mNotificationColorModel.isDarkNotificationBg()) {
                mNotificationColorModel.setContentColor(EVENTCONTENT_COLOR);
            } else {
                mNotificationColorModel.setContentColor(NOTIFICATION_LINE2_COLOR);
            }
        }

        remoteView.setTextColor(viewId, mNotificationColorModel.getContentColor());
    }

    public synchronized boolean isDarkNotificationBar(final Context context, final Notification notification) {
        if (mNotificationColorModel == null) {
            mNotificationColorModel = new NotificationColorModel();
            boolean isInMainThread = Looper.myLooper() == Looper.getMainLooper();
            CountDownLatch countDownLatch = null;
            if (!isInMainThread) {
                countDownLatch = new CountDownLatch(1);
            }

            final CountDownLatch finalCountDownLatch = countDownLatch;
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        int notiTextColor = getNotificationColor(context, notification);
                        if (notiTextColor == COLOR_UNDEF) {
                            mNotificationColorModel.setTitleColor(COLOR_UNDEF);
                            mNotificationColorModel.setContentColor(COLOR_UNDEF);
                            mNotificationColorModel.setDarkNotificationBg(true);
                        } else {
                            //!isTextColorSimilar(-16777216, notiTextColor);
                            boolean isDark = ColorUtils.calculateLuminance(notiTextColor) > 0.5;
                            mNotificationColorModel.setDarkNotificationBg(isDark);
                        }
                    } catch (Exception var3) {
                        var3.printStackTrace();
                        mNotificationColorModel.setTitleColor(COLOR_UNDEF);
                        mNotificationColorModel.setContentColor(COLOR_UNDEF);
                        mNotificationColorModel.setDarkNotificationBg(true);
                    }

                    if (mNotificationColorModel.getTitleColor() == COLOR_UNDEF && Build.VERSION.SDK_INT >= 21) {
                        if (mNotificationColorModel.isDarkNotificationBg()) {
                            mNotificationColorModel.setTitleColor(EVENTCONTENT_TITLE_COLOR);
                        } else {
                            mNotificationColorModel.setTitleColor(NOTIFICATION_TITLE_COLOR);
                        }
                    }

                    if (mNotificationColorModel.getContentColor() == COLOR_UNDEF && Build.VERSION.SDK_INT >= 21) {
                        if (mNotificationColorModel.isDarkNotificationBg()) {
                            mNotificationColorModel.setContentColor(EVENTCONTENT_COLOR);
                        } else {
                            mNotificationColorModel.setContentColor(NOTIFICATION_LINE2_COLOR);
                        }
                    }

                    if (finalCountDownLatch != null) {
                        finalCountDownLatch.countDown();
                    }

                }
            };
            if (isInMainThread) {
                runnable.run();
            } else {
                (new Handler(Looper.getMainLooper())).post(runnable);
                try {
                    countDownLatch.await();
                } catch (InterruptedException var6) {
                    var6.printStackTrace();
                }
            }
        }

        return mNotificationColorModel.isDarkNotificationBg();
    }

    private int getNotificationColor(Context context, Notification notification) {
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        ViewGroup viewGroup = (ViewGroup) notification.contentView.apply(context, layout);
        getTextView(viewGroup, false);
        if (titleView == null) {
            return COLOR_UNDEF;
        } else {
            int color = titleView.getCurrentTextColor();
            mNotificationColorModel.setTitleColor(color);
            if (contentView != null) {
                int contentColor = contentView.getCurrentTextColor();
                mNotificationColorModel.setContentColor(contentColor);
            }

            return color;
        }
    }

    private TextView getTextView(ViewGroup viewGroup, boolean isSetTextColor) {
        if (viewGroup == null) {
            return null;
        } else {
            int count = viewGroup.getChildCount();
            for (int i = 0; i < count; ++i) {
                View view = viewGroup.getChildAt(i);
                if (view instanceof TextView) {
                    TextView newDtv = (TextView) view;
                    if (isSetTextColor) {
                        if (newDtv.getText().equals(NOTIFICATION_TITLE)) {
                            titleView = newDtv;
                        }
                        if (newDtv.getText().equals(NOTIFICATION_CONTENT)) {
                            contentView = newDtv;
                        }
                    } else {
                        titleView = newDtv;
                        contentView = newDtv;
                    }
                } else if (view instanceof ViewGroup) {
                    getTextView((ViewGroup) view, isSetTextColor);
                }
            }

            return null;
        }
    }

    private boolean isDark(int notiTextColor) {
        return ColorUtils.calculateLuminance(notiTextColor) < 0.5;
    }

    private boolean isTextColorSimilar(int baseColor, int color) {
        int simpleBaseColor = baseColor | -16777216;
        int simpleColor = color | -16777216;
        int baseRed = Color.red(simpleBaseColor) - Color.red(simpleColor);
        int baseGreen = Color.green(simpleBaseColor) - Color.green(simpleColor);
        int baseBlue = Color.blue(simpleBaseColor) - Color.blue(simpleColor);
        double value = Math.sqrt((double) (baseRed * baseRed + baseGreen * baseGreen + baseBlue * baseBlue));
        return value < 180.0D;
    }

    static class NotificationColorModel {
        private int titleColor;
        private int contentColor;
        private boolean isDarkNotificationBg;

        NotificationColorModel() {
            this.titleColor = COLOR_UNDEF;
            this.contentColor = COLOR_UNDEF;
            this.isDarkNotificationBg = true;
        }

        public int getTitleColor() {
            return this.titleColor;
        }

        public void setTitleColor(int titleColor) {
            this.titleColor = titleColor;
        }

        public int getContentColor() {
            return this.contentColor;
        }

        public void setContentColor(int contentColor) {
            this.contentColor = contentColor;
        }

        public boolean isDarkNotificationBg() {
            return this.isDarkNotificationBg;
        }

        public void setDarkNotificationBg(boolean darkNotificationBg) {
            this.isDarkNotificationBg = darkNotificationBg;
        }
    }
}
