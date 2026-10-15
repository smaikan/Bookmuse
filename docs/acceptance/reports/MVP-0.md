# MVP-0 kabul raporu

Tarih: 14 Ekim 2026. Durum: **Derleme doğrulandı; fiziksel cihaz doğrulaması bekliyor.** MVP-0 kabul edilmiş değildir. Bu rapor MVP-0 artifact'ının tarihsel kaydıdır; sonraki geliştirme [MVP-1 raporunda](MVP-1.md) izlenir.

## Üretilen çıktı

- React Native uygulaması `mobile/` içinde.
- İmzalı, R8/resource shrink uygulanmış release APK: `artifacts/mvp-0/kitap-mvp0-arm64-v8a.apk`.
- Boyut: **20.624.312 byte = 19,6689 MiB**; başlangıç 30 MiB APK hedefi karşılanıyor.
- SHA256: `685434f99f6db97b407f3adaca3b6ce60a1fbfb11da847299e5e4c730b35b915`.
- Package `com.kitap`, versionCode 1, versionName 1.0, minSdk 24, targetSdk 36, compileSdk 37.
- İlk artifact yalnız ARM64 (`arm64-v8a`). Tablet ABI'si henüz ADB ile ölçülmedi; bu dosya bütün 32-bit cihazlara kurulabilir diye sunulmaz.
- Debug anahtarı yerine repository dışında kalıcı release anahtarı kullanıldı. APK Signature Scheme v2 doğrulandı; minimum API 24 için v2 desteklenir.

## Bu prototip ne yapıyor?

Özgün Türkçe örnek PDF'yi cihazda oluşturma; aynı PDFium belge oturumunda render ve metin/karakter konumu çıkarma; Orijinal/Sepya/Gece native filtresi; aç/kapat; 20 aç/kapat ve text eşleşmesi denemesi. Bitmap JS'ye taşınmaz; tek bounded worker, generation, 24 MiB owned-buffer bütçesi ve fit çözünürlük sınırı vardır.

Bu kaynak davranışları ve derlemeleri hazırlanmıştır; APK'nın fiziksel tablette gerçekten çalıştığı henüz gözlenmedi. PDF seçicisi/kişisel kitaplık MVP-1, tam selection/highlight MVP-4, notlar MVP-5'tedir. UI'da görülen render ms ve bitmap MiB tüm uygulama startup/PSS ölçümü değildir.

## Çalıştırılan kontroller

| Kontrol | Sonuç | Kanıt |
|---|---|---|
| TypeScript strict (`npm run typecheck`) | PASS | Komut exit 0 |
| ESLint (`npm run lint`) | PASS | Komut exit 0 |
| Jest UI davranışları | PASS, 2 test | `mobile/__tests__/App.test.tsx`: native hata sonrası retry; session aç/kapat |
| Kotlin/JUnit bütçe | PASS, 2 test | `ByteBudgetTest`: inflight/visible byte sınırı, double-release ve overflow |
| Kotlin/JUnit fit boyutu | PASS, 3 test | `RenderSizeTest`: piksel üst sınırı/aspect ratio, farklı viewport, invalid viewport |
| Fabric/TurboModule Codegen | PASS | Gradle `generateCodegenArtifactsFromSchema`; Java/C++ generated specs |
| Kotlin/C++/release build | PASS | Gradle `BUILD SUCCESSFUL`, 154 actionable tasks; `build-result.json` |
| APK imzası | PASS, v2 | [MVP-0-signature.txt](MVP-0-signature.txt) |
| Package/ABI/permissions | PASS, statik | [MVP-0-package.txt](MVP-0-package.txt); INTERNET izni yok |
| Paket içinde JS/Hermes/PDFium | PASS, statik | [MVP-0-apk-inspection.json](MVP-0-apk-inspection.json) |
| ARM64 ELF LOAD hizalama | PASS, 13 native kütüphane ≥16 KB | Aynı inspection JSON |
| ZIP native data alignment | PASS, 16 KB; resmi zipalign exit 0 | Aynı inspection JSON + `zipalign -c -P 16 4` |
| Fiziksel kurulum/launch/render/text | INCOMPLETE | ADB'de yetkili cihaz yok |
| Gerçek total PSS/peak/OOM/ANR | INCOMPLETE | Fiziksel örnekler toplanmadı; sıfır uydurulmadı |

