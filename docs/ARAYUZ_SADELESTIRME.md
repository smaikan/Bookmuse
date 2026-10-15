# Bookmuse arayüz sadeleştirmesi — 0.8.1 / code 16

15 Ekim 2026. Son kullanıcı düzeltmesi dahil güncel sözleşme; eski UX-MVP-1–6 kararlarının aşağıdaki bölümlerini değiştirir. Yeni bir MVP değildir.

- Kitap kartında beş işlem, bu sırayla: Notlar, Altı çizilenler, Favoriye ekle/çıkar, Adı düzenle, Kaldır. İlk üç işlem ve bunların kayıt/listeden konuma gitme akışları korunur. Menü kısa ekranda/büyük yazıda kayabilir, kartın ölçüsünü değiştirmez.
- Adı düzenle mevcut adı doldurur. Boş ad kaydedilmez; kayıt hatası editörde gösterilir. Kitap kimliği ve ilerleme değişmez. Kaldır açık onay ister; vazgeç hiçbir kayıt silmez. Onay seçilen kitabın özel kopyasını ve ilişkili uygulama kayıtlarını kaldırır; dış orijinal PDF'yi silmez.
- Bağımsız Kitapları Yönet, Yedekleme ve Depolama ekranları ve bunlara özel native/JS backup/restore/export/cache temizleme/yalnız kopya silme API'leri kaldırılmıştır. PDF ekleme, yerel özel kopya, otomatik sınırlı thumbnail cache bakımı, konum ve not/işaret verileri korunur. Erişim kurtarma için motorun relink yolu korunur; yeni yönetim arayüzü eklenmez.
- Alt Kitaplık/Ayarlar sekmeleri yoktur. Kitaplık başlığındaki erişilebilir, en az 48 dp dişli doğrudan Ayarlar'ı açar. Aynı kaydırılabilir ekranda yalnız Okuma Görünümü ve Ekran başlıkları bulunur. Görünüm/düzen ve ekran/erişilebilirlik seçenekleri doğrudandır.
- Ayarlar sabit #F5F3ED zeminde yeşil kontrollerle kalır. Gece/özel koyu okuma rengi ayarlara uygulanmaz. Krem seçimden çıkarılmıştır; Orijinal=0, Sepya=2, Gece=3, Özel=4. Eski Krem=1 verisi okunur, ID'ler kaydırılmaz. Kaydetme hatasında pencere açık kalır; aynı anda çift kayıt/çıkış engellenir.
- Üç sütunlu Sayfalar ekranında Yardımcı Kontroller, bu gruba ait zoom/pan/önceki-sonraki düğmeleri ve `helperControls` tercihi kaldırılmıştır. Numarayla gitme, yer imleri, sanal küçük resimler, dokunarak seçim, pinch/zoom, sistem geri ve not/işaret konumundan Okumaya dön korunur.
- SQLite schema 7 sabit; veritabanı sıfırlama veya tablo silme yoktur. Eski APK ve kabul raporları korunur. Yeni build 0.8.1/code16, aynı package ve imza ile güncellenir.

Kabul: gerçek beşli menü ve eski işlevler; önceden dolu ad düzenleme ve hata; kaldırmada vazgeç/onay; ayarların tek adımla ve sabit açık görünümle açılması; Krem olmadan doğru tema kimlikleri; gece okuyucusu; üç sütun/numara/yer imi; güncellemede kayıtların korunması. Kaynak testleri, APK ve emülatör kanıtı [raporda](acceptance/reports/ARAYUZ-SADELESTIRME.md) ayrı belirtilir. Fiziksel Huawei performans kabulü açık kalır.
