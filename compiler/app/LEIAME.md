# compiler/app — classes só do app (Tap Ult)

Hoje **nenhuma** classe do pacote é exclusiva do app: tudo o que vinha no
appCompiler (`hdatChar`/`hdatGD`/`hdatBack`, `errors/`, builders com
`ErrorReporter` e callbacks de progresso) é usado também pelo jogo e está em
`../global/`. O que muda no app é só o comportamento, pela flag:

```java
PacBuilder.isApp = true; // uma vez, antes de extrair/compilar em lote
```

Projeto do app = `global/` + `app/`. Coloque aqui as classes que só o app usa
(telas, lote de extração/compilação etc.).
