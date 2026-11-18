# CevherOto

AutoPickup, AutoBlock ve BestTools gibi üç ayrı eklentiyi tek eklentide topladık. Oyuncu `/oto` menüsünden
istediği özelliği kendisi açıp kapatıyor.

## Özellikler

- **Otomatik toplama**: kırılan blokların eşyaları ve XP'si direkt envantere gelir
- **Otomatik eritme**: cevher kırınca eritilmiş hali düşer
- **Yaratık eşyaları**: öldürülen yaratığın drop'ları direkt envantere gelir
- **Otomatik blok**: envanterdeki külçe/madenleri bloğa çevirir (`/blok`, `/blok oto`)
- **En iyi alet**: kırılacak bloğa uygun aleti seçer
- **Otomatik doldurma**: eldeki stack bitince envanterden yenisini getirir
- Envanter dolunca mesaj ve başlık uyarısı

## Gerekenler

- Paper 1.21+ (Java 21)
- PlaceholderAPI isteğe bağlı (`%cevheroto_toplama%` gibi placeholder'lar için)

## Komutlar

| Komut | Açıklama |
|-------|----------|
| `/oto` | Ayar menüsü |
| `/oto yenile` | config'i yeniler (`cevheroto.admin`) |
| `/autopickup` | Otomatik toplama aç/kapat |
| `/autosmelt` | Otomatik eritme aç/kapat |
| `/autodrops` | Yaratık eşyaları aç/kapat |
| `/besttools [hotbar]` | En iyi alet aç/kapat |
| `/refill` | Otomatik doldurma aç/kapat |
| `/autoblock [oto]` | Bloğa çevir / otomatik modu aç-kapat |

## Yetkiler

| Yetki | Varsayılan |
|-------|-----------|
| `cevheroto.toplama` | herkes |
| `cevheroto.eritme` | op |
| `cevheroto.yaratik` | herkes |
| `cevheroto.blok` | herkes |
| `cevheroto.alet` | herkes |
| `cevheroto.doldurma` | herkes |
| `cevheroto.admin` | op |

## Ayarlar

Hepsi `config.yml` içinde: varsayılan değerler, kapalı dünyalar, kara listeler, eritme listesi, mesajlar.
Oyuncunun seçimleri dosyaya değil kendi verisine (PDC) kaydedilir, ayrı bir veri dosyası oluşmaz.

## Derleme

Paper API ve Vault classpath'te olsun, `src/main/java` altını `javac --release 21` ile derle,
`src/main/resources` içindekini jar'a ekle.

MIT lisanslı.
