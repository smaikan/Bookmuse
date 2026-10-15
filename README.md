# Bookmuse

Kitap okumak için tam olarak istediğim PDF okuyucuyu piyasada bulamadığım için Bookmuse'u geliştirdim. Aradığım şey; reklamlarla bölünmeyen, sade ve akıcı bir arayüzü olan, telefonda ve özellikle tablette rahatça kitap okuyabileceğim bir uygulamaydı.

Bookmuse, bu ihtiyaca odaklanan reklamsız ve çevrimdışı bir Android PDF kitap okuyucu. PDF kitaplarını cihazından ekleyebilir, okuma görünümünü kendine göre ayarlayabilir, önemli bölümleri işaretleyip not alabilir ve kaldığın yerden devam edebilirsin. Hesap açman veya internet bağlantısı kurman gerekmez.

## Öne çıkan özellikler

- **Kitap okumaya odaklanan tam ekran:** Kontroller yalnız dokunduğunda görünür; sayfanın üstünde açılır ve içeriği küçültmez.
- **Dokunarak okuma:** Yumuşak kaydırmalı sayfa geçişleri, iki parmakla yakınlaştırma ve yakınlaştırılmış sayfada gezinme.
- **Kişiselleştirilebilir görünüm:** Orijinal, sepya, gece ve özel renkler; kontrast, okuma parlaklığı ve isteğe bağlı yatay çift sayfa görünümü.
- **Düzenli kitaplık:** Kapak ızgarası, başlık araması, sıralama, favoriler ve kart üzerinde okuma ilerlemesi.
- **Kaldığın yerden devam:** Kitabı kapatıp yeniden açtığında son okuduğun sayfaya dönme.
- **Metin seçme ve altını çizme:** Önemli satırları seçip fosforlu sarı kalemle işaretlenmiş gibi PDF üzerinde vurgulama, alıntıyı kopyalama ve kaydedilen bölümlere Altı çizilenler listesinden dönme. İşaretler uygulamada saklanır; orijinal PDF değiştirilmez.
- **Not alma:** Kitabın tamamına, belirli bir sayfaya veya seçilmiş bir pasaja not ekleme; kitap başına Notlar listesinde arama, düzenleme ve son kaydedilen taslağı kurtarma.
- **Kolay sayfa bulma:** Üç sütunlu sayfa önizlemeleri, numarayla gitme ve yer imleri; ek sıkıştırma hareketiyle sayfalara genel bakış.

## Altını çizme ve not alma

Okurken önemli bulduğun bir metne uzun basarak veya **Metin seç / Altını çiz** işlemini açarak seçim yapabilirsin. **Sarıyla işaretle** seçeneği, seçtiğin satırları fosforlu sarı kalemle işaretlenmiş gibi doğrudan PDF üzerinde gösterir. Kitabı kapatıp yeniden açtığında kayıtlı işaretler yerinde kalır.

Her kitabın kartındaki **⋮ → Altı çizilenler** bölümünde kaydettiğin alıntıları, hangi sayfada olduklarını ve satır parçası bilgisini görebilir, kayıtlı alıntılarda arama yapabilir ve **Konuma git** ile ilgili bölüme dönebilirsin. İşaretler uygulama içinde saklanır; orijinal PDF dosyasına yazılmaz.

Notlarını üç farklı şekilde tutabilirsin:

- **Kitap notu:** Kitabın geneliyle ilgili düşünceler, değerlendirmeler ve özetler.
- **Sayfa notu:** Belirli bir sayfayla ilişkili açıklamalar.
- **Pasaj notu:** Seçtiğin bir alıntıya bağlı yorumlar; metin seçimindeki **Pasaja not ekle** işlemiyle oluşturulur.

**⋮ → Notlar** bölümünden o kitaba ait notları bulabilir, başlık ve içerikte arama yapabilir, not türüne göre filtreleyebilir ve düzenleyebilirsin. Notlar ve işaretler silindiğinde geri alma seçenekleri bulunur. Yazarken kaydedilen son taslak, yarım kalan notuna devam etmene yardımcı olur.

Metin katmanı bulunmayan taranmış PDF'lerde bölge işaretleme ve sayfa notu kullanılabilir; uygulama OCR ile metin tanıma yapmaz.

