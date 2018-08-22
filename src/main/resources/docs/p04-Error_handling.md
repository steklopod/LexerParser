## Обработка ошибок

Давайте попробуем разобрать синтаксически некорректную программу. 
Предположим, что мы забыли указать `PT` в первом случае коммутатора:

<!-- code -->
```scala
    scala> WorkflowCompiler(invalidCode)
    res1: Either[WorkflowCompilationError,WorkflowAST] = Left(WorkflowParserError(string literal expected))
```

Мы получаем четкое сообщение об ошибке, сообщающее ожидаемый строковый литерал, но где? 
Было бы неплохо узнать местоположение источника этой ошибки. 
К счастью для нас, компиляторы синтаксического анализатора Scala поддерживают запись исходного местоположения 
оригинала маркера при его анализе.

Для этого наши свойства `WorkflowToken` и `WorkflowAST` должны быть смешаны с позицией. 
Это предоставляет мутабельную переменную `pos` и метод `setPos`, который можно использовать один раз, 
чтобы отметить экземпляр номерами строк и столбцов.

Во-вторых, мы должны использовать `positioned` оператор для каждого из наших парсеров. 
_Например_, синтаксический анализатор токена `IDENTIFIER` будет записан как:

<!-- code -->
```scala
     def identifier: Parser[IDENTIFIER] = positioned {
       "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => IDENTIFIER(str) }
     }
```

One ugly side effect of the Positional mixin is that all of our tokens must now become case classes instead of case objects, 
since each one now holds mutable state.

Нежелательный побочный **эффект подмешивания `Positional`** заключается в том, что теперь все наши **токены должны стать case-классами** 
 вместо case-объектов, поскольку каждый из них теперь имеет изменяемое состояние.

Подтипы `WorkflowCompilationError` теперь включают информацию о местоположении...

<!-- code -->
```scala
    case class WorkflowLexerError(location: Location, msg: String) extends WorkflowCompilationError
    case class WorkflowParserError(location: Location, msg: String) extends WorkflowCompilationError
    
    case class Location(line: Int, column: Int) {
      override def toString = s"$line:$column"
    }
```

... который сообщается в каждой фазе метода `apply`:


<!-- code -->
```scala
    def apply(code: String): Either[WorkflowLexerError, List[WorkflowToken]] = {
      parse(tokens, code) match {
        case NoSuccess(msg, next) => Left(WorkflowLexerError(Location(next.pos.line, next.pos.column), msg))
        case Success(result, next) => Right(result)
      }
    }
```

Давайте теперь попытаемся снова проанализировать некорректный код:

<!-- code -->
```scala
      scala> WorkflowCompiler(invalidCode)
      res1: Either[WorkflowCompilationError,WorkflowAST] = Left(3:14,WorkflowParserError(string literal expected))
```

## Финальные замечания

Это все! Мы перешли от разбиения текстового потока на последовательность токенов, чтобы окончательно собрать их в 
типизированное абстрактное синтаксическое дерево, что привело к гораздо более эффективному способу рассуждать о программе.

Теперь мы можем расширять компилятор для выполнения других не связанных с парсингом задач, таких как проверка 
_(например, обеспечение того, чтобы все пути кода заканчивались ключевым словом `exit`)_ или наиболее очевидным: 
_генерация кода, т.е. пересечение этого АСТ для создания последовательность инструкций_.


* <= [Построение цепочки парсеров](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p03-Pipelining.md)

<== [Начало](https://github.com/steklopod/LexerParser/blob/master/README.md)

[пример переведен мной отсюда](https://enear.github.io/2016/03/31/parser-combinators/)
