# CevherAfkTab

EssentialsX'te AFK olan oyuncuları diğer oyuncuların **tab listesinden gizler**. AFK'dan dönünce geri ekler.
Oyuncu dünyada görünmez olmaz (vanish değildir), yalnızca tab listesinden çıkar.

*Hides AFK players (EssentialsX) from other players' tab list using Paper's `Player#unlistPlayer`.*

## Gereksinimler
- Paper 1.21+ (Java 21)
- EssentialsX

## Yetki
| Yetki | Açıklama |
|-------|----------|
| `cevherafktab.gor` | AFK oyuncuları tab listesinde görmeye devam eder (varsayılan: yok) |

## Derleme
Paper API ve EssentialsX jar'larını sınıf yoluna ekleyerek `src/main` altını `javac --release 21` ile derleyin
ve `src/main/resources` içeriğini jar'a ekleyin.

## Lisans
MIT
