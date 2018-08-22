# Parser combinators - лексический анализатор

**Задача:** создать свой [dsl-язык](https://ru.wikipedia.org/wiki/Предметно-ориентированный_язык), 
состоящий из блоков с отступами, аналогично языкам, таким как Python. В качестве лексического анализатора используется библиотека,
которая ранее являлась частью языка Скала - `parser combinators`.

Пример парсинга файла `to.parse` (придуманный dsl):

<!-- code -->
```python
    read input name, country
    switch:
      country == "PT" ->
        call service "A"
        exit
      otherwise ->
        call service "B"
        switch:
          name == "unknown" ->
            exit
          otherwise ->
            call service "C"
            exit
```

В код следующего вида:

<!-- code -->
```scala
AndThen(
  ReadInput(List(name, country)),
  Choice(List(
    IfThen(
      Equals(country, PT),
      AndThen(CallService(A), Exit)
    ),
    OtherwiseThen(
      AndThen(
        CallService(B),
        Choice(List(
          IfThen(Equals(name, unknown), Exit),
          OtherwiseThen(AndThen(CallService(C), Exit))
        ))
      )
    )
  ))
)
```

[Parser combinator](https://en.wikipedia.org/wiki/Parser_combinator) - это просто функция, 
которая принимает парсеры в качестве входных данных и возвращает новый синтаксический анализатор 
в качестве вывода, аналогично тому, как функции более высокого порядка полагаются на вызов других функций, 
которые передаются в качестве входных данных для создания новой функции в качестве вывода.

В качестве примера, предположим, что у нас есть **парсер `int`**, который распознает целочисленные _литералы_ 
и **парсер `plus`**, который распознает символ _«+»_. 
Следовательно, мы можем **создать парсер**, 
который распознает последовательность **_`int plus int`_** как целочисленное дополнение.

Стандартная библиотека Scala включает в себя реализацию комбинаторов парсеров, 
которая размещается по адресу: [github.com/scala/scala-parser-combinators](https://github.com/scala/scala-parser-combinators)

Чтобы использовать его, вам просто потребуется следующая зависимость в вашем `build.sbt`: 
<!-- code -->
```sbtshell
    «org.scala-lang.modules» %% »scala-parser-combinators«% »1.1.1"
```

>ДАЛЕЕ:

* [Создание лексического анализатора](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p01-Building_Lexer.md)

* [Создание парсера](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p02-Building_Parser.md)

* [Построение цепочки парсеров](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p03-Pipelining.md)