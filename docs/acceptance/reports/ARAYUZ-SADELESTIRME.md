# Bookmuse — arayüz sadeleştirmesi

15 Ekim 2026. Kullanıcının son düzeltmesine göre menü beş seçeneklidir. İlk üç işlem korunur: **Notlar / Altı çizilenler / Favori / Adı düzenle / Kaldır**.

**Kaynak, imzalı APK ve aşağıda belirtilen emülatör kontrolleri tamamlandı.** Emülatör sonuçları bağlı JSON içinde ayrı izlenir. Bu rapor fiziksel Huawei veya genel performans kabulü değildir.

## Değişen davranış

- Kitaplık başlığında dişli: Ayarlar tek adımda açılır, alt sekmeler kaldırıldı. Aynı ekranda yalnız Okuma Görünümü ve Ekran başlıkları ve doğrudan seçenekler bulunur.
- Ayarlar sabit krem #F5F3ED ve yeşil kalır. Gece/özel koyu tema yalnız okuyucuyu etkiler. Krem seçeneği kaldırıldı; native tema kimlikleri 0/2/3/4 değişmedi, eski 1 uyumlu okunur.
- Beşli kart menüsünde ilk üç işlem ve liste/konuma gitme yolları korunur. Ad düzenleme mevcut adla açılır, kayıt hatasında editör açık kalır. Kaldır açık onay ister, dış PDF'yi silmez.
- Bağımsız kitap yönetimi ve yedek/depolama ekranı, özel JS/native backup/restore/export/cache temizleme/yalnız PDF kopyası silme API'leri kaldırıldı. Depolama, import, otomatik cache sınırları, SQLite tabloları ve kayıtlar korundu; schema 7 sabit.
- Sayfalar ekranındaki Yardımcı Kontroller ve özel state/tercih/zoom callback kaldırıldı. Üç sütun, numara, yer imleri ve not/işaret konumundan Okumaya dön korundu. ReaderScreen değişikliği yalnız artık kullanılmayan PagePanel zoom prop'unun kaldırılmasıdır; native PDF/gesture çizim yolu değiştirilmedi.
- Yeni UI veya PDF bağımlılığı eklenmedi; mevcut React Native, TypeScript, PDFium ve SQLite korunur.

## Kaynak kontrolleri

- TypeScript `tsc --noEmit`: geçti.
- ESLint: 0 hata; mevcut PagePanel/ReaderScreen inline style için 4 uyarı.
- Jest: 4 suite / **37 test** geçti. Beşli sıra, eski işlemler, ad başlangıcı/hata/tekrar deneme, silme onayı, global ayar ve ID, koyu bağlamda açık ayarlar, kaydetme hatası/çift basma, panel sanallaştırması ve zoom geri dönüşü.
- Native Robolectric/JUnit: **78 test**, 0 hata/başarısızlık. SQLite migration/revision, import ve dış PDF'nin korunması, not/işaret/konum/favori, legacy helper alanı ve tema ID uyumu, geometri/cache/gesture regresyonları dahil. Kaldırılmış yedek özelliğinin testleri artık uygulanabilir test sayısında değildir; önceki 126 sayısı bu sürüm için kullanılmaz.
- Codegen, Kotlin/JNI, R8/resource shrink ve imzalı release: geçti. Statik ELF/ZIP16 KB hizası, shrunk geometry JNI/DEX ve bundled Hermes/JS: geçti. Runtime 16 KB ayrıca test edilmedi.

Kanıtlar: [Jest](UI-SIMPLIFICATION-jest.json), [JUnit](UI-SIMPLIFICATION-universal-junit-summary.json), [paket](UI-SIMPLIFICATION-universal-package.txt), [imza](UI-SIMPLIFICATION-universal-signature.txt), [APK incelemesi](UI-SIMPLIFICATION-universal-apk-inspection.json), [JNI](UI-SIMPLIFICATION-universal-geometry-bridge.json).

## APK

- Sürüm **0.8.1 / code16**, görünen ad **Bookmuse**, package **com.kitap**. Kullanıcının sağladığı ikon bu APK'ya derlendi.
- [Universal ARM64+x86_64 APK](../../../artifacts/ui-simplification/kitap-uisimplification-universal.apk): **42.329075 MiB**, Metro'dan bağımsız release. ARM32 içermez.
- SHA256: `e34213877da2ab2eb0a1597db3f5b319a10b5aa191d157d190fb9fd98e6f2136`.
- Kalıcı signing cert SHA256: `1a1f883707fe5009a8c25582a2ca0fa9ee55c844d802936f4b311a8a2502dc5c`. Önceki imzayla aynı; uninstall yapılmadı.
- Önceki `artifacts/ux-mvp-6/` APK'ları ve raporları değiştirilmedi. Yeni derleme etiketi `ui-simplification`; build script varsayılanı eski UX6 çıktısını ezmez.

## Emülatör kapsamı ve açık kapılar

Android API36 x86_64, emulator-5580, 4014280 KiB toplam RAM, 4096 byte runtime sayfası; 800×1200 test penceresi. Bu ortam 2 GB Huawei kanıtı değildir. Fiziksel Huawei, tam TalkBack/geri kenarı matrisi, FPS/PSS/p95 ve 30–60 dakika stres testi bu değişiklik için yürütülmedi; geçti sayılmaz.

