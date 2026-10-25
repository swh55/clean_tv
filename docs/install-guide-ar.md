# دليل تثبيت سكوب TV · خطوة بخطوة

دليل تثبيت **سكوب TV** — شاشة التلفاز الرئيسية التي تدور حول يوم الصلاة — على تلفاز أندرويد، من أول
تشغيل تصحيح الأخطاء حتى مواقيت الصلاة على الشاشة. **كل أوامر هذا الدليل أوامر ADB مباشرة** تكتبها في
سطر أوامر الحاسوب: لا بايثون ولا أدوات وسيطة ولا شيء غير `adb` نفسه.

---

## ١) ما تحتاجه

| الشيء | التفاصيل |
|---|---|
| التلفاز | تلفاز أندرويد (أندرويد 8 أو أحدث)، متصل بالشبكة نفسها التي على حاسوبك |
| حاسوب | عليه **adb** (من [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)) |
| ملف APK | [`scoop-tv.apk`](https://github.com/swh55/clean_tv/releases/latest/download/scoop-tv.apk) من آخر إصدار |
| على التلفاز | تفعيل **خيارات المطوّرين** ثم **تصحيح الأخطاء عبر الشبكة** |

> كل أوامر الأمثلة أدناه تفترض أن عنوان تلفازك `192.168.1.50:5555` — غيّره إلى عنوانك أنت في كل مرة.
> ستجد العنوان في التلفاز: الإعدادات ← الشبكة والإنترنت ← (اسم شبكتك) ← حالة الشبكة، أو شغّل «تصحيح
> الأخطاء عبر الشبكة» وسيظهر العنوان على الشاشة.

## ٢) تفعيل تصحيح الأخطاء على التلفاز

1. **الإعدادات ← إعدادات الجهاز ← حول** (أو «حول التلفاز»).
2. انقر على **بناء / Build** سبع مرات متتالية حتى تظهر رسالة «أصبحت الآن مطوّرًا».
3. ارجع إلى **الإعدادات ← إعدادات الجهاز ← خيارات المطوّرين**.
4. فعّل **تصحيح الأخطاء**، ثم **تصحيح الأخطاء عبر الشبكة** (Network debugging).
5. ستظهر رسالة بالعنوان، مثلاً: `عنوان IP: 192.168.1.50` والمنفذ `5555`.

## ٣) الاتصال بالتلفاز

من سطر أوامر الحاسوب (في ويندوز استخدم CMD أو PowerShell، وفي ماك ولينكس الطرفية):

```bash
adb connect 192.168.1.50:5555
adb devices
```

- في **أجهزة Google TV** ستظهر على شاشة التلفاز نافذة «السماح بتصحيح الأخطاء؟» — علّم «السماح دائمًا
  من هذا الحاسوب» ثم اختر **سماح**.
- يجب أن يُظهر `adb devices` سطرًا مثل:

```
192.168.1.50:5555    device
```

> إن ظهرت `offline` أو رُفض الاتصال: أعد تشغيل «تصحيح الأخطاء عبر الشبكة» في التلفاز، ثم
> `adb disconnect` وأعد `adb connect`. وإن استمرت المشكلة أعد تشغيل التلفاز من الكهرباء.

## ٤) تثبيت سكوب TV

```bash
adb -s 192.168.1.50:5555 install -r scoop-tv.apk
```

الرسالة `Success` تعني نجاح التثبيت. (العلم `-r` يعني «حدّث فوق نسخة قائمة إن وُجدت»).

## ٥) جعل سكوب TV الشاشة الرئيسية

هذا هو الأمر الرسمي الذي يطلب من أندرويد نفسها أن تجعل سكوب TV هو المُطلِق الافتراضي:

```bash
adb -s 192.168.1.50:5555 shell cmd package set-home-activity pro.datascoop.tv/.MainActivity
```

ثم أكمل باقي إعداد سكوب TV (شاشة التوقف، والصلاحيات التي تحتاجها الميزات):

```bash
adb -s 192.168.1.50:5555 shell settings put secure screensaver_components pro.datascoop.tv/.AfnonDream
adb -s 192.168.1.50:5555 shell pm grant pro.datascoop.tv android.permission.RECORD_AUDIO
adb -s 192.168.1.50:5555 shell pm grant pro.datascoop.tv android.permission.WRITE_SECURE_SETTINGS
adb -s 192.168.1.50:5555 shell pm grant pro.datascoop.tv android.permission.ACCESS_FINE_LOCATION
```

ثم اضغط زر **الرئيسية** في الريموت — تظهر شاشة سكوب TV. وفعّل **أزرار ريموت سكوب TV** من
إعدادات التلفاز ← **إمكانية الوصول**، لتعمل أزرار التطبيقات والإعدادات في الريموت مع سكوب TV، وكي يأتي
الأذان ووقتُه في كل صلاة.

> كل أوامر القسمين ٥ و٦ مجتمعة في ملف واحد إن أحببت النسخ دفعة واحدة — انظر «أوامر الإعداد كاملة»
> في آخر الدليل.

---

## ٦) تلفازك يرفض تغيير الشاشة الرئيسية؟ (الحلول بالترتيب)

بعض الأجهزة — خاصة التي تأتي بنظام خاص من المصنع — تجعل الأمر أعقد: زر الرئيسية يعيد دائمًا إلى
الشاشة الأصلية، أو يفتح متجر التطبيقات لاختيار مُطلِق، أو يتجاهل طلبك كليًا. إليك الحلول **كلها
أوامر ADB خالصة**، جرّبها بالترتيب، وكل واحد منها آمن وقابل للرجوع:

### الحل 1: تأكد أن سكوب TV مرشح فعلًا

```bash
adb -s 192.168.1.50:5555 shell cmd package set-home-activity pro.datascoop.tv/.MainActivity
```

- إن نجح الأمر بصمت: انتهيت — اضغط زر الرئيسية في الريموت.
- إن ظهر خطأ مثل `Neither user xxx nor current process has android.permission.SET_PREFERRED_ACTIVITYS`
  أو `Activity ... not found`: انتقل إلى الحل 2.
- إن ظهرت قائمة باسم `Resolver` أو أكثر من خيار: انتقل إلى الحل 3.

### الحل 2: تعطيل مُطلِق الشاشة الأصلي مؤقتًا (قابل للرجوع بسهولة)

الفكرة: إذا لم يعد هناك مُطلِق آخر ينافس، يجعل أندرويد سكوب TV الشاشة الرئيسية وحدها. أولاً
**اكتشف اسم حزمة الشاشة الأصلية**:

```bash
adb -s 192.168.1.50:5555 shell cmd package resolve-activity -c android.intent.category.HOME -a android.intent.action.MAIN
```

ابحث في الناتج عن `name=` — أمثلة شائعة:

| النظام | اسم الحزمة المعتاد |
|---|---|
| Google TV / أندرويد TV الأصلي | `com.google.android.apps.tv.launcherx` |
| أندرويد TV قديم | `com.google.android.tvlauncher` |
| TCL/Philips وأشباهها | `com.google.android.apps.tv.launcherx` غالبًا |
| أجهزة صناعة صينية | `com.android.launcher3` أو اسم الشركة نفسه |

ثم عطّله **لمستخدم 0 فقط** (لا يُحذف، ولا يتأثر أي مستخدم آخر، ويمكن إرجاعه بأمر واحد):

```bash
# استبدل اسم الحزمة بما ظهر عندك:
adb -s 192.168.1.50:5555 shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

ثم اجعل سكوب TV الرئيسية:

```bash
adb -s 192.168.1.50:5555 shell cmd package set-home-activity pro.datascoop.tv/.MainActivity
adb -s 192.168.1.50:5555 shell input keyevent KEYCODE_HOME
```

> **الرجوع متى شئت:**
> ```bash
> adb -s 192.168.1.50:5555 shell pm enable com.google.android.apps.tv.launcherx
> ```
>
> **تنبيه قبل تعطيل المُطلِق الأصلي:** تأكد أولًا أن سكوب TV مثبّت ويعمل (افتحه من قائمة التطبيقات).
> لا تعطّل المُطلِق الأصلي على جهاز تظهر فيه سكوب TV غير مثبّتة، حتى لا تبقى بلا شاشة رئيسية إلى أن
> تثبّت أحدهما.

### الحل 3: من نافذة «اختيار الشاشة الرئيسية» على التلفاز نفسه

إن كان التلفاز يعرض كل مرة نافذة «استخدام تطبيق مختلف للرئيسية»:

1. اضغط زر الرئيسية في الريموت بعد تثبيت سكوب TV.
2. اختر **سكوب TV** من القائمة.
3. اختر **دائمًا / Always** — وليس «مرة واحدة فقط».

وإن لم يظهر خيار «دائمًا»، فبعض الأجهزة تخفيه حتى تُزال حالة «الافتراضي» عن الشاشة القديمة أولًا:

```bash
adb -s 192.168.1.50:5555 shell pm clear-preferred-activities com.google.android.apps.tv.launcherx 2>/dev/null || \
adb -s 192.168.1.50:5555 shell pm preferred-activities --clear 2>/dev/null || true
```

ثم اضغط زر الرئيسية واختر سكوب TV ثم «دائمًا». (كلا الأمرين محميان: إن لم يكن اسمهما في نظامك فلا يفعلان شيئًا).

### الحل 4: أندرويد 9 وأقدم — عبر نظام الأدوار

الأجهزة القديمة تخزّن الشاشة الرئيسية كـ«دور» (Role). أعطِ الدور لسكوب TV مباشرة:

```bash
adb -s 192.168.1.50:5555 shell cmd role add-role-holder android.app.role.HOME pro.datascoop.tv
```

ثم أعد تشغيل التلفاز:

```bash
adb -s 192.168.1.50:5555 shell reboot
```

### الحل 5: آخر السبيل — افحص من يحمل دور الرئيسية الآن

```bash
# من يحمل دور HOME حاليًا:
adb -s 192.168.1.50:5555 shell cmd role holders android.app.role.HOME

# من يجيب على نية MAIN/HOME (المُطلِق الفعلي):
adb -s 192.168.1.50:5555 shell cmd package resolve-activity -c android.intent.category.HOME -a android.intent.action.MAIN

# كل المطلِقات المثبتة القادرة على أن تكون شاشة رئيسية:
adb -s 192.168.1.50:5555 shell pm query-activities -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null || \
adb -s 192.168.1.50:5555 shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME
```

إن ظهرت سكوب TV (`pro.datascoop.tv`) هي صاحبة الدور أو الإجابة، فأنت جاهز رغم أي رسالة خطأ سابقة — اضغط
زر الرئيسية فقط.

> **ملاحظة لأجهزة Xumo / Coocaa / Haier وبعض أجهزة TCL الاقتصادية:** نظام بعض هذه الأجهزة يستدعي
> شاشته الأصلية بخدمة خلفية لا بعلاقة بدور HOME. هنا يكفي عادة تعطيل حزمة الشاشة الأصلية (الحل 2)،
> وإن عادت بعد إعادة التشغيل فابحث عن حزمة خدمتها الخاصة في قائمة التطبيقات المعطّلة ضوئيًا
> `adb shell pm list packages -d` وعطّلها هي الأخرى بنفس الأمر، فعلت مع مستخدمي سكوب TV.

## ٧) استعادة كل شيء كما كان

```bash
# إعادة المُطلِق الأصلي (استبدل الاسم باسم حزمتك):
adb -s 192.168.1.50:5555 shell pm enable com.google.android.apps.tv.launcherx
adb -s 192.168.1.50:5555 shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity
adb -s 192.168.1.50:5555 shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
adb -s 192.168.1.50:5555 shell settings delete secure enabled_accessibility_services
# وإزالة سكوب TV إن أردت:
adb -s 192.168.1.50:5555 uninstall pro.datascoop.tv
```

(في أجهزة أندرويد TV القديمة تكون الحزم: `com.google.android.tvlauncher/.tv.TvActivity` و
`com.google.android.tv.dream/.TvDreamService`).

## ٨) بعد التثبيت — خطوات أول ٥ دقائق

1. **المدينة**: زر الإعدادات في الريموت (أو الترس في الرئيسية) ← مواقيت الصلاة ← الموقع ← المنطقة
   (الدول العربية) ← الدولة ← المدينة. **دمشق هي الافتراضية.**
2. **الطقس**: مفعّل افتراضيًا ويظهر تحت التاريخ. أوقفه من العرض واللغة ← الطقس إن أردت.
3. **قفل الإعدادات** (اختياري): الأمان ← قفل الإعدادات ← عيّن رمزًا من أربعة أرقام.
4. **أزرار الريموت**: إعدادات التلفاز ← إمكانية الوصول ← **أزرار ريموت سكوب TV** ← فعّلها.
5. **جرّب الأذان الآن بلا انتظار**:

```bash
adb -s 192.168.1.50:5555 shell am start -n pro.datascoop.tv/.MainActivity --es test call    # شاشة الأذان
adb -s 192.168.1.50:5555 shell am start -n pro.datascoop.tv/.MainActivity --es test friday  # شاشة الجمعة
adb -s 192.168.1.50:5555 shell am start -n pro.datascoop.tv/.MainActivity --es test ramadan # شاشة رمضان
adb -s 192.168.1.50:5555 shell am start -n pro.datascoop.tv/.MainActivity --es test now     # عد إلى اليوم
```

## ٩) الأمان بعد الانتهاء

تصحيح الأخطاء عبر الشبكة (المنفذ 5555) نص صريح بلا تشفير، وأي جهاز في شبكتك قد يحصل على صَدفة
عبره في بعض الأنظمة. عندما تنتهي من الإعداد:

```bash
# أطفئه من التلفاز: خيارات المطوّرين ← تصحيح الأخطاء عبر الشبكة ← إيقاف
# ثم من الحاسوب، تحقق أنه مغلق فعلًا (يفشل الاتصال):
nc -vz 192.168.1.50 5555
```

وسحب تفويضات الحاسوب إن أردت: خيارات المطوّرين ← إبطال تفويضات تصحيح USB، ثم أعد تشغيل التلفاز.

## ١٠) أوامر الإعداد كاملة (انسخها دفعة واحدة)

```bash
TV=192.168.1.50:5555
adb connect $TV
adb -s $TV install -r scoop-tv.apk
adb -s $TV shell cmd package set-home-activity pro.datascoop.tv/.MainActivity
adb -s $TV shell settings put secure screensaver_components pro.datascoop.tv/.AfnonDream
adb -s $TV shell pm grant pro.datascoop.tv android.permission.RECORD_AUDIO
adb -s $TV shell pm grant pro.datascoop.tv android.permission.WRITE_SECURE_SETTINGS
adb -s $TV shell pm grant pro.datascoop.tv android.permission.ACCESS_FINE_LOCATION
# خدمة أزرار الريموت + إبقاؤها بعد إعادة التشغيل:
adb -s $TV shell settings put secure enabled_accessibility_services pro.datascoop.tv/.RemoteKeys
adb -s $TV shell settings put secure accessibility_enabled 1
adb -s $TV shell appops set pro.datascoop.tv APP_AUTO_START allow 2>/dev/null || true
adb -s $TV shell appops set pro.datascoop.tv APP_ASSOC_START allow 2>/dev/null || true
# افتح الشاشة الرئيسية:
adb -s $TV shell input keyevent KEYCODE_HOME
```

> **ملاحظة لمن يبني من المصدر:** أدوات المستودع (`tools/install.sh` وغيرها) سكربتات Bash تعتمد على
> `adb` وحده — لا بايثون فيها إطلاقًا. الاستثناء الوحيد `tools/fetch-announce.sh` وهو **اختياري**
> لا علاقة له بإدارة الشاشة: يولّد ملفات صوت التنبيه فقط عبر edge-tts، وبدونه يعمل كل شيء ما عدا
> التذكير الصوتي.

---

*سكوب TV · شاشة رئيسية لتلفاز أندرويد تدور حول يوم الصلاة — بترخيص MIT.*
