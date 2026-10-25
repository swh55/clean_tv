package pro.datascoop.tv;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Opens another app whenever a given app's screen comes up. For remote buttons the firmware hard-wires to a system
 * app that can't be replaced: on a CVTE TV the ESHARE button always opens EShare, and "com.ecloud.eshare.server=
 * com.droidlogic.miracast" turns it into the Miracast button. Hosted by RemoteKeys, from its window events, so it
 * needs no key filtering and no timer.
 *
 * The "redirects" preference is a comma-separated list of from=to package pairs; empty (the default) does nothing.
 * Set it on a TV with: adb shell run-as pro.datascoop.tv (edit shared_prefs/afnon.xml), then restart the service.
 *
 * The window events also feed the kids watch timer (Settings → Family & startup): {@link #tick}, called from
 * PrayerWatch's 15-second loop, sends the TV back Home once another app has been in front for over the limit.
 */
final class Redirects {
    private Redirects() {}

    /** The package in front now (window events), and when it came up — elapsedRealtime; 0 counts as unset. */
    private static String front = "";
    private static long frontSince;

    /** Called for every window change; opens the target over Home when a redirected app's activity shows. */
    static void onWindowChanged(AccessibilityService s, AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName(), cls = e.getClassName();
        if (pkg == null || cls == null) return;
        String p = pkg.toString();
        if (!p.equals(front)) {
            front = p;
            frontSince = 0;   // the timer restarts with every app that comes to the front
        }
        String to = target(s, p);
        if (to == null || !isActivity(s, pkg.toString(), cls.toString())) return;
        Intent open = Apps.launchIntent(s, to);
        if (open == null) return; // Target missing: leave the original app open.
        // Home underneath, so Back from the target returns Home rather than to the app it replaced.
        Intent home = new Intent(s, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            s.startActivities(new Intent[] {home, open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)});
        } catch (RuntimeException ignored) {
            // The target refuses outside launches; stay where we are.
        }
    }

    private static String target(AccessibilityService s, String from) {
        String list = Prefs.of(s).getString("redirects", "");
        for (String pair : list.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).trim().equals(from)) return pair.substring(eq + 1).trim();
        }
        return null;
    }

    // ---------------------------------------------------------------- kids watch timer

    /** Called every few seconds by PrayerWatch: back Home once another app ran over the kids limit. */
    static void tick(AccessibilityService s) {
        int minutes = Prefs.kidsMinutes(s);
        if (minutes <= 0 || front.isEmpty() || front.equals(s.getPackageName())) {
            frontSince = 0;   // Home is in front, or the timer is off: nothing is running over
            return;
        }
        long now = android.os.SystemClock.elapsedRealtime();
        if (frontSince == 0) {
            frontSince = now;   // first sight of this app: the count starts now
            return;
        }
        if (now - frontSince >= minutes * 60_000L) {
            frontSince = 0;
            android.widget.Toast.makeText(s, Screen.loadText(s).kidsDone(), android.widget.Toast.LENGTH_LONG).show();
            Intent home = new Intent(s, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            try {
                s.startActivity(home);
            } catch (RuntimeException ignored) {
                // Blocked on this firmware; the next tick tries again.
            }
        }
    }

    /** True for the app's activities only, so its toasts and dialogs don't trigger a redirect. */
    private static boolean isActivity(AccessibilityService s, String pkg, String cls) {
        try {
            s.getPackageManager().getActivityInfo(new ComponentName(pkg, cls), 0);
            return true;
        } catch (PackageManager.NameNotFoundException notAnActivity) {
            return false;
        }
    }
}
