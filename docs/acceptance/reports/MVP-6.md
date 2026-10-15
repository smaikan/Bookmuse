# MVP-6 teslim ve genel kontrol raporu

14 Ekim 2026. **Kaynak, imzalı release, host/statik ve aşağıdaki gerçek Android
emülatörü işlev kontrolleri tamamlandı. Tam V1 ve fiziksel Huawei kabulü açık.**
Kullanıcının İndirilenler PDF'leriyle genel kontrol talebi uygulandı. Çalıştırılan
kısa corpus/testlerde bulunan hatalar düzeltildi; bütün cihaz ve PDF'lerin
sorunsuz olduğu iddia edilmiyor.

## Teslim

| Paket | Sürüm | Boyut | SHA256 |
|---|---|---|---|
| [ARM64 APK](../../../artifacts/mvp-6/kitap-mvp6-arm64-v8a.apk) | 0.6.2 / code 9 | 23,035643 MiB | `add7b1c26982fc14b5c6966cdfd7573b36fdb51de29d583f0880baa478709d12` |
| [Universal ARM64+x86_64](../../../artifacts/mvp-6/kitap-mvp6-universal.apk) | 0.6.2 / code 9 | 42,143085 MiB | `71b4704820c7bc7a380305c2f5296f65055bc41828c6eea2a78b29a09255920f` |

Package `com.kitap`, DB schemaVersion 5, minSdk 24, targetSdk 36.
Kalıcı imza sertifikası SHA256:
`1a1f883707fe5009a8c25582a2ca0fa9ee55c844d802936f4b311a8a2502dc5c`.
Önceki milestone APK'ları ve code 7/8 adayları korunur. Universal iki 64 bit
ABI içerir; 32 bit desteği iddiası yok. Fiziksel Huawei ABI'si henüz ölçülmedi.
[Manifest/hash dosyaları](../../../artifacts/mvp-6/delivery.json).
[Son teslim tutarlılık kontrolü](MVP-6-delivery-check.json): APK hash'leri,
package/lock sürümleri ve belge bağlantıları geçti; kısa final logda Kitap native
fatal/tombstone kaydı da bulunmadı. Test emülatörü iş bitiminde kapatıldı;
private test kanıtları korundu.

## Eklenen davranışlar ve düzeltilen hatalar

- Stream tabanlı sürümlü JSON yedek; disk SQLite staging, sıkı veri/ilişki/quad
  doğrulama, UUID/fingerprint dedupe ve 20 kitaplık çatışma önizlemesi.
- Açık **yereli koru** veya **yedek kitap snapshot'ını kullan** kararı;
  baseline değişirse yeniden önizleme, herhangi bir apply hatasında transaction rollback.
- PDF'siz kitapların alıntı/notlarını okuyabilme, aynı belgeyi hash/byte/sayfa
  sayısıyla yeniden bağlama; cache, PDF kopyası ve tüm kitap kayıtlarını ayrı silme.
- Kitap başına TXT/Markdown; final notlar ve etiketli dirty draft. JSON PDF,
  parola, dosya yolu/URI ve editör lease'i taşımaz. Sınır 256 MiB / bir milyon satır.
- Android content URI Aç/Paylaş → Kitap, güvenli private copy/dedupe ve import
  tamamlandıktan sonra istenen kitabı açma; beklenmeyen SEND parcel'i reddetme.
- Okuyucu sade açılır; araçlar dokunma/düğmeyle görünür. Kitap işlemleri açılır
  bölümde, not/işaret erişimi doğrudan.
- Gerçek ilk APK kontrolünde PDF çizildiği hâlde Fabric event coalescing `ready`
  olayını `saved` ile ezebiliyordu. Semantic olaylarda coalescing kapatıldı;
  native PDF yüzeyi/sayaç ve gerçek metin seçimi tekrar doğrulandı.
- Markdown provider'ının `.md.txt` eklemesi `text/markdown` MIME ile düzeltildi;
  final APK'da gerçek `.md` dosyası ve pasaj notu içeriği doğrulandı.
- Etkinlik değişen butonlar için host view yeniden oluşturma tedbiri eklendi;
  fiziksel TalkBack focus davranışı ayrıca doğrulanmalı.

Ayrıntı: [yedek sözleşmesi](../../architecture/MVP6_BACKUP.md).

## Çalıştırılan kontroller

Strict TypeScript ve ESLint geçti. **35 Jest + 81 JUnit/Robolectric = 116 host
testi**, 0 failure/error. Gerçek SQLite migration 1–4→5, revision/lease,
idempotency, geometri/transform, cache/queue, bozuk UTF-8/JSON, sınırlar,
restore rollback/dedupe/çatışma, başka cihaz UUID eşleme, silme/undo,
1000 kayıt ve Unicode/50.000 karakter sınırları kapsanır. Host testi fiziksel
cihaz kanıtı değildir. [Jest](MVP-6-jest.json), [JUnit](MVP-6-junit-summary.json).

