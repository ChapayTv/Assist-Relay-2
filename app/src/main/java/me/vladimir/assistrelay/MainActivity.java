package me.vladimir.assistrelay;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Главный экран: тёмный интерфейс с карточками. */
public class MainActivity extends Activity {

    // Палитра
    private static final int BG = 0xFF0B0B0D;
    private static final int CARD = 0xFF1C1D21;
    private static final int INNER = 0xFF2A2C31;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8E9096;
    private static final int RED = 0xFFEE2E3B;
    private static final int BLUE = 0xFF4A90FF;
    private static final int GREEN = 0xFF5FD3BE;
    private static final int ORANGE = 0xFFEE6A3B;
    private static final int BAR_OFF = 0xFF4A4C52;

    private static final int LEAD_STEP = 50;

    private TextView leadValue;
    private TextView leadLabel;
    private TextView logText;
    private TextView chimeState;
    private TextView fallbackValue;
    private TextView micChip;
    private View statusBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(8), dp(16), dp(24));
        scroll.addView(page, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Заголовок
        TextView title = text("Assist Relay 2", 20, WHITE, true);
        title.setGravity(Gravity.CENTER);
        page.addView(title, lp(0));
        TextView subtitle = text("Ассистент и сигнал активации", 15, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle, lp(2));

        // Главная карточка
        LinearLayout main = card(CARD, 28, 20);
        page.addView(main, lp(20));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout valueCol = new LinearLayout(this);
        valueCol.setOrientation(LinearLayout.VERTICAL);
        leadValue = text("", 38, WHITE, true);
        leadLabel = text("тишина перед сигналом", 14, MUTED, false);
        valueCol.addView(leadValue);
        valueCol.addView(leadLabel);
        topRow.addView(valueCol, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView test = text("ПРОВЕРИТЬ", 14, WHITE, true);
        test.setLetterSpacing(0.06f);
        test.setGravity(Gravity.CENTER);
        test.setPadding(dp(22), dp(14), dp(22), dp(14));
        test.setBackground(bg(RED, 28));
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RelayActivity.class));
            }
        });
        topRow.addView(test);
        main.addView(topRow);

        // Плашка про root
        LinearLayout info = card(INNER, 20, 16);
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text("!", 18, WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(bg(ORANGE, 8));
        infoRow.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView infoTitle = text("Нужен root", 20, WHITE, true);
        LinearLayout.LayoutParams itp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itp.leftMargin = dp(12);
        infoRow.addView(infoTitle, itp);
        info.addView(infoRow);
        info.addView(text("Команда cmd voiceinteraction show выполняется через su. "
                + "Без root включится запасной запуск ассистента.", 16, MUTED, false), lp(8));
        main.addView(info, lp(16));

        // Две карточки в ряд: последний запуск и сигнал
        LinearLayout pair = new LinearLayout(this);
        pair.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout logCard = card(INNER, 20, 16);
        logCard.addView(text("Последний запуск", 17, WHITE, true));
        logText = text("", 13, MUTED, false);
        logCard.addView(logText, lp(6));
        statusBar = new View(this);
        logCard.addView(statusBar, barLp());
        LinearLayout.LayoutParams lcp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        pair.addView(logCard, lcp);

        LinearLayout chimeCard = card(INNER, 20, 16);
        chimeCard.addView(text("Сигнал", 17, WHITE, true));
        chimeState = text("", 14, MUTED, false);
        chimeCard.addView(chimeState, lp(6));
        Switch sw = new Switch(this);
        sw.setChecked(Prefs.getChime(this));
        sw.setThumbTintList(csl(BLUE, 0xFFB0B2B8));
        sw.setTrackTintList(csl(0x804A90FF, BAR_OFF));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                Prefs.setChime(MainActivity.this, checked);
                refresh();
            }
        });
        LinearLayout.LayoutParams swp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        swp.topMargin = dp(12);
        chimeCard.addView(sw, swp);
        LinearLayout.LayoutParams ccp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        ccp.leftMargin = dp(12);
        pair.addView(chimeCard, ccp);
        main.addView(pair, lp(12));

        // Карточка: настройка тишины
        LinearLayout delayCard = card(CARD, 28, 20);
        page.addView(delayCard, lp(12));
        TextView delayTitle = text("СИГНАЛ АКТИВАЦИИ", 20, WHITE, true);
        delayCard.addView(delayTitle);

        LinearLayout delayInner = card(INNER, 20, 16);
        TextView delayHint = text("Тишина перед сигналом будит Bluetooth-канал. "
                + "Если сигнал слышен из динамика, увеличьте значение.", 15, MUTED, false);
        delayInner.addView(delayHint);
        SeekBar seek = new SeekBar(this);
        seek.setMax(2000 / LEAD_STEP);
        seek.setProgress(Prefs.getLead(this) / LEAD_STEP);
        seek.setProgressTintList(ColorStateList.valueOf(BLUE));
        seek.setThumbTintList(ColorStateList.valueOf(BLUE));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(BAR_OFF));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    Prefs.setLead(MainActivity.this, progress * LEAD_STEP);
                    refresh();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });
        delayInner.addView(seek, lp(12));
        delayCard.addView(delayInner, lp(14));

        // Карточка: запасной запуск
        LinearLayout fbCard = card(CARD, 28, 20);
        page.addView(fbCard, lp(12));
        fbCard.addView(text("ЗАПАСНОЙ ЗАПУСК", 20, WHITE, true));
        LinearLayout fbInner = card(INNER, 20, 16);
        fbInner.addView(text("Откроется, если root недоступен", 15, MUTED, false));
        fallbackValue = text("", 18, WHITE, true);
        fbInner.addView(fallbackValue, lp(6));
        fbInner.addView(text("Выбрать приложение  →", 15, BLUE, true), lp(10));
        fbInner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppPicker();
            }
        });
        fbCard.addView(fbInner, lp(14));

        // Чипы
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chipScroll.addView(chips);

        micChip = chip("");
        micChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasMic()) {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName())));
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        });
        chips.addView(micChip);

        TextView assistChip = chip("Ассистент по умолчанию");
        assistChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
                } catch (ActivityNotFoundException ignored) {
                    // на этом устройстве экрана нет
                }
            }
        });
        LinearLayout.LayoutParams acp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        acp.leftMargin = dp(10);
        chips.addView(assistChip, acp);
        page.addView(chipScroll, lp(16));

        // Android 15 (targetSdk 35): окно рисуется под системными панелями, добавляем отступы
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom());
            return insets;
        });

        setContentView(scroll);

        // Разрешение на микрофон: запрашиваем при открытии, если ещё не выдано
        if (!hasMic()) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        refresh();
    }

    private boolean hasMic() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void refresh() {
        leadValue.setText(Prefs.getLead(this) + " мс");

        chimeState.setText(Prefs.getChime(this) ? "Включён" : "Выключен");

        String log = Prefs.getLastLog(this);
        logText.setText(log);
        int color = BAR_OFF;
        if (log.contains("код 0")) {
            color = GREEN;
        } else if (log.contains("код")) {
            color = RED;
        }
        statusBar.setBackground(bg(color, 3));

        fallbackValue.setText(appLabel(Prefs.getTarget(this)));

        micChip.setText(hasMic() ? "Микрофон: разрешён" : "Микрофон: нажмите, чтобы разрешить");
    }

    private String appLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg + " (не установлено)";
        }
    }

    private void showAppPicker() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);

        final List<String[]> rows = new ArrayList<>();
        for (ResolveInfo ri : infos) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            rows.add(new String[]{String.valueOf(ri.loadLabel(pm)), pkg});
        }
        Collections.sort(rows, (a, b) -> a[0].compareToIgnoreCase(b[0]));

        String[] labels = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            labels[i] = rows.get(i)[0];
        }
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Запасной запуск")
                .setItems(labels, (d, which) -> {
                    Prefs.setTarget(MainActivity.this, rows.get(which)[1]);
                    refresh();
                })
                .show();
    }

    // ---------- помощники оформления ----------

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable bg(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout card(int color, int radiusDp, int padDp) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(padDp), dp(padDp), dp(padDp), dp(padDp));
        c.setBackground(bg(color, radiusDp));
        return c;
    }

    private TextView chip(String s) {
        TextView t = text(s, 15, WHITE, false);
        t.setPadding(dp(18), dp(12), dp(18), dp(12));
        t.setBackground(bg(CARD, 22));
        return t;
    }

    private LinearLayout.LayoutParams lp(int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(topDp);
        return p;
    }

    private LinearLayout.LayoutParams barLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        p.topMargin = dp(14);
        return p;
    }

    private static ColorStateList csl(int checked, int unchecked) {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{checked, unchecked});
    }
}
