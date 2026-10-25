package pro.datascoop.tv;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Madhab;

/**
 * Interface strings for the five languages — Arabic, English, French, Turkish and Urdu — each string picked by
 * {@link #p}, taken from the `t` tables in design/Home.dc.html. Arabic and Urdu run right to left ({@link #ar});
 * Eastern Arabic digits (٠١٢٣) are their own setting, on by default in both of them.
 */
final class Text {
    static final int AR = 0, EN = 1, FR = 2, TR = 3, UR = 4;

    final int lang;
    /** Right-to-left: Arabic and Urdu. Drives layout direction and the display typeface. */
    final boolean ar;
    /** Eastern Arabic digits (٠١٢٣). Its own setting; defaults to on in Arabic and Urdu. */
    final boolean eastern;

    Text(int lang, boolean eastern) {
        this.lang = lang;
        this.ar = lang == AR || lang == UR;
        this.eastern = eastern;
    }

    /** The string in the screen's language; every argument is required, so a new language is never half done. */
    private static String p(int lang, String ar, String en, String fr, String tr, String ur) {
        switch (lang) {
            case AR: return ar;
            case FR: return fr;
            case TR: return tr;
            case UR: return ur;
            default: return en;
        }
    }

    private String p(String ar, String en, String fr, String tr, String ur) {
        return p(lang, ar, en, fr, tr, ur);
    }

    private String[] pa(String[] ar, String[] en, String[] fr, String[] tr, String[] ur) {
        switch (lang) {
            case AR: return ar;
            case FR: return fr;
            case TR: return tr;
            case UR: return ur;
            default: return en;
        }
    }

