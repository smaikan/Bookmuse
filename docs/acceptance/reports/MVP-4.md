# MVP-4 kabul raporu

14 Ekim 2026. **Kaynak, imzalı release, host ve statik APK kontrolleri tamamlandı.**
Fiziksel Huawei kabulü **INCOMPLETE**; tam V1 veya ölçülmüş akıcılık iddiası yoktur.
Kullanıcının istediği MVP-2 → MVP-3 → MVP-4 geliştirme sırası yürütüldü.

## Artifact ve kontroller

| Alan | Gerçek sonuç |
|---|---|
| APK | `artifacts/mvp-4/kitap-mvp4-arm64-v8a.apk` |
| Version/package | 0.4.0 / versionCode 5 / com.kitap |
| ABI / min / target | arm64-v8a / API 24 / API 36; Huawei API/ABI ölçülmedi |
| Boyut | **22,942129 MiB**, 30 MiB başlangıç APK hedefinin altında |
| SHA256 | `9a2cac5c822579d8b6b35ee085b322c7d62d698f7d37847d9c006bacc222e2ec` |
| İmza | Aynı kalıcı release certificate; apksigner v2 doğrulaması geçti |
| Certificate SHA256 | `1a1f883707fe5009a8c25582a2ca0fa9ee55c844d802936f4b311a8a2502dc5c` |
| Native / 16 KB | 15 ARM64 library; tüm ELF ve ZIP alignment + zipalign geçti |
| Release bundle | Hermes, paketlenmiş JS ve tek PDFium mevcut; Metro gerekmiyor |
| İzinler | INTERNET ve geniş storage izinleri yok |
| TypeScript / ESLint | Strict typecheck geçti; lint **0 hata, 0 uyarı** |
| Host testleri | **22 Jest + 53 JUnit/Robolectric = 75 geçti**, sıfır failure |
| JNI shrink kontrolü | Release DEX'te beş exact JVM metot tanımı/public/native kontrolü ve ELF JNI export geçti |
| Fiziksel cihaz | ADB yeniden incelendi: `no_authorized_device` |

Build `scripts/build-android.ps1 -Milestone mvp-4`: Codegen, debug unit tests,
ARM64 CMake/JNI, R8/resource shrink ve assembleRelease başarıyla tamamlandı.
`scripts/verify-android.ps1 -Milestone mvp-4` gerçek son APK üzerinde çalıştırıldı.
AGP legacy DSL/Kotlin opt-out ve bazı Android API deprecation uyarıları var;
compile failure veya metadata check bypass yoktur. Git repository/commit SHA
yok; teslim kimliği gerçek APK SHA256'sıdır.

Ham kanıt: `MVP-4-jest.json`, `MVP-4-junit/`, `MVP-4-junit-summary.json`,
`MVP-4-verification.json`, `MVP-4-geometry-bridge.json`,
`MVP-4-apk-inspection.json`, `MVP-4-signature.txt`, `MVP-4-package.txt`,
`MVP-4-permissions.txt`. Önceki MVP-0/1/2/3 APK ve raporları korunmuştur.

## Uygulanan kullanıcı davranışı

- Uzun basma/düğmeyle gerçek PDF metin seçimi; native kulplar, kelime bazında
  erişilebilir aralık düğmeleri ve alıntı önizlemesi. Metadata beklerken de swipe
  seçim gesture'ına geçmez. Seçim ve hazırlanmakta olan seçim geriyle kapanır.
- Altını çiz/vurgula, dört renk, kopyala; tek sayfada çok satır ve Unicode scalar
  indeksleri. Taramada metin varmış gibi davranılmaz: manuel region, açıklama,
  resize kulpları ve büyüt/küçült düğmeleri.
- Kitap başına Altı çizilenler; Sayfalar panelinde sekme, okuyucuda düğme ve
  kitaplık kartında erişim. Sayfa, hesaplanmış satır parçası, quote preview,
  tür/renk/tarih; talep üzerine tam alıntı. Literal kayıt araması, page/date
  sırası, tür/renk filtreleri, 40'lık SQL pagination ve sabit büyüklükte liste.
- Konuma git gerçek quad odağına yönelir. Okumaya dön önceki fiziksel sayfa,
  zoom ve normalized odağı saklar. Kırpma işareti gizliyorsa açık açıklama ve
  kullanıcı eylemiyle kırpmayı sıfırlama yolu vardır.
- Renk/tür düzenleme ve revision kontrollü sil/geri al. Commit başarısızsa
  Kaydedildi gösterilmez, seçim kalır; aynı UUID/komutla explicit retry vardır.
- Persisted PDF quad/fingerprint/boxes/rotation/engine version/quote/context;
  migration reset yapmaz. Dosya eksik olsa bile alıntı listesi erişilebilir,
  jump hatası panelde açıklanır. Yanlış/stale oturum kaydetmez.
- Tema filtresi sonrası overlay; işaret başına bitmap yok. 24 MiB bitmap ve
  4 MiB derived-text sahiplik sınırı. Text yalnız aktif page ve talep üzerine;
  sayfa değişimi/detach/background/idle trim'de bırakılır.