R8/resource shrink, lintVital, imza, Hermes/PDFium/JS paketleme, exact DEX
accessor/JNI export ve ELF+ZIP 16 KB kontrolü geçti. ARM64 15, universal 30 `.so`.
İki APK aynı Hermes bundle'ını taşır; Metro gerekmez. Release manifestinde
INTERNET veya geniş depolama izni yok. APK içinde PDF/keystore dosyası yok.
[ARM64 doğrulama](MVP-6-verification.json), [universal doğrulama](MVP-6-universal-verification.json).
Statik geometry raporunun fiziksel yürütme alanı açık kalır; aşağıdaki emülatör
seçim kanıtı fiziksel ARM64 testi olarak işaretlenmedi.

## İndirilenler ve gerçek Android işlev turu

İndirilenler'de **10 PDF / toplam 12 sayfa**, tamamı metin katmanlı ve kısa kişisel
belgeler bulundu. Kişisel ad/içerikler rapora alınmadı; DL-01..DL-10 ile
anonimleştirildi. Orijinaller değiştirilmedi veya projeye kopyalanmadı; son SHA256
kontrolü değişmediğini doğruladı. [Corpus](downloads-corpus.json),
[orijinal dosya kontrolü](MVP-6-originals-recheck.json).

Windows pdfplumber/pypdfium2 ile 12 sayfanın tamamı açıldı/çizildi. Bu tek başına
Android testi değildir. Aynı dosyaların anonim kopyaları private emülatöre
aktarıldı; code 7 adayında gerçek ACTION_OPEN_DOCUMENT seçicisiyle import edildi.
**Final code 9 APK'da 10 PDF'nin 12 sayfası native PDFium yüzeyi ve tam okuyucu
sayacıyla yeniden açıldı.** İki sayfalı belgelerde ileri/geri swipe ve son sayfada
wrap olmaması geçti. [İlk SAF turu](MVP-6-android-corpus.json),
[final Android corpus](MVP-6-final-android-corpus.json).

Emülatör Android 16/API 36 x86_64, pencere 1200×800/density160, gözlenen RAM
**3920 MiB**, refresh 60,000004 Hz, runtime native sayfa boyutu **4096 byte**.
İstenen 2048 MB RAM'i emülatör 4096 MB'a yükseltti. Bu nedenle 2 GB Huawei
performansına veya gerçek 16 KB Android runtime'ına kanıt değildir.
[Envanter](MVP-6-emulator-inventory.json).

Gerçek APK/UI ve DB üzerinden:

- Bölge işareti, gerçek PDFium metin seçimi/underline; kaydedilmiş underline'a
  pasaj notu ekleme, doğru annotation ilişkisi ve gövdesi.
- Son başarılı 6 karakterlik sayfa taslağını force-stop/cold reopen'da kurtarma;
  code 8'de recovered draft'ı final nota kaydetme.
- Aynı imzayla **code 7→8 ve 8→9 `install -r`**, uninstall yok. Her güncellemede
  ilk açılıştan önce ana DB bayt düzeyinde aynı kaldı; ilk turda draft da korundu.
- SAF yedek oluşturma/önizleme/reimport; final APK'da yeni yedek tekrar import
  edildiğinde 0 değişen kitap ve mükerrer yok.
- Code 8'de 1000 sentetik ek not restore edilip gerçek Notlar sayfalaması
  kullanıldı; tekrar restore duplicate oluşturmadı. Yereli koru seçimi 1000 notu
  korudu, yedeği kullan seçimi eski snapshot'a açıkça döndürdü. Bu veri doğruluk
  testi; latency/PSS benchmark'ı değil.
- TXT/Markdown sayfa/alıntı/not ilişkileri; final `.md` ve yerelleştirilmiş türler.
- PDF kopyasını silince açık erişim hatası; not/alıntı erişimi sürüyor; aynı hash
  SAF relink okuyucuyu ve kayıtları geri getiriyor. Cache temizliği geçti.
- Code 8'de Dosyalar → Aç ile mevcut okuyucudan DL-10→DL-07; code 9'da
  Dosyalar → Paylaş → Kitap ile DL-10→DL-09; doğru belge, toplam 10 kitap,
  duplicate yok. Yanlış tipli SEND extra final APK'yı çökertmedi/kitabı değiştirmedi.
- Final APK'da yer imi, büyüt/zoom sıfırla ve kitaplığı yeniden açınca ikinci
  sayfadan devam geçti. Test sonunda 10 kitap, 2 işaret, 3 not, 1 yer imi.

[Draft kill](MVP-6-draft-kill.json), [7→8 update](MVP-6-upgrade.json),
[8→9 update](MVP-6-final-upgrade.json), [code 8 birleşik tur](MVP-6-functional-code8.json),
[VIEW](MVP-6-external-view.json), [final işlev turu](MVP-6-final-functional.json).
Private PDF, backup, export, ekran görüntüsü ve ham loglar repo'ya alınmadı.

