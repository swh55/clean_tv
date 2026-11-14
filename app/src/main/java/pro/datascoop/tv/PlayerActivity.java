package pro.datascoop.tv;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The recitation player: the reciter and chapter now playing, the position in the chapter — draggable along
 * the bar, left and right step 30 seconds — play/pause, the previous and next surah, the repeat mode (auto-
 * continue, repeat the chapter, stop after), and downloading the chapter for offline. The recitation itself
 * plays on across screens (Back returns and the sound carries on, exactly like the radio); every pause, stop
 * and few seconds of play are remembered, so Continue listening returns to the very spot.
 */
public class PlayerActivity extends Screen {

    private TextView reciterLine, surahName, surahSub, stateLine, timeLine;
    private Bar bar;
    private FocusTile playBtn, prevBtn, nextBtn, repeatBtn, downloadBtn, reciterBtn;
    private Icon playIcon;
    private TextView repeatLabel, downloadLabel;
    private FocusTile seekBtnBack, seekBtnFwd;
    private LinearLayout hints;
    private final Tilawa.Listener onTilawa = s -> render();
    private int surah;

    @Override
    void build() {
        newRoot();
        surah = getIntent().getIntExtra("surah", 0);

        // ---- header: back (the sound carries on)
        FocusTile back = new FocusTile(this, 36);
        back.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        Icon backIcon = new Icon(this, Icon.BACK, 2f);
        back.addView(backIcon, new FrameLayout.LayoutParams(Ui.ipx(32), Ui.ipx(32), Gravity.CENTER));
        back.setLook(f -> backIcon.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        back.setContentDescription(t.back());
        back.setOnClickListener(v -> finish());
        placeTile(back, 96, 56, 72, 72);

        // ---- the big card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.ipx(64), Ui.ipx(44), Ui.ipx(64), Ui.ipx(40));
        card.setClipChildren(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_800);
        bg.setCornerRadius(Ui.px(Tokens.RADIUS_PANEL));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        card.setBackground(bg);
        place(card, 240, 170, 1440, 620);

        reciterLine = caps(text(ui400, 22, Tokens.MIST_400));
        reciterLine.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(reciterLine);
        surahName = text(displayBold, 72, Tokens.PLASTER_100);
        surahName.setGravity(Gravity.CENTER_HORIZONTAL);
        surahName.setTextDirection(View.TEXT_DIRECTION_RTL);
        surahName.setMaxLines(1);
        lineHeight(surahName, 1.15f);
        card.addView(surahName, lp(6, 0));
        surahSub = text(ui400, 24, Tokens.GOLD_400);
        surahSub.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(surahSub, lp(10, 0));
        stateLine = text(ui400, 26, Tokens.MIST_400);
        stateLine.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(stateLine, lp(24, 0));

        // ---- the position bar, draggable
        bar = new Bar(this);
        card.addView(bar, lp(16, 0));
        timeLine = text(ui400, 22, Tokens.MIST_400);
        timeLine.setGravity(Gravity.CENTER_HORIZONTAL);
        timeLine.setTextDirection(View.TEXT_DIRECTION_LTR);
        card.addView(timeLine, lp(8, 0));

        card.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));

