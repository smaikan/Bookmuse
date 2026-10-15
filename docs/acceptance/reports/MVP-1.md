# MVP-1 kabul raporu

14 Ekim 2026. Durum: **Kaynak, 26 host testi ve imzalı release derlemesi doğrulandı. Fiziksel kabul verilmedi.** Git repository henüz başlatılmadığı için commit SHA yok; APK SHA256 aşağıda kaydedilir.

## Artifact

- İmzalı APK: `artifacts/mvp-1/kitap-mvp1-arm64-v8a.apk`, **22,7786 MiB**; 30 MiB statik boyut hedefi geçti.
- SHA256: `9291f7fe946ae54980007c6f146687466e8bbda06c5af9985dba014c9850cc31`.
- Package `com.kitap`, versionCode **2**, versionName **0.1.0**, minSdk 24, targetSdk 36, compileSdk 37 minor 0.
- Yalnız `arm64-v8a`; gerçek Huawei ABI'si henüz ölçülmedi. 32-bit veya tüm Android cihazlarında kurulum iddiası yok.
- APK v2 imzası doğrulandı. Sertifika SHA256 `1a1f883707fe5009a8c25582a2ca0fa9ee55c844d802936f4b311a8a2502dc5c`: MVP-0 ile aynı. Fiziksel update testi ayrıca bekliyor.
- R8/resource shrink ve release lint-vital geçti; Gradle BUILD SUCCESSFUL, 221 görev. JS bundle, Hermes ve PDFium paket içinde, Metro bağımsız çıktı.
- 15 ARM64 native kütüphane ELF LOAD ≥16 KB; stored ZIP offset ve resmi `zipalign -c -P 16 4` geçti. Yeni Android'de runtime 16 KB kabulü ayrı.
- Paket izin listesinde INTERNET ve geniş depolama izni yok; yalnız uygulamanın DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION kaydı var.
- Önceki MVP-0 APK'sı kendi klasöründe korundu. [Build sonucu](../../../artifacts/mvp-1/build-result.json), [statik inspection](MVP-1-apk-inspection.json), [imza](MVP-1-signature.txt), [package](MVP-1-package.txt), [izinler](MVP-1-permissions.txt).

## Kullanıcıya gelen davranış

- Sistem PDF seçicisinden gerçek kitap ekleme. Uygulama 64 KiB native buffer ile özel kopya tutar; ek disk kullanımı açıklanır. Dış orijinal değişmez.
- Küçük sorgular ve FlatList ile kitaplık. Kitap adı, kaydedilmiş fiziksel sayfa ve dosya boyutu; aynı URI tekrar eklenince duplicate oluşturmaz.
- PDFium ile istenen tek sayfayı sığdırarak çizme, önceki/sonraki, sınır kontrollü numarayla gitme. Hazırlanırken önceki görüntü/sayaç korunur.
- Sayfa onDraw sonrası yerleşince native SQLite konum kaydı. Her kitap bağımsız; reopen son başarılı commit'e gider. Background'da PDF view ve bitmap referansı bırakılır.
- İptal, boş/bozuk/parolalı PDF ve provider/kopya hataları; geçici/final orphan dosyaları temizleme. Yerel kopya eksik veya değişmişse aynı PDF'ye SHA256+boyut+sayfa sayısıyla yeniden bağlama.
- Native stack kitaplık/okuyucu ekranları; PDF yüzeyinde MVP-1'de swipe recognizer veya sistem kenarı exclusion rect'i yok. Orijinal/Sepya/Gece oturum filtresi, etiketli 48 dp düğmeler ve kaydırılabilir numara penceresi.

Kaydırma, zoom, kalıcı özel renk/crop MVP-2; sayfa paneli MVP-3; altını çizme MVP-4; notlar MVP-5; yedek MVP-6. Bunları bu teslimin özelliği olarak göstermiyoruz.

## Host doğrulama

