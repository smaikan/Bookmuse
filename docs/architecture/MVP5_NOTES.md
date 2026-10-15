# MVP-5 — not, taslak ve ilişki sözleşmesi

14 Ekim 2026. React Native düz metin editörü, mevcut TurboModule ve aynı serial
SQLite worker; ek editör paketi, WebView, GMS veya ikinci PDF motoru yoktur.
MVP-4 geometri/JNI/gesture sözleşmesi korunur. Tam V1, backup/export ve cihaz
kabulü MVP-6/gerçek ölçüm kapılarındadır.

## Üç bağ ve tek kayıt

Kitap notu page=-1; sayfa notu gerçek fiziksel page; pasaj/bölge notu annotationId,
sayfa ve ayrı quote snapshot taşır. Not gövdesi alıntıdan bağımsız düzenlenir.
Her işaretin bir aktif pasaj notu vardır; Altı çizilenler'den veya Notlar'dan
aynı UUID açılır. Kitap/sayfa genelinde birden çok not oluşturulabilir.

Seçim menüsündeki **Pasajı kaydet ve not ekle** kalıcı underline/region anchor
ve editor draft'ını tek outer SQLite transaction ile hazırlar. Draft insert
hatası annotation+ranges'ı da rollback eder. Aynı operation UUID tekrarında
aynı anchor ve aynı pasaj notu kullanılır. Native session/selection/fingerprint
kontrolü MVP-4 ile aynıdır. Metadata/bitmaps JS'ye gönderilmez.

## Schema 5

- `notes`: id, document FK, scope, physical page, nullable annotation FK,
  quote snapshot, title/body/search, revision, created/updated, deleted,
  deletion_group, last_editor/last_draft_revision (commit retry identity).
- `note_drafts`: aynı bağ ve metin; editor_token, content_revision,
  base_revision, dirty, updated, deleted ve deletion_group.
- Annotation'a `note_delete_group` eklenir. FK hard annotation delete'de
  annotation_id SET NULL; quote/page korunur. Normal işaret silme tombstone'dur.
- Book/date/passage indeksleri ve tek aktif passage note partial unique index.

SQLiteOpenHelper schema 4→5 transaction migration'dır. V4 fixture kitap,
position/theme/bookmark/annotation/range içerir; v5 upgrade bunları korur.
DDL hatası sürümü ilerletmez veya DB reset yapmaz. Documents silme UI'da kitap
kopyası/kayıtları kapsadığını açıkça söyler; notes/drafts doc FK ile silinir.
Diğer kitap ve dış orijinal etkilenmez.

## Editör lease ve revision

Editör açılışında tek yeni UUID token ile lease alınır. Draft içeriği ve
content_revision korunur; base_revision son final note revision'ıdır.
Yeni editör eski token'ı geçersiz kılar. Write her zaman mevcut lease ister;
draft yoksa eski callback yeni row oluşturamaz.

Content revision monotoniktir. Eski revision reddedilir; eşit revision yalnız
aynı title/body için idempotenttir. Her tuş UI ref'ini ve revision'ı günceller,
DB write **650 ms debounce** ile yapılır. ACK yalnız revision/dirty/updated
metadata taşır; 50.000 karakter her autosave cevabında JS'ye geri kopyalanmaz.
Gecikmiş ACK/hata daha yeni editör metnini veya kayıt durumunu değiştirmez.

Kaydet, latest draft + final note insert/update + draft removal'ı transaction
ile yapar; expected base revision doğrulanır. Final save aynı token/version/
title/body ile tekrar edilirse aynı commit döner. Native/JS hata durumunda
metin ve taslak durur; UI commit öncesi `Kaydedildi` demez. Save sırasında
input/write controls kilitlenir; save sonrası late autosave row'u canlandıramaz.

## Kapanış ve kurtarma

AppState background/inactive latest ref'i hemen dispatch eder. Taslağı sakla ve
kapat / geri, latest revision'ı native `endNoteEditor` ile yazar ve token'ı
kapatır. Değişmemiş/boş editör row'u temizlenir, listeyi boş taslaklar doldurmaz.
Unmount cleanup pending debounce'u iptal eder ve son ref'i flush eder; başarılı
final save cleanup'ta yeniden draft oluşturmaz.

OS kill cleanup'ı atlayabilir. Yalnız **son başarılı DB commit** kurtarma
garantisidir; henüz gönderilmemiş/committed olmayan son tuşa garanti yoktur.
Dirty draft cold reopen'da görünür, `Son kaydedilmiş taslak kurtarıldı` mesajıyla
açılır. Yeni token ve kalmış content revision ile devam edilir. Geri sırası
platform IME + Keyboard event/visibility fallback → draft korunarak editör →
panel → reader'dır. Dialog ve reading surface ayrı; editor typing ReaderScreen
root'unu her tuşta yeniden render etmez.

## Klavye, uzun metin ve liste

Safe inset + KeyboardAvoidingView + scroll form kullanılır. Kaydet/kapat footer
form dışında; secondary keyboard/retry actions formda. Android multiline field
220 dp ile sınırlandırılır; uzun not dev bir reader surface oluşturmaz.
200 karakter başlık, **50.000 UTF-16 birimi** body limiti UI'da görünür ve native
kontrol edilir. Sistem fontu, 48 dp kontroller, labels/live status ve koyu reader
içinde koyu modal vardır. Gerçek IME/TalkBack/büyük font testi cihaz bekler.

Notlar kitabın workspace sekmesi, reader düğmesi ve library card'ında bulunur.
SQL 40'lık pagination, max 400 body/300 quote preview; seçilmiş tek not editor'de
tam alınır. Son değişim/page sırası, book/page/passage/draft filtresi ve kayıtlı
title/body'de literal Türkçe arama. LIKE kaçış karakteri `!`; `%`, `_`, `!`
literal escape edilir. PDF içerik araması değildir. Yeni sayfa önceki 40'ı
replace eder; 1000 full note veya quad set'i JS state'e birikmez.

## Silme / geri alma

Not silme expected revision ile tombstone, dirty draft içeriğini korur ve
editor token'ını geçersiz kılar. Unsaved draft da revision kontrollü
delete/undo yapılabilir. Final not silindiğinde onun draft'ı listeden saklanır;
undo aynı içeriği geri getirir.

Notlu işarette UI açık seçim sunar: **İşareti sil, notları koru** veya
**İşaret ve notları sil**. Birlikte silme unique deletion_group ile yalnız o
işlemde aktif olan notes/drafts'ı tombstone yapar. Annotation undo aynı group'u
transaction içinde geri getirir; önceden bağımsız silinmiş notları diriltmez.
Note link silinmiş işarette korunur; quote/page erişilebilir, listede ilişki
durumu görünür. Böyle notun jump'ı page'e gider; aktif mark varsa quad focus ve
Okumaya dön yolu kullanılır. Group bilgisi DB'de; restart'ta da restore tutarlıdır.

## Kanıt sınırı

Robolectric gerçek SQLite ve tarihsel fixtures; Jest controlled async outcomes
ile debounce/late ACK/background/IME fallback/save retry/list/binding/choice
sözleşmesini test eder. Testler gerçek Android process kill, keyboard window,
PDFium/JNI, font shaping, touch latency veya Huawei PSS/FPS kanıtı değildir.
Shared serial worker'da uzun arama/50k note ve render rekabeti gerçek cihazda
ölçülmelidir; canonical bütçeler geçirilmiş sayılmamıştır.