        // ---- the controls
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);
        controls.setClipChildren(false);
        float r = FocusTile.REACH;
        prevBtn = round(Icon.PREV);
        seekBtnBack = seekPill(-30);
        playBtn = round(Icon.PLAY);
        seekBtnFwd = seekPill(30);
        nextBtn = round(Icon.NEXT);
        controls.addView(prevBtn, ctrlLp());
        controls.addView(seekBtnBack, ctrlLp());
        controls.addView(playBtn, ctrlLp());
        controls.addView(seekBtnFwd, ctrlLp());
        controls.addView(nextBtn, ctrlLp());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, Ui.ipx(76 + 2 * r));
        clp.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(controls, clp);

        // ---- the mode row: repeat, download, reciter
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setGravity(Gravity.CENTER_HORIZONTAL);
        modes.setClipChildren(false);
        repeatBtn = pill(Icon.RADIO, t.plRepeatFollow());
        repeatBtn.setOnClickListener(v -> cycleRepeat());
        downloadBtn = pill(Icon.DOWNLOAD, t.plDownload());
        downloadBtn.setOnClickListener(v -> download());
        reciterBtn = pill(Icon.BOOK, t.rcTitle());
        reciterBtn.setOnClickListener(v -> startSafely(new Intent(this, RecitersActivity.class)));
        modes.addView(repeatBtn, pillLp());
        modes.addView(downloadBtn, pillLp());
        modes.addView(reciterBtn, pillLp());
        card.addView(modes, lp(8, 0));

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 96, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setHints(hints, hints2());

        // ---- wire up
        prevBtn.setOnClickListener(v -> {
            int s = Math.max(1, currentSurah() - 1);
            Tilawa.switchTo(this, Prefs.reciter(this), s);
        });
        playBtn.setOnClickListener(v -> {
            if (Tilawa.on()) {
                if (Tilawa.state() == Tilawa.PAUSED) Tilawa.resume();
                else if (Tilawa.state() == Tilawa.PLAYING) Tilawa.pause();
                else startPlay();
            } else {
                startPlay();
            }
        });
        nextBtn.setOnClickListener(v -> {
            int s = Math.min(Quran.SURAHS, currentSurah() + 1);
            Tilawa.switchTo(this, Prefs.reciter(this), s);
        });
        seekBtnBack.setOnClickListener(v -> Tilawa.seek(Tilawa.position() - 30_000));
        seekBtnFwd.setOnClickListener(v -> Tilawa.seek(Tilawa.position() + 30_000));

        render();
    }

    private List<String> hints2() {
        List<String> h = new ArrayList<>();
        h.add(t.hintPlayer());
        h.add(t.hintBackHome());
        return h;
    }

    // ---------------------------------------------------------------- playback

    /** Starts (or resumes) the chapter this screen was opened for. */
    private void startPlay() {
        String r = Tilawa.on() ? Tilawa.reciter() : Prefs.reciter(this);
        int s = currentSurah();
        int at = Tilawa.on() && Tilawa.surah() == s && Tilawa.reciter().equals(r) ? Tilawa.position()
            : Tilawa.savedPos(this, r, s);
        Tilawa.start(this, r, s, at);
    }

    private int currentSurah() {
        if (Tilawa.on() && surah == 0) return Tilawa.surah();
        if (surah == 0) {
            int[] last = UserDb.of(this).lastListened(Prefs.reciter(this));
            return last == null ? 1 : last[0];
        }
        return surah;
    }

    private void cycleRepeat() {
        int now = Tilawa.repeatMode();
        Tilawa.setRepeatMode(this, (now + 1) % 3);
        render();
    }

    private void download() {
        String r = Tilawa.on() ? Tilawa.reciter() : Prefs.reciter(this);
        int s = currentSurah();
        if (Downloads.running(this, r, s)) return;
        Downloads.start(this, r, s);
        render();
    }

    // ---------------------------------------------------------------- rendering

    /** Everything this screen shows, from the player's state. */
    private void render() {
        if (surahName == null) return;
        boolean on = Tilawa.on();
        String rId = on ? Tilawa.reciter() : Prefs.reciter(this);
        int s = currentSurah();
        final Reciters.Reciter r = Reciters.get(this).of(rId);
        if (r != null) reciterLine.setText(t.rcTitle() + t.sep() + r.name(t));

        // the playing chapter's name and line, once the database is open
        final int surahNow = on ? Tilawa.surah() : surahNow0();
        Quran.ready(this, () -> {
            Quran q = Quran.opened();
            if (q == null || surahName == null || isFinishing()) return;
            Quran.Surah info = q.surah(on ? Tilawa.surah() : surahNow);
            surahName.setText(t.ar ? info.name : info.nameEn);
            surahSub.setText((info.meccan ? t.qiMeccan() : t.qiMedinan()) + t.sep() + t.qiAyahs(info.ayahs));
        });

        int st = Tilawa.state();
        String reciterNow = on ? Tilawa.reciter() : Prefs.reciter(this);
        int surahPlaying = on ? Tilawa.surah() : surahNow0();
        if (!on) stateLine.setText("");
        else stateLine.setText(st == Tilawa.PLAYING
            ? (Downloads.file(this, reciterNow, surahPlaying) != null ? t.plLocal() : "")
            : st == Tilawa.CONNECTING || st == Tilawa.RETRYING ? t.plConnecting()
            : st == Tilawa.FAILED ? t.plError() : t.plPause());

        playIcon.setKind(on && st == Tilawa.PLAYING ? Icon.STOP : Icon.PLAY);
        int mode = Tilawa.repeatMode();
        repeatLabel.setText(mode == Tilawa.FOLLOW ? t.plRepeatFollow() : mode == Tilawa.REPEAT_ONE ? t.plRepeatOne() : t.plRepeatStop());

        // the download state of this chapter under this reciter
        int dl = UserDb.of(this).downloadStatus(rId, surahNow);
        float p = Downloads.progress(this, rId, surahNow);
        downloadLabel.setText(dl == UserDb.DL_DONE ? t.plDownloaded()
            : dl == UserDb.DL_RUNNING && p > 0 ? t.plDownloading((int) Math.floor(p * 100))
            : t.plDownload());

        bar.set(on ? Tilawa.position() : 0, on ? Tilawa.duration() : 0, st == Tilawa.PLAYING);
        timeLine.setText(t.d(Screen.duration((on ? Tilawa.position() : 0) / 1000L, false)) + " / "
            + t.d(Screen.duration((on ? Tilawa.duration() : 0) / 1000L, false)));
        bar.invalidate();
    }

    @Override
    void onTick() {
        if (Tilawa.on()) render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tilawa.listen(onTilawa);
        render();
    }

    @Override
    protected void onPause() {
        Tilawa.unlisten(onTilawa);
        super.onPause();
    }

    // ---------------------------------------------------------------- the position bar

    /** The chapter's position: a gold fill on a track, dragged by the finger-free remote (focus + ◀▶). */
    private final class Bar extends View {
        private int pos, dur;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Bar(android.content.Context c) {
            super(c);
            setFocusable(true);
            fill.setColor(Tokens.GOLD_400);
            track.setColor(Tokens.BANNER_TRACK);
            knob.setColor(Tokens.PLASTER_100);
            setOnFocusChangeListener((v, has) -> invalidate());
        }

        void set(int position, int duration, boolean playing) {
            pos = position;
            dur = duration;
        }

        @Override
        protected void onDraw(Canvas c) {
            float h = Ui.px(8);
            float y = getHeight() / 2f - h / 2;
            r.set(0, y, getWidth(), y + h);
            c.drawRoundRect(r, h, h, track);
            float w = dur <= 0 ? 0 : Math.max(0, Math.min(1, pos / (float) dur)) * getWidth();
            r.set(0, y, Math.max(w, 0), y + h);
            c.drawRoundRect(r, h, h, fill);
            if (isFocused()) {
                float kx = dur <= 0 ? 0 : Math.max(0, Math.min(1, pos / (float) dur)) * getWidth();
                c.drawCircle(Math.min(Math.max(kx, Ui.px(18)), getWidth() - Ui.px(18)), y + h / 2, Ui.px(14), knob);
            }
        }

        @Override
        public boolean onKeyDown(int keyCode, android.view.KeyEvent e) {
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT || keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                boolean back = (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) != rtl;
                Tilawa.seek(Tilawa.position() + (back ? -10_000 : 10_000));
                render();
                return true;
            }
            return super.onKeyDown(keyCode, e);
        }
    }

    // ---------------------------------------------------------------- chrome

    private int surahNow0() {
        return currentSurah();
    }

    private FocusTile round(int iconKind) {
        FocusTile b = new FocusTile(this, 999);
        b.setRest(Tokens.LAPIS_700, Tokens.LINE_SOFT, 2);
        Icon icon = new Icon(this, iconKind, 2f);
        b.addView(icon, new FrameLayout.LayoutParams(Ui.ipx(34), Ui.ipx(34), Gravity.CENTER));
        b.setLook(f -> icon.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        if (iconKind == Icon.PLAY) playIcon = icon;
        return b;
    }

    private FocusTile seekPill(int seconds) {
        FocusTile b = new FocusTile(this, 999);
        b.setRest(Tokens.LAPIS_700, Tokens.LINE_SOFT, 2);
        TextView v = text(ui500, 24, Tokens.PLASTER_100);
        v.setText(t.plSeek(seconds));
        v.setTextDirection(View.TEXT_DIRECTION_LTR);
        FrameLayout fl = new FrameLayout(this);
        fl.addView(v, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        b.addView(fl, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> v.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        return b;
    }

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
        if (iconKind == Icon.RADIO) repeatLabel = v;
        if (iconKind == Icon.DOWNLOAD) downloadLabel = v;
        return b;
    }

    private LinearLayout.LayoutParams ctrlLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.ipx(76 + 2 * FocusTile.REACH), Ui.ipx(76 + 2 * FocusTile.REACH));
        lp.setMarginStart(Ui.ipx(16 - 2 * FocusTile.REACH));
        return lp;
    }

    private LinearLayout.LayoutParams pillLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, Ui.ipx(60 + 2 * FocusTile.REACH));
        lp.setMarginStart(Ui.ipx(16 - 2 * FocusTile.REACH));
        return lp;
    }

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }
}
