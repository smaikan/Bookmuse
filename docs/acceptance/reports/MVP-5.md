# MVP-5 kabul raporu

14 Ekim 2026. **Kod, imzalı release, host ve statik APK kontrolleri tamamlandı.**
Gerçek Huawei kabulü INCOMPLETE; tam V1 değildir. Önceki MVP-0–4 APK ve kanıtları
korundu. Kullanıcının MVP-4 tesliminden sonraki `devam et` talebiyle yürütüldü.

## Teslim

| Alan | Gerçek sonuç |
|---|---|
| APK | `artifacts/mvp-5/kitap-mvp5-arm64-v8a.apk` |
| Sürüm / package | 0.5.0 / versionCode 6 / com.kitap |
| ABI / API | arm64-v8a, min 24 / target 36; Huawei API/ABI ölçülmedi |
| Boyut | **23,002710 MiB**, başlangıç 30 MiB paket hedefinin altında |
| SHA256 | `f66f9a08ca1e40d79f2967df98bcf568949e2b0fdcb5572459a18c853f5b1201` |
| İmza | Aynı kalıcı certificate; apksigner verify geçti |
| Certificate SHA256 | `1a1f883707fe5009a8c25582a2ca0fa9ee55c844d802936f4b311a8a2502dc5c` |
| ELF / ZIP | 15 ARM64 native library, 16 KB + zipalign geçti |
| Release | Hermes + JS bundle + tek PDFium; R8/resource shrink ve lintVital geçti |
| İzinler | INTERNET veya geniş storage izinleri yok |
| TypeScript / ESLint | Strict typecheck geçti; 0 lint hata/uyarı |
| Host testleri | **31 Jest + 67 JUnit/Robolectric = 98 geçti**, sıfır failure |
| JNI/DEX | Beş exact JVM definition ve ARM64 JNI export son shrunk APK'da doğrulandı |
| Cihaz | ADB yeniden okundu; `no_authorized_device` |

Build `scripts/build-android.ps1 -Milestone mvp-5`, son kaynaklarda başarıyla
tamamlandı; `scripts/verify-android.ps1 -Milestone mvp-5` gerçek son APK üzerinde
çalıştırıldı. Native SQLite testleri aramadaki çift escape hatasını yakaladı;
literal `!` escape ile düzeltildi. İlk geliştirme derlemesindeki Kotlin array
inference ve lint analiz hataları finalde yok; hiçbir lint/metadata check
devre dışı bırakılmadı. AGP legacy DSL/Kotlin opt-out deprecation uyarıları var.

Ham kanıt: `MVP-5-build.log`, `MVP-5-jest.json`, `MVP-5-junit/`,
`MVP-5-junit-summary.json`, `MVP-5-verification.json`,
`MVP-5-geometry-bridge.json`, `MVP-5-apk-inspection.json`, imza/paket/izin
metinleri. Git commit yok; teslim kimliği APK hash'idir.

## Değişen davranış

Kitap geneli, fiziksel sayfa ve pasaj/bölge notu. Alıntı snapshot'ı gövdeden ayrı;
Notlar ve Altı çizilenler aynı passage note UUID'sini açar. Seçimden not,
kalıcı underline/region anchor ve draft'ı tek transaction'da oluşturur.
Kitaplık kartı, reader ve Sayfalar workspace içindeki Notlar sekmesi erişim
sağlar. Arama/scope/draft filtresi, tarih/page sırası, 40 kayıtlık pagination ve
yalnız seçilen notta tam gövde vardır.

650 ms autosave draft, editor lease ve monoton content revision. Background,
back/explicit close ve cleanup son ref'i dispatch eder. Final Kaydet transaction
ile draft + final note + draft removal yapar; error/retry içerik kaybetmez.
Son başarılı draft reopen'da kurtarılır. Gönderilmemiş/committed olmayan son
tuşa OS kill garantisi yoktur. Boş/değişmemiş editör draft listesi oluşturmaz.

IME önce kapanır; editör ardından taslağı koruyarak kapanır. Safe inset, keyboard
avoidance ve scroll form; Save/Close footer ayrıdır. Uzun multiline field 220 dp
ile sınırlandırılır. 200 title / 50.000 body UTF-16 birimi, görünür sayaç ve
native validation. Sistem fontu/48 dp hedefler/live status ve reader'da koyu
modal. Editör yerel state'i parent reader'ı her tuşta yeniden render etmez.

