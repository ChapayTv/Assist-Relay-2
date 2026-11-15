package me.vladimir.assistrelay;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Невидимая Activity. Ловит ACTION_VOICE_COMMAND (его шлёт LibrePods), сразу закрывается,
 * чтобы не стоять поверх экрана, и в фоновом потоке: играет сигнал и выполняет
 * `cmd voiceinteraction show` через root, с повтором при ошибке.
 * Если root недоступен или все попытки не удались: ACTION_ASSIST, затем выбранное приложение.
 */
public class RelayActivity extends Activity {

    private static final String COMMAND = "cmd voiceinteraction show";
    private static final int ATTEMPTS = 3;
    private static final int SECOND_COMMAND_DELAY_MS = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                work(app);
            }
        }).start();
        finish();
        overridePendingTransition(0, 0);
    }

    private static void work(final Context app) {
        // Сигнал активации: тишина (будит Bluetooth) + два тона
        if (Prefs.getChime(app)) {
            int lead = Prefs.getLead(app);
            ChimePlayer.play(lead);
            sleep(lead);
        }

        Result r = null;
        int used = 0;
        for (int i = 1; i <= ATTEMPTS; i++) {
            used = i;
            r = runAsRoot(COMMAND);
            if (r.exit == 0) break;
            sleep(250);
        }

        // Вторая команда подряд через 400 мс
        sleep(SECOND_COMMAND_DELAY_MS);
        Result r2 = runAsRoot(COMMAND);

        String stamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        Prefs.setLastLog(app, stamp + " | код " + r.exit + " | попыток " + used
                + (r.out.isEmpty() ? "" : " | " + r.out)
                + " | 2-я: код " + r2.exit);

        // Запасной запуск только если не сработала ни одна из двух команд
        if (r.exit != 0 && r2.exit != 0) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    fallback(app);
                }
            });
        }
    }

    private static final class Result {
        final int exit;
        final String out;

        Result(int exit, String out) {
            this.exit = exit;
            this.out = out;
        }
    }

    /** Выполняет команду через su, ждёт максимум 5 секунд. */
    private static Result runAsRoot(String cmd) {
        Process p = null;
        try {
            p = new ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start();
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroy();
                return new Result(-2, "таймаут");
            }
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null && sb.length() < 200) {
                sb.append(line).append(' ');
            }
            return new Result(p.exitValue(), sb.toString().trim());
        } catch (Exception e) {
            return new Result(-1, String.valueOf(e.getMessage()));
        } finally {
            if (p != null) p.destroy();
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            // продолжаем без паузы
        }
    }

    private static void fallback(Context app) {
        // ACTION_ASSIST у нас в манифесте не зарегистрирован, поэтому его обработает система
        try {
            app.startActivity(new Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // идём дальше
        }
        Intent launch = app.getPackageManager().getLaunchIntentForPackage(Prefs.getTarget(app));
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(launch);
        } else {
            Toast.makeText(app, "Root недоступен и приложение не найдено", Toast.LENGTH_LONG).show();
        }
    }
}
