# Bookmuse uygulama ikonu

Kaynak `bookmuse-icon.png`: kullanıcı tarafından sağlanan 1254×1254 PNG. Kaynak dosya
korunur; sadece platformun gerektirdiği yeniden boyutlandırma ve dairesel maskeleme yapılır.
Yeni görsel üretilmez, marka çizimi değiştirilmez. Windows System.Drawing kullanılır;
uygulamaya yeni paket veya runtime bağımlılığı eklenmez.

Proje kökünde yeniden üret:

```powershell
.\scripts\update-launcher-icons.ps1
```

Android `src/main/res` içinde beş mipmap yoğunluğu için 48/72/96/144/192 px normal
ve yuvarlak PNG üretilir. Yuvarlak PNG köşeleri saydam, normal PNG opaktır.
Android 8+ için `mipmap-anydpi-v26` iki adaptive-icon XML'i aynı resource adlarını
korur. Arka plan `#F5F3ED`; foreground 432 px/108 dp, kaynak %80 ölçekle ortalanır.
Bu kaynak görselde algılanan yeşil sembolün en uzak noktası merkezden 30,89 dp;
33 dp güvenli yarıçap içinde kalır. Farklı çizim eklenirse güvenli alan yeniden incelenir.

Manifest zaten `@mipmap/ic_launcher` ve `@mipmap/ic_launcher_round` kullanır;
yeni kaynaklar hem Bookmuse hem Bookmuse Dev için geçerlidir. Android 7.x ve
öncesinde yoğunluğa uygun PNG, Android 8+ sistem maskelerinde adaptive XML seçilir.
[Android ikon katmanları ve güvenli alan belgesi](https://developer.android.com/codelabs/basic-android-kotlin-compose-training-change-app-icon).

AAPT2 kaynak compile/link ve PNG boyut/alfa kontrolleri geçti. Tam uygulama APK'sı
bu ikon işleminde yeniden üretilmedi; cihaz/launcher kabulü ayrıca gerekir.
Yeni ikon ve Bookmuse adı yeni APK derleyip üzerine yükleyince görünür.
Fast Refresh native launcher ikonunu güncellemez. Kaynak/hash/çıktı bilgisi
`launcher-resources.json` dosyasındadır; tekrar üretim sonrasında hash'ler yeniden kontrol edilir.
