package pro.datascoop.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * The weather glyphs, drawn by hand on a 24px grid like the stroke icons, so they cost nothing and follow the
 * gold of the Home lines: sun and moon for a clear sky (by day and by night), clouds, drizzle, rain, snow and a
 * storm. A filled cloud with thin drops keeps them readable at 30–40 design px.
 */
final class WeatherIcon extends View {
    static final int SUN = 0, MOON = 1, PARTLY = 2, CLOUD = 3, FOG = 4, DRIZZLE = 5, RAIN = 6, SNOW = 7, STORM = 8;

    /** The glyph for a Weather.group() and the hour (is_day); the callers only know the group. */
    static int kind(int group, boolean day) {
        switch (group) {
            case 0: return day ? SUN : MOON;
            case 1: return PARTLY;
            case 2: return CLOUD;
            case 3: return FOG;
            case 4: return DRIZZLE;
            case 5: return RAIN;
            case 6: return SNOW;
            case 7: return STORM;
            default: return PARTLY;
        }
    }

    private int kind = SUN;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();

    WeatherIcon(Context c) {
        super(c);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    void setKind(int k) {
        if (kind == k) return;
        kind = k;
        invalidate();
    }

    void setColor(int color) {
        p.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float k = getWidth() / 24f;
        p.setStrokeWidth(1.7f * k);
        p.setStyle(Paint.Style.STROKE);
        switch (kind) {
            case SUN: {
                c.drawCircle(12 * k, 12 * k, 4.2f * k, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    float x1 = 12 + 6.2f, y1 = 12, x2 = 12 + 8.2f, y2 = 12;
                    float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    c.drawLine(12 * k + ca * 6.2f * k, 12 * k + sa * 6.2f * k,
                        12 * k + ca * 8.4f * k, 12 * k + sa * 8.4f * k, p);
                }
                break;
            }
            case MOON: {
                path.reset();
                r.set(4 * k, 4 * k, 20 * k, 20 * k);
                path.addCircle(12 * k, 12 * k, 8 * k, Path.Direction.CW);
                Path cut = new Path();
                cut.addCircle(16 * k, 9 * k, 7 * k, Path.Direction.CW);
                path.op(cut, Path.Op.DIFFERENCE);
                p.setStyle(Paint.Style.FILL);
                c.drawPath(path, p);
                break;
            }
            case PARTLY: {
                // A small sun peeking behind a cloud.
                c.drawCircle(8.5f * k, 8 * k, 3.4f * k, p);
                c.drawLine(8.5f * k, 2.2f * k, 8.5f * k, 3.4f * k, p);
                c.drawLine(8.5f * k, 12.6f * k, 8.5f * k, 13.8f * k, p);
                c.drawLine(2.7f * k, 8 * k, 3.9f * k, 8 * k, p);
                cloud(c, k, 13f, 15f, 7.5f, 6f);
                break;
            }
            case CLOUD:
                cloud(c, k, 12f, 13.5f, 8.5f, 7f);
                break;
            case FOG: {
                cloud(c, k, 12f, 10f, 8f, 6.2f);
                c.drawLine(5 * k, 17.5f * k, 19 * k, 17.5f * k, p);
                c.drawLine(7 * k, 20.5f * k, 17 * k, 20.5f * k, p);
                break;
            }
            case DRIZZLE: {
                cloud(c, k, 12f, 11f, 8f, 6.4f);
                c.drawLine(9 * k, 17.5f * k, 9 * k, 19 * k, p);
                c.drawLine(13 * k, 17.5f * k, 13 * k, 19 * k, p);
                break;
            }
            case RAIN: {
                cloud(c, k, 12f, 10.5f, 8f, 6.4f);
                c.drawLine(8.5f * k, 16.5f * k, 7.8f * k, 19.5f * k, p);
                c.drawLine(12.5f * k, 16.5f * k, 11.8f * k, 20.5f * k, p);
                c.drawLine(16.5f * k, 16.5f * k, 15.8f * k, 19.5f * k, p);
                break;
            }
            case SNOW: {
                cloud(c, k, 12f, 10.5f, 8f, 6.4f);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(8.5f * k, 18.5f * k, 1.1f * k, p);
                c.drawCircle(12.5f * k, 20.5f * k, 1.1f * k, p);
                c.drawCircle(16.5f * k, 18.5f * k, 1.1f * k, p);
                break;
            }
            default: {   // STORM
                cloud(c, k, 12f, 10f, 8f, 6.2f);
                path.reset();
                path.moveTo(12.8f * k, 15f * k);
                path.lineTo(9.8f * k, 20f * k);
                path.lineTo(12 * k, 20f * k);
                path.lineTo(10.8f * k, 23.5f * k);
                path.lineTo(15 * k, 18f * k);
                path.lineTo(12.6f * k, 18f * k);
                path.lineTo(14.2f * k, 15f * k);
                path.close();
                p.setStyle(Paint.Style.FILL);
                c.drawPath(path, p);
                break;
            }
        }
    }

    /** A filled cloud centred near (cx, cy): one big arc and one small, closed along a flat bottom. */
    private void cloud(Canvas c, float k, float cx, float cy, float w, float h) {
        path.reset();
        r.set((cx - w * 0.55f) * k, (cy - h * 0.9f) * k, (cx + w * 0.55f) * k, (cy + h * 0.5f) * k);
        path.arcTo(r, 160, 200);
        r.set((cx + w * 0.12f) * k, (cy - h * 1.15f) * k, (cx + w * 0.9f) * k, (cy + h * 0.25f) * k);
        path.arcTo(r, 210, 150);
        path.close();
        p.setStyle(Paint.Style.FILL);
        c.drawPath(path, p);
    }
}
