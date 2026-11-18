<img src="docs/logo.png" alt="logo" width="140" align="right">

# CevherAfkTab

Sunucumuzda tab listesi AFK bekleyen oyuncularla doluyordu, o yüzden bu küçük eklentiyi yazdık.
EssentialsX'te AFK'ya giren oyuncu diğerlerinin tab listesinden kalkıyor, geri dönünce tekrar geliyor.

Vanish değil. Oyuncu oyunda normal duruyor, kafasındaki `[AFK]` yazısı da yerinde. Sadece tab'daki satırı yok oluyor.

![örnek](docs/ornek.png)

Ekran görüntüsünde Mrceviz AFK, tab'da sadece Mrcevher var ama Mrceviz aşağıda hâlâ görünüyor.

## Gerekenler

- Paper 1.21+ (Java 21), Spigot'ta çalışmaz çünkü `Player#unlistPlayer` Paper'a ait
- EssentialsX

## Kurulum

Jar'ı `plugins` klasörüne at, sunucuyu yeniden başlat. Ayar dosyası yok.

## Yetki

`cevherafktab.gor` : AFK oyuncuları tab'da görmeye devam eder. Yetkililere verince işe yarıyor.

## Nasıl çalışıyor

Essentials AFK durumu değişince `AfkStatusChangeEvent` atıyor, biz de ona göre herkese `unlistPlayer` / `listPlayer` çağırıyoruz.
AFK olanların listesini tutuyoruz ki sonradan giren oyuncuya da onları gizleyebilelim. Eklenti kapanırken herkes geri gösteriliyor.

## TAB eklentisiyle ilgili not

TAB kullanıyorsan ve suffix olarak `%essentials_afk%` koyduysan, PlaceholderAPI'nin `boolean` ayarı yüzünden
isminin yanında `✔` / `✖` çıkabilir. TAB `config.yml`'de şöyle temizleniyor:

```yaml
placeholder-output-replacements:
  '%essentials_afk%':
    'yes': ' &7[AFK]'
    'no': ''
    '&a✔': ' &7[AFK]'
    '&c✖': ''
```

## Derleme

Paper API ve EssentialsX jar'ları classpath'te olsun:

```
javac --release 21 -cp paper-api.jar;EssentialsX.jar -d out src/main/java/com/cevher/afktab/*.java
```

`src/main/resources` içindekini de `out` içine kopyalayıp jar yap.

MIT lisanslı.
