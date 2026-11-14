package pro.datascoop.tv;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Choosing the reciter: a card per reciter with a drawn monogram plate (no photos are shipped — the launcher
 * stays offline-first), his name, the reading he is known by, the preview of Al-Fatihah, and what is downloaded
 * of him. OK makes him the default reciter — saved for the player and Continue listening; the preview plays a
 * short passage and stops on its own (or with the Stop chip), like the adhan voice preview in Settings.
 */
public class RecitersActivity extends Screen {

    private LinearLayout list;
    private LinearLayout hints;
    private MediaPlayer preview;
    private FocusTile previewing;
    private String previewingId;

    @Override
    void build() {
        newRoot();
        stopPreview();

        FocusTile back = new FocusTile(this, 36);
        back.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        Icon backIcon = new Icon(this, Icon.BACK, 2f);
        back.addView(backIcon, new FrameLayout.LayoutParams(Ui.ipx(32), Ui.ipx(32), Gravity.CENTER));
        back.setLook(f -> backIcon.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        back.setContentDescription(t.back());
        back.setOnClickListener(v -> finish());
        placeTile(back, 96, 56, 72, 72);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(displayBold, 56, Tokens.PLASTER_100);
        title.setText(t.rcTitle());
        lineHeight(title, 1f);
        head.addView(title);
        TextView sub = text(ui400, 22, Tokens.MIST_400);
        sub.setText(t.rcRewaya());
        head.addView(sub, lp(4, 0));
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, 700, 84);

        FocusTile manage = chip(Icon.DOWNLOAD, t.rcManage());
        manage.setOnClickListener(v -> startSafely(new Intent(this, DownloadsActivity.class)));
        placeEnd(manage, 96, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 72);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Ui.ipx(8), Ui.ipx(4), Ui.ipx(8), Ui.ipx(48));
        list.setClipChildren(false);
        scroll.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        place(scroll, 96, 200, 1728, 740);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 96, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setHints(hints, hints2());

