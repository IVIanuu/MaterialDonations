package com.ivianuu.materialdonations;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;

/**
 * Author IVIanuu.
 */

public class ColorUtils {

    public static int getHintColor(Context context) {
        return resolveColor(context, android.R.attr.textColorHint);
    }

    public static int getPrimaryTextColor(Context context) {
        return resolveColor(context, android.R.attr.textColorPrimary);
    }

    public static int getSecondaryTextColor(Context context) {
        return resolveColor(context, android.R.attr.textColorSecondary);
    }

    public static int getAccentColor(Context context) {
        return resolveColor(context, android.R.attr.colorAccent);
    }

    private static int resolveColor(Context context, @AttrRes int attr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }
}
