# Compiler unificado (`com.zue.gd.util`)

Um único pacote para o **jogo** e para o **app** (Tap Ult). Junta o antigo
`gameCompiler` e o `appCompiler`; o comportamento muda só pela flag
`PacBuilder.isApp`.

## Estrutura

| Pasta | Onde entra | Conteúdo |
|---|---|---|
| `compiler/global/` | jogo **e** app | Java puro: extractors, builders, `hdatChar`/`hdatGD`/`hdatBack`, DAC, SPR, Extras (parser/builder/extractor), `errors/` |
| `compiler/jogo/` | **só no jogo** | `Compiler.java` (estende `GameData`), `PacBuilderView`, `apk/` (export de Mod APK), runtime de Extras (`ExtrasReader`, `video/`, `webp/`, pacote `com.namcobandaigames.dragonballtap.apk`) |
| `compiler/app/` | **só no app** | hoje vazio (ver `app/LEIAME.md`) — nenhuma classe é exclusiva do app |

- Projeto do jogo = `global/` + `jogo/`.
- Projeto do app = `global/` + `app/`.

Para compilar:

- **App:** não copie `jogo/` (depende de `GameData`/`TCBManajer` e o
  `jogo/apk/ApkProcessor` colide com o `ApkProcessor` do app). Use
  `PacBuilder.isApp = true` — o `PacExtractor.isApp` não existe mais.
- **Jogo:** use o `GameData.java` (e o `TCBManajer.java`) deste repositório:
  o `Compiler` chama `decodeData`, `registerForGLReload`, `loadPng` e
  `reloadDerivedGL`, que só existem no `GameData` corrigido.

O `global/` não importa nenhuma classe do jogo (`Logue`, `GameData`, `TCBManajer`…),
então compila no app sem stubs. O `errors/` (que só vinha no appCompiler) fica
em `global/` porque os builders compartilhados o usam — no jogo eles recebem
`reporter == null`.

## A flag `PacBuilder.isApp`

É a **única** flag de modo (o `PacExtractor.isApp` separado foi removido; o
próprio comentário dele já dizia que devia reaproveitar a do `PacBuilder`).

| | `isApp = false` (jogo, padrão) | `isApp = true` (app) |
|---|---|---|
| `PacExtractor.extract` | `<raiz>/<pack>/` | `<raiz>/BACKS\|CARDS\|CHARS\|SYSTEM/<pack>/` |
| `PacBuilder.build` | grava os binários soltos na pasta (o `Compiler` carrega de lá) | grava os binários e empacota `<pacOutRoot>/<pack>.pac` (obrigatório passar `pacOutRoot`) |

No app: `PacBuilder.isApp = true;` uma vez, antes de extrair/compilar. Se o app
fazia `PacExtractor.isApp = true;`, troque por essa linha.

## Arquivos do gameCompiler que não existem mais

`ExtractBinChar/hdat*.java` e `gamedata/hdatgd*.java` → `hdatChar.java` e
`hdatGD.java`; `back/backBin.java` → `hdatBack.java`; `BinExtractor.java.bak`
removido. No projeto do jogo, apague os antigos ao copiar este pacote.

## Garantia de ida e volta

O `BinExtractor` só grava um HDAT no formato detalhado se esse texto,
recompilado, devolve **exatamente** os mesmos bytes; senão grava `$RAW` com um
comentário. Assim, extrair → compilar sem editar nada nunca muda o binário
(o mesmo vale para DAC, texto do `text00` e o `.pac` reempacotado).

## Mapa dos HDATs

`HDATS.md` lista cada HDAT do `gamedata.pac` e do `charNN.pac` que o
`TCBManajer` lê (inclusive por índice em variável), a função e o extractor.
