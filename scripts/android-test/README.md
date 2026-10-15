# Android işlev regresyon sürücüsü

Güncel 0.8.1 kontrolü `ui-simplification.py` dosyasındadır: `before_after_upgrade`,
`library`, `settings`, `overview`, `removal`, `responsive`, `custom_dark_settings` ve
`final_audit` senaryoları ayrı ve sırayla çalıştırılır. Bir senaryo bitmeden
ikinci UI istemcisi başlatılmaz.
`before_after_upgrade` private `ui-before-update.json` ön kurulum baseline
kopyası ister. `removal` yalnız özel `UX-Remove-Only-Test.pdf` fixture
kaydını kaldırır. Yeni sonuç `UI-SIMPLIFICATION-functional.json` dosyasındadır.
Eski `ux-regression*.py` UX-MVP-6 tarihsel sürücüleridir; kaldırılan settings/backup
senaryoları 0.8.1 üzerinde çalıştırılmaz. `ui-simplification.py` ortak DB/UI
yardımcılarını eski sürücüden alır, eski raporu değiştirmez.


Bu araçlar yalnız özel root edilebilir `emulator-5580` test emülatörü içindir;
`EmulatorUI` fiziksel cihaz seri numarasını reddeder. Kurulu paket `com.kitap`
imzalı release olmalıdır. Metro kullanılmaz. Testler sentetik kitap/not/işaret
oluşturur ve corpus kitaplarının son okuma sayfalarını değiştirir. Günlük kullanım
veritabanında çalıştırılmaz; uygulama verisi silinmez. Tam fiziksel kabul değildir.

`ux-regression.py` kitaplık/overlay, 1000 sayfa, tarama, farklı sayfa oranı/bozuk
PDF, Downloads corpus ve pencere/font senaryolarını yürütür.
`ux-regression-extra.py` çift sayfa, taslak force-stop, v2 çatışma/restore,
sarı pasaj, over-pinch, PSS noktaları, wipe piksel takibi ve veri/hash audit'ini yürütür.
UIA istemcisi aynı anda yalnız bir sürücü tarafından kullanılmalıdır.

Ön koşullar: Python3 + Pillow; private toolchain altında SDK/ADB ve imzalı
final universal APK kurulu. Sentetik `UX-80.pdf`, `UX-1000.pdf`, `UX-scan.pdf`,
`UX-layouts.pdf`, `UX-broken.pdf` ve uzun başlık fixture'ı
`/sdcard/Download/kitap-tests/` içindedir. Bu çalışma bilgisayarındaki üretilmiş
corpus `%USERPROFILE%/.codex/toolchains/kitap-android/ux-corpus/` altındadır.
Kişisel PDF ve ham DB repo'ya eklenmez. Final `audit` eski kullanıcı verisini
aynı private dizindeki `ux1-after.db` snapshot'ıyla karşılaştırır; bu baseline
olmadan audit çalıştırılmaz ve geçmiş veri korundu iddiası üretilmez.

`KitapTouch.java`, SDK javac/D8 ile dex jar olarak derlenip yalnız emülatörde
`/data/local/tmp/kitap-touch.jar` yoluna gönderilir. `CLASSPATH=... app_process
/system/bin KitapTouch ...` shell kimliğiyle pinch/drag/double stream'i enjekte eder;
APK'ya veya manifest'e eklenmez. İki parmak biçimi x1 y1 x2 y2 hedefx1 hedefy1
hedefx2 hedefy2 durationMs; ayrıca `drag ... duration holdMs [cancel]` ve `double x y`.

Proje kökünde, aynı anda bir süreç:

```powershell
python -X utf8 scripts/android-test/ux-regression.py basic long_pdf scan layouts_pdf corpus responsive memory
python -X utf8 scripts/android-test/ux-regression-extra.py pair draft backup mark pinch reading_memory wipe audit
```

Gerçek sonuçlar `docs/acceptance/reports/UX-MVP-6-functional.json` içine yazılır;
ekran/ham DB/dumpsys/hata ayrıntısı private toolchain'de kalır. Scriptin adb/UIA
beklemelerini p95 veya FPS sayma. PSS birkaç nokta gözlemidir; peak/uzun oturum
ve Huawei performans kabulü değildir. Corpus verisini ve ekranı fiziksel olarak
incelemek gereklidir; a11y bounds doğru olsa bile native boya taşabilir.
