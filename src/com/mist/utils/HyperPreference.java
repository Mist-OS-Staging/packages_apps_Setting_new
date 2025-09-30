package com.mist.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.SystemProperties;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.MotionEvent;
import android.view.View;
import android.view.HapticFeedbackConstants;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.widget.LayoutPreference;
import com.airbnb.lottie.LottieAnimationView;
import com.mist.utils.AboutPhoneData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import com.android.settings.R;

public class HyperPreference extends BasePreferenceController implements View.OnTouchListener {

    private Context context;
    private AboutPhoneData phoneData;
    private boolean fullKernelVersion = false;
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private final Handler longPressHandler = new Handler();
    private boolean isLongPressDetected = false;

    public HyperPreference(Context context, String key) {
        super(context, key);
        this.context = context;
        this.phoneData = new AboutPhoneData(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference mPreference = screen.findPreference("hyper_about_layout");
        if (mPreference != null) {
            onBindItems(mPreference.findViewById(R.id.hyper_mod));
        }
    }

    private static void setInfo(String prop, TextView textView) {
        String value = SystemProperties.get(prop);
        textView.setText(TextUtils.isEmpty(value) ? "Unknown" : value);
    }

    private void onBindItems(View holder) {
        TextView display = holder.findViewById(R.id.resolution_about);
        TextView battery = holder.findViewById(R.id.battery_about);
        TextView soc = holder.findViewById(R.id.soc_about);
        TextView rear = holder.findViewById(R.id.rear_camera_about);
        TextView front = holder.findViewById(R.id.front_camera_about);
        TextView platform = holder.findViewById(R.id.platform_about);
        TextView screen = holder.findViewById(R.id.screen_about);
        TextView device = holder.findViewById(R.id.device_name);
        TextView buildDate = holder.findViewById(R.id.build_date);
        TextView deviceSec = holder.findViewById(R.id.security_update);
        TextView kernel = holder.findViewById(R.id.kernel_version);
        TextView maintainer = holder.findViewById(R.id.mantainer_about);
        TextView mistVersion = holder.findViewById(R.id.mist_version);
        TextView ramTextView = holder.findViewById(R.id.ram_about);
        ProgressBar ramProgressBar = holder.findViewById(R.id.ram_progress_bar);
        TextView romTextView = holder.findViewById(R.id.storage_about);
        ProgressBar romProgressBar = holder.findViewById(R.id.rom_progress_bar);
        ImageView statusChip = holder.findViewById(R.id.status_chip);
        TextView buildTypeText = holder.findViewById(R.id.mist_build_type);
        TextView buildNumber = holder.findViewById(R.id.build_number);
        RelativeLayout androidBarInfo = holder.findViewById(R.id.android_bar_info);

    	display.setText(phoneData.getDisplay());
    	battery.setText(phoneData.getBattery());
    	soc.setText(phoneData.getSoc());
    	rear.setText(phoneData.getCamera());
    	front.setText(phoneData.getFront());
    	platform.setText(phoneData.getPlatform());
    	screen.setText(phoneData.getScreen());
    	deviceSec.setText(DeviceInfoUtils.getSecurityPatch());
    	kernel.setText(DeviceInfoUtils.getFormattedKernelVersion(context));
    	setInfo("ro.mistos.maintainer", maintainer);
    	
        String buildNumberText = SystemProperties.get("ro.build.display.id", "X.X.X");
        buildNumber.setText(buildNumberText);

    	String buildDateText = SystemProperties.get("ro.build.date", "X.X.X");
    	buildDate.setText(buildDateText);

    	String buildTypeValue = SystemProperties.get("ro.mist.buildtype", "Unofficial");
    	
    	String versionBase = SystemProperties.get("ro.mist.version.base", "X.X");
    	String codename = SystemProperties.get("ro.mist.codename", "Unknown");
    	
    	String mistVersionText = versionBase + " | " + codename;
    	
        mistVersion.setText(mistVersionText);

    	if ("OFFICIAL".equalsIgnoreCase(buildTypeValue)) {
        buildTypeText.setText("OFFICIAL");
        buildTypeText.setTextColor(holder.getContext().getColor(R.color.mist_official_color));
        statusChip.setImageResource(R.drawable.icon_official);
        statusChip.setColorFilter(holder.getContext().getColor(R.color.mist_official_color));

        } else {
        buildTypeText.setText("UNOFFICIAL");
        buildTypeText.setTextColor(holder.getContext().getColor(R.color.mist_unofficial_color));

        statusChip.setImageResource(R.drawable.icon_unofficial);
        statusChip.setColorFilter(holder.getContext().getColor(R.color.mist_unofficial_color));
        }
        
    	SystemInfoUtils.setDeviceName(device);

    	new Thread(() -> {
            SystemInfoUtils.StorageInfo storageInfo = SystemInfoUtils.getStorageInfo(context);
            SystemInfoUtils.MemoryInfo memoryInfo = SystemInfoUtils.getMemoryInfo(context);
        	
            holder.post(() -> {
            	romTextView.setText(String.format(Locale.getDefault(), "%s / %s",
                    Formatter.formatFileSize(context, storageInfo.usedBytes),
                    Formatter.formatFileSize(context, storageInfo.totalBytes)));
            	animateProgressBar(romProgressBar, storageInfo.usedPercentage);

                ramTextView.setText(String.format(Locale.ENGLISH, "%d GB (%s)",
                memoryInfo.roundedRamInGB,
                context.getString(R.string.ram_active_label)));

            	animateProgressBar(ramProgressBar, memoryInfo.usedRamPercentage);
            });
    	}).start();

    	final Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName(
                        "android", com.android.internal.app.PlatLogoActivity.class.getName());
                        
        androidBarInfo.setOnTouchListener(this);

        androidBarInfo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    context.startActivity(intent);
                    return true;
                } catch (Exception ignored) {
                }
                return false;
            }
        });
    	
    	kernel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fullKernelVersion) {
                    kernel.setText(DeviceInfoUtils.getFormattedKernelVersion(context));
                    fullKernelVersion = false;
                } else {
                    kernel.setText(SystemInfoUtils.getFullKernelVersion());
                    fullKernelVersion = true;
                }
            }
        });
    }

    private void animateProgressBar(ProgressBar progressBar, int progress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", 0, progress);
        animator.setDuration(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        Drawable progressDrawable = progressBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            Drawable progressLayer = ((LayerDrawable) progressDrawable).findDrawableByLayerId(android.R.id.progress);
            if (progressLayer != null) {
                int color = ContextCompat.getColor(context, progress >= 85 ? R.color.red_percentage : R.color.green_percentage);
                progressLayer.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }
    
    public class SystemInfoUtils {

        public static void setDeviceName(TextView deviceTextView) {
    String modelName = SystemProperties.get("ro.product.model");
    String mistDevice = SystemProperties.get("ro.mist.device");
    String userDeviceName = SystemProperties.get("ro.mist.device.name"); // User configurable

    SpannableStringBuilder builder = new SpannableStringBuilder();

    String mainName = !TextUtils.isEmpty(userDeviceName) ? userDeviceName
                      : !TextUtils.isEmpty(SystemProperties.get("ro.product.marketname")) ? SystemProperties.get("ro.product.marketname")
                      : modelName;

    if (!TextUtils.isEmpty(mainName)) {
        SpannableString nameText = new SpannableString(mainName + "\n");
        nameText.setSpan(new AbsoluteSizeSpan(21, true), 0, nameText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(nameText);
    }

    if (!TextUtils.isEmpty(mistDevice)) {
        SpannableString mistDeviceText = new SpannableString("(" + mistDevice + ")\n");
        mistDeviceText.setSpan(new AbsoluteSizeSpan(15, true), 0, mistDeviceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(mistDeviceText);
    }

    if (!TextUtils.isEmpty(modelName)) {
        SpannableString modelText = new SpannableString("(" + modelName + ")");
        modelText.setSpan(new AbsoluteSizeSpan(13, true), 0, modelText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(modelText);
    }

    deviceTextView.setText(builder);
  }

/*    	public static void setDeviceName(TextView deviceTextView) {
    	    String modelName = SystemProperties.get("ro.product.model");
    	    String marketName = SystemProperties.get("ro.product.marketname");
    	    String mistDevice = SystemProperties.get("ro.mist.device");

    	    SpannableStringBuilder builder = new SpannableStringBuilder();
    	    if (!TextUtils.isEmpty(marketName)) {
    	        SpannableString marketNameText = new SpannableString(marketName + "\n");
    	        marketNameText.setSpan(new AbsoluteSizeSpan(21, true), 0, marketNameText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    	        builder.append(marketNameText);
    	    } else if (!TextUtils.isEmpty(modelName)) {
    	        SpannableString modelNameText = new SpannableString(modelName + "\n");
    	        modelNameText.setSpan(new AbsoluteSizeSpan(21, true), 0, modelNameText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    	        builder.append(modelNameText);
    	    }
    	    if (!TextUtils.isEmpty(mistDevice)) {
    	        SpannableString mistDeviceText = new SpannableString("(" + mistDevice + ")");
    	        mistDeviceText.setSpan(new AbsoluteSizeSpan(15, true), 0, mistDeviceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    	        builder.append(mistDeviceText);
    	    }
    	    deviceTextView.setText(builder);
    	}
*/
    	public static StorageInfo getStorageInfo(Context context) {
    	    StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
    	    StorageManagerVolumeProvider volumeProvider = new StorageManagerVolumeProvider(storageManager);
    	    PrivateStorageInfo privateStorageInfo = PrivateStorageInfo.getPrivateStorageInfo(volumeProvider);

    	    long totalBytes = privateStorageInfo.totalBytes;
    	    long freeBytes = privateStorageInfo.freeBytes;
    	    long usedBytes = totalBytes - freeBytes;
    	    int usedPercentage = (int) ((usedBytes * 100) / totalBytes);

    	    return new StorageInfo(totalBytes, usedBytes, usedPercentage);
    	}

    	public static MemoryInfo getMemoryInfo(Context context) {
    	    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    	    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    	    activityManager.getMemoryInfo(memoryInfo);

    	    long totalMemory = memoryInfo.totalMem;
    	    long availableMemory = memoryInfo.availMem;
    	    long usedMemory = totalMemory - availableMemory;
    	    int usedRamPercentage = (int) ((usedMemory * 100) / totalMemory);

    	    double ramInGB = Double.parseDouble(getMem()) / Math.pow(1000, 2);
    	    int roundedRamInGB = (int) Math.ceil(ramInGB);
    	    String swapSize = totalSwap();

    	    return new MemoryInfo(roundedRamInGB, swapSize, usedRamPercentage);
    	}

    	private static String getMem() {
    	    Map<String, String> memInfo = getMemInfoMap();
    	    if (memInfo.containsKey("MemTotal")) {
    	        return memInfo.get("MemTotal").split(" ")[0];
    	    }
    	    return "Unknown";
    	}

    	private static String totalSwap() {
    	    Map<String, String> memInfo = getMemInfoMap();
    	    if (memInfo.containsKey("SwapTotal")) {
    	        long swapSizeBytes = Long.parseLong(memInfo.get("SwapTotal").split(" ")[0]) * 1024;
    	        return String.valueOf((int) Math.ceil(swapSizeBytes / Math.pow(1024, 3)));
    	    }
    	    return "0";
    	}

    	private static Map<String, String> getMemInfoMap() {
    	    Map<String, String> map = new HashMap<>();
    	    try (Scanner scanner = new Scanner(new File("/proc/meminfo"))) {
    	        while (scanner.hasNextLine()) {
    	            String[] vals = scanner.nextLine().split(": ");
    	            if (vals.length > 1) {
    	                map.put(vals[0].trim(), vals[1].trim());
    	            }
    	        }
    	    } catch (Exception e) {
    	        Log.e("SystemInfoUtils", "Error reading meminfo", e);
    	    }
    	    return map;
    	}

    	public static class StorageInfo {
    	    public long totalBytes;
    	    public long usedBytes;
    	    public int usedPercentage;
    	    
            public StorageInfo(long totalBytes, long usedBytes, int usedPercentage) {
            	this.totalBytes = totalBytes;
            	this.usedBytes = usedBytes;
            	this.usedPercentage = usedPercentage;
            }
        }

    	public static class MemoryInfo {
            public int roundedRamInGB;
            public String swapSize;
            public int usedRamPercentage;

            public MemoryInfo(int roundedRamInGB, String swapSize, int usedRamPercentage) {
                this.roundedRamInGB = roundedRamInGB;
                this.swapSize = swapSize;
                this.usedRamPercentage = usedRamPercentage;
            }
        }
        
        private static String getFullKernelVersion() {
            String procVersionStr;
            try {
                procVersionStr = readLine(FILENAME_PROC_VERSION);
                return procVersionStr;
            } catch (IOException e) {
                Log.e("DeviceInfoUtils", "IO Exception when getting kernel version", e);
                return "Unavailable";
            }
        }
        
        private static String readLine(String filename) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
            try {
                return reader.readLine();
            } finally {
                reader.close();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        int action = motionEvent.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isLongPressDetected = false;

                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(200).start();

                longPressHandler.postDelayed(() -> {
                    isLongPressDetected = true;
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    if (v.getOnLongClickListener() != null) {
                        v.getOnLongClickListener().onLongClick(v);
                    }
                }, ViewConfiguration.getLongPressTimeout());
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isLongPressDetected) {
                    longPressHandler.removeCallbacksAndMessages(null);
                    isLongPressDetected = false;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                longPressHandler.removeCallbacksAndMessages(null);

                AnimatorSet animatorSet = new AnimatorSet();
                ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(v, "scaleX", 1.25f);
                ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(v, "scaleY", 1.25f);
                scaleXUp.setDuration(200);
                scaleYUp.setDuration(200);

                scaleXUp.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(v, "scaleX", 1f);
                        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(v, "scaleY", 1f);
                        scaleXDown.setDuration(200);
                        scaleYDown.setDuration(200);

                        AnimatorSet bounceBackSet = new AnimatorSet();
                        bounceBackSet.play(scaleXDown).with(scaleYDown);
                        bounceBackSet.start();
                    }
                });

                animatorSet.play(scaleXUp).with(scaleYUp);
                animatorSet.start();

                if (!isLongPressDetected) {
                    v.performClick();
                }
                return true;
        }
        return false;
    }
}
