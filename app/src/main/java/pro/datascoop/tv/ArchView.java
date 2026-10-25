package pro.datascoop.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** The mihrab: a 600×900 pointed arch. Symmetric, so it needs no mirroring in Arabic. */
final class ArchView extends View {
    static final int REST = 0, FOCUSED = 1, APPROACHING = 2;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outer = new Path();
    private final Path innerLine = new Path();
    private int state = REST;
    /** The arch's fill follows the time of day; LAPIS_800 unless setTimeTint said otherwise. */
    private int fillColour = Tokens.LAPIS_800;

    /** How the fill shifts with the hour: deep night, a warm dawn, the day lapis, a warm sunset, night again. */
    private static final Object[][] TINT = {
        {0f, 0xFF0A1730}, {4f, 0xFF0A1730}, {5.5f, 0xFF2B3147}, {9f, Tokens.LAPIS_800},
        {15f, Tokens.LAPIS_800}, {18.2f, 0xFF303A4F}, {20f, Tokens.LAPIS_900}, {22f, 0xFF0A1730},
        {24f, 0xFF0A1730}};

    /** Tints the fill for an hour of day (0–24, fractions allowed); -1 puts the plain lapis back. */
    void setTimeTint(float hour) {
        fillColour = hour < 0 ? Tokens.LAPIS_800 : tint(hour);
        invalidate();
    }

    private static int tint(float hour) {
        for (int i = 1; i < TINT.length; i++) {
            float from = (Float) TINT[i - 1][0], to = (Float) TINT[i][0];
            if (hour <= to) {
                float t = hour <= from ? 0 : (hour - from) / (to - from);
                return lerp((Integer) TINT[i - 1][1], (Integer) TINT[i][1], t);
            }
        }
        return Tokens.LAPIS_800;
    }

    private static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000
            | ((Math.round(ar + (br - ar) * t)) << 16)
            | ((Math.round(ag + (bg - ag) * t)) << 8)
            | Math.round(ab + (bb - ab) * t);
    }

    ArchView(Context c) {
        super(c);
        fill.setColor(Tokens.LAPIS_800);
        stroke.setStyle(Paint.Style.STROKE);
        inner.setStyle(Paint.Style.STROKE);
        inner.setColor(Tokens.ARCH_INNER);
        inner.setStrokeWidth(Ui.px(1.5f));
    }

    void setState(int s) {
        if (s == state) return;
        state = s;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float k = w / 600f;
        // design/Home.dc.html: M0 900 L0 330 C0 170 140 70 300 0 C460 70 600 170 600 330 L600 900 Z
        outer.reset();
        outer.moveTo(0, 900 * k);
        outer.lineTo(0, 330 * k);
        outer.cubicTo(0, 170 * k, 140 * k, 70 * k, 300 * k, 0);
        outer.cubicTo(460 * k, 70 * k, 600 * k, 170 * k, 600 * k, 330 * k);
        outer.lineTo(600 * k, 900 * k);
        outer.close();
        // M18 900 L18 334 C18 184 150 92 300 22 C450 92 582 184 582 334 L582 900
        innerLine.reset();
        innerLine.moveTo(18 * k, 900 * k);
        innerLine.lineTo(18 * k, 334 * k);
        innerLine.cubicTo(18 * k, 184 * k, 150 * k, 92 * k, 300 * k, 22 * k);
        innerLine.cubicTo(450 * k, 92 * k, 582 * k, 184 * k, 582 * k, 334 * k);
        innerLine.lineTo(582 * k, 900 * k);
    }

    @Override
    protected void onDraw(Canvas c) {
        // border.arch-rest / arch-focused / arch-approaching
        if (state == APPROACHING) {
            stroke.setColor(Tokens.GOLD_200);
            stroke.setStrokeWidth(Ui.px(6));
        } else if (state == FOCUSED) {
            stroke.setColor(Tokens.PLASTER_100);
            stroke.setStrokeWidth(Ui.px(5));
        } else {
            stroke.setColor(Tokens.GOLD_400);
            stroke.setStrokeWidth(Ui.px(3));
        }
        fill.setColor(fillColour);
        c.drawPath(outer, fill);
        c.drawPath(innerLine, inner);
        c.drawPath(outer, stroke);
    }
}
