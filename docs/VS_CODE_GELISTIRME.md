# VS Code'dan canlı geliştirme

15 Ekim 2026. VS Code'da **kitap kök klasörünü** aç:
`C:\Users\Serhat\Desktop\kitap`. Görevler kökteki `.vscode/tasks.json` içindedir.
Yalnız `mobile` klasörü açılırsa bu görevler listelenmez.

1. `mobile/App.tsx` veya `mobile/src/reader/LibraryScreen.tsx` dosyasını aç.
2. **Ctrl+Shift+B**: varsayılan **Kitap: Android canlı geliştir** görevi.
3. Görev Metro'yu localhost:8087 üzerinde açar; Android debug APK'yı derler;
   ilk JavaScript paketini önceden hazırlar;
   `kitap-functional` emülatörünü başlatır/bulup açılışını bekler; kurar ve açar.
   İlk native derleme zaman alabilir. Terminalde hata varsa sonraki adım durur.
4. Ekran Android emülatöründedir. `.tsx` dosyalarını **Ctrl+S** ile kaydet;
   Fast Refresh değişikliği uygulamaya iletir. Metro ve emülatör görev terminalleri
   açık kalmalı; emülatör ayrı bir uzun süreli görevde tutulur.
5. Kotlin/JNI/native kaynak değiştirdiysen **Ctrl+Shift+B** ile yeniden derle.

Geliştirme uygulaması **Bookmuse Dev / com.kitap.dev**, debug imzalıdır.
İmzalı release **Bookmuse / com.kitap** uygulamasının yanına kurulur; iki uygulamanın
SQLite/PDF/not kayıtları ayrıdır. Release'i kaldırmak veya verilerini temizlemek
gerekmez. Dev kitaplığının ilk kurulumda boş olması normaldir.
PDF ekle ile yeni kopya al; emülatörün dahili Download/kitap-tests klasöründe
önceki anonim test dosyaları bulunabilir.

## Hangi dosyayı düzenleyeceğim?

| Dosya | Görevi |
|---|---|
| `mobile/App.tsx` | Uygulama giriş bileşeni ve navigasyon |
| `mobile/src/reader/LibraryScreen.tsx` | Kitaplık arayüzü |
| `mobile/src/reader/ReaderScreen.tsx` | Okuyucu arayüzü ve araçlar |
| `mobile/src/reader/NoteEditor.tsx` | Not editörü |
| `mobile/android/app/src/main/java/com/kitap/pdf/` | Native PDF motoru, çizim, SQLite ve hareketler |

Komut paleti → **Tasks: Run Task** ile Metro veya yalnız emülatör görevini de
açabilirsin. VS Code görev terminalini kapatırken Metro'yu sonlandırabilirsin.
Metro'nun `r/d/j` kısayolları reload, dev menü ve React Native DevTools içindir;
bunlar mobil JS debugger'ıdır, Kotlin/JNI debugger'ı değildir.
Mevcut başka bir terminalde çalışan proje Metro'su kullanılıyorsa bu kısayollar
o terminaldedir. Kod kaydetmeyle Fast Refresh için terminale tuş basılmaz.

Başlangıç kısayolu Ctrl+Shift+B; ek bir VS Code debug eklentisi gerekmez.
Bu native PDFium uygulamasının ekranı Android yüzeyinde çalışır; `.tsx` dosyası
tarayıcıda HTML önizlemesi gibi çalıştırılmaz.

Metro görevi yalnız bu `mobile` klasörüne ait server'ı yeniden kullanır. Port
başka bir projeye aitse o süreç kapatılmaz ve Kitap ona bağlanmaz. Emülatör
bulma yalnız belirtilen AVD adına göre yapılır; bağlı fiziksel tablet otomatik
kurulum hedefi değildir. SDK/JDK `environment.json` üzerinden seçilir.

Debug/Metro geliştirme kolaylığı içindir; Huawei performans kabulü için imzalı
release APK kullanılmalıdır. Release hash ve kabul raporu:
[MVP-6](acceptance/reports/MVP-6.md).

Yerel doğrulama kanıtı: [VS Code geliştirme kontrolü](acceptance/reports/VS-CODE-DEV.json).
Görevlerin script bileşenleri, ayrı debug kurulumu ve kaydet → Fast Refresh →
geri al akışı gerçek Android emülatöründe denendi. VS Code kısayol tuşu UI
otomasyonuyla basılmadı; görev tanımları ve hazır olma desenleri ayrıca kontrol edildi.
