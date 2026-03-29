package com.example.apartmentmanagementsystem;

import android.app.Activity;
import android.content.Intent;

public final class ActivityNavigationHelper {

    private ActivityNavigationHelper() {
    }

    public static void navigate(Activity source, Class<?> targetActivity, boolean finishCurrent) {
        if (source == null || targetActivity == null) {
            return;
        }

        // Avoid relaunching the same screen repeatedly from bottom-nav taps.
        if (source.getClass().equals(targetActivity)) {
            return;
        }

        Intent intent = new Intent(source, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        source.startActivity(intent);
        source.overridePendingTransition(0, 0);

        if (finishCurrent) {
            source.finish();
        }
    }
}