React Native ve TypeScript ile geliştirilen uygulama, PDF görüntüleme için yerel PDFium motorunu ve kalıcı kayıtlar için SQLite'ı kullanır. Tablet önceliklidir; telefon ekranlarına da uyum sağlar. Hafiflik, düşük bellek kullanımı ve akıcı bir okuma deneyimi geliştirme öncelikleridir.

Görünen uygulama adı **Bookmuse**, geliştirme sürümünde **Bookmuse Dev**.
Teknik package ve kalıcı imza güncellemelerde korunur.
Referans cihaz kullanıcı beyanıyla **2 GB Huawei MatePad SE AGS5-W09 / HarmonyOS 3.0**;
fiziksel cihazın API/ABI/RAM ve performans doğrulaması açık.

## Güncel teslim

**Arayüz sadeleştirmesi — 0.8.1 / versionCode 16**, SQLite schemaVersion 7.
Kapak ızgarası; beş işlemli kart menüsü (Notlar / Altı çizilenler / Favori / Adı düzenle / Kaldır).
Kitaplık başlığındaki dişli doğrudan tek Ayarlar ekranını açar. Okuma Görünümü
ve Ekran seçenekleri aynı ekrandadır; ayarlar sabit krem/yeşil kalır. Krem tema
seçeneği ve üçlü sayfalardaki Yardımcı Kontroller kaldırılmıştır. Tam ekran PDF,
zoom/swipe, üç sütun, sarı işaretler ve not/taslaklar korunur. Yedek/depolama ve
bağımsız kitap yönetimi ekranları ile bunlara özel API artık yoktur.

- [Bookmuse 0.8.1 universal APK](artifacts/ui-simplification/kitap-uisimplification-universal.apk), 42,3291 MiB (ARM64+x86_64)
- [Güncel kapsam](docs/ARAYUZ_SADELESTIRME.md)
- [Derleme ve kontrol raporu](docs/acceptance/reports/ARAYUZ-SADELESTIRME.md)
- [Kullanım ve kayıt koruma](docs/KULLANIM_VE_KURTARMA.md)
- Önceki UX-MVP-6 APK'ları `artifacts/ux-mvp-6/` altında değişmeden korunur;
  0.8.0/code15 eski Kitap adını, eski ayar/yedek arayüzünü içerir.
- [Önceki UX kabul raporu](docs/acceptance/reports/UX-MVP-6.md)
- [Destek matrisi](docs/DESTEK_MATRISI.md)

Host ve emülatör kanıtı fiziksel Huawei kabulü değildir. Tam V1 kabulü ve
2 GB cihazda FPS/PSS/p95 hedefleri **açık**. Emülatör gerçek RAM'i yaklaşık
3920 MiB'dir. Npm bağımlılık denetiminin mevcut build/test bildirimleri önceki
[MVP-6 raporunda](docs/acceptance/reports/MVP-6.md) açıklanır.
Önceki MVP ve UX-MVP APK'ları kendi milestone klasörlerinde korunur.

## Uygulama ikonu

Kaynak [Bookmuse PNG](mobile/assets/branding/bookmuse-icon.png) Android normal,
yuvarlak ve adaptive ikonlarına bağlandı. Güncelleme sonrası proje kökünde
`.\scripts\update-launcher-icons.ps1` çalıştır; ardından yeni APK derle.
[İkon üretimi ve kontroller](mobile/assets/branding/README.md). Mevcut APK dosyaları
bu kaynak değişikliğiyle kendiliğinden güncellenmez.

## Kullanım

VS Code'da kodu düzenleyerek çalıştırmak için kök `kitap` klasörünü aç ve
**Ctrl+Shift+B** kullan. Ayrı **Bookmuse Dev** kurulur; TypeScript değişiklikleri
kaydedince emülatörde Fast Refresh ile görünür. [VS Code adımları](docs/VS_CODE_GELISTIRME.md).

1. PDF ekle → sistem seçicisi. Özel uygulama kopyası ek disk alanı kullanır;
   dış orijinal değişmez. Başka Android uygulamasında Aç/Paylaş → Bookmuse da kullanılabilir.
2. Kitaplık kartındaki ⋮ → Notlar / Altı çizilenler / Favoriye ekle–çıkar / Adı düzenle / Kaldır.
   Kart ilerlemesi son gerçekten gösterilen sayfadır. Kitap aynı yerden açılır.