Jest native modülleri mock eder. JVM testleri gerçek PDFium'u veya Android bitmap üretimini yürütmez. Bunların yedi testinin geçmesi, PDF render veya Türkçe extraction cihaz kabulü değildir. 16 KB statik hizalama da yeni Android'de runtime testinin yerine geçmez.

## Kriter bazında durum

| Kriter | Durum | Açıklama |
|---|---|---|
| M0-A01 | INCOMPLETE | Kullanıcı modeli/OS verdi; gerçek API/ABI/RAM/ekran henüz ölçülmedi |
| M0-A02 | INCOMPLETE | Release APK ve offline paket hazır; Huawei'de kurulum/uçak modu launch yok |
| M0-A03 | INCOMPLETE | Native renderer derlendi; fiziksel sayfanın okunabilirliği görülmedi |
| M0-A04 | INCOMPLETE | Native quote/hit-test API bağlandı; Türkçe text match/konum sonucu cihazda alınmadı |
| M0-A05 | INCOMPLETE | Aynı bitmap için üç filtre yazıldı; cihazda görüntü ve resim davranışı görülmedi |
| M0-A06 | INCOMPLETE | 20 lifecycle probe ve kaynak sahipliği mevcut; native/FD/bitmap sızıntı ölçümü yok |
| M0-A07 | PASS, teknik karar kaydı | Pinned stable wrapper/source/POM, lisans haritası, ABI hizalama ve birlikte release derlenmiş matrix [teknoloji kaydında](../../architecture/MVP0_TECHNOLOGY.md) |
| M0-A08 | INCOMPLETE | APK boyutu geçti; gerçek baseline/reading/peak PSS ve runtime sayaç örnekleri yok |

Native engine'in tüm üçüncü taraf notice paketi ve fiziksel destek matrisi final dağıtımda tamamlanmalıdır. Bu rapor hukuk görüşü veya bütün PDF çeşitlerinin desteklendiği iddiası değildir.

## Cihaz bilgisi

Kullanıcı beyanı: Huawei MatePad SE AGS5-W09, HarmonyOS 3.0; performans referansı 2 GB RAM. Android API ve ABI HarmonyOS etiketinden türetilmedi. [Reference device](../reference-device.json) kullanıcı beyanı, [device inventory](../device-inventory.json) ADB gözlemidir; şimdilik `no_authorized_device`.

## Fiziksel doğrulama için sıradaki işlem

1. USB debugging ile yetkilendirilmiş hedef Huawei'yi bağla; `scripts/inspect-device.ps1` ile model/API/ABI/RAM doğrula.
2. ARM64 uyumluysa bu APK'yı kaldırmadan `adb -s <hedef> install -r <APK>` ile kur. ABI farklıysa uygun ABI artifact'ı derle; yanlış tablet seçme.
3. Metro kapalı ve uçak modunda aç; Örnek sayfayı aç → okunabilirlik, Türkçe doğru/konum ≥0 → üç tema.
4. 20 aç/kapat → 20/20 text eşleşmesi; render/close/rotation yarışları ve FD/native/cache sayaçları.
5. Aynı cihazda boş/örnek sayfa baseline, reading/peak PSS ve runtime crash/ANR/OOM kanıtı topla. MVP-0 aktif key'lerini canonical bütçeye göre değerlendir.
6. Kalan kriterleri gerçek kanıtla güncelle; başarı olmadan roadmap'i Kabul edildi yapma.

Sırlar/key parolası rapora yazılmaz. APK linki sideload denemesi içindir; kullanıcı bir ekran görüntüsü iletirse görüntü davranışı değerlendirilebilir, bu total PSS kabulünün yerine geçmez.
