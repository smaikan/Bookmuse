# UX-MVP-1 — kapak kitaplığı ve konum

15 Ekim 2026. Kaynak, imzalı derleme ve aşağıda belirtilen emülatör senaryoları doğrulandı. Fiziksel Huawei kabulü açık.

Kapak ızgarası tek sanallaştırılmış liste içinde kayar. Sağ üst ⋮ yalnız Notlar / Altı çizilenler / Favoriye ekle–çıkar içerir. Tümü/Favoriler filtresi sıralamadan ayrıdır. Yönetim Ayarlar'a taşındı. SQLite 5→6 favori geçişi, yedek v2/v1 uyumluluğu ve son gerçekten gösterilen sayfayı kapatmadan önce kaydeden bariyer eklendi.

## Kanıt

- 85 native host testi; 36 Jest testi, TypeScript ve ESLint kontrolü geçti.
- Android 36 x86_64 emülatörde eski 0.6.2 sürüm üzerine kaldırmadan 0.7.1/code10 kuruldu.
- Üç sütun, kart üzerinden dikey kaydırma, tam üç menü işlemi, favori filtresi ve uygulamayı yeniden başlatınca favorinin korunması yürütüldü.
- Sentetik 80 sayfalı PDF, gerçek sistem dosya seçicisinden eklendi. Sayfa 45 → normal kapat → `(45/80)` kartı → sayfa 45 yeniden açılış geçti.
- Önceki 10 kitap / 2 işaret / 3 not / 1 yer imi korundu. Sentetik kitap eklendikten sonra 11 kitap oldu. Ham DB ve kişisel ekranlar repository dışında tutuldu.
- APK imzası önceki sürümle aynı; shrunk DEX/JNI, iki ABI, ELF ve ZIP 16 KB statik kontrolleri geçti. INTERNET veya geniş depolama izni yok.

Artifact: `artifacts/ux-mvp-1/kitap-uxmvp1-universal.apk`, 42.147640 MiB. SHA-256: `99657830347ddcb853c81982e6232119ecaf2aa8cda0cbebca34c16ecabcbed2`.

## Kriter durumu

U1-03/04/05/07 belirtilen emülatör akışında geçti. U1-06 native sorgu ve RN testlerinde doğrulandı. U1-08 konumun yalnız presentation sonrası kaydıyla korunur; final gesture/overview testinde yeniden yürütülecek. U1-09 mevcut kayıtların korunması, dış dosyaya dokunmayan mevcut yönetim ve host regresyonlarıyla desteklenir. U1-01/02 için bütün pencere/font/kapaksız varyasyonlarının görsel matrisi final aşamasında açık.

ADB ile dış VIEW başlatmanın URI izin hatası kabul kanıtı sayılmadı; PDF gerçek sistem seçicisinden eklendi. İlk kısa otomasyon sürüklemesinin yanlışlıkla kitabı açtığı koşu geçersiz sayıldı, karttan başlayan 900 ms sürükleme tekrar yürütüldü. Performans veya Huawei sonucu çıkarılmadı.

Makine okunur kayıtlar: `UX-MVP-1-functional.json`, `UX-MVP-1-universal-verification.json`, aynı önekli paket/JUnit/ABI raporları.

Güncel birleşik sonuç ve bu aşamanın final sınırları [UX-MVP-6 raporundadır](UX-MVP-6.md). Önceki artifact tarihsel olarak korunur; güncel önerilen APK0.8.0/code15’tir.
