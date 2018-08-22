## Создание лексического анализатора

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

Создание парсеров для _ключевых слов_ тривиально:

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
Мы будем использовать следующие **операторы**:

* **`|`** (или) - для распознавания любого из наших токенов-парсеров;

* **`rep1`** - который распознает одно или несколько повторений своих аргументов;

* **`phrase`** - которая пытается поглотить все входные данные, пока больше не останется.

<!-- code -->
```scala
    def tokens: Parser[List[WorkflowToken]] = {
      phrase(rep1(exit | readInput | callService | switch | otherwise | colon | arrow
         | equals | comma | literal | identifier | indentation)) ^^ { rawTokens =>
        processIndentations(rawTokens)
      }
    }
```

>Обратите внимание, что порядок операндов имеет значение при работе с __двусмысленными токенами__. 

Если бы мы отправили `identifier` перед `exit`, `readInput` и др. - наш анализатор никогда не узнал бы их
 как специальные ключевые слова, так как они были бы успешно проанализированы как идентификаторы.

## Обработка отступа

We apply a brief post-processing step to our parse result with the processIndentations method. 
This is used to produce the artifical INDENT and DEDENT tokens from the INDENTATION tokens. 
Each increase in indentation level will be pushed to a stack, producing an INDENT, 
and decreases in indentation level will be popped from the indentation stack, producing DEDENTs.

Мы применяем короткий шаг после обработки к нашему результату синтаксического анализа методом `processIndentations`. 
Он используется для создания искусственных токенов `INDENT` и `DEDENT` из токенов `INDENTATION`. 
Каждое увеличение уровня отступов будет перенесено в стек, создавая `INDENT`, 
и уменьшение уровня отступов будет выведено из стека отступов, производя `DEDENT`.

<!-- code -->
```scala
    private def processIndentations(tokens: List[WorkflowToken],
                                    indents: List[Int] = List(0)): List[WorkflowToken] = {
      tokens.headOption match {
    
        // если есть увеличение (increase) уровня отступов, мы добавляем этот новый уровень в стек и создаем INDENT
        case Some(INDENTATION(spaces)) if spaces > indents.head =>
          INDENT :: processIndentations(tokens.tail, spaces :: indents)
    
      // если снижение (decrease), мы выходим из стека, пока не достигнем нового уровня и
      // мы производим DEDENT для каждого взятия из стека (`pop`-операции)
        case Some(INDENTATION(spaces)) if spaces < indents.head =>
          val (dropped, kept) = indents.partition(_ > spaces)
          (dropped map (_ => DEDENT)) ::: processIndentations(tokens.tail, kept)
    
      // если уровень отступов остается неизменным, токены не создаются
        case Some(INDENTATION(spaces)) if spaces == indents.head =>
          processIndentations(tokens.tail, indents)
    
      // другие токены игнорируются
        case Some(token) =>
          token :: processIndentations(tokens.tail, indents)
    
      // последний шаг - создать DEDENT для каждого оставшегося уровня отступов, таким образом
      // «закрывая» оставшиеся открытые объекты
        case None =>
          indents.filter(_ > 0).map(_ => DEDENT)
    
      }
    }
```


Все настроено! Этот парсер токенов будет генерировать `ParseResult[List[WorkflowToken]]`, потребляя `Reader[Char]`. 
`RegexParsers` определяет свой собственный `Reader[Char]`, который внутренне вызывается методом `parse`, который он предоставляет. 
Давайте затем определим метод `apply` для `WorkflowLexer`:

<!-- code -->
```scala
    trait WorkflowCompilationError
    case class WorkflowLexerError(msg: String) extends WorkflowCompilationError
```

<!-- code -->
```scala
    object WorkflowLexer extends RegexParsers {
      ...
    
      def apply(code: String): Either[WorkflowLexerError, List[WorkflowToken]] = {
        parse(tokens, code) match {
          case NoSuccess(msg, next) => Left(WorkflowLexerError(msg))
          case Success(result, next) => Right(result)
        }
      }
    }
```

_Попробуем наш лексический парсер из примера выше_:

<!-- code -->
```jshelllanguage
    scala> WorkflowLexer(code)
    res0: Either[WorkflowLexerError,List[WorkflowToken]] = Right(List(READINPUT, IDENTIFIER(name), COMMA,
    IDENTIFIER(country), SWITCH, COLON, INDENT, IDENTIFIER(country), EQUALS, LITERAL(PT), ARROW, INDENT, 
    CALLSERVICE, LITERAL(A), EXIT, DEDENT, OTHERWISE, ARROW, INDENT, CALLSERVICE, LITERAL(B), SWITCH, COLON, 
    INDENT, IDENTIFIER(name), EQUALS, LITERAL(unknown), ARROW, INDENT, EXIT, DEDENT, OTHERWISE, ARROW, 
    INDENT, CALLSERVICE, LITERAL(C), EXIT, DEDENT, DEDENT, DEDENT, DEDENT))
```

>НАВИГАЦИЯ:

* => [Создание парсера](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p02-Building_Parser.md)

* <== [Начало](https://github.com/steklopod/LexerParser/blob/master/README.md)
