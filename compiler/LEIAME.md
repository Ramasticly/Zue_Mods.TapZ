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
| `compiler/help/` | **em nenhum** (fonte) | `binChar.md`, `gamedata.md` (o que cada campo de cada HDAT faz) e o gerador `GerarHelpTexts.java` |

- Projeto do jogo = `global/` + `jogo/`.
- Projeto do app = `global/` + `app/`.

## Pasta `#HELP`

O Compiler cria uma pasta `#HELP` com `binChar.md` (HDATs do `charNN`) e
`gamedata.md` (HDATs do `gamedata`): o que cada campo faz, o que muda no
gameplay e quais campos o jogo não lê.

| | Onde fica | Quando é criada |
|---|---|---|
| Jogo | `Tap Battle/assets/#HELP/`, junto das pastas dos packs | ao extrair um `.pac` e no primeiro load de pack de cada execução |
| App | `<raiz>/SYSTEM/#HELP/` | ao extrair um `.pac` |

O texto vai embutido no código (`global/help/HelpTexts.java`), então não
precisa de nenhum arquivo extra no APK. Se o `.md` da pasta for diferente do
embutido (versão antiga ou editado), ele é regravado. A `#HELP` nunca vira
`.pac`: `PacBuilder.build`, `RealPacBuilder.buildSingle` e o "Gerar todas" a
ignoram.

Para mudar a ajuda: edite `compiler/help/*.md` e, na pasta `compiler/help`,
rode `java GerarHelpTexts.java`. Isso regrava `global/help/HelpTexts.java`.
Não copie `compiler/help/` para o projeto do jogo ou do app.

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

## Textos (`$TXT` / `$T_N`)

Cada string guarda a codificação original, para voltar aos mesmos bytes:

| No `.txt` | Vira no binário |
|---|---|
| `$T_0:"texto"` | UTF-8 — é o que o jogo lê (`TCBManajer.gub()` é sempre `utf-8`) |
| `$T_0:SJIS"テスト"` | Shift_JIS (do Windows), como os textos japoneses originais do `.pac` |
| `\xHH` dentro das aspas | o byte `HH` cru (caracteres de controle, ou uma string que não é nem UTF-8 nem Shift_JIS) |

Como o jogo lê UTF-8, um texto `SJIS"..."` aparece errado no jogo, igual ao
`.pac` original. Para o jogo mostrar o texto certo, apague o `SJIS` da frente:
ele passa a ser gravado em UTF-8. Textos extraídos antes dessa mudança (sem
`SJIS`) continuam compilando em UTF-8, como antes.

## Desligar o extractor de um HDAT (`BinExtractor.SKIP_HDATS`)

Para descobrir se um HDAT é a causa de um comportamento estranho, coloque-o em
`BinExtractor.SKIP_HDATS`: na extração ele vai direto em `$RAW` (bytes
originais), sem passar pelo `hdatGD`/`hdatChar`/`hdatBack` nem pelo `$TXT`.

```java
public static String[] SKIP_HDATS = {
    "gamedata:175",    // HDAT 175 do gamedata.pac
    "gamedata:50-60",  // faixa
    "char:13",         // HDAT 13 de todos os charNN.pac
    "char05:4",        // só do char05.pac
    "back:*",          // todos os HDATs dos backNN.pac
};
```

Vale só na extração: apague a pasta do pack (no jogo, em `Tap Battle/assets/`;
guarde antes o que tiver editado nela) para ele ser extraído de novo. O HDAT
pulado fica marcado no `.txt` com
`// Desligado em BinExtractor.SKIP_HDATS`.

## Mapa dos HDATs

`HDATS.md` lista cada HDAT do `gamedata.pac` e do `charNN.pac` que o
`TCBManajer` lê (inclusive por índice em variável), a função e o extractor.
