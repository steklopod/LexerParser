## Построение цепочки парсеров (Pipelining)

Мы рассмотрели как лексические, так и синтаксические анализаторы. Осталось только объединить их:

<!-- code -->
```scala
    object WorkflowCompiler {
      def apply(code: String): Either[WorkflowCompilationError, WorkflowAST] = {
        for {
          tokens <- WorkflowLexer(code).right
          ast <- WorkflowParser(tokens).right
        } yield ast
      }
    }
```

Давайте попробуем это в нашей примерной программе:

<!-- code -->
```scala
    scala> WorkflowCompiler(code)
    res0: Either[WorkflowCompilationError,WorkflowAST] = Right(AndThen(ReadInput(List(name, country)),
    Choice(List(IfThen(Equals(country,PT),AndThen(CallService(A),Exit)),OtherwiseThen(AndThen(CallService(B),
    Choice(List(IfThen(Equals(name,unknown),Exit), OtherwiseThen(AndThen(CallService(C),Exit))))))))))
```

Замечательно! Наш компилятор доказал, что способен анализировать действующие программы.

* => [Обработка ошибок](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p04-Error_handling.md)

* <= [Создание синтаксического анализатора](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p02-Building_Parser.md)

<=== [Начало](https://github.com/steklopod/LexerParser/blob/master/README.md)

