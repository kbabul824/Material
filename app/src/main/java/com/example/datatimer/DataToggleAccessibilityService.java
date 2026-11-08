package com.example.datatimer;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class DataToggleAccessibilityService extends AccessibilityService {
    public static final String ACTION_TRIGGER_DISABLE = "com.example.datatimer.TRIGGER_DISABLE_DATA";
    private static boolean isExecutingToggle = false;

    private final BroadcastReceiver triggerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_TRIGGER_DISABLE.equals(intent.getAction())) {
                isExecutingToggle = true;
                Intent settingsIntent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    settingsIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                } else {
                    settingsIntent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                }
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settingsIntent);
            }
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isExecutingToggle) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        if (findAndDisableSwitch(rootNode)) {
            isExecutingToggle = false;
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }

    private boolean findAndDisableSwitch(AccessibilityNodeInfo node) {
        if (node == null) return false;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String textStr = (text != null) ? text.toString().toLowerCase() : "";
        String descStr = (desc != null) ? desc.toString().toLowerCase() : "";

        boolean matchesData = textStr.contains("mobile data") || descStr.contains("mobile data")
                || textStr.contains("cellular data") || descStr.contains("cellular data");

        if (matchesData || "android.widget.Switch".contentEquals(node.getClassName())) {
            if (node.isCheckable() && node.isChecked()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (findAndDisableSwitch(child)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        isExecutingToggle = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(ACTION_TRIGGER_DISABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(triggerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(triggerReceiver, filter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(triggerReceiver);
        } catch (Exception ignored) {}
    }
}
