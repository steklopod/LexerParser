# Parser combinators

**Задача:** создать свой [dsl-язык](https://ru.wikipedia.org/wiki/Предметно-ориентированный_язык), состоящий из блоков с отступами, аналогично языкам, таким как Python.

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

## Создание Лексера

Нам понадобятся токены для идентификаторов и строковых литералов, а также все зарезервированные ключевые 
слова и знаки препинания: 
`exit`, `read input`, `call service`, `switch`, `otherwise`, `:`, `->`, `==`, и `,`.

Нам также необходимо создавать искусственные токены, которые представляют собой увеличение и уменьшение 
в идентификации: 
`INDENT` и `DEDENT`, соответственно. 
Пожалуйста, проигнорируйте их сейчас, так как мы перейдем к ним на более позднем этапе.

<!-- code -->
```scala
    sealed trait WorkflowToken
    
    case class IDENTIFIER(str: String) extends WorkflowToken
    case class LITERAL(str: String) extends WorkflowToken
    case class INDENTATION(spaces: Int) extends WorkflowToken
    
    case object EXIT extends WorkflowToken
    case object READINPUT extends WorkflowToken
    case object CALLSERVICE extends WorkflowToken
    case object SWITCH extends WorkflowToken
    case object OTHERWISE extends WorkflowToken
    case object COLON extends WorkflowToken
    case object ARROW extends WorkflowToken
    case object EQUALS extends WorkflowToken
    case object COMMA extends WorkflowToken
    
    case object INDENT extends WorkflowToken
    case object DEDENT extends WorkflowToken
```

`RegexParsers` создан для построения парсеров символов с использованием `регулярных выражений`. 
Он обеспечивает неявные преобразования из `String` и `Regex` в `Parser [String]`, 
что позволяет использовать их в качестве отправной точки для составления все более сложных парсеров.

Наш лексер расширяет [RegexParsers](https://github.com/scala/scala-parser-combinators/blob/1.1.x/docs/Getting_Started.md), который является подтипом [Parsers](https://github.com/scala/scala-parser-combinators/blob/1.1.x/docs/Getting_Started.md): 

<!-- code -->
```scala
    object WorkflowLexer extends RegexParsers {
```

Начнем с указания, какие символы следует игнорировать как пробельные символы. 
Мы не можем игнорировать `\n` (_перенос строки_), так как нам нужно, чтобы он распознавал уровень 
идентификации, определяемый количеством пробелов, которые следуют за ним. 
_Любой другой символ пробела можно игнорировать_:

<!-- code -->
```scala
    override def skipWhitespace = true
    override val whiteSpace = "[ \t\r\f]+".r
```

[справка:](https://ru.wikibooks.org/wiki/Регулярные_выражения)
<!-- code -->
```php
    \t - табуляция;
    \r - возврат каретки;
    \f - конец (разрыв) страницы;
    \v - вертикальная табуляция.
    
    *метод `.r` создаёт регулярное выражение из строки
    
```

Теперь построим парсер для идентификаторов:

<!-- code -->
```scala
    def identifier: Parser[IDENTIFIER] = {
      "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => IDENTIFIER(str) }
    }
```

Метод **`^^`** действует как отображение по результату синтаксического анализа. 
Регулярное выражение `[a-zA-Z_][a-zA-Z0-9 _]*".r` неявно преобразуется в экземпляр `Parser [String]`, 
на котором мы отображаем функцию `(String => IDENTIFIER)`, возвращая таким образом экземпляр Parser `[IDENTIFIER]`.

Парсеры для _строковых литералов_ и _идентификаторов_ аналогичны:

<!-- code -->
```scala
    //все символы кроме двойных кавычек являются классом LITERAL
    def literal: Parser[LITERAL] = {
      """"[^"]*"""".r ^^ { str =>
        val content = str.substring(1, str.length - 1)
        LITERAL(content)
      }
    }
    
    //перенос строки, затем пробелы являются классом INDENTATION
    def indentation: Parser[INDENTATION] = {
      "\n[ ]*".r ^^ { whitespace =>
        val nSpaces = whitespace.length - 1
        INDENTATION(nSpaces)
      }
}
```

Создание парсеров для ключевых слов тривиально:

<!-- code -->
```scala
    def exit          = "exit"          ^^ (_ => EXIT)
    def readInput     = "read input"    ^^ (_ => READINPUT)
    def callService   = "call service"  ^^ (_ => CALLSERVICE)
    def switch        = "switch"        ^^ (_ => SWITCH)
    def otherwise     = "otherwise"     ^^ (_ => OTHERWISE)
    def colon         = ":"             ^^ (_ => COLON)
    def arrow         = "->"            ^^ (_ => ARROW)
    def equals        = "=="            ^^ (_ => EQUALS)
    def comma         = ","             ^^ (_ => COMMA)
```

Теперь мы составим все это в синтаксический анализатор, способный распознавать каждый токен. 
Мы будем использовать следующие операторы:

* `|` (или) - для распознавания любого из наших токенов-парсеров;

* `rep1` - который распознает одно или несколько повторений своих аргументов;

* `phrase` - которая пытается поглотить все входные данные, пока больше не останется.

<!-- code -->
```scala
    def tokens: Parser[List[WorkflowToken]] = {
      phrase(rep1(exit | readInput | callService | switch | otherwise | colon | arrow
         | equals | comma | literal | identifier | indentation)) ^^ { rawTokens =>
        processIndentations(rawTokens)
      }
    }
```

Обратите внимание, что порядок операндов имеет значение при работе с `**двусмысленностью** (неоднозначностью)`. 
Если бы мы отправили `identifier` перед `exit`, `readInput` и др. - наш анализатор никогда не узнал бы их
 как специальные ключевые слова, так как они были бы успешно проанализированы как идентификаторы.

>Обработка отступа

[пример переведен мной отсюда](https://enear.github.io/2016/03/31/parser-combinators/)
