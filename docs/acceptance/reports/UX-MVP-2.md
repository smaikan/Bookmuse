# UX-MVP-2 — tam ekran ve ortak ayarlar

15 Ekim 2026. Kaynak ve imzalı derleme tamamlandı; emülatör etkileşim doğrulaması sürüyor. Fiziksel Huawei kabulü açık.

Okuyucu başlığı, sürekli sayaç/yan tutamaç ve alt gezinme kaldırıldı. PDF yüzeyi mutlak tam alan kullanır; işlem katmanı 160 ms opacity ile PDF üzerinde açılır. Ana işlemler sayfa notu / kitap notu / metin seçimi, yanında ayrı Sayfalar simgesidir. Görünüm ayarları ana Ayarlar'dan tüm kitaplara uygulanır. Eski kitap tercihleri veritabanında korunur. Normal çıkışta konum bariyeri başarısız olursa okuyucu açık kalır ve tekrar deneme bildirimi sunar.

86 native host testi ve 27 Jest testi; TypeScript ve ESLint geçti. İmza, shrunk DEX/JNI, ABI, ELF/ZIP 16 KB ve izin kontrolleri geçti. Emülatörde ilk tam ekran açılışında Android'in kendi “Viewing full screen” tanıtım katmanı görüldü; bu katmanın kapatılmadığı ilk UI koşusu ürün başarısı sayılmadı.

Artifact: `artifacts/ux-mvp-2/kitap-uxmvp2-universal.apk`, 0.7.2/code11, 42.142406 MiB. SHA-256: `a63ad75961aee5b56810d26c5536c0e23af853e20e86ac054016e3e991e15ebf`.

Emülatörde PDF erişilebilirlik sınırları menü öncesi/sonrası `[0,0][800,1200]` kaldı. Menüde yalnız üç ana işlem ve Sayfalar vardı; zoom/tema/kayıt listeleri yoktu. Boş alana tekrar dokununca kapandı. Sayfa 45 korunarak kitaplığa dönüş, ortak Gece ayarını kaydetme ve tekrar açılış yürütüldü. `UX-MVP-2-functional.json` bu koşunun sonuçlarını içerir. Sentetik PDF ekranları özel test klasöründe saklandı.

U2-01/04/05/06 bu kapsamda emülatörde geçti. U2-02/03 geometri host testleriyle desteklenir; farklı oranlı corpus finalde genişletilecek. U2-07/08 için tam TalkBack/yön/split-window matrisi ve fiziksel performans açık.

Güncel birleşik sonuç ve bu aşamanın final sınırları [UX-MVP-6 raporundadır](UX-MVP-6.md). Önceki artifact tarihsel olarak korunur; güncel önerilen APK0.8.0/code15’tir.
