# CevherOto

AutoPickup, AutoBlock ve BestTools'u tek eklentide topladık. Her oyuncu `/oto` menüsünden istediği özelliği
kendisi açıp kapatıyor, hangisinin varsayılan açık olacağını `config.yml` belirliyor.

- **Toplama**: kırılan blokların eşyası ve XP'si direkt envantere gelir
- **Eritme**: cevher kırınca eritilmiş hali düşer
- **Yaratık eşyaları**: drop'lar direkt envantere gelir
- **Blok yapma**: külçe/madenleri bloğa çevirir, `/blok oto` ile otomatik
- **En iyi alet**: kırılacak bloğa uygun aleti seçer
- **Doldurma**: eldeki yığın bitince envanterden yenisini getirir
- Envanter dolunca mesaj ve başlık uyarısı

Paper 1.21+ (Java 21) ister. PlaceholderAPI zorunlu değil, varsa `%cevheroto_toplama%` gibi placeholder'lar çalışır.

## Komutlar

| Komut | |
|-------|-|
| `/oto` | ayar menüsü |
| `/oto yenile` | config'i yeniler (`cevheroto.admin`) |
| `/autopickup` `/autosmelt` `/autodrops` `/refill` | ilgili özelliği aç/kapat |
| `/besttools [hotbar]` | en iyi alet |
| `/autoblock [oto]` | blok yapma |

## Yetkiler

`cevheroto.toplama`, `.yaratik`, `.blok`, `.alet`, `.doldurma` herkese açık gelir.
`cevheroto.eritme` ve `cevheroto.admin` varsayılan olarak sadece op.

## Ayarlar

Varsayılanlar, kapalı dünyalar, kara listeler, eritme listesi ve mesajların hepsi `config.yml` içinde.
Oyuncu tercihleri dosyaya değil oyuncunun kendi verisine (PDC) yazılır.

## Derleme

Paper API, Vault ve PlaceholderAPI classpath'te olsun:

```
javac --release 21 -cp paper-api.jar;Vault.jar;PlaceholderAPI.jar -d out src/main/java/com/cevher/oto/*.java
```

`src/main/resources` içindekini `out`'a kopyalayıp jar yap. MIT lisanslı.
