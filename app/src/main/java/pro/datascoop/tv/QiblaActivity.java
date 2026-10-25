package pro.datascoop.tv;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

/**
 * The Qibla on its own screen: the dial for the saved city, the bearing in degrees and the direction in words,
 * turned from the sofa when the room direction is set (Prayer day → Room direction). Opened by holding the
 * remote's Settings button, or from anywhere with
 * `am start -n pro.datascoop.tv/.QiblaActivity`. Back closes it.
 */
public class QiblaActivity extends Screen {

    private PrayerDay day;
    private QiblaView dial;
    private TextView degrees, direction, marker;

    @Override
    void build() {
        newRoot();
        root.setBackgroundColor(Tokens.LAPIS_900);
        day = new PrayerDay(this, new Date());

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setClipChildren(false);

        TextView label = caps(text(ui400, 22, Tokens.MIST_400));
        label.setText(t.qibla());
        dial = new QiblaView(this);
        degrees = text(display, 56, Tokens.GOLD_400);
        degrees.setTextDirection(View.TEXT_DIRECTION_LTR);
        degrees.setFontFeatureSettings("tnum");
        direction = text(ui400, 26, Tokens.PLASTER_100);
        direction.setGravity(Gravity.CENTER_HORIZONTAL);
        marker = text(ui400, 20, Tokens.MIST_400);
        col.addView(label);
        col.addView(dial, new LinearLayout.LayoutParams(Ui.ipx(280), Ui.ipx(280)));
        col.addView(degrees, new LinearLayout.LayoutParams(-2, -2));
        col.addView(direction, new LinearLayout.LayoutParams(-2, -2));
        col.addView(marker, new LinearLayout.LayoutParams(-2, -2));
        ((LinearLayout.LayoutParams) dial.getLayoutParams()).topMargin = Ui.ipx(20);
        ((LinearLayout.LayoutParams) degrees.getLayoutParams()).topMargin = Ui.ipx(12);
        ((LinearLayout.LayoutParams) direction.getLayoutParams()).topMargin = Ui.ipx(8);
        ((LinearLayout.LayoutParams) marker.getLayoutParams()).topMargin = Ui.ipx(8);
        lineHeight(label, 1.3f);
        lineHeight(degrees, 1f);
        lineHeight(direction, 1.3f);
        lineHeight(marker, 1.3f);

        TextView hint = text(ui400, 22, Tokens.MIST_400);
        hint.setText(t.hintBackHome());
        hint.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(Tokens.LAPIS_800);
        chip.setCornerRadius(Ui.px(30));
        hint.setBackground(chip);
        hint.setPadding(Ui.ipx(28), Ui.ipx(12), Ui.ipx(28), Ui.ipx(12));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.addView(col);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-2, -2);
        hlp.topMargin = Ui.ipx(36);
        page.addView(hint, hlp);
        place(page, 660, 130, 600, ViewGroup.LayoutParams.WRAP_CONTENT);

        setContentView(root);
        render();
    }

    /** North-up until the room direction is set (then the top of the dial is the TV). */
    private void render() {
        int facing = facing();
        if (facing < 0) {
            dial.setNorth(Double.NaN);
            dial.setBearing(day.qibla);
            marker.setText(t.north());
            direction.setText(t.qiblaDirection(day.qibla));
        } else {
            double relative = ((day.qibla - facing) % 360 + 360) % 360;
            dial.setNorth((360 - facing) % 360);
            dial.setBearing(relative);
            marker.setText(t.facingLine(facing));
            direction.setText(t.qiblaRelative(relative));
        }
        degrees.setText(t.d(Math.round(day.qibla) + "°"));
    }

    @Override
    void onTick() {
        // A fixed diagram for the saved city; nothing moves here.
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) finish();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }
}
