package com.example.fonoss;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;

public final class UiNotifier {

    private UiNotifier() {}

    public static void success(Context context, String message) {
        show(context, message, R.color.green_600, R.color.white, R.drawable.ic_check_white);
    }

    public static void warning(Context context, String message) {
        show(context, message, R.color.amber_500, R.color.white, R.drawable.ic_warning_rounded);
    }

    public static void error(Context context, String message) {
        show(context, message, R.color.red_600, R.color.white, R.drawable.ic_warning_rounded);
    }

    public static void info(Context context, String message) {
        show(context, message, R.color.slate_900, R.color.white, R.drawable.ic_info_rounded);
    }

    private static void show(Context context, String message, int backgroundColorRes, int textColorRes, int iconRes) {
        if (context == null) return;

        Activity activity = findActivity(context);
        if (activity == null) {
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }

        View root = activity.findViewById(android.R.id.content);
        if (root == null) {
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }

        int textColor = ContextCompat.getColor(activity, textColorRes);
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT);
        View bottomNav = activity.findViewById(R.id.bottom_navigation);
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(bottomNav);
        }

        View snackbarView = snackbar.getView();
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(activity, backgroundColorRes));
        background.setCornerRadius(dp(activity, 24));
        background.setStroke(dp(activity, 1), ContextCompat.getColor(activity, R.color.outline_soft));
        snackbarView.setBackground(background);
        snackbarView.setElevation(dp(activity, 10));

        ViewGroup.LayoutParams params = snackbarView.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
            marginParams.setMargins(dp(activity, 18), 0, dp(activity, 18),
                    bottomNav != null && bottomNav.getVisibility() == View.VISIBLE ? dp(activity, 10) : dp(activity, 24));
            snackbarView.setLayoutParams(marginParams);
        }

        TextView text = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (text != null) {
            text.setTextColor(textColor);
            text.setTextSize(14);
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            text.setGravity(Gravity.CENTER_VERTICAL);
            text.setMaxLines(3);

            Drawable icon = ContextCompat.getDrawable(activity, iconRes);
            if (icon != null) {
                icon = icon.mutate();
                icon.setTint(textColor);
                icon.setBounds(0, 0, dp(activity, 18), dp(activity, 18));
                text.setCompoundDrawables(icon, null, null, null);
                text.setCompoundDrawablePadding(dp(activity, 10));
            }
        }

        snackbar.show();
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
