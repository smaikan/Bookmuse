# MVP-3 kabul raporu

14 Ekim 2026. Kaynak, host kontrolleri ve imzalı ARM64 release derlemesi tamamlandı.
Fiziksel Huawei kabulü açık; MVP-4 geliştirmesi kullanıcı talebiyle sürer.

## Teslim ve kanıt

- `artifacts/mvp-3/kitap-mvp3-arm64-v8a.apk`, sürüm 0.3.0 / versionCode 4.
- Boyut **22,902803 MiB**; SHA256 `f91f4a58f50c965fa397675b7b73a15622668fe1534be50edc8e6923c25f53f9`.
- 14 Jest + 36 JUnit/Robolectric = **50 host testi geçti**. TypeScript ve ESLint hata yok; ESLint üç görsel stil/callback uyarısı verdi.
- Aynı kalıcı imza, v2 doğrulaması, 15 ARM64 native library, ELF/ZIP 16 KB hizalaması ve zipalign geçti. Release INTERNET veya geniş dosya erişim izni istemiyor.
- Ham kanıt: `MVP-3-jest.json`, `MVP-3-junit/`, `MVP-3-verification.json`, `MVP-3-apk-inspection.json` ve imza/paket/izin raporları. Derleme zamanı 10:28:07; doğrulama scripti gerçek APK üzerinde çalıştırıldı.

## Uygulanan davranış

Sayfalar panelinde sayfa sayısı büyüklüğünde JS dizi yok: VirtualizedList yalnız
görünür satırları oluşturur. Native küçük resimler görünürlükle etkinleşir;
thumbnail RAM 2 MiB, toplam bitmap 24 MiB, disk LRU 64 MiB. Slider yalnız
bırakıldığında gezinir. İç 48 dp tutamaç iki sistem geri kenarının dışındadır.
Yer imleri SQLite'ta kitap/sayfa bazında tekildir. Kitaplık SQL pagination,
Türkçe başlık araması, literal joker karakterler, sıralama, ad düzenleme,
liste/ızgara tercihi ve son okunan kitaba devam sunar.

Geniş yatay pencerede isteğe bağlı çift sayfa aynı motor oturumunu kullanır;
iki pane en fazla 750 bin piksel hedefler, komşu prefetch kapalıdır. İki sayfa
hazırlanınca birlikte sunulur, yalnız aktif fiziksel sayfa konumu commit edilir.
Kapak tek sayfa tercihi ve sol el tutamacı/paneli vardır.

## Kabul durumu

| Kriter | Kod/host kanıtı | Fiziksel durum |
|---|---|---|
| M3-A01–A02 | İç tutamaç, düğme, sistem insets ve modal geri | INCOMPLETE |
| M3-A03–A04 | 2000 sayfa virtual list ve slider-release Jest; byte cache testleri | INCOMPLETE |
| M3-A05 | SQLite idempotent bookmark/reopen/book-scope testleri | INCOMPLETE |
| M3-A06 | Türkçe/literal arama, rename, son okunma, SQL pagination testleri | INCOMPLETE |
| M3-A07 | Spread boundary ve paired prepared/ready/commit sırası testleri | INCOMPLETE |
| M3-A08 | Pencere/font ölçeğine göre panel ve iç hit alanı uygulandı | INCOMPLETE |
| M3-A09 | Sayısal performans bütçeleri değiştirilmedi | INCOMPLETE |

ADB ile yetkili Huawei yok; gerçek PDFium render, çift sayfa dokunma, TalkBack,
predictive back, APK update, panel p95, FPS/PSS ve uzun oturum ölçümü yapılmadı.
Host testleri veya statik APK incelemesi bunların yerine geçmez. MVP-0/1/2
APK'ları korunmuştur; tam V1 veya fiziksel kabul iddiası yoktur.