| Kontrol | Sonuç | Kanıt ve sınır |
|---|---|---|
| TypeScript strict | PASS | npm run typecheck exit 0 |
| ESLint | PASS | npm run lint exit 0; üretilmiş Android build JS'leri ignore edilir |
| Jest | PASS, 7 test | [MVP-1-jest.json](MVP-1-jest.json); native hatadan retry, restored session/page, import cancel, settled counter, stale session/page reddi, render hata/retry, geçersiz numara |
| Streaming copy / cancel | PASS, 6 JVM test | [AtomicPdfCopy XML](MVP-1-junit/TEST-com.kitap.pdf.AtomicPdfCopyTest.xml); hash, provider I/O hata, midstream cancel, boş dosya, commit sınırı, mevcut dosyayı koruma |
| SQLite repository | PASS, 6 Robolectric test | [Repository XML](MVP-1-junit/TEST-com.kitap.pdf.BookRepositoryTest.xml); reopen/iki kitap/pagination, URI duplicate, injected position-write rollback, remove/orijinal koruma, startup orphan cleanup, relink/invalid page |
| Fiziksel sayfa sınırı | PASS, 2 JVM test | [PageIndex XML](MVP-1-junit/TEST-com.kitap.pdf.PageIndexTest.xml) |
| Önceki bütçe/fit regressions | PASS, 5 JVM test | ByteBudget 2 + RenderSize 3; [host özeti](MVP-1-junit-summary.json) |
| Codegen + Kotlin/C++ | PASS | TurboModule, Fabric page/session olayları ve screens entegrasyonu; Gradle BUILD SUCCESSFUL |
| Release R8/shrink, imza, paket/hizalama | PASS, statik | Üstteki artifact ve ayrı imza/inspection/package kanıtları |
| Huawei kurulum/SAF/PDFium/okunabilirlik | INCOMPLETE | Yetkili ADB cihazı yok |

Robolectric 4.17 API 28 üzerinde gerçek repository/SQLite kodunu simüle Android ortamında çalıştırır. Standart test Application kullanır; RN/Hermes/PDFium Android .so kütüphaneleri Windows JVM'de yürütülmez. Jest native modülleri mock eder. Host süreleri uygulama startup veya dokunma benchmark'ı değildir.

## Kriter bazında durum

| Kriter | Durum | Uygulanan/host kanıtı ve bekleyen doğrulama |
|---|---|---|
| M1-A01 | INCOMPLETE | SAF seçici, özel kopya ve tek renderer derlendi; tablette metin/tarama PDF seçme-açma yok |
| M1-A02 | INCOMPLETE | Stream hata/iptal/cleanup ve DB transaction fault injection host'ta geçti; gerçek provider ve dolu disk senaryosu bekliyor |
| M1-A03 | INCOMPLETE | Sınır/numara/stale sonuç/sayaç host'ta geçti; gerçek PDF'de ilk/orta/son sayfa eşleşmesi bekliyor |
| M1-A04 | INCOMPLETE | SQLite reopen konumu ve onDraw sonrası yazı uygulanmış; native background/OS kill cihaz testi bekliyor |
| M1-A05 | INCOMPLETE | İki kitap DB konumu + eski session/page olayı test edildi; render sürerken hızlı switch cihaz testi bekliyor |
| M1-A06 | INCOMPLETE | Unique URI, rollback, private copy silme/orijinal koruma host'ta geçti; gerçek picker duplicate/kaldır denemesi bekliyor |
| M1-A07 | INCOMPLETE | Özel kopya dış taşıma/izin kaybından bağımsız; relink ID/konum host'ta korunuyor, native fingerprint doğrulaması yazıldı; erişim kaybı cihaz denemesi yok |
| M1-A08 | INCOMPLETE | Boş stream test edildi, PDFium validation ve error UI uygulandı; gerçek bozuk/parolalı corpus'ta sonuç yok |
| M1-A09 | INCOMPLETE | Tek aktif document, lazy sayfa, 1,5 MP fit, 24 MiB owned-buffer sınırı; 100–500 sayfa Huawei süre/PSS ölçümü yok |
| M1-A10 | INCOMPLETE | Aynı package/sertifika ve versionCode 2 statik doğrulandı; TalkBack/font/önceki APK üstüne fiziksel update yok |

