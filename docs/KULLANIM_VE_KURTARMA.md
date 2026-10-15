# Kurulum, güncelleme ve kayıtları koruma

## Kurulum

Güncel kaynak **Bookmuse 0.8.1/code16** adını ve yeni ikonu içerir. Son APK ve
hash için [güncel raporu](acceptance/reports/ARAYUZ-SADELESTIRME.md) kullan.
`artifacts/ux-mvp-6/` eski Kitap 0.8.0/code15 teslimidir; yeni ekranları içermez.
Universal APK ARM64 ve x86_64 içerir; 32-bit ARM içermez. Cihaz ABI'si uygun olmalıdır. Huawei'nin bildirilen HarmonyOS
sürümünden Android API/ABI tahmini yapılmadı; gerçek cihaz doğrulaması açık.
Yüklemeden önce dosyanın rapordaki SHA256'sını karşılaştır.
Android'in bu kaynaktan APK yükleme iznini verip dosyaya dokun. Metro, hesap,
GMS/HMS veya internet bağlantısı gerekmez.

## Güncelleme

Asıl PDF dosyalarını uygulama dışında ayrıca sakla. Bu sürümde uygulama içi
JSON yedek/restore ve TXT/Markdown dışa aktarma kaldırılmıştır. Önceden üretilmiş
JSON dosyaları değiştirilmez fakat güncel arayüzde geri yükleme bulunmaz.
Yeni APK'yı mevcut uygulamanın üzerine yükle. **Uygulamayı kaldırma / verileri
temizleme**, özel PDF kopyalarını ve çalışma verilerini siler. Aynı package ve
kalıcı imza kullanılır; farklı imzalı APK üzerine yüklenmez. İmza key'i/parolası
repo/APK'ya eklenmedi; private toolchain signing klasöründe kullanıcı kontrolündedir.
USB yolu: tam doğru cihaz seri numarasını seçerek `adb -s SERIAL install -r APK`.
Fiziksel eski APK → yeni APK upgrade testi yapılmadan yapılmış kabul edilmez.

## PDF ekleme ve okuma

PDF ekle sistem seçicisini açar. PDF özel uygulama klasörüne güvenli kopyalanır;
ek disk alanı kullanır. Orijinal değişmez. Dosya sağlayıcıda/yavaş ağda hazırlanıyorsa
yüzde uydurulmaz; iptal düğmesi yarım kopyayı temizler. Başka Android uygulamasında
**Birlikte aç / Paylaş → Bookmuse** ile `application/pdf` content URI kabul edilir.

Kitaplık kartları kapak, başlık ve `(45/80)` ilerlemesini gösterir. Sağ üst ⋮
Notlar / Altı çizilenler / Favoriye ekle–çıkar / Adı düzenle / Kaldır içerir. Kitabı kapatıp tekrar açınca
son gerçekten gösterilen sayfadan devam eder.

Okuyucu tam ekrandır. Dokununca PDF üstünde Sayfaya not ekle / Kitaba not ekle /
Metin seç–Altını çiz ve ayrı Sayfalar simgesi görünür. PDF'nin ölçüsü değişmez.
Görünüm ayarları ana ekrandaki **kitaplık başlığındaki dişli → Ayarlar** ekranındadır;
bütün kitaplara uygulanır. Okuma Görünümü ve Ekran seçenekleri doğrudan aynı ekrandadır.
Ayarlar Gece veya koyu özel tema seçilince de sabit açık renkli kalır. Krem
seçeneği yoktur; diğer temalar korunur. Eski kitap başına ayarlar veride korunur.

İç alanda sürükleme sayfa değiştirir; zoom'da pan olur. İki parmak ve çift dokunma
yakınlaştırır. İki dış kenar sistem geri içindir. Normal ölçekte ek sıkıştırıp bırakma
veya Sayfalar simgesi üç sütunlu genel görünümü açar. Burada kaydırıp kapatmak
okuma sayfasını değiştirmez; sayfa seçme gerçekten gösterilince kaydedilir.

Uzun basma güvenilir metin katmanında kelime seçer. Sarıyla işaretle / Kopyala /
Pasaja not ekle / Seçimi kapat araçları bulunur. Bütün metin işaretleri sarıdır;
eski işaretlerin kimliği, geometrisi ve not ilişkileri korunur. İşaret uygulama içinde
çizilir; orijinal PDF'ye yazılmaz. Metin olmayan taramada bölge işareti ve sayfa
notu kullanılır. OCR yoktur.

## Not ve taslak

Genel, sayfa veya pasaj notu oluştur. Kaydet sonucu onaylanınca final not olur.
Yazarken debounce/lifecycle flush son başarılı taslağı korur. Zorla kill'de henüz
yazılmamış son tuş garanti edilmez. Klavye açıkken ilk geri klavyeyi kapatır.
Notlu işareti silerken notu koruma veya ikisini silme seçimi vardır; undo grubunu korur.

## Kitap adı ve kaldırma

Kartın ⋮ menüsünden Adı düzenle mevcut adı doldurur; kaydetme ilerlemeyi ve
notları değiştirmez. Kaldır onay sorar. Vazgeç hiçbir veri silmez; onay bu kitabın
uygulama kopyasını ve çalışma kayıtlarını siler. Dış orijinal PDF korunur.
Bağımsız Kitapları Yönet, Yedekleme ve Depolama ekranları kaldırılmıştır.
PDF yokken kartın Notlar / Altı çizilenler listelerindeki kayıtlar okunabilir.

## Hata kurtarma

Disk dolarsa alan aç ve işlemi yeniden dene; DB'yi temizleyerek çözmeye çalışma.
Bozuk/boş/parola isteyen PDF açık hata verir; parola isteyen PDF
bu sürümde desteklenmez. Büyük/ağır dosya veya cihazdaki çökme için sürüm, cihaz
modeli ve hata adımını bildir; kişisel PDF/alıntıları herkese açık loga ekleme.