Emülatör boot/derleme yükü sırasında **System UI ANR** ve ilk sürücüde eşzamanlı
UiAutomation bağlantı hatası görüldü. Bunlar Kitap sonucu diye sayılmadı veya
gizlenmedi; System UI yeniden başladıktan sonra tur yürütüldü. Final yakalanmış
logda Kitap'a ait AndroidRuntime fatal/ANR bulunmadı. Bu kısa turun sonucu;
uzun oturum sağlamlık garantisi değil. UI sürücüsü stale XML kullanmaz ve tek
UiAutomation istemcisiyle çalışır; sürücü beklemeleri latency ölçümü değildir.

## Kabul kriterlerinin mevcut durumu

| Kriter | Kanıt ve açık kapı |
|---|---|
| M6-A01 | Host portable alan/Unicode/ilişkiler ve gerçek SAF roundtrip geçti; fiziksel başka cihaz turu açık |
| M6-A02 | Host ve Android dedupe/çatışma/iki açık seçim geçti |
| M6-A03 | Eksik kopya, kayıt erişimi ve aynı hash relink Android'de geçti; farklı baskı/UUID eşleme host ile; başka fiziksel cihaz/provider matrisi açık |
| M6-A04 | TXT/Markdown içerik ve final doğru `.md` geçti |
| M6-A05 | Cache/kopya ayrımı gerçek Android, transaction/yarım import hata yolları host ile; gerçek disk dolması/SD kart/uzak sağlayıcı matrisi açık |
| M6-A06 | Gerçek Android VIEW ve SEND, doğru kitap/dedupe ve bozuk extra geçti |
| M6-A07 | Statik ARM64/x86_64 ve 16 KB hizalama, API36 x86_64 runtime geçti; fiziksel ARM64 ve 16 KB runtime açık |
| M6-A08 | Aynı imzalı 7→8→9 kayıt/draft update gerçek emülatörde; eski şema migrations host ile; eski MVP-5→yeni Huawei update açık |
| M6-A09 | Kısa Downloads corpus ve 1000 not paginasyonu geçti; tam ağır corpus, 30–60 dk/500 swipe/20 zoom/10 rotation ve canonical performans kabulü **INCOMPLETE** |
| M6-A10 | Release izin/imza/shrink/private asset kontrolleri geçti; npm audit temiz değil; fiziksel font/TalkBack, predictive back, iki dış kenar/IME/seçim/zoom/split-screen matrisi açık |
| M6-A11 | Kaynak/lockfile/APK/hash/build/test/kurulum/kurtarma/destek belgeleri hazır; tam V1 fiziksel kabulü verilmedi |

## Performans ve güvenlik sınırları

30 MiB ARM64 ve 50 MiB universal APK hedefleri gerçek dosya boyutlarıyla geçti.
Diğer p95/FPS/PSS/cache/uzun oturum değerleri uydurulmadı; canonical bütçeler
gevşetilmedi. Tam budget evaluator sonucu **INCOMPLETE**: emülatör üretici/RAM ve
physical kapısını karşılamıyor, diğer zorunlu örnekler eksik.
[Ölçüm girdisi](MVP-6-measurements.json), [tam değerlendirme](MVP-6-budget-evaluation.json).
Fiziksel `device-inventory.json` no_authorized_device kaydını korur.

`npm audit`: 48 high + 5 moderate bağımlılık/ancestor kaydı; **2 bağımsız advisory**,
0 critical. Braces 3.0.3 recursion/stack DoS
[GHSA-vfj7-8cjw-p6xm](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm),
sprintf-js precision DoS
[GHSA-hp3w-g68c-fv3c](https://github.com/advisories/GHSA-hp3w-g68c-fv3c).
Kontrol tarihinde bu advisory'lerde patched sürüm yoktu. Otomatik düzeltme önerisi
RN downgrade veya Jest major geçişi içeriyor; sırf audit sayısını azaltmak için
uygulanmadı. Final release source map'in 773 kaynağında iki paket bulunmadı;
ARM64/universal aynı Hermes bundle'ını taşır. Kalan risk Node derleme/test
zincirindedir; **audit temiz veya kapsamlı güvenlik sertifikası** iddiası yok.
[Npm audit](MVP-6-npm-audit.json), [release kapsamı](MVP-6-security-scope.json).
PDFium parser için kapsamlı güvenlik/hostile PDF denetimi de bu kısa testten çıkmaz.

Downloads corpus uzun 100–500/1000+ kitap, yüksek çözünürlük tarama, vector-heavy,
RTL/eğik metin/değişken kutu, parolalı/bozuk/kısmi/çok büyük dosya matrisini
karşılamıyor. Parola isteyen PDF desteklenmiyor; OCR V1 dışında. Gerçek Huawei,
ARM64 çalışma, düşük bellek, sağ/sol geri kenarı ve uzun oturum ölçümleri
alınmadan “2 GB cihazda sorunsuz/akıcı” veya tam V1 kabulü verilmez.

[Kurulum ve kurtarma](../../KULLANIM_VE_KURTARMA.md) ·
[Destek matrisi](../../DESTEK_MATRISI.md) ·
[Build matrisi](../../architecture/MVP0_TECHNOLOGY.md).
