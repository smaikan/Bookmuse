# MVP-0 teknoloji kararı ve yeniden derleme

14 Ekim 2026. Fiziksel kabul henüz yapılmadı. Bu dosya seçilmiş/konfigüre edilmiş araçları kaydeder; derleme ve cihaz sonuçları `docs/acceptance/reports/MVP-0.md` içinde ayrıca tutulur.

## Seçimler

| Alan | Sürüm/karar |
|---|---|
| React Native | 0.87.1, Community CLI |
| React | 19.2.3 |
| CLI | 20.2.0 |
| Node | Bu bilgisayarda 26.2.0; RN uyumlu engine aralığı package.json içinde |
| JDK | Temurin 17.0.20.1+1, proje araç dizininde |
| Gradle | 9.4.1, checked-in wrapper |
| Android Gradle Plugin | RN Gradle plugin sürüm kataloğunun 9.2.1'i |
| Kotlin compiler | 2.4.10 açık classpath sürümü; PDFium 2.4 metadata'sına uyum |
| SDK | android-37.0, target 36, min 24 |
| Build tools / NDK | 37.0.0 / 27.1.12297006; safe-area modülünün 36 build tools bağımlılığı da kurulur |
| PDF | io.legere:pdfiumandroid:2.0.3 (kararlı), aynı oturumda render+text |
| JS/native sınırı | Fabric view + Codegen TurboModule, bitmapler JS'ye taşınmaz |
| İlk APK | arm64-v8a; gerçek tablet ABI'si ADB ile henüz doğrulanmadı |
| Kimlik | com.kitap, teknik prototip boyunca aynı imza/package; kesin marka seçilmedi |

Template'in `platforms;android-37` adı mevcut SDK paket kataloğunda `platforms;android-37.0` olarak yayımlanmıştır. Gerçek paketin adı kullanılır. Root Kotlin version değişkenini yazmak tek başına derleyiciyi seçmez; Kotlin plugin classpath açık 2.4.10'dur. Metadata check kapatılmaz ve PDF motoru yerine ikinci engine eklenmez.

AGP 9'un built-in Kotlin/DSL geçişi için RN 0.87 template'indeki opt-out'lar korunmuştur; AGP 10'a rastgele yükseltme yapılmaz. Bu uyumluluk alanı final destek matrisi için yeniden değerlendirilir.

## Motor sınırı

MVP-0 yalnız cihazda üretilen özgün örnek PDF'yi açar. Gerçek kullanıcı dosya seçicisi MVP-1'dedir. Aynı native document görüntüleme ve text için kullanılır. Bir worker, bounded queue, generation ile eski sonuç reddi, 24 MiB buffer bütçesi ve 1,5 megapiksel fit üst sınırı vardır. Bu sınırlar total PSS/GPU/native parser belleği yerine geçmez.

Örnek PDF Android PdfDocument ile oluşturulur; Android PdfRenderer ile görüntülenmez. PDFium belgeyi açınca descriptor ownership belgeye geçer, engine ikinci kez FD kapatmaz. Text/page önce, document sonra kapanır. Render edilmiş bitmapler Android RenderThread'in hâlâ referans tutabilmesi nedeniyle elle recycle edilmez; uygulama sahipliği bırakılır, gerçek PSS ayrıca ölçülür.

UI Orijinal/Sepya/Gece, aç/kapat, text extraction ve 20 lifecycle denemesi sunar. Gösterilen ms yalnız native page render süresidir, tüm uygulama açılışı veya kabul percentile'ı değildir. Örnek alıntı özgün ve kişisel veri değildir.

## Yerel build

Proje kökünde PowerShell:

```powershell
& .\scripts\bootstrap-android.ps1 -InstallSdk
Push-Location .\mobile
npm ci
npm run typecheck
npm run lint
npm test -- --runInBand
Pop-Location
& .\scripts\build-android.ps1 -Milestone mvp-0
& .\scripts\inspect-device.ps1
```

Araçlar varsayılan olarak kullanıcı profilindeki `.codex/toolchains/kitap-android` altında; sistem JAVA_HOME/PATH kalıcı değiştirilmez. SDK/JDK indirmelerinin upstream checksum'u kontrol edilir. Modern Android CLI komutlarında telemetry `--no-metrics` ile kapalıdır. ToolchainRoot parametresi desteklenir; engine bağımlılıkları Maven Central üzerinden Gradle ile çözülür.

Build script Codegen, Kotlin/JUnit ve shrink edilmiş release APK üretir. npm kontrolleri yukarıdaki ayrı adımlardır. APK `artifacts/mvp-0/` altında; build sonucu hash/boyut/ABI içerir. `-SkipTests` geliştirme kolaylığıdır, test geçti kanıtı değildir.

Bu dosya MVP-0'ın tarihsel araç/ürün kaydıdır. Güncel kaynak MVP-5'e geçti;
`-Milestone` çıktı klasörü adını seçer, eski kaynak sürümünü geri getirmez.
Önceki MVP-0 APK'sı korunur. MVP-1 API37 minor0 yapılandırma ve navigation
ekleri [MVP1_STORAGE.md](MVP1_STORAGE.md) içindedir. MVP-2/3/4 sözleşmeleri aynı
klasördeki MVP2_READING, MVP3_NAVIGATION ve MVP4_ANNOTATIONS dosyalarındadır.
Güncel release için `build-android.ps1 -Milestone mvp-5` ve ardından
`verify-android.ps1 -Milestone mvp-5` çalıştırılır. Wrapper'ın tek core 2.0.3
artifact'ı JNI için açıkça pinlenir; APK'da ikinci motor yoktur.

Signing key ve parolalı yapılandırma araç dizininin `signing/` klasöründedir, repository dışında. Kullanıcı bunları güvenli yedeklemelidir; burada parola/anahtar içeriği yazılmaz. Başka makinede aynı uygulamayı update etmek için aynı key gerekir; yeniden key üretip update başarıldı deme. Debug keystore release için kullanılmaz.

## Lisans ve bağımlılık izleme

PDFium Android wrapper'ı Maven POM'unda Apache-2.0; upstream PDFium BSD biçimli ve üçüncü taraf notice'ları ayrıca incelenecektir. React Native/React MIT, Guava Apache-2.0. Bu kısa kayıt tam lisans paketi değildir; dağıtımda engine'in native üçüncü taraf bildirimleri de sağlanmalıdır. Alpha 3.x yerine kararlı 2.0.3 kullanılır. Exact AAR/source sürümü üzerinden API doğrulanır; main branch API'si sürüm varsayımı değildir.

Kaynaklar: [React Native sürümleri](https://reactnative.dev/versions), [PDFiumAndroidKt](https://github.com/johngray1965/PdfiumAndroidKt), [pinned Maven POM](https://repo.maven.apache.org/maven2/io/legere/pdfiumandroid/2.0.3/pdfiumandroid-2.0.3.pom), [Android CLI](https://developer.android.com/tools/agents/android-cli).

MVP-5 notes/draft schema ve UI/lifecycle sözleşmesi [MVP5_NOTES.md](MVP5_NOTES.md) içindedir.