## Kriter bazında kanıt

| Kriter | Kaynak/host kanıtı | Gerçek cihaz kabulü |
|---|---|---|
| M4-A01 | Native long press/handles/swipe ownership; Türkçe, combining/supplementary word-range ve pending-back testleri | INCOMPLETE |
| M4-A02 | Annotation+ranges transaction/reopen/idempotency; çift basma ve aynı komut retry Jest | INCOMPLETE |
| M4-A03 | Ortak normalized transform, post-filter overlay; baseline y inversion ve crop reset/jump host testleri | INCOMPLETE |
| M4-A04 | PDFium page-map + quarter-turn geometry; column/baseline split, Unicode/skew reject helper testleri | INCOMPLETE |
| M4-A05 | Jump/return page+zoom+normalized focus Jest; region/quad native focus | INCOMPLETE |
| M4-A06 | 1000 gerçek SQLite kaydı ile pagination/Türkçe/literal/scope/filter; native-only quad cursor | INCOMPLETE |
| M4-A07 | Revision stale reject + edit/delete/undo ranges testi ve UI commit/refresh/undo testi | INCOMPLETE |
| M4-A08 | Native scan region/manual provenance ve text'e dönüştürme reddi testi | INCOMPLETE |
| M4-A09 | DB trigger ile range insert hata/rollback/retry; fingerprint/range validation; stale event/retry UI testleri | INCOMPLETE |
| M4-A10 | İki kenar touch-down guard; iç 48 dp kulp ve pending/ready selection geri sırası | INCOMPLETE |
| M4-A11 | ByteBudget ve retained metadata testleri; bounded packed arrays/snapshot/overlay | INCOMPLETE |

Host PDF geometry testleri üretilmiş fixture koordinatlarıdır; gerçek PDFium
JNI render/text extraction Windows'ta yürütülmedi. Robolectric SQL/host testleri
native Android engine, GPU, gerçek gesture veya TalkBack kanıtı değildir.
Statik shrink kontrolü cihazda JNI smoke testinin yerine geçmez.

## Ölçümler ve sınırlar

Canonical `performance-budgets.json` değişmedi. Prepared selection p95 ≤100 ms,
annotation commit p95 ≤300 ms, panel p95 ≤250 ms, bitmap/text 24/4 MiB,
reading/peak PSS 160/220 MiB, delayed frames <%5 ve uzun oturum büyümesi ≤%10
Huawei release üzerinde henüz ölçülmedi. Jest/Gradle çalışma süreleri bu
ürün ölçümleri değildir; boş veya sıfır runtime ölçümü üretilmedi.

Key metadata sınırları: aktif sayfada 32.000 glyph, tek seçimde 4096 karakter ve
512 geometri parçası, native page overlay'de 8192 range. Aşım açık hata verir,
kayıt listesi korunur; büyük text layer için region alternatifi vardır. Arbitrary
skew quarter-turn quad gibi sunulmaz. Çok sütun/RTL/ligature için engine sırası
önizlenir; bütün corpus'ta quote/geometry doğruluğu kabul edilmemiştir.
Return anchor oturum içidir; process death sonrası son committed fiziksel page
açılır. Notlar ve taslaklar MVP-5, yedek/export/nihai recovery MVP-6'dır.

## Huawei üzerinde kalan senaryolar

1. Aynı imza ile eski release'i kaldırmadan APK güncelle; konum, theme/crop,
   kitaplık adı ve yer imleri korunmalı. Model/API/ABI/RAM/refresh ölç.
2. Türkçe 3 satır, ligature, çok sütun/RTL, non-zero box ve 90/270 rotation
   corpus'unda alıntıyı ve gerçek quad'ı karşılaştır. Skew/mapping hatası explicit.
3. Underline/highlight kaydet, kapat/aç; quote/page/style/quad aynı olmalı.
   Zoom/crop/tema/rotation/split-window'da görsel yer kayması olmamalı.
4. Sağ ve sol OS kenarında fit/zoom/pending selection/kulp/panel/IME geri;
   yanlış page/panel açılmamalı. Predictive back iptali konumu değiştirmemeli.
5. Region/kopyala/list-jump/Okumaya dön, büyük sistem fontu ve TalkBack;
   kırpılmış mark için sıfırla akışı ve klavye önünde erişilebilir kontroller.
6. 1000 tipik işaretli kitap, search/filter/page/date/edit/delete/undo; DB disk
   dolu/kill ve aynı kitap relink; eski quote listesi erişilebilir kalmalı.
7. 30–60 dk release oturumu, 500 page turn, 20 zoom, 10 rotation; Perfetto/
   dumpsys ile gerçek FPS/native/GPU/PSS, ilk sayfa, panel ve commit örnekleri.

Kaynak/motor sözleşmesi: [MVP4_ANNOTATIONS.md](../../architecture/MVP4_ANNOTATIONS.md).