INCOMPLETE, başarısız ölçüm veya kabul edilmiş ürün anlamına gelmez. Bütün fiziksel M1 kriterleri açık kalır.

## Aktif performans hedefleri

Canonical kaynak [performance-budgets.json](../performance-budgets.json). M1'de reading_pss_mib, peak_pss_mib, bitmap_cache_mib, derived_text_cache_mib, device_apk_mib, crashes, anrs, ooms; ayrıca cold_library_ms, warm_resume_ms, first_readable_page_ms, touch_feedback_ms aktiftir. device_apk_mib statik artifact boyutu **22,7786 MiB ≤30 MiB**; cihaz uyumu hâlâ açık. Diğer metriklerde gerçek örnek yok: ölçüm/medyan/p95/peak için boş kanıtı sıfırla doldurmadık. Sonraki panel/annotation metrikleri bu aşamada ölçülmüş gibi sunulmaz.

24 MiB allocator limiti runtime total PSS değildir. Native parser, RenderThread/GPU ve JS ayrıca ölçülecek; bitmapin app referansını bırakmak anında fiziksel RAM'in sıfırlanmasını garanti etmez.

## Cihaz ve bekleyen deneme

Kullanıcı beyanı Huawei MatePad SE AGS5-W09, HarmonyOS 3.0, 2 GB RAM referansı. Android API/ABI/gerçek RAM/Hz bilinmiyor; HarmonyOS sürümünden türetilmedi. Son [ADB envanteri](../device-inventory.json): no_authorized_device.

1. Doğru Huawei'yi yetkilendirip model/API/ABI/RAM ölç. ABI uygun release APK'yı uygulamayı kaldırmadan kur/güncelle; Metro kapalı, uçak modu.
2. Yerel metin ve tarama PDF ekle; aynı URI'yi tekrar seç. Kopyayı iptal et, sağlayıcı/disk hatası dene; kitaplıkta yarım kayıt/dosya yok.
3. İlk/orta/son sayfa, geçersiz sayı, 42. sayfada kitaplığa dönüş/reopen; ikinci kitapta farklı konum; background/kill sonrası son commit.
4. Dış orijinali taşı/sil: kopya açılır. Yerel kopya kaybında aynı PDF relink; farklı baskı reddedilir. Kaldırınca dış orijinal kalır.
5. Sağ/sol sistem geri, font büyütme/TalkBack, yatay/telefon/tablet/numara klavyesi. Predictive back/cancel modern cihazda ayrıca.
6. Release tipik PDF ile 20 cold/warm/first-page/touch örneği, total/peak PSS ve runtime crash/ANR/OOM kanıtı; canonical hedeflerle karşılaştır.

[Dosya ve konum sözleşmesi](../../architecture/MVP1_STORAGE.md), [MVP planı](../../../MVP_PLANI.md).

## Açık araç bağımlılığı bulgusu

`npm audit --omit=dev` 20 high kaydı bildirdi; bu sayım bağımlılık zincirindeki
paketleri de sayar. İncelenen kök bulgu braces 3.0.3'ün iç içe glob pattern DoS'udur;
CLI/Metro/Jest dosya eşleştirme zincirinde bulunur. [Audit özeti](MVP-1-node-audit-summary.json),
[upstream advisory](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm).
14 Ekim 2026 advisory'sinde yama sürümü belirtilmemiştir. npm'in önerdiği eski RN/CLI
sürümlerine force downgrade yapılmadı. PDF adını glob pattern olarak Node araçlarına
gönderen uygulama akışı yok; bu analiz APK için tam güvenlik kabulü yerine geçmez.
Uyumlu upstream düzeltme çıktığında toolchain/lockfile ve ilgili build kontrolleri
yeniden değerlendirilir.
