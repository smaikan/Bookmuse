# MVP-3 — gezinme ve kitaplık sözleşmesi

14 Ekim 2026. React Native kitaplık ve panelleri; PDF yüzeyi native kalır.
Güncel derleme/kanıt `docs/acceptance/reports/MVP-3.md` içindedir.

## Sayfalar ve yer imleri

VirtualizedList veri kaynağı sayfa sayısıdır; 1000/2000 sayfalık JS dizi veya
bütün sayfaların bitmapi oluşturulmaz. Bir satır iki/üç hücredir. Sabit thumbnail
boyu ve sistem font ölçeğine bağlı satır yüksekliği hesaplanır. Görünür hücreler
native thumbnail isteğini etkinleştirir; görünmeyenler generation ile bırakılır.
Numara ve slider bütün fiziksel sayfalara ulaşır; slider yalnız bırakıldığında
render başlatır. Native SeekBar erişilebilir düğme/klavye eylemini de tamamlar.

İç tutamaç 48 dp, korunan iki kenarın dışında 8 dp marjlıdır. Sol el tercihi
tutamacı/paneli sola taşır; geniş gesture exclusion yoktur. Panel düğmesi
alternatiftir. Modal sistem geriyle önce kapanır. Kayıt sekmeleri aynı kitap
kimliğini kullanır; MVP-4 Altı çizilenler sekmesini ekler.

SQLite schema 3: tekil bookmarks(document_id,page_index), app_preferences,
thumbnail_cache metadata; documents.title_search Türkçe NFKC/lowercase,
reading_positions.has_read. Henüz okunmamış import son kitaba devam sayılmaz.
Yer imi açık enabled bool ile idempotenttir; yalnız commit sonrası UI güncellenir.

## Küçük resim sahipliği

160×224 sınırında JPEG özel cache, RAM **2 MiB** alt bütçe ve toplam **24 MiB**
bitmap bütçesi birlikte reserve edilir. Aynı reader belge oturumu kullanılır;
başka kitap aktifken ikinci belge açılmaz. Kitaplık kapakları yalnız görünür ve
odaklı ekranda hazırlanır. Disk metadata LRU hedefi **64 MiB**; cache kullanıcı
kayıtlarının kaynağı değildir. Process death sırasında kalmış cache .part
dosyalarının temizliği final recovery aşamasında ayrıca denetlenmelidir.

## Kitaplık ve çift sayfa

SQL arama/sıra/limit-offset, 40'lık yükleme, görünür kapak, stable id ile ad
düzenleme ve kalıcı liste/ızgara tercihi vardır. LIKE jokerleri literal kaçırılır.
Genişlik eşiği 720 dp ve yatay pencere + açık kullanıcı tercihiyle çift sayfa.
Varsayılan tek sayfadır. Kapak tek sayfa açıkken eşleme 0, ardından 1–2, 3–4;
ilk/son sınırda döngü yoktur. Çift sayfada pane başına 750 bin piksel ve komşu
prefetch kapalıdır. İki pane hazır olmadan presentation id ilerlemez; yalnız
aktif fiziksel sayfa settled/onDraw sonrası kaydedilir. Hazır/saved olaylarının
sırası farklı olabilir; JS ACK kümeleri bunu ayrı ele alır.

Bu tasarım/host testleri gerçek iki parmak gesture, TalkBack veya Huawei PSS/FPS
kanıtı değildir. Fiziksel kabul milestone raporunda açık kalır.