Not silme/undo revision kontrollüdür. Notlu annotation için **notları koru** veya
**birlikte sil** açık seçimi vardır. Group tombstone aynı işlemde silinen
note/draft'ları undo'da restore eder; daha önce silinmiş notlar dirilmez.
Soft-deleted mark'a bağlı not quote/page ile erişilir; jump gerektiğinde page'e
gider. PDF eksik olsa da not editörü/liste çalışır; dosya açma ayrı hatadır.

## Kabul kriterleri

| Kriter | Uygulama/host kanıtı | Fiziksel kabul |
|---|---|---|
| M5-A01 | Gerçek SQLite book/page/passage scope, quote/body ayrımı; displayed-page ve selection-note UI testleri | INCOMPLETE |
| M5-A02 | Tek aktif passage note index, tab-independent UUID/lease testi; iki panel open API'si | INCOMPLETE |
| M5-A03 | Repository close/reopen dirty draft; AppState-before-debounce ve recovered mesajı Jest | INCOMPLETE |
| M5-A04 | Revision 2→1 reject, eski lease reject; late failure yeni ACK durumunu ezmiyor Jest | INCOMPLETE |
| M5-A05 | SQLite trigger save failure/rollback/retry; aynı commit idempotent; final save error UI retry | INCOMPLETE |
| M5-A06 | IME-first/back/latest payload Jest; reader session/page korunması; safe footer ve font kontrolleri | INCOMPLETE |
| M5-A07 | 1000 SQLite note + 50k body + literal/Türkçe search/pagination; 40-row replace ve lazy body UI | INCOMPLETE |
| M5-A08 | Preserve/joint delete/group undo + previously deleted note + dirty draft Native; açık seçenek UI testi | INCOMPLETE |
| M5-A09 | Book scope/remove isolation, v4 fixture→5 position/theme/bookmark/mark/quad; failed migration rollback | INCOMPLETE |
| M5-A10 | Önceki 24/4 MiB cache ve canonical bütçeler korundu; runtime örnekleri yok | INCOMPLETE |

14 yeni native NoteRepository testi + önceki 53; 31 UI testi önceki 22 ve yeni
debounce/ACK/lifecycle/IME/binding/pagination/delete-choice akışlarını içerir.
Host testleri gerçek SQLite davranışını simulated Android'de yürütür; gerçek
Android process kill, JNI/PDFium, keyboard window, predictive back, TalkBack,
50k text shaping veya APK install/update kanıtı değildir.

## Açık cihaz kapısı ve sınırlar

Huawei MatePad SE AGS5-W09 / HarmonyOS 3.0 kullanıcı beyanıdır. Android API,
ABI, gerçek RAM ve refresh ölçülmedi. Gerçek release'de cold/warm, ilk sayfa,
touch/panel/prepared selection/annotation commit p95, FPS/frozen frame,
bitmap/text counter, Java/native/GPU PSS ve uzun oturum ölçümü yoktur.
Canonical `performance-budgets.json` değiştirilmedi; ölçülmemiş değerler sıfır
veya PASS gibi yazılmadı. Host test/derleme süreleri ürün timing ölçümü değildir.

Kalan senaryolar: aynı imza ile M4→M5 install-r update; kişisel olmayan corpus'ta
üç note scope; typing background/screen lock/force-stop/cold reopen; son commit
taslağı; IME + iki OS kenarı + predictive cancel; large font/TalkBack ve landscape
keyboard; 1000 mark/note/50k body ile render/SQL rekabeti; disk dolu ve grup
sil/undo sonrası reopen; 30–60 dk / 500 turn / 20 zoom / 10 rotation.

MVP-4 skew/Unicode/text-layer/overlay sınırları korunur. Search stored title/body
içindir; OCR veya PDF content search değildir. Undo düğmeleri son UI silme
işlemine ait; tombstone/group modeli DB'de kalır. Editor token local ve geçicidir;
backup/restore taslağın lease'ini yeniden kurmalıdır. Yedek/export ve birleşik
V1 doğrulaması MVP-6; uygulamayı kaldırmak yerel çalışma verisini siler.

Detay: [MVP5_NOTES.md](../../architecture/MVP5_NOTES.md).