3. Kitaplık başlığındaki **dişli → Ayarlar** bütün kitapların temasını,
   kontrastını, çift sayfasını, parlaklığını ve erişilebilirlik tercihlerini yönetir.
4. Okurken dokun: **Sayfaya not ekle / Kitaba not ekle / Metin seç–Altını çiz**
   ve ayrı Sayfalar simgesi açılır; PDF küçülmez. Tekrar boş alana dokununca kapanır.
5. İç alanda sürükle: sayfa çevir. Yakınlaştırılmışken tek parmak pan;
   iki parmak pinch veya çift dokunma zoom. İki dış kenar sistem geri içindir.
   1×'in altında ek sıkıştırıp bırak veya Sayfalar'a dokun: üç sütun genel görünüm.
   Kaydırıp kapatmak okuma yerini değiştirmez; bir sayfaya dokunmak onu açar.
6. Uzun bas veya Metin seç → alıntıyı kontrol et → **Sarıyla işaretle / Kopyala /
   Pasaja not ekle / Seçimi kapat**. Eski metin işaretleri de sarı gösterilir.
   Orijinal PDF değişmez. Metinsiz taramada bölge işareti/sayfa notu; OCR yok.
7. Notlar'da taslak son başarılı commit'i korur; **Notu kaydet** final notu oluşturur.
8. Ad değiştirme ve onaylı kaldırma kartın ⋮ menüsündedir. Kaldırma dış orijinal
   PDF'yi silmez; uygulama kopyasını ve o kitaba ait kayıtları kaldırır.
9. Yedekleme/depolama ve bağımsız kitap yönetimi bu sürümde kaldırılmıştır.


Yeni APK'yı **uygulamayı kaldırmadan** aynı uygulamanın üzerine yükle.
ABI'nin uygun olması gerekir; ARM64 fiziksel cihaz henüz ölçülmedi.
Metro, GMS/HMS, hesap ve internet gerekmez. Parola isteyen PDF desteklenmez.

## Yeniden derleme

Windows PowerShell, proje kökü:

```powershell
Push-Location .\mobile
npm ci
npm run typecheck
npm run lint
npm test -- --runInBand
Pop-Location
& .\scripts\bootstrap-android.ps1 -InstallSdk
& .\scripts\build-android.ps1 -Milestone ui-simplification
& .\scripts\verify-android.ps1 -Milestone ui-simplification
# İsteğe bağlı ARM64+x86_64 paket:
& .\scripts\build-android.ps1 -Milestone ui-simplification -Architectures 'arm64-v8a,x86_64'
& .\scripts\verify-android.ps1 -Milestone ui-simplification -Artifact universal
& .\scripts\inspect-device.ps1
```

`-Milestone` yalnız çıktı klasörünü seçer; kaynak sürümünü geri getirmez.
APK dosyaları, imza anahtarları, yerel SDK/cache çıktıları ve kişisel PDF/veritabanı
dosyaları Git'e eklenmez. `bootstrap-android.ps1`, yeni klonda eksik olan
`mobile/android/app/debug.keystore` geliştirme anahtarını yerel olarak oluşturur;
var olan anahtarı değiştirmez. Release imzası bu geliştirme anahtarından ayrıdır.
Tarihsel kabul raporlarındaki yerel APK yolları GitHub üzerinde indirilebilir dosya bağlantısı değildir.

Lockfile/toolchain sabitlenir. İmza anahtarı kullanıcı profilindeki private
`.codex/toolchains/kitap-android/signing/` altında; repo/loga yazılmaz.
Kişisel PDF/alıntı/not ve ekran görüntüleri kaynak kontrolüne girmez.

[Geliştirme sözleşmesi](AGENTS.md) · [MVP planı](MVP_PLANI.md) · [Ürün planı](UYGULAMA_PLANI.md).
[Toolchain](docs/architecture/MVP0_TECHNOLOGY.md) · [Dosya/konum](docs/architecture/MVP1_STORAGE.md) ·
[Okuma](docs/architecture/MVP2_READING.md) · [Kitaplık](docs/architecture/MVP3_NAVIGATION.md) ·
[İşaretler](docs/architecture/MVP4_ANNOTATIONS.md) · [Notlar](docs/architecture/MVP5_NOTES.md) ·
[Tarihsel yedek sözleşmesi — eski sürüm](docs/architecture/MVP6_BACKUP.md).