    String d(String s) {
        if (!eastern) return s;
        // "·" beside Eastern digits reads as a zero (٠); Arabic punctuation takes its own comma instead.
        s = s.replace(" · ", "، ");
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= '0' && c <= '9' ? (char) ('٠' + (c - '0')) : c);
        }
        return b.toString();
    }

    /** The " · " between parts of a line; a comma where "·" would sit beside Eastern digits. */
    String sep() { return d(" · "); }

    /** The arch verse's reference (An-Nisa 4:103), in the screen's digits like every other number. */
    String ref() { return p(d("النساء 103"), d("An-Nisa 4:103"), d("An-Nisa 4:103"), d("Nisâ 4:103"), d("النساء 103")); }
    String nextPrayer() { return p("الصلاة القادمة", "NEXT PRAYER", "PROCHAINE PRIÈRE", "SONRAKİ NAMAZ", "اگلی نماز"); }
    String today() { return p("مواقيت اليوم والقبلة", "Today & Qibla", "Aujourd'hui & qibla", "Bugün & kıble", "آج و قبلہ"); }
    String settings() { return p("الإعدادات", "Settings", "Réglages", "Ayarlar", "ترتیبات"); }
    /** The saved city in this language; set by {@link Screen#loadText}. */
    String cityName = "";
    String city() { return cityName; }
    String fav() { return p("المفضلة", "FAVOURITES", "FAVORIS", "FAVORİLER", "پسندیدہ"); }
    String more() { return p("المزيد", "MORE", "PLUS", "DAHA FAZLA", "مزید"); }
    /** Home's tile: the label over the count. */
    String all(int n) { return d(p("كل التطبيقات", "All apps", "Toutes les applis", "Tüm uygulamalar", "تمام ایپس") + "\n" + n); }
    String ampm(boolean am) {
        return p(am ? "ص" : "م", am ? "AM" : "PM", am ? "AM" : "PM", am ? "ÖÖ" : "ÖS", am ? "ص" : "ش");
    }
    String adhanAt() { return p("الأذان", "Adhan at", "Adhan à", "Ezan", "اذان"); }
    String nowTag() { return p("الآن", "NOW", "MAINTENANT", "ŞİMDİ", "ابھی"); }
    String nextTag() { return p("التالية", "NEXT", "SUIVANTE", "SONRAKİ", "اگلی"); }
    String jumuahLabel() { return p("صلاة الجمعة", "JUMU'AH", "JUMU'AH", "CUMA NAMAZI", "جمعہ کی نماز"); }
    String jumuah() { return p("الجمعة", "Jumu'ah", "Jumu'ah", "Cuma", "جمعہ"); }
    String kahf() { return p("سورة الكهف اليوم", "Surat Al-Kahf today", "Sourate Al-Kahf aujourd'hui", "Bugün Kehf Suresi", "آج سورۃ الکہف"); }
    String iftarIn() { return p("الإفطار بعد", "IFTAR IN", "IFTAR DANS", "İFTARA KALAN", "افطار میں"); }
    String iftar() { return p("الإفطار", "Iftar", "Iftar", "İftar", "افطار"); }
    String iftarTag() { return p("الإفطار", "IFTAR", "IFTAR", "İFTAR", "افطار"); }
    String suhoorTag() { return p("الإمساك", "SUHOOR", "SUHOOR", "İMSAK", "سحری"); }
    String suhoorEnds(String time) { return d(p("السحور حتى " + time, "Suhoor ends " + time, "Suhoor finit " + time, "Sahur " + time + " biter", "سحری " + time + " تک")); }
    String comingSoon() {
        return p("هذه الشاشة قادمة في التحديث التالي", "This screen comes in the next update",
            "Cet écran arrive dans la prochaine mise à jour", "Bu ekran sonraki güncellemede gelecek",
            "یہ اسکرین اگلی اپ ڈیٹ میں آئے گی");
    }

    String hintOk(String what) { return "OK  " + what; }
    String hintApps() { return p("◀  التطبيقات", "▶  Apps", "▶  Applis", "▶  Uygulamalar", "◀  ایپس"); }
    String hintMore() { return p("▼  المزيد", "▼  More", "▼  Plus", "▼  Daha fazla", "▼  مزید"); }
    String hintFavs() { return p("▲  المفضلة", "▲  Favourites", "▲  Favoris", "▲  Favoriler", "▲  پسندیدہ"); }
    String open(String app) { return p("فتح " + app, "Open " + app, "Ouvrir " + app, app + " aç", app + " کھولیں"); }
    String network() { return p("إعدادات الشبكة", "Network settings", "Réseau", "Ağ ayarları", "نیٹ ورک ترتیبات"); }

    // Screensaver (design/Screensaver.dc.html)
    /**
     * "in 27 min". Arabic counts take their proper forms: دقيقة واحدة، دقيقتين، ٣–١٠ دقائق، ١١+ دقيقة. An hour
     * or more reads as h:mm, as on Prayer day.
     */
    String inMinutes(long minutes, String hoursAndMinutes) {
        if (minutes >= 60) return in(hoursAndMinutes);
        if (lang == EN || lang == FR) return d(p("", "", "", "", "") + (lang == FR ? "dans " + minutes + " min" : "in " + minutes + " min"));
        if (lang == TR) return d(minutes + " dk sonra");
        if (minutes <= 1) return p("بعد دقيقة واحدة", "", "", "", "ایک منٹ بعد");
        if (minutes == 2) return p("بعد دقيقتين", "", "", "", "دو منٹ بعد");
        return d(p("بعد " + minutes + (minutes <= 10 ? " دقائق" : " دقيقة"), "", "", "",
            minutes <= 10 ? minutes + " منٹ بعد" : minutes + " منٹ بعد"));
    }

    // Reminder and call to prayer (design/Home.dc.html approach/now, design/ApproachVideo.dc.html)
    String apLabel() { return p("اقتربت الصلاة", "PRAYER APPROACHING", "PRIÈRE PROCHE", "NAMAZ YAKLAŞIYOR", "نماز قریب ہے"); }
    /** "Asr in 13 minutes". */
    String apTitle(String prayer, long minutes, String hoursAndMinutes) {
        if (lang == EN && minutes < 60) return d(prayer + " in " + minutes + (minutes == 1 ? " minute" : " minutes"));
        if (lang == FR && minutes < 60) return d(prayer + " dans " + minutes + (minutes == 1 ? " minute" : " minutes"));
        if (lang == TR && minutes < 60) return d(prayer + " " + minutes + " dk sonra");
        return prayer + " " + inMinutes(minutes, hoursAndMinutes);
    }
    String apSub(CharSequence time) {
        return d(p("الأذان " + time + " · رجوع للإغلاق", "Adhan at " + time + " · Back to dismiss",
            "Adhan à " + time + " · Retour pour fermer", "Ezan " + time + " · Kapatmak için Geri",
            "اذان " + time + " · بند کرنے کے لیے Back"));
    }
    String apOk() { return p("حسنًا", "Got it", "D'accord", "Anladım", "ٹھیک ہے"); }
    String bannerSub(CharSequence time, int seconds) {
        return d(p("الأذان " + time + " · يختفي خلال " + seconds + " ث", "Adhan " + time + " · hides in " + seconds + " s",
            "Adhan " + time + " · disparaît en " + seconds + " s", "Ezan " + time + " · " + seconds + " sn içinde kapanır",
            "اذان " + time + " · " + seconds + " سیکنڈ میں چھپ جائے گا"));
    }
    String nowLabel() { return p("حان الآن وقت صلاة", "IT'S TIME FOR", "C'EST L'HEURE DE", "NAMAZ ZAMANI", "اب نماز کا وقت ہے"); }
    String play() { return p("تشغيل الأذان", "Play adhan", "Jouer l'adhan", "Ezanı çal", "اذان چلائیں"); }
    String stop() { return p("إيقاف الأذان", "Stop adhan", "Arrêter l'adhan", "Ezanı durdur", "اذان روکیں"); }
    String dismiss() { return p("إغلاق", "Dismiss", "Fermer", "Kapat", "بند کریں"); }
    // Radio: Home's pill and card, the screen-off view, the screensaver line and the Settings section.
    String radio() { return p("الإذاعة", "Radio", "Radio", "Radyo", "ریڈیو"); }
    String station(Radio.Station s) { return d(ar ? s.ar : s.en); }
    String stationWhere(Radio.Station s) { return d(ar ? s.whereAr : s.whereEn); }
    /** A station in a list: its name and place, since several are called إذاعة القرآن الكريم. */
    String stationChoice(Radio.Station s) { return s.reciter ? station(s) : station(s) + sep() + stationWhere(s); }
    String stationRow() { return p("المحطة", "Station", "Station", "İstasyon", "اسٹیشن"); }
    String[] stationGroups() {
        return pa(new String[] {"إذاعات القرآن الكريم", "القرّاء"},
                  new String[] {"Quran radio stations", "Reciters"},
                  new String[] {"Radios coraniques", "Récitateurs"},
                  new String[] {"Kur'an radyoları", "Hafızlar"},
                  new String[] {"قرآن ریڈیو اسٹیشن", "قاری"});
    }
    String radioStatus(int s) {
        switch (s) {
            case Radio.PLAYING: return p("على الهواء", "On air", "À l'antenne", "Yayında", "نشر جاری");
            case Radio.CONNECTING: return p("جارٍ الاتصال…", "Connecting…", "Connexion…", "Bağlanıyor…", "جڑ رہا ہے…");
            case Radio.RETRYING: return p("انقطع البث · نعيد المحاولة", "Signal lost · retrying", "Signal perdu · nouvelle tentative",
                "Sinyal kesildi · yeniden deneniyor", "سگنل منقطع · دوبارہ کوشش");
            case Radio.PAUSED: return p("متوقفة للأذان", "Paused for the adhan", "Pause pour l'adhan", "Ezan için duraklatıldı", "اذان کے لیے رکا");
            default: return p("متوقفة", "Off", "Arrêtée", "Kapalı", "بند");
        }
    }
    String radioPlay() { return p("تشغيل", "Play", "Lecture", "Çal", "چلائیں"); }
    String radioStop() { return p("إيقاف", "Stop", "Arrêter", "Durdur", "روکیں"); }
    String screenOff() { return p("إطفاء الشاشة", "Screen off", "Écran off", "Ekranı kapat", "اسکرین بند"); }
    String radioNote() {
        return d(p("تستمر في الرئيسية وشاشة التوقف · تتوقف إذا شغّل تطبيق آخر صوتًا",
                   "Keeps playing on Home and the screensaver · stops when another app plays sound",
                   "Continue sur l'accueil et l'écran de veille · s'arrête si une autre appli joue du son",
                   "Ana ekranda ve ekran koruyucuda çalar · başka bir uygulama ses çalarsa durur",
                   "ہوم اور اسکرین سیور پر چلتی رہتی ہے · دوسری ایپ آواز چلائے تو رک جاتی ہے"));
    }
    String screenOffNote() {
        return d(p("الشاشة مطفأة والإذاعة تعمل · اضغط أي زر للعودة", "Screen off, radio playing · press any key to come back",
            "Écran off, radio en lecture · appuyez sur une touche", "Ekran kapalı, radyo çalıyor · dönmek için bir tuşa basın",
            "اسکرین بند، ریڈیو چل رہا ہے · واپس آنے کے لیے کوئی بٹن دبائیں"));
    }
    String hintClose() { return p("رجوع  إغلاق", "Back  Close", "Retour  Fermer", "Geri  Kapat", "Back  بند کریں"); }
    String sectionRadio() { return p("الإذاعة", "Radio", "Radio", "Radyo", "ریڈیو"); }
    String radioRowHelp(Radio.Station s) {
        return stationWhere(s) + p(" · تحتاج إلى الإنترنت", " · needs the internet", " · nécessite Internet",
            " · internet gerekir", " · انٹرنیٹ درکار");
    }
    String afterAdhanRow() { return p("بعد الأذان", "After the adhan", "Après l'adhan", "Ezandan sonra", "اذان کے بعد"); }
    String afterAdhanHelp() {
        return p("تتوقف الإذاعة أثناء الأذان ثم…", "The radio pauses for the adhan, then…",
            "La radio s'interrompt pour l'adhan, puis…", "Radyo ezan sırasında durur, sonra…", "اذان میں ریڈیو رک جاتی ہے، پھر…");
    }
    String[] afterAdhanChoices() {
        return pa(new String[] {"تُستأنف", "تبقى متوقفة"}, new String[] {"Resumes", "Stays off"},
                  new String[] {"Reprend", "Reste arrêtée"}, new String[] {"Devam eder", "Kapalı kalır"},
                  new String[] {"دوبارہ چلے", "بند رہے"});
    }
    String radioDarkRow() { return p("إطفاء الشاشة أثناء الاستماع", "Screen off while listening", "Écran off pendant l'écoute",
        "Dinlerken ekran kapansın", "سنتے وقت اسکرین بند"); }
    String radioDarkHelp() {
        return d(p("توفير الكهرباء: تنطفئ الشاشة وتستمر الإذاعة · أي زر يعيدها",
                   "Saves power: the screen goes dark, the radio keeps playing · any key brings it back",
                   "Économie : l'écran s'éteint, la radio continue · une touche la rallume",
                   "Güç tasarrufu: ekran kapanır, radyo çalar · bir tuş geri getirir",
                   "بجلی کی بچت: اسکرین بند، ریڈیو جاری · کوئی بٹن واپس لا دیتا ہے"));
    }
    String[] radioDarkChoices() {
        return pa(new String[] {"مع شاشة التوقف", "بعدها بـ 15 د", "أبدًا"}, new String[] {"With screensaver", "15 min later", "Never"},
                  new String[] {"Avec l'écran de veille", "15 min après", "Jamais"},
                  new String[] {"Ekran koruyucuyla", "15 dk sonra", "Hiçbir zaman"},
                  new String[] {"اسکرین سیور کے ساتھ", "15 منٹ بعد", "کبھی نہیں"});
    }

    /** The call screen's hint: how it closes by itself (Settings → Reminders); 0 is when the adhan ends. */
    String nowHint(int minutes) {
        if (minutes == 0)
            return d(p("رجوع للإغلاق · يُغلق تلقائيًا بعد الأذان", "Back to dismiss · closes by itself after the adhan",
                "Retour pour fermer · se ferme après l'adhan", "Kapatmak için Geri · ezandan sonra kapanır",
                "بند کرنے کے لیے Back · اذان کے بعد خود بند"));
        return d(p("رجوع للإغلاق · يُغلق تلقائيًا بعد " + (minutes == 1 ? "دقيقة" : minutes + " دقائق"),
                   "Back to dismiss · closes by itself after " + minutes + " min",
                   "Retour pour fermer · se ferme après " + minutes + " min",
                   "Kapatmak için Geri · " + minutes + " dk sonra kapanır",
                   "بند کرنے کے لیے Back · " + minutes + " منٹ بعد خود بند"));
    }
    String callCloseRow() { return p("إغلاق شاشة الأذان", "Close the call screen", "Fermer l'écran d'adhan", "Ezan ekranını kapat", "اذان اسکرین بند کریں"); }
    String callCloseHelp() { return p("ثم تعود إلى ما كنت تشاهده", "Then back to what you were watching",
        "Puis retour à ce que vous regardiez", "Sonra izlediğinize döner", "پھر جو دیکھ رہے تھے اسی پر واپس"); }
    /** In the order of the row's minutes: 0 (after the adhan), 1, 5, 10. */
    String[] callCloseChoices() {
        return pa(new String[] {"بعد الأذان", "1 د", "5 د", "10 د"}, new String[] {"After adhan", "1 min", "5 min", "10 min"},
                  new String[] {"Après adhan", "1 min", "5 min", "10 min"},
                  new String[] {"Ezandan sonra", "1 dk", "5 dk", "10 dk"},
                  new String[] {"اذان کے بعد", "1 منٹ", "5 منٹ", "10 منٹ"});
    }
    String voiceRow() { return p("صوت الأذان", "Adhan voice", "Voix de l'adhan", "Ezan sesi", "اذان کی آواز"); }
    String voiceHelp() {
        return p("لكل الصلوات عدا الفجر · يُسمع عند التنقل في القائمة", "Every prayer but Fajr · plays as you move through the list",
            "Toutes les prières sauf Fajr · joue en parcourant la liste", "Fajr hariç her namaz · listede gezerken çalar",
            "فجر کے سوا ہر نماز · فہرست میں حرکت پر سنائی دیتا ہے");
    }
    String fajrVoiceRow() { return p("أذان الفجر", "Fajr adhan", "Adhan du Fajr", "Fajr ezanı", "فجر کا اذان"); }
    String fajrVoiceHelp() {
        return p("يُفضَّل أذان فيه «الصلاة خير من النوم»", "Best with “prayer is better than sleep”",
            "Idéal avec « la prière vaut mieux que le sommeil »", "“Namaz uyumdan hayırlıdır” ile en iyisi",
            "«نماز نیند سے بہتر ہے» والا بہترین");
    }

    // Iqamah (Settings → Reminders; the call screen's countdown)
    String iqamahRow() { return p("عدّاد الإقامة", "Iqamah countdown", "Compte à rebours de l'iqaama", "Kâmet sayacı", "اقامتہ کاؤنٹر"); }
    String iqamahHelp() {
        return p("يعدّ على شاشة الأذان من وقت الصلاة", "Counts on the call screen from the prayer's time",
            "Compte sur l'écran d'adhan depuis l'heure de la prière", "Ezan ekranında namaz vaktinden sayar",
            "نماز کے وقت سے اذان اسکرین پر گنتا ہے");
    }
    /** In the row's order: 0 (off), 10, 15, 20, 30 minutes. */
    String[] iqamahChoices() {
        return pa(new String[] {"مغلق", "10 د", "15 د", "20 د", "30 د"},
                  new String[] {"Off", "10 min", "15 min", "20 min", "30 min"},
                  new String[] {"Arrêté", "10 min", "15 min", "20 min", "30 min"},
                  new String[] {"Kapalı", "10 dk", "15 dk", "20 dk", "30 dk"},
                  new String[] {"بند", "10 منٹ", "15 منٹ", "20 منٹ", "30 منٹ"});
    }
    String iqamahIn(String duration) { return d(p("الإقامة بعد " + duration, "Iqamah in " + duration,
        "Iqaama dans " + duration, "Kâmet " + duration + " sonra", "اقامتہ " + duration + " بعد")); }
    String iqamahNow() { return p("حان وقت الإقامة", "It's time for the Iqamah", "C'est l'heure de l'iqaama", "Kâmet vakti", "اقامتہ کا وقت ہو گیا"); }

    // A short tone with the reminder (Settings → Reminders)
    String toneRow() { return p("نغمة مع التنبيه", "Reminder tone", "Sonnerie du rappel", "Hatırlatma tonu", "یاد دہانی کی آواز"); }
    String toneHelp() {
        return p("نغمة قصيرة تُسمع مع بطاقة التنبيه", "A short tone with the reminder card",
            "Une courte sonnerie avec la carte de rappel", "Hatırlatma kartıyla kısa bir ton", "یاد دہانی کارڈ کے ساتھ مختصر آواز");
    }

    // All apps (design/AllApps.dc.html)
    String allTitle() { return p("كل التطبيقات", "All apps", "Toutes les applis", "Tüm uygulamalar", "تمام ایپس"); }
    String back() { return p("رجوع", "Back", "Retour", "Geri", "واپس"); }
    /** "Asr in", followed by the countdown. */
    String chip(String prayer) { return p(prayer + " بعد", prayer + " in", prayer + " dans", prayer + " sonra", prayer + " بعد"); }
    String favNote(int position) {
        String[] ord = {"الأول", "الثاني", "الثالث", "الرابع"};
        return p("في المفضلة · " + ord[position - 1], d("In favourites · position " + position),
            d("En favoris · position " + position), d("Favorilerde · " + position + ". sıra"),
            "پسندیدہ میں · نمبر " + position);
    }
    String notFav() {
        return p("اضغط مطولًا على OK لوضعه في المفضلة", "Hold OK to put it in favourites",
            "Maintenez OK pour le mettre en favoris", "Favorilere eklemek için OK'yi basılı tutun",
            "پسندیدہ میں رکھنے کے لیے OK دبائیں");
    }
    String hintOpen() { return p("OK  فتح", "OK  Open", "OK  Ouvrir", "OK  Aç", "OK  کھولیں"); }
    String hintHold() { return p("اضغط مطولًا  ترتيب المفضلة", "Hold OK  Arrange favourites", "Maintenir OK  Trier les favoris",
        "Basılı tut  Favorileri sırala", "دبائیں  ترتیب دیں"); }
    String moveEarlier() { return p("تقديمه في المفضلة", "Move earlier in favourites", "Remonter dans les favoris",
        "Favorilerde öne al", "پسندیدہ میں آگے کریں"); }
    String moveLater() { return p("تأخيره في المفضلة", "Move later in favourites", "Reculer dans les favoris",
        "Favorilerde geriye al", "پسندیدہ میں پیچھے کریں"); }
    String putInPlaceOf(String app) {
        return p("ضعه في المفضلة مكان " + app, "Put in favourites instead of " + app,
            "Mettre en favoris à la place de " + app, app + " yerine favorilere koy", app + " کی جگہ پسندیدہ میں رکھیں");
    }
    String cancel() { return p("إلغاء", "Cancel", "Annuler", "İptal", "منسوخ"); }
    String favourite() { return p("مفضل", "Favourite", "Favori", "Favori", "پسندیدہ"); }

    // Prayer day (design/PrayerDay.dc.html)
    String dayTitle() { return p("مواقيت اليوم", "Today's prayers", "Prières du jour", "Bugünün namazları", "آج کی نمازیں"); }
    String home() { return p("الرئيسية", "Home", "Accueil", "Ana ekran", "ہوم"); }
    String school(Madhab m) {
        if (m == Madhab.HANAFI) return p("العصر: الحنفي", "Asr: Hanafi", "Asr : hanafite", "Asr: Hanefi", "عصر: حنفی");
        return p("العصر: الجمهور (شافعي)", "Asr: standard (Shafi‘i)", "Asr : standard (chafiite)", "Asr: standart (Şafii)", "عصر: جمہور (شافعی)");
    }

    String methodName(CalculationMethod m) {
        switch (m) {
            case UMM_AL_QURA: return p("أم القرى، مكة المكرمة", "Umm al-Qura, Makkah", "Oum al-Qoura, La Mecque", "Umm al-Qura, Mekke", "ام القرٰی، مکہ مکرمہ");
            case MUSLIM_WORLD_LEAGUE: return p("رابطة العالم الإسلامي", "Muslim World League", "Ligue islamique mondiale", "İslam Dünyası Ligi", "مسلم ورلڈ لیگ");
            case KARACHI: return p("جامعة العلوم الإسلامية، كراتشي", "University of Islamic Sciences, Karachi", "Université des sciences islamiques, Karachi",
                "İslam Bilimleri Üniversitesi, Karaçi", "اسلامک سائنسز یونیورسٹی، کراچی");
            case DUBAI: return p("دبي", "Dubai", "Dubaï", "Dubai", "دبئی");
            case KUWAIT: return p("الكويت", "Kuwait", "Koweït", "Kuveyt", "کویت");
            case QATAR: return p("قطر", "Qatar", "Qatar", "Katar", "قطر");
            case NORTH_AMERICA: return p("الجمعية الإسلامية لأمريكا الشمالية", "ISNA, North America", "ISNA, Amérique du Nord",
                "ISNA, Kuzey Amerika", "ISNA، شمالی امریکہ");
            case MOON_SIGHTING_COMMITTEE: return p("لجنة رؤية الهلال", "Moonsighting Committee", "Comité d'observation du croissant",
                "Hilal Gözlem Komitesi", "ہلال دیکھنے کی کمیٹی");
            case SINGAPORE: return p("سنغافورة", "Singapore", "Singapour", "Singapur", "سنگاپور");
            default: return p("الهيئة المصرية العامة للمساحة", "Egyptian General Authority of Survey", "Autorité égyptienne générale du survey",
                "Mısır Genel Harita Kurumu", "مصروی سروے جنرل اتھارٹی");
        }
    }

    /** "Fajr angle 19.5° · Isha 17.5°", or Isha as minutes after Maghrib where the method fixes it that way. */
    String methodAngles(CalculationMethod m) {
        CalculationParameters params = m.getParameters();
        String fajr = p("زاوية الفجر ", "Fajr angle ", "Angle du Fajr ", "Fajr açısı ", "فجر کا زاویہ ") + trim(params.fajrAngle) + "°";
        String isha = params.ishaInterval > 0
            ? p("العشاء بعد المغرب بـ " + params.ishaInterval + " دقيقة", "Isha " + params.ishaInterval + " min after Maghrib",
                "Isha " + params.ishaInterval + " min après le Maghrib", "Isha, Maghrib'den " + params.ishaInterval + " dk sonra",
                "عشاء مغرب کے " + params.ishaInterval + " منٹ بعد")
            : p("العشاء ", "Isha ", "Isha ", "Isha ", "عشاء ") + trim(params.ishaAngle) + "°";
        return d(fajr + " · " + isha);
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    // Settings (design/Settings.dc.html)
    String sectionPrayer() { return p("مواقيت الصلاة", "Prayer times", "Heures de prière", "Namaz vakitleri", "نماز کے اوقات"); }
    String sectionReminders() { return p("التنبيهات والأذان", "Reminders & adhan", "Rappels & adhan", "Hatırlatmalar & ezan", "یاد دہانی و اذان"); }
    String sectionDisplay() { return p("العرض واللغة", "Display & language", "Affichage & langue", "Görünüm & dil", "ڈسپلے و زبان"); }
    String sectionNetwork(String ssid) { return ssid == null ? p("الشبكة", "Network", "Réseau", "Ağ", "نیٹ ورک") : p("الشبكة · ", "Network · ", "Réseau · ", "Ağ · ", "نیٹ ورک · ") + ssid; }
    String sectionDevice() { return p("إعدادات الجهاز", "Device settings", "Réglages de l'appareil", "Cihaz ayarları", "ڈیوائس ترتیبات"); }
    String sectionFamily() { return p("الأسرة والتشغيل", "Family & startup", "Famille & démarrage", "Aile & başlangıç", "خاندان و آغاز"); }
    String preview() { return p("العصر اليوم بهذه الإعدادات", "Asr today with these settings", "Asr aujourd'hui avec ces réglages",
        "Bu ayarlarla bugünkü Asr", "ان ترتیبات کے ساتھ آج کا عصر"); }
    String location() { return p("الموقع", "Location", "Emplacement", "Konum", "مقام"); }
    String country() { return p("الدولة", "Country", "Pays", "Ülke", "ملک"); }
    String region() { return p("المنطقة", "Region", "Région", "Bölge", "علاقہ"); }
    /** Location's first step, in the order of Prefs.REGIONS. */
    String[] regions() {
        return pa(new String[] {"الدول العربية", "آسيا", "أوروبا", "الأمريكتان", "أفريقيا", "أوقيانوسيا"},
                  new String[] {"Arab world", "Asia", "Europe", "Americas", "Africa", "Oceania"},
                  new String[] {"Monde arabe", "Asie", "Europe", "Amériques", "Afrique", "Océanie"},
                  new String[] {"Arap dünyası", "Asya", "Avrupa", "Amerika", "Afrika", "Okyanusya"},
                  new String[] {"عرب دنیا", "ایشیا", "یورپ", "امریکہ", "افریقہ", "اوقیانوسیہ"});
    }
    String locationHelp(double lat, double lng) {
        String ns = lat >= 0 ? p("ش", "N", "N", "K", "ش") : p("ج", "S", "S", "G", "ج");
        String ew = lng >= 0 ? p("ق", "E", "E", "D", "ق") : p("غ", "W", "O", "B", "غ");
        return d(String.format(java.util.Locale.US, "%.2f° %s · %.2f° %s", Math.abs(lat), ns, Math.abs(lng), ew)
            + p(" · يُختار من القائمة", " · chosen from the list", " · choisi dans la liste",
                " · listeden seçilir", " · فہرست سے منتخب"));
    }
    String place(String city, String country) { return city + (ar ? "، " : ", ") + country; }
    String method() { return p("طريقة الحساب", "Calculation method", "Méthode de calcul", "Hesap yöntemi", "حساب کا طریقہ"); }
    String asrSchool() { return p("مذهب العصر", "Asr school", "École de l'Asr", "Asr mezhebi", "عصر کا مسلک"); }
    String asrSchoolHelp() { return p("يغيّر وقت العصر فقط", "Only changes the Asr time", "Ne change que l'heure de l'Asr",
        "Sadece Asr saatini değiştirir", "صرف عصر کا وقت بدلتا ہے"); }
    String[] asrSchools() {
        return pa(new String[] {"الجمهور", "الحنفي"}, new String[] {"Standard", "Hanafi"},
                  new String[] {"Standard", "Hanafite"}, new String[] {"Standart", "Hanefi"},
                  new String[] {"جمہور", "حنفی"});
    }
    String hijriAdjust() { return p("تعديل التاريخ الهجري", "Hijri date adjustment", "Ajustement de la date hégirienne",
        "Hicri tarih düzeltmesi", "ہجری تاریخ ایڈجسٹمنٹ"); }
    String hijriToday(String date) { return p("اليوم: ", "Today: ", "Aujourd'hui : ", "Bugün: ", "آج: ") + date; }
    String days(int n) {
        String sign = n > 0 ? "+" : "";
        if (lang == AR) return d(sign + n + (Math.abs(n) == 2 ? " يومان" : " يوم"));
        if (lang == UR) return d(sign + n + (Math.abs(n) == 2 ? " دن" : " دن"));
        if (lang == TR) return d(sign + n + " gün");
        return d(sign + n + (Math.abs(n) == 1 ? " jour" : " jours"));
    }
    String roomRow() { return roomTitle(); }
    String roomHelp() { return p("لمعرفة القبلة بالنسبة لمكان جلوسك", "So the Qibla is shown from where you sit",
        "Pour montrer la qibla depuis votre siège", "Oturduğunuz yerden kıbleyi göstermek için", "آپ کی نشست کے مطابق قبلہ دکھانے کے لیے"); }
    String notSet() { return p("لم يُحدَّد", "Not set", "Non défini", "Ayarlanmadı", "متعین نہیں"); }
    String remind() { return p("التنبيه قبل الصلاة", "Remind me before prayer", "Me rappeler avant la prière",
        "Namazdan önce hatırlat", "نماز سے پہلے یاد دہانی"); }
    String remindHelp() { return p("على الرئيسية وفوق الفيديو", "On Home and over video", "Sur l'accueil et sur la vidéo",
        "Ana ekranda ve videonun üzerinde", "ہوم اور ویڈیو پر"); }
    String[] remindChoices() {
        return pa(new String[] {"5 د", "10 د", "15 د", "20 د", "30 د", "إيقاف"},
                  new String[] {"5 min", "10 min", "15 min", "20 min", "30 min", "Off"},
                  new String[] {"5 min", "10 min", "15 min", "20 min", "30 min", "Arrêté"},
                  new String[] {"5 dk", "10 dk", "15 dk", "20 dk", "30 dk", "Kapalı"},
                  new String[] {"5 منٹ", "10 منٹ", "15 منٹ", "20 منٹ", "30 منٹ", "بند"});
    }
    String remindVoiceRow() { return p("تذكير صوتي", "Spoken reminder", "Rappel parlé", "Sözlü hatırlatma", "زبانی یاد دہانی"); }
    String remindVoiceHelp() {
        return p("«اقترب موعد صلاة …» مع التذكير", "Says «اقترب موعد صلاة …» with the reminder",
            "Dit «اقترب موعد صلاة …» avec le rappel", "Hatırlatmayla «اقترب موعد صلاة …» der", "یاد دہانی کے ساتھ کہتا ہے");
    }
    String announceRow() { return p("إعلان دخول الوقت", "Announce the time", "Annoncer l'heure", "Vaktin girdiğini duyur", "وقت کا اعلان"); }
    String announceHelp() {
        return p("«حان الآن موعد صلاة …» للصلاة التي أذانها متوقف", "Says «حان الآن موعد صلاة …» when a prayer's adhan is off",
            "Dit «حان الآن موعد صلاة …» quand l'adhan est coupé", "Ezanı kapalı namazda «حان الآن موعد صلاة …» der",
            "اذان بند نماز کے لیے کہتا ہے");
    }
    String adhanRow() { return p("الأذان", "Adhan", "Adhan", "Ezan", "اذان"); }
    String adhanHelp() {
        return p("يُشغَّل عند دخول الوقت · يمكن إيقافه لكل صلاة", "Plays when the time begins · can be muted per prayer",
            "Joue au début du temps · peut être coupé par prière", "Vakit girince çalar · her namaz için kapatılabilir",
            "وقت شروع ہونے پر چلتا ہے · ہر نماز کے لیے بند ہو سکتا ہے");
    }
    String language() { return p("اللغة", "Language", "Langue", "Dil", "زبان"); }
    String languageHelp() { return p("«التلفاز» يتبع لغة التلفاز", "“TV” follows the TV's language", "« TV » suit la langue de la TV",
        "“TV” TV'nin dilini izler", "«ٹی وی» ٹی وی کی زبان کے مطابق"); }
    String[] languages() {
        return pa(new String[] {"التلفاز", "العربية", "English", "Français", "Türkçe", "اردو"},
                  new String[] {"TV", "العربية", "English", "Français", "Türkçe", "اردو"},
                  new String[] {"TV", "العربية", "English", "Français", "Türkçe", "اردو"},
                  new String[] {"TV", "العربية", "English", "Français", "Türkçe", "اردو"},
                  new String[] {"ٹی وی", "العربية", "English", "Français", "Türkçe", "اردو"});
    }
    String digits() { return p("الأرقام", "Digits", "Chiffres", "Rakamlar", "اعداد"); }
    String clockRow() { return p("الساعة", "Clock", "Horloge", "Saat", "گھڑی"); }
    String clockHelp() { return p("«التلفاز» يتبع إعداد الوقت في التلفاز", "“TV” follows the TV's time setting",
        "« TV » suit le réglage horaire de la TV", "“TV” TV'nin saat ayarını izler", "«ٹی وی» ٹی وی کے وقت کی ترتیب کے مطابق"); }
    String[] clocks() {
        return pa(new String[] {"التلفاز", "12 س", "24 س"}, new String[] {"TV", "12 h", "24 h"},
                  new String[] {"TV", "12 h", "24 h"}, new String[] {"TV", "12 s", "24 s"},
                  new String[] {"ٹی وی", "12 گھ", "24 گھ"});
    }
    String networkRow() { return p("إعدادات الشبكة في التلفاز", "TV network settings", "Réseau de la TV", "TV ağ ayarları", "ٹی وی نیٹ ورک ترتیبات"); }
    String networkHelp() { return p("واي فاي وإيثرنت", "Wi-Fi and Ethernet", "Wi-Fi et Ethernet", "Wi-Fi ve Ethernet", "وائی فائی ایتھرنیٹ"); }
    String deviceRow() { return p("إعدادات التلفاز", "TV settings", "Réglages de la TV", "TV ayarları", "ٹی وی ترتیبات"); }
    String deviceHelp() { return p("الصورة والصوت والتطبيقات والنظام", "Picture, sound, apps and system",
        "Image, son, applis et système", "Resim, ses, uygulamalar ve sistem", "تصویر، آواز، ایپس اور سسٹم"); }
    String hintMove() { return p("▲▼  تنقل", "▲▼  Move", "▲▼  Déplacer", "▲▼  Gezin", "▲▼  حرکت"); }
    String hintChange() { return p("◀▶  تغيير القيمة", "◀▶  Change value", "◀▶  Changer", "◀▶  Değiştir", "◀▶  بدلیں"); }
    String hintChoose() { return p("OK  اختيار", "OK  Choose", "OK  Choisir", "OK  Seç", "OK  منتخب"); }
    String hintBackMenu() { return p("رجوع  القائمة", "Back  Menu", "Retour  Menu", "Geri  Menü", "Back  مینو"); }
    String hintEdit() { return p("OK  تعديل", "OK  Edit", "OK  Modifier", "OK  Düzenle", "OK  تبدیل"); }
    String hintSwitch() { return p("OK  تشغيل/إيقاف", "OK  On/Off", "OK  Marche/Arrêt", "OK  Aç/Kapa", "OK  آن/آف"); }
    String hintSave() { return p("OK  حفظ", "OK  Save", "OK  Enregistrer", "OK  Kaydet", "OK  محفوظ"); }
    String hintBackCancel() { return p("رجوع  إلغاء", "Back  Cancel", "Retour  Annuler", "Geri  İptal", "Back  منسوخ"); }
    String saved() { return p("✓  تم الحفظ", "✓  Saved", "✓  Enregistré", "✓  Kaydedildi", "✓  محفوظ ہو گیا"); }
    String qibla() { return p("القبلة", "QIBLA", "QIBLA", "KIBLE", "قبلہ"); }
    String north() { return p("الشمال ▲", "▲ North", "▲ Nord", "▲ Kuzey", "▲ شمال"); }
    String qiblaHint() { return p("وجّه الشمال على البوصلة نحو الشمال الحقيقي", "Line up the compass north with true north",
        "Alignez le nord de la boussole avec le nord vrai", "Pusulanın kuzeyini gerçek kuzeyle hizalayın",
        "قطب نما کے شمال کو حقیقی شمال سے ملا دیں"); }
    String bellToggle() { return p("تشغيل/إيقاف الأذان لهذه الصلاة", "Adhan on/off for this prayer", "Adhan marche/arrêt pour cette prière",
        "Bu namaz için ezan aç/kapa", "اس نماز کے لیے اذان آن/آف"); }
    String hintBackHome() { return p("رجوع  الرئيسية", "Back  Home", "Retour  Accueil", "Geri  Ana ekran", "Back  ہوم"); }
    String bell(boolean on) { return p(on ? "الأذان مفعّل" : "الأذان متوقف", on ? "Adhan on" : "Adhan off",
        on ? "Adhan activé" : "Adhan coupé", on ? "Ezan açık" : "Ezan kapalı", on ? "اذان آن" : "اذان آف"); }
    String rowNow() { return p("الآن", "now", "maintenant", "şimdi", "ابھی"); }
    /** Sunrise is masculine in Arabic (الشروق مضى); the prayers are feminine (الصلاة مضت). */
    String passed(boolean sunrise) {
        return p(sunrise ? "مضى" : "مضت", "passed", "passée", "geçti", sunrise ? "گزرا" : "گزری");
    }
    String in(String duration) { return d(p("بعد " + duration, "in " + duration, "dans " + duration, duration + " sonra", duration + " بعد")); }

    /** "South-east · from Cairo" for a bearing in degrees from true north. */
    String qiblaDirection(double bearing) {
        return octant(bearing) + p(" · من ", " · from ", " · depuis ", " · itibaren ", " · سے ") + city();
    }

    String octant(double bearing) {
        String[] names = pa(new String[] {"شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب"},
                            new String[] {"North", "North-east", "East", "South-east", "South", "South-west", "West", "North-west"},
                            new String[] {"Nord", "Nord-est", "Est", "Sud-est", "Sud", "Sud-ouest", "Ouest", "Nord-ouest"},
                            new String[] {"Kuzey", "Kuzeydoğu", "Doğu", "Güneydoğu", "Güney", "Güneybatı", "Batı", "Kuzeybatı"},
                            new String[] {"شمال", "شمال مشرق", "مشرق", "جنوب مشرق", "جنوب", "جنوب مغرب", "مغرب", "شمال مغرب"});
        return names[sector(bearing)];
    }

    /**
     * Where the Qibla is for someone facing the TV, from its angle clockwise from straight ahead. Right and left
     * are the viewer's own, so they never mirror with the layout.
     */
    String qiblaRelative(double clockwise) {
        String[] where = pa(new String[] {"أمامك", "أمامك إلى اليمين", "على يمينك", "خلفك إلى اليمين", "خلفك", "خلفك إلى اليسار", "على يسارك", "أمامك إلى اليسار"},
                            new String[] {"ahead of you", "ahead, to your right", "to your right", "behind, to your right", "behind you",
                                          "behind, to your left", "to your left", "ahead, to your left"},
                            new String[] {"devant vous", "devant, à votre droite", "à votre droite", "derrière, à votre droite", "derrière vous",
                                          "derrière, à votre gauche", "à votre gauche", "devant, à votre gauche"},
                            new String[] {"önünüzde", "önünüzde, sağınızda", "sağınızda", "arkanızda, sağınızda", "arkanızda",
                                          "arkanızda, solunuzda", "solunuzda", "önünüzde, solunuzda"},
                            new String[] {"آپ کے سامنے", "سامنے، دائیں طرف", "دائیں طرف", "پیچھے، دائیں طرف", "آپ کے پیچھے",
                                          "پیچھے، بائیں طرف", "بائیں طرف", "سامنے، بائیں طرف"});
        return p("القبلة ", "Qibla is ", "La qibla est ", "Kıble ", "قبلہ ") + where[sector(clockwise)];
    }

    /** "North-west · 320°". */
    String bearingLabel(int degrees) { return d(octant(degrees) + " · " + degrees + "°"); }

    String facingLine(int facing) { return p("تجلس مواجهًا: ", "You face: ", "Vous faites face : ", "Baktığınız yön: ", "آپ کا رخ: ") + bearingLabel(facing); }
    String tvMarker() { return p("التلفاز ▲", "▲ TV", "▲ TV", "▲ TV", "▲ ٹی وی"); }
    String roomButton(boolean set) {
        return p(set ? "تغيير اتجاه الغرفة" : "حدّد اتجاه الغرفة", set ? "Change room direction" : "Set room direction",
            set ? "Changer l'orientation" : "Définir l'orientation", set ? "Oda yönünü değiştir" : "Oda yönünü ayarla",
            set ? "کمرے کی سمت بدلیں" : "کمرے کی سمت متعین کریں");
    }
    String roomTitle() { return p("اتجاه الغرفة", "Room direction", "Orientation de la pièce", "Oda yönü", "کمرے کی سمت"); }
    String roomTip() {
        return p("قف مواجهًا للتلفاز، وافتح تطبيق البوصلة في هاتفك وأمسكه مستويًا أمامك، ثم اضبط الرقم هنا على ما تقرؤه. يكفي ذلك مرة واحدة ما دام التلفاز في مكانه.",
                 "Stand facing the TV, open the compass app on your phone and hold it flat in front of you, then set the number here to what it reads. Once is enough while the TV stays where it is.",
                 "Tenez-vous face à la TV, ouvrez la boussole sur votre téléphone, tenez-la à plat devant vous, puis réglez ici le chiffre qu'elle affiche. Une seule fois suffit tant que la TV ne bouge pas.",
                 "TV'ye dönerek durun, telefonunuzdaki pusula uygulamasını açıp düz bir şekilde önünüzde tutun ve buradaki sayıyı okuduğunuza ayarlayın. TV yerinde kaldığı sürece bir kez yeter.",
                 "ٹی وی کے سامنے کھڑے ہوں، فون پر قطب نما کھول کر اسے سطحی رکھیں، پھر یہاں نمبر وہی لگائیں جو وہ دکھائے۔ جب تک ٹی وی اپنی جگہ ہے ایک بار کافی ہے۔");
    }
    String roomKeys() {
        return p("◀ ▶  ٥°     ▲ ▼  ٤٥°     OK  حفظ     رجوع  إلغاء",
                 "◀ ▶  5°     ▲ ▼  45°     OK  Save     Back  Cancel",
                 "◀ ▶  5°     ▲ ▼  45°     OK  Enregistrer     Retour  Annuler",
                 "◀ ▶  5°     ▲ ▼  45°     OK  Kaydet     Geri  İptal",
                 "◀ ▶  ۵°     ▲ ▼  ۴۵°     OK  محفوظ     Back  منسوخ");
    }
    String hintQibla() { return p("◀  القبلة", "▶  Qibla", "▶  Qibla", "▶  Kıble", "◀  قبلہ"); }
    String hintTimes() { return p("▶  المواقيت", "◀  Times", "◀  Horaires", "◀  Vakitler", "▶  اوقات"); }

    private static int sector(double degrees) {
        return (int) Math.round(((degrees % 360) + 360) % 360 / 45) % 8;
    }

    /** Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    String[] prayerNames() {
        return pa(new String[] {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"},
                  new String[] {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"},
                  new String[] {"Fajr", "Lever du soleil", "Dhuhr", "Asr", "Maghrib", "Isha"},
                  new String[] {"İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı"},
                  new String[] {"فجر", "طلوع آفتاب", "ظہر", "عصر", "مغرب", "عشاء"});
    }

    String[] hijriMonths() {
        return pa(new String[] {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
                                "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"},
                  new String[] {"Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula",
                                "Jumada al-Akhirah", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qadah",
                                "Dhu al-Hijjah"},
                  new String[] {"Mouharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Joumada al-Oula",
                                "Joumada al-Akhira", "Rajab", "Chaabane", "Ramadan", "Chawwal", "Dhou al-Qi'da",
                                "Dhou al-Hijja"},
                  new String[] {"Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir",
                                "Receb", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce"},
                  new String[] {"محرم", "صفر", "ربیع الاول", "ربیع الثانی", "جمادی الاول", "جمادی الآخر",
                                "رجب", "شعبان", "رمضان", "شوال", "ذوالقعدہ", "ذوالحجہ"});
    }

    String hijriSuffix() { return p(" هـ", " AH", " AH", " H.", " ہجری"); }

    /** "23 days" with the Arabic count's proper forms: يوم، يومان، ٣–١٠ أيام، ١١+ يوماً. */
    String daysWord(int n) {
        switch (lang) {
            case AR:
                if (n == 1) return "يوم";
                if (n == 2) return "يومان";
                return d(n + (n <= 10 ? " أيام" : " يوماً"));
            case UR:
                return d(n == 1 ? "دن" : n + " دن");
            case TR:
                return d(n + " gün");
            default:
                return d(n + (n == 1 ? " day" : " days"));
        }
    }

    /** "Ramadan in 23 days" for the arch's sub line; Arabic reads "باقي على رمضان ٢٣ يومًا". */
    String countdown(String name, int days) {
        if (lang == AR) return d("باقي على " + name + " " + daysWord(days));
        if (lang == TR) return d(name + " " + daysWord(days) + " kaldı");
        if (lang == UR) return d(name + " میں " + daysWord(days) + " باقی");
        return d(name + " in " + daysWord(days));
    }
    String ramadanName() { return p("رمضان", "Ramadan", "le Ramadan", "Ramazan", "رمضان"); }
    String arafahName() { return p("يوم عرفة", "the day of Arafah", "le jour d'Arafat", "Arefe günü", "یوم عرفہ"); }

    // ------------------------------------------------------------------ weather (Home line, Settings rows)

    String weatherRow() { return p("الطقس", "Weather", "Météo", "Hava durumu", "موسم"); }
    String weatherHelp() {
        return p("درجة الحرارة والجو الآن على الرئيسية · من Open-Meteo · يحتاج الإنترنت",
                 "Temperature and sky now on Home · from Open-Meteo · needs the internet",
                 "Température et ciel sur l'accueil · via Open-Meteo · nécessite Internet",
                 "Ana ekranda sıcaklık ve gökyüzü · Open-Meteo'dan · internet gerekir",
                 "ہوم پر درجہ حرارت اور آسمان · Open-Meteo سے · انٹرنیٹ درکار");
    }
    /** The detail after the temperature: humidity or wind, by the display row. */
    String humidityPart(int humidity) {
        return d(p("الرطوبة " + humidity + "٪", "Humidity " + humidity + "%", "Humidité " + humidity + "%",
            "Nem %" + humidity, "نمی " + humidity + "٪"));
    }
    String windPart(int kmh, double bearing) {
        return d(p("الرياح " + kmh + " كم/س " + octant(bearing), "Wind " + kmh + " km/h " + octant(bearing),
            "Vent " + kmh + " km/h " + octant(bearing), kmh + " km/s rüzgâr " + octant(bearing),
            "ہوا " + kmh + " کلومیٹر/گھنٹہ " + octant(bearing)));
    }
    /** "صافٍ · ٢٤° · الرطوبة ٥٥٪". */
    String weatherLine(String desc, String temp, String detail) {
        return d(desc + " · " + temp + "°" + (detail.isEmpty() ? "" : " · " + detail));
    }
    String weatherWaiting() { return p("الطقس…", "Weather…", "Météo…", "Hava durumu…", "موسم…"); }
    /** The sky's description, by Weather.group(). */
    String weatherDesc(int group) {
        String[] names = pa(new String[] {"صافٍ", "غائم جزئيًا", "غائم", "ضباب", "رذاذ", "مطر", "ثلج", "عواصف رعدية"},
                            new String[] {"Clear", "Partly cloudy", "Overcast", "Fog", "Drizzle", "Rain", "Snow", "Thunderstorm"},
                            new String[] {"Dégagé", "Partiellement nuageux", "Couvert", "Brouillard", "Bruine", "Pluie", "Neige", "Orage"},
                            new String[] {"Açık", "Parçalı bulutlu", "Kapalı", "Puslu", "Çisenti", "Yağmurlu", "Karlı", "Gök gürültülü"},
                            new String[] {"صاف", "جزوی بادل", "بادلوں سے ڈھکا", "دھند", "بوندا باندی", "بارش", "برف", "گرج چمک"});
        return names[Math.max(0, Math.min(names.length - 1, group))];
    }
    String unitRow() { return p("وحدة الحرارة", "Temperature unit", "Unité de température", "Sıcaklık birimi", "درجہ حرارت کی اکائی"); }
    String[] unitChoices() {
        return pa(new String[] {"مئوية °C", "فهرنهايت °F"}, new String[] {"Celsius °C", "Fahrenheit °F"},
                  new String[] {"Celsius °C", "Fahrenheit °F"}, new String[] {"Santigrat °C", "Fahrenhayt °F"},
                  new String[] {"سیلسیس °C", "فارن ہائیٹ °F"});
    }
    String windRow() { return p("تفصيل الطقس", "Weather detail", "Détail météo", "Hava ayrıntısı", "موسم کی تفصیل"); }
    String[] windChoices() {
        return pa(new String[] {"الرطوبة", "الرياح والاتجاه"}, new String[] {"Humidity", "Wind & direction"},
                  new String[] {"Humidité", "Vent & direction"}, new String[] {"Nem", "Rüzgâr & yön"},
                  new String[] {"نمی", "ہوا و سمت"});
    }
    String forecastRow() { return p("توقعات 3 أيام", "3-day forecast", "Prévisions sur 3 jours", "3 günlük tahmin", "3 دن کی پیشن گوئی"); }
    String forecastHelp() {
        return p("الصغرى والعظمى لثلاثة أيام على الرئيسية", "Low and high for three days on Home",
            "Mini et maxi pour trois jours sur l'accueil", "Ana ekranda üç günün en düşüğü ve en yükseği",
            "ہوم پر تین دن کی کم اور زیادہ درجہ");
    }
    String dayToday() { return p("اليوم", "Today", "Aujourd'hui", "Bugün", "آج"); }
    String dayTomorrow() { return p("غدًا", "Tomorrow", "Demain", "Yarın", "کل"); }
    /** "احتمال مطر اليوم · ٧٠٪" — the gold alert line under the weather. */
    String rainAlert(int pct) {
        return d(p("احتمال مطر اليوم · " + pct + "٪", "Rain likely today · " + pct + "%",
            "Pluie probable aujourd'hui · " + pct + "%", "Bugün yağmur olasılığı · %" + pct,
            "آج بارش کا امکان · " + pct + "٪"));
    }
    String tintRow() { return p("لون القوس مع الوقت", "Arch tint by time", "Teinte de l'arche selon l'heure", "Saate göre kemer rengi", "وقت کے مطابق محراب کا رنگ"); }
    String tintHelp() {
        return p("تدرج خلفي يتبع اليوم: فجر، نهار، غروب، ليل", "A background that follows the day: dawn, noon, sunset, night",
            "Un fond qui suit la journée : aube, midi, crépuscule, nuit", "Günü izleyen arka plan: şafak, öğle, akşam, gece",
            "دن کے مطابق پس منظر: فجر، دوپہر، غروب، رات");
    }

    // ------------------------------------------------------------------ Eid and Arafah (Home's arch)

    String eidLabel() { return p("عيد مبارك", "EID MUBARAK", "AÏD MOUBARAK", "MÜBAREK BAYRAM", "عید مبارک"); }
    /** fitr: Eid al-Fitr (1 Shawwal); otherwise Eid al-Adha (10 Dhu al-Hijjah). */
    String eidName(boolean fitr) {
        return fitr
            ? p("عيد الفطر المبارك", "Eid al-Fitr", "l'Aïd el-Fitr", "Ramazan Bayramı", "عید الفطر المبارک")
            : p("عيد الأضحى المبارك", "Eid al-Adha", "l'Aïd el-Adha", "Kurban Bayramı", "عید الاضحی المبارک");
    }
    String eidGreeting() {
        return p("تقبل الله طاعتكم وكل عام وأنتم بخير",
                 "May Allah accept your good deeds · every year, may you be well",
                 "Qu'Allah accepte vos bonnes œuvres · chaque année, que vous alliez bien",
                 "Allah ibadetlerinizi kabul etsin · her yıl size iyi olsun",
                 "اللہ آپ کی عبادتیں قبول کرے ہر سال آپ خوش رہیں");
    }
    String arafahLine() { return p("اليوم يوم عرفة", "Today is the day of Arafah", "Aujourd'hui, c'est le jour d'Arafat",
        "Bugün Arefe günüdür", "آج یوم عرفہ ہے"); }

    // ------------------------------------------------------------------ Ramadan (the arch, the imsakiya)

    /** The Ramadan verse on the imsakiya (the Quran stays Arabic in every language). */
    String ramadanVerse() { return "﴿شَهْرُ رَمَضَانَ الَّذِي أُنْزِلَ فِيهِ الْقُرْآنُ﴾"; }
    String ramadanRef() { return p("البقرة 185", "Al-Baqarah 2:185", "Al-Baqarah 2:185", "Bakara 2:185", "البقرہ 185"); }
    String imsakiya() { return p("الإمساكية", "Imsakiya", "Imsakiya", "İmsakiye", "امساکیہ"); }
    String imsakiyaHelp() {
        return p("الفجر والمغرب لكل يوم من الشهر", "Fajr and Maghrib for every day of the month",
            "Fajr et Maghrib pour chaque jour du mois", "Ayın her günü için Fajr ve Akşam",
            "مہینے کے ہر دن کا فجر اور مغرب");
    }
    String imsakLabel() { return p("الإمساك", "Imsak", "Imsak", "İmsak", "امساک"); }
    String imsakiyaDay(int day) { return d(p("اليوم " + day, "Day " + day, "Jour " + day, day + ". gün", day + " روز")); }

    // ------------------------------------------------------------------ family & startup

    String kidsRow() { return p("مؤقت مشاهدة الأطفال", "Kids watch timer", "Minuteur pour enfants", "Çocuk izleme süresi", "بچوں کی دیکھائی کا ٹائمر"); }
    String kidsHelp() {
        return p("العودة إلى الرئيسية بعد هذه المدة من أي تطبيق", "Back to Home after this long in any app",
            "Retour à l'accueil après ce temps dans une appli", "Herhangi bir uygulamada bu süre sonra Ana ekrana döner",
            "کسی بھی ایپ میں اتنی دیر کے بعد ہوم پر واپس");
    }
    String[] kidsChoices() {
        return pa(new String[] {"مغلق", "15 د", "30 د", "45 د", "60 د"},
                  new String[] {"Off", "15 min", "30 min", "45 min", "60 min"},
                  new String[] {"Arrêté", "15 min", "30 min", "45 min", "60 min"},
                  new String[] {"Kapalı", "15 dk", "30 dk", "45 dk", "60 dk"},
                  new String[] {"بند", "15 منٹ", "30 منٹ", "45 منٹ", "60 منٹ"});
    }
    String kidsDone() {
        return p("انتهى وقت المشاهدة · نعود إلى الرئيسية", "Watch time is over · back to Home",
            "Le temps d'écran est fini · retour à l'accueil", "İzleme süresi doldu · Ana ekrana dönülüyor",
            "دیکھنے کا وقت ختم · ہوم پر واپس");
    }
    String autoRow() { return p("تطبيق يفتح عند الإقلاع", "App at startup", "Appli au démarrage", "Açılışta uygulama", "آغاز پر ایپ"); }
    String autoHelp() {
        return p("يُفتح تلقائيًا بعد إقلاع التلفاز بقليل", "Opens by itself shortly after the TV starts",
            "S'ouvre seul peu après le démarrage de la TV", "TV açıldıktan kısa süre sonra kendi açılır",
            "ٹی وی آن ہونے کے تھوڑی دیر بعد خود کھل جاتی ہے");
    }
    String autoNone() { return p("بلا تطبيق", "No app", "Aucune appli", "Uygulama yok", "کوئی ایپ نہیں"); }
    String autoDelayRow() { return p("بعد الإقلاع", "After startup", "Après le démarrage", "Açılıştan sonra", "آغاز کے بعد"); }
    String[] autoDelayChoices() {
        return pa(new String[] {"10 ث", "20 ث", "30 ث"}, new String[] {"10 s", "20 s", "30 s"},
                  new String[] {"10 s", "20 s", "30 s"}, new String[] {"10 sn", "20 sn", "30 sn"},
                  new String[] {"10 سی", "20 سی", "30 سی"});
    }

    // ------------------------------------------------------------------ Security: the settings PIN lock

    String sectionSecurity() { return p("الأمان", "Security", "Sécurité", "Güvenlik", "سیکیورٹی"); }
    String pinLockRow() { return p("قفل الإعدادات", "Lock Scoop TV settings", "Verrouiller les réglages", "Ayarları kilitle", "ترتیبات لاک کریں"); }
    String pinLockHelp() {
        return p("رمز من أربعة أرقام قبل فتح الإعدادات", "A four-digit PIN before settings open",
            "Un code à quatre chiffres avant les réglages", "Ayarlar açılmadan dört haneli PIN",
            "ترتیبات کھلنے سے پہلے چار ہندسوں کا PIN");
    }
    String pinChangeRow() { return p("تغيير الرمز", "Change PIN", "Changer le code", "PIN'i değiştir", "PIN بدلیں"); }
    String pinChangeHelp() { return p("يظهر بعد تفعيل القفل", "There once the lock is on", "Présent une fois le verrou actif",
        "Kilit açılınca görünür", "لاک آن ہونے پر نظر آتا ہے"); }
    String pinEnterTitle() { return p("أدخل الرمز", "Enter the PIN", "Entrez le code", "PIN'i girin", "PIN درج کریں"); }
    String pinSetTitle() { return p("عيّن رمزًا من أربعة أرقام", "Set a four-digit PIN", "Définissez un code à quatre chiffres",
        "Dört haneli bir PIN belirleyin", "چار ہندسوں کا PIN متعین کریں"); }
    String pinRepeatTitle() { return p("أعد إدخال الرمز", "Repeat the PIN", "Répétez le code", "PIN'i tekrarlayın", "PIN دہرائیں"); }
    String pinWrong() { return p("رمز خاطئ، حاول مجددًا", "Wrong PIN, try again", "Code erroné, réessayez", "Yanlış PIN, tekrar deneyin", "غلط PIN، دوبارہ کوشش کریں"); }
    String pinMismatch() {
        return p("الرقمان غير متطابقين، حاول مجددًا", "The two don't match, try again", "Les deux ne concordent pas, réessayez",
            "İkisi eşleşmiyor, tekrar deneyin", "دونوں مختلف ہیں، دوبارہ کوشش کریں");
    }
    String pinLockedHint() {
        return p("OK  اختيار الرقم     ⌫  حذف     رجوع  إغلاق", "OK  pick a digit     ⌫  delete     Back  close",
            "OK  choisir un chiffre     ⌫  effacer     Retour  fermer", "OK  rakam seç     ⌫  sil     Geri  kapat",
            "OK  ہندسہ منتخب     ⌫  حذف     Back  بند");
    }
    String pinSetHint() {
        return p("OK  اختيار الرقم     ⌫  حذف     رجوع  إلغاء", "OK  pick a digit     ⌫  delete     Back  cancel",
            "OK  choisir un chiffre     ⌫  effacer     Retour  annuler", "OK  rakam seç     ⌫  sil     Geri  iptal",
            "OK  ہندسہ منتخب     ⌫  حذف     Back  منسوخ");
    }
    String pinEnabled() { return p("✓ تفعّل القفل", "✓ Lock is on", "✓ Verrou activé", "✓ Kilit açık", "✓ لاک آن ہے"); }
    String pinDisabled() { return p("✓ أُلغي القفل", "✓ Lock is off", "✓ Verrou désactivé", "✓ Kilit kapalı", "✓ لاک آف ہے"); }
    /** The forgotten-PIN way out, in the security section's lock row's help while the lock is on. */
    String pinLockedNote() {
        return p("مفعّل · نسيان الرمز: حذف بيانات التطبيق يمسح الإعدادات كلها",
                 "On · forgot the PIN: clearing the app's data wipes every setting",
                 "Actif · code oublié : effacer les données de l'appli réinitialise tout",
                 "Açık · PIN unutuldu: uygulama verilerini silmek her şeyi sıfırlar",
                 "آن · PIN بھول گئے: ایپ کا ڈیٹا صاف کرنے سے سب ترتیبات مٹ جاتی ہیں");
    }
}