0.8.0/code15 → 0.8.1/code16 **install -r** güncellemesinde, ilk açılış sonrasında documents, reading_positions, annotations, annotation_ranges, notes, note_drafts, bookmarks, app_preferences ve book_preferences satırları kurulum öncesi özel snapshot ile tamamen aynı çıktı. **15 kitap, 7 işaret, 6 not, 0 taslak, 1 yer imi**, schema7 ve foreign_key_check korundu. Ham DB ve kişisel PDF/alıntılar rapora/repo'ya eklenmedi.

Ayrıntılı yürütülen işlev sonuçları [JSON](UI-SIMPLIFICATION-functional.json) içinde bulunur. Henüz yürütülmemiş senaryolar bu dosyada başarı olarak yazılmaz.

## Yürütülen ekran senaryoları

- Beşli menü sırası doğrulandı. Notlar ve Altı çizilenler açıldı/kapatıldı; favori değiştirildi ve eski durumuna döndürüldü. Mevcut ad başlangıcı ve iki yönlü yeniden adlandırmada kimlik/sayfa korundu.
- Üç sütunlu Sayfalar ekranında Yardımcı Kontroller/zoom düğmeleri yoktu. Kapatma konumu korudu; 60. sayfaya numarayla gidildi, yer imi eklenip kaldırıldı, başlangıçtaki 49. sayfaya dönülüp kapat/aç ile doğrulandı.
- Dişli doğrudan Okuma Görünümü/Ekran seçeneklerini açtı; kaldırılan kategori/yönlendirmeler ve Krem seçeneği yoktu. Orijinal/Sepya/Gece/Özel seçimlerinin her birinde ayarlar kenar yüzeyi #F5F3ED ölçüldü. Gece kaydedilip yeniden açıldı; iki farklı kitapta koyu okuma görünümü ve açık ayarlar doğrulandı.

İlk emülatör açılışında uygulama dışı System UI yanıt vermeme penceresi görüldü ve kapatıldı; bu durum Bookmuse performans ölçümü olarak kullanılmadı. UI sürücüsünün eski panel etiketi ve donanım klavyesinin `mInputShown=true` bilgisini görünür klavye sanması düzeltildi. Düzeltilmiş senaryolar tekrar yürütülür; tamamlanmamış denemeler başarı kanıtına dahil edilmez. Editör hedefleri metin girişinden önce taze hiyerarşiden alınır; kişisel içerik loglanmaz.

- Özel arka plan alanına `#000000` girildiği özel ekran görüntüsünde görsel olarak kontrol edildi; ayar zemini yine #F5F3ED ve yeşil kontroller sabit kaldı. Vazgeç ile kayıtlı Gece teması korundu.
- Kitaplık FlatList'ine `keyboardShouldPersistTaps="handled"` eklendi: arama odaklıyken ilk işlem dokunuşunun klavye tarafından tüketilmesi önlenir. Bu, kaldırma onayından vazgeçip aynı menüyü yeniden açma senaryosunda tespit edildi; yalnız JS güncellemesiyle yeniden paketlendi.

Son paket, arama odağının ilk dokunmayı tüketmesi düzeltmesini içerir; native/R8 çıktıları değişmedi, JS paketi ve imza yeniden üretildi. Menü/tema/üçlü sayfa senaryoları ilk 0.8.1 derlemesinde yürütüldü; son APK üzerindeki kaldırma/telefon/büyük yazı ve artifact/veri audit sonuçları bağlı JSON içinde ayrıca belirtilir.

- Son APK üzerinde sentetik kaldırma kitabı: ilk onayda Vazgeç kitabı korudu; menü ilk dokunuşta yeniden açıldı; ikinci onayda yalnız seçilen test kitabı kaldırıldı. `/sdcard/Download/kitap-tests/` içindeki dış orijinal PDF yerinde kaldı. Diğer kitap/not/işaret/taslak/yer imi sayıları korundu.

- Son APK: 360×800 telefon penceresinde ve 800×360 yatay pencerede %200 yazıyla menünün son Kaldır işlemine erişildi; Ayarlar doğrudan açıldı ve açık yüzey korundu. Kısa yatay menü kaydırıldı; erişilemeyen son işlem bırakılmadı. Test sonrası pencere 800×1200, yazı ölçeği 1.0'a geri alındı.

## Son bütünlük sonucu

Final APK SHA256'sı emülatörde kurulu `/data/app/.../base.apk` ile aynı çıktı. Başlangıçtaki bütün kitap, işaret/geometri, not, taslak, yer imi ve eski kitap tercihleri satırları korundu; bütün fiziksel sayfa indeksleri başlangıç değerindedir (okuma işlemleri güncellendiği için konum tarihleri değişebilir). 15 özel PDF kopyasının SHA256'ları veritabanındaki import fingerprint'leriyle tek tek eşleşti. `quick_check` ve `foreign_key_check` temiz. Uygulamaya ait crash eldeki crash buffer'da görülmedi; bu, ölçülmemiş OOM/ANR/FPS/uzun oturum kabulünün yerine geçmez.

Durum: kaynak/derleme ve raporlanan emülatör işlev kontrolü **geçti**; fiziksel Huawei ve performans kapıları **açık**. Son kod değişikliğinden sonra TypeScript ve 37 Jest tekrar geçti; native 78 test değişmeyen native girdiler için Gradle tarafından UP-TO-DATE doğrulandı. Yeni PDF/UI bağımlılığı eklenmedi. İmza ve statik APK kontrolleri final hash için tekrar yürütüldü.
