# MVP-2 kabul raporu

14 Ekim 2026. **İmzalı release/host/statik kontroller geçti; fiziksel kabul açık.**

APK: `artifacts/mvp-2/kitap-mvp2-arm64-v8a.apk`, 22,8550 MiB. SHA256:
`b1e528fdb8d9c8ae7b57debaf3525e8d1e4f9c3f7ecb31174f9e1fa979b478aa`.
com.kitap, versionCode 3 / 0.2.0, ARM64; önceki aynı sertifikayla güncellenebilir
paket hazır, gerçek update testi henüz yok. R8/shrink, JS/Hermes/PDFium paketi,
15 native ELF/ZIP 16 KB hizalama ve APK v2 imzası geçti. İnternet/geniş depolama
izni yok. [Verification](MVP-2-verification.json), [signature](MVP-2-signature.txt),
[inspection](MVP-2-apk-inspection.json), [package](MVP-2-package.txt).

40 host testi: 10 Jest + 30 JVM/Robolectric. TypeScript strict ve ESLint exit 0.
[Jest](MVP-2-jest.json), [JUnit özeti](MVP-2-junit-summary.json).
Yeni testler: iki geri kenarı/bar insets, yetersiz/diagonal/ters fling,
crop/zoom/pan inverse, rotation focus, zoom bounds, küçük görünür tile planı,
bounded priority/FIFO queue, eski SQLite v1 fixture'ından v2 migration ve tercih
reopen; native navigation/settled page/iptal/per-book tema/window restore.
Jest native API'leri mock eder; JVM Android PDFium/GPU/gerçek gestures çalıştırmaz.

| Kriter | Durum | Host/kaynak kanıtı ve eksik fiziksel senaryo |
|---|---|---|
| M2-A01 | INCOMPLETE | Native drag/220 ms geçiş/iptal ve settled commit; gerçek iki yönde sürükleme bekliyor |
| M2-A02 | INCOMPLETE | Root WindowInsets ve touch-down rejection, iki kenar pure test; gerçek sistem geri/predictive cancel bekliyor |
| M2-A03 | INCOMPLETE | Pinch/double-tap/pan + yalnız görünür 512 px tiles; gerçek netlik/gesture testi bekliyor |
| M2-A04 | INCOMPLETE | 24 MiB owned-buffer, queue16, generation, maxzoom5, tek native page retention; 10 hızlı geçiş cihaz testi yok |
| M2-A05 | INCOMPLETE | Kitap bazında SQLite tema/özel renk/kontrast, aynı bitmap filtresi ve night yükleme; görsel cihaz kontrolü yok |
| M2-A06 | INCOMPLETE | Normalize crop/reset/PDF değişmeme, rotation center focus host testi; gerçek dönüş bekliyor |
| M2-A07 | INCOMPLETE | Araç/sayaç, ReaderWindow brightness/screenOn/orientation/fullscreen baseline restore; gerçek OS lifecycle testi yok |
| M2-A08 | INCOMPLETE | Sistem/user hareket azaltma; 48 dp düğme, native accessible SeekBar, scroll/keyboard form; gerçek TalkBack/font yok |
| M2-A09 | INCOMPLETE | 30–60 dk/500 geçiş/20 zoom/10 dönüş, PSS/frame/growth/ANR/OOM ölçülmedi |
| M2-A10 | INCOMPLETE | Eski frame hedef hazırlığında korunur; geçiş iptal düğmesi ve stale result testi; ağır corpus cihaz testi yok |

Canonical budget'taki M0/M1 anahtarlarına delayed_frames_percent, frozen_frames
ve quiescent_pss_growth_percent eklendi. APK boyutu statik 22,8550 ≤30 MiB geçti;
FPS/PSS/latency/crash/ANR/OOM için değer veya sahte sıfır üretilmedi. Huawei
MatePad SE AGS5-W09 / HarmonyOS 3.0 kullanıcı beyanı; API/ABI/RAM/Hz hâlâ ADB
bekliyor. [Teknik sözleşme](../../architecture/MVP2_READING.md).

MVP-3/MVP-4 bağımsız kaynak geliştirmesi kullanıcı tarafından açıkça istendi;
MVP-2 fiziksel kapısının geçildiği iddia edilmeden devam edilir.