        fill();
    }

    private List<String> hints2() {
        List<String> h = new ArrayList<>();
        h.add(t.hintOk(t.rcTitle()));
        h.add(t.hintBackHome());
        return h;
    }

    private void fill() {
        final RecitersActivity self = this;
        Lib.io(() -> {
            final List<int[]> counts = new ArrayList<>();
            for (Reciters.Reciter r : Reciters.get(this).all()) {
                counts.add(new int[] {UserDb.of(this).downloads(r.id).size()});
            }
            Lib.main(() -> {
                if (self.isFinishing() || list == null) return;
                list.removeAllViews();
                int i = 0;
                for (Reciters.Reciter r : Reciters.get(this).all()) {
                    list.addView(reciterCard(r, counts.get(i)[0]), lp(0, 14));
                    i++;
                }
                if (list.getChildCount() > 0) list.getChildAt(0).requestFocus();
            });
        }, null);
    }

    /** One reciter's card: monogram, name, reading, downloads, preview chip, chosen ring. */
    private View reciterCard(Reciters.Reciter r, int downloads) {
        FocusTile tile = new FocusTile(this, Tokens.RADIUS_ROW);
        boolean chosen = r.id.equals(Prefs.reciter(this));
        tile.setRest(Tokens.LAPIS_800, chosen ? Tokens.GOLD_400 : Tokens.LINE_SOFT, 2);
        tile.setFocusScale(Tokens.FOCUS_SCALE_ROW);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(28), Ui.ipx(22), Ui.ipx(28), Ui.ipx(20));
        col.setClipChildren(false);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        // the monogram plate: the reciter's initial letter on gold, in Amiri
        FrameLayout plate = new FrameLayout(this);
        GradientDrawable pb = new GradientDrawable();
        pb.setColor(chosen ? Tokens.GOLD_400 : Tokens.LAPIS_700);
        pb.setCornerRadius(Ui.px(Tokens.RADIUS_CELL));
        plate.setBackground(pb);
        TextView mono = text(displayBold, 40, chosen ? Tokens.LAPIS_900 : Tokens.GOLD_400);
        mono.setText(r.name(t).substring(0, 1));
        mono.setGravity(Gravity.CENTER);
        plate.addView(mono, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        top.addView(plate, new LinearLayout.LayoutParams(Ui.ipx(84), Ui.ipx(84)));

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(displayBold, 34, Tokens.PLASTER_100);
        name.setText(r.name(t));
        name.setTextDirection(View.TEXT_DIRECTION_RTL);
        words.addView(name);
        TextView line = text(ui400, 20, Tokens.MIST_400);
        line.setText(t.rcRewaya() + t.sep() + (downloads > 0 ? t.rcDownloads(downloads) : ""));
        line.setMaxLines(1);
        line.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(line, lp(4, 0));
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, -2, 1);
        wlp.setMarginStart(Ui.ipx(24));
        top.addView(words, wlp);
        col.addView(top);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setClipChildren(false);
        FocusTile prev = pill(Icon.PLAY, t.rcPreview());
        prev.setOnClickListener(v -> preview(r, prev));
        FocusTile stop = pill(Icon.STOP, t.rcStop());
        stop.setOnClickListener(v -> stopPreview());
        float rr = FocusTile.REACH;
        chips.addView(prev, new LinearLayout.LayoutParams(-2, Ui.ipx(60 + 2 * rr)));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, Ui.ipx(60 + 2 * rr));
        slp.setMarginStart(Ui.ipx(16 - 2 * rr));
        chips.addView(stop, slp);
        col.addView(chips, lp(14, 0));

        tile.addView(col, new FrameLayout.LayoutParams(-1, -1));
        tile.setLook(f -> name.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        tile.setOnClickListener(v -> {
            Prefs.setReciter(this, r.id);
            fill();
        });
        tile.setContentDescription(r.name(t));
        return tile;
    }

    // ---------------------------------------------------------------- preview

    /** Plays a short passage of Al-Fatihah from the reciter's server; it ends on its own. */
    private void preview(Reciters.Reciter r, FocusTile from) {
        stopPreview();
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build());
        try {
            mp.setDataSource(r.url(1));
            mp.prepareAsync();
            mp.setOnPreparedListener(p -> {
                if (preview != p) {
                    p.release();
                    return;
                }
                p.start();
                // a passage is enough: stop by itself after twenty seconds
                new android.os.Handler(getMainLooper()).postDelayed(() -> {
                    if (preview == p) stopPreview();
                }, 20_000);
            });
            mp.setOnCompletionListener(p -> stopPreview());
            mp.setOnErrorListener((p, what, extra) -> {
                stopPreview();
                return true;
            });
            preview = mp;
            previewing = from;
            previewingId = r.id;
        } catch (Exception e) {
            mp.release();
        }
    }

    private void stopPreview() {
        if (preview != null) {
            try {
                preview.stop();
            } catch (RuntimeException ignored) {
            }
            preview.release();
            preview = null;
        }
        previewing = null;
        previewingId = null;
    }

    @Override
    protected void onPause() {
        stopPreview();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopPreview();
        super.onDestroy();
    }

    @Override
    void onTick() {
    }

    // ---------------------------------------------------------------- chrome

    private FocusTile pill(int iconKind, String label) {
        FocusTile b = new FocusTile(this, 34);
        b.setRest(Tokens.LAPIS_900, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(24), 0, Ui.ipx(28), 0);
        Icon icon = new Icon(this, iconKind, 1.8f);
        TextView v = text(ui400, 22, Tokens.PLASTER_100);
        v.setText(label);
        v.setSingleLine(true);
        row.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(24), Ui.ipx(24)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(10));
        row.addView(v, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
            v.setTextColor(c);
            icon.setColor(c);
        });
        return b;
    }

    private FocusTile chip(int iconKind, String label) {
        return pill(iconKind, label);
    }

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }
}
