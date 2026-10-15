# UX okuyucu sözleşmesi — 0.8.1

15 Ekim 2026. Ürün kararları [UX planındadır](../UX_YENIDEN_TASARIM_PLANI.md).
Eski MVP mimari belgelerindeki kitap başına görünüm, tutamaç ve renk seçicisi
bu sürümün kullanıcı akışını tarif etmez; eski veriler korunur.

- `LibraryScreen.tsx`: tek FlatList, responsive sabit genişlikli kapaklar;
  favori/sıralama/arama; beş işlem popup (Notlar/Altı çizilenler/Favori/Adı düzenle/Kaldır); başlıkta dişli.
  Ayarlar tek sabit açık renkli ekrandır; okuma temasına göre yeniden renklendirilmez.
- `ReaderScreen.tsx`: başlıksız native stack, mutlak PDF yüzeyi; 160 ms
  transparan işlem katmanı. Ortak `readerDefaults` bütün kitaplarda okunur.
  Okuyucudan çıkış `flushReadingPosition` bariyerini bekler.
- `KitapGestureHost.kt`: çift sayfa dahil tek native iki parmak sahibi;
  insets koruması, pointer değişimi ve kalan parmağı tüketme. PDF View'ler
  yalnız küçük seyrek olaylar gönderir; kare başına JS state yoktur.
- `WipeTransition.kt`: sürükleme ofsetinden devam eden en çok 220 ms settle.
  Render edilmiş hedef hazırlanmadıkça mevcut sayfa korunur. Sayfa/işaret
  aynı transform ile hareket eder. Tamamlanmamış geçiş konum yazmaz.
- `OverviewPinch.kt`: sanal ölçek 1 altına iner, görünür PDF ölçeği inmez;
  %18 ek sıkıştırma hazırlar, %10 üstüne geri açma iptal eder. Commit release'tir.
- `PagePanel.tsx`: üç sütun VirtualizedList satırları, görünür thumbnail talepleri,
  numarayla git/yer imleri; Yardımcı Kontroller kaldırılmıştır; scroll okuma konumu değildir.
  Kapat zoom/pan komutu göndermez. Sayfa seçimi kapanış animasyonundan sonra açılır.
- `KitapPdfView.kt` ve `KitapThumbnailView`: Canvas sınırı açık clip edilir.
  Sadece React hücre geometrisinin doğru olması native boya sınırının doğru
  olduğunu kanıtlamaz; screenshot'ta gutter ve sayfa numarası da kontrol edilir.
- `BookRepository.kt`: schema 5→6 favoriler; 6→7 sarı metin işareti geçişi.
  Stil geçmişi aynı transaction içinde saklanır, migration tekrar güvenlidir.
- `MarkerStyle.kt` / `AnnotationRepository.kt`: metin kayıtları canonical
  `highlight/#FFEB3B`; region korunur. Yeni kayıt ve düzenleme aynı normalizasyondan geçer. Eski stil API'si sarı olmayan metin üretemez.
- Kaydedilmiş overlay filtre sonrası tek winding Path ile alpha 82/255 çizilir;
  kesişen bölgeler koyulaşmaz. Quad yönü normalize edilir. Native path/geometri
  maliyeti text bütçesinde hesaba katılır; kalıcı geometri PDF user-space'tedir.
- Yedek/restore/export ekranı ve ona özel native/JS API kaldırılmıştır. SQLite
  schema 7, tablolar, import/recovery, kalıcı kayıtlar ve orijinal PDF korunur.
  `helperControls` artık serialize edilmez; eski JSON alanı zararsızca yok sayılır.
  Eski Krem kimliği 1 okunur; seçenekler 0/2/3/4 kimliklerini korur.

Tek PDFium oturumu/seri worker, generation kontrolü ve byte tabanlı cache devam
eder. Bitmap sınırı 24 MiB, text/geometri 4 MiB; görünür thumbnail alt bütçedir.
APK, host testi ve emülatör işlev kanıtları [final rapordadır](../acceptance/reports/UX-MVP-6.md).
Huawei üzerinde bellek/FPS/p95 ve tam geri/TalkBack matrisi ayrıca kabul edilir.
