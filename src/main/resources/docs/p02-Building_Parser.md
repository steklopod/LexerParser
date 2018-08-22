## Построение парсера

Теперь, когда мы позаботились о лексическом анализе, нам все еще не хватает этапа синтаксического анализа, т.е. 
преобразования последовательности токенов в `абстрактное синтаксическое дерево (АСД)` [abstract syntax tree (AST)]. 
В отличие от `RegexParsers`, которые генерируют синтаксические анализаторы строк, нам понадобится анализатор `WorkflowToken`.

<!-- code -->
```scala
    object WorkflowParser extends Parsers {
      override type Elem = WorkflowToken
```

Нам также необходимо определить `Reader[WorkflowToken]`, который будет использоваться синтаксическим анализатором для чтения 
из последовательности `WorkflowTokens`. Это довольно просто:

<!-- code -->
```scala
    class WorkflowTokenReader(tokens: Seq[WorkflowToken]) extends Reader[WorkflowToken] {
      override def first: WorkflowToken = tokens.head
      override def atEnd: Boolean = tokens.isEmpty
      override def pos: Position = NoPosition
      override def rest: Reader[WorkflowToken] = new WorkflowTokenReader(tokens.tail)
    }
```

Процесс реализации парсера аналогичен процессу, используемому для создания лексического анализатора. 
Мы определяем простые парсеры и композируем их в более сложные. 
Только на этот раз наши парсеры будут возвращать `АСД` вместо токенов:

<!-- code -->
```scala
    sealed trait WorkflowAST
    case class AndThen(step1: WorkflowAST, step2: WorkflowAST) extends WorkflowAST
    case class ReadInput(inputs: Seq[String]) extends WorkflowAST
    case class CallService(serviceName: String) extends WorkflowAST
    case class Choice(alternatives: Seq[ConditionThen]) extends WorkflowAST
    case object Exit extends WorkflowAST
    
    sealed trait ConditionThen { def thenBlock: WorkflowAST }
    case class IfThen(predicate: Condition, thenBlock: WorkflowAST) extends ConditionThen
    case class OtherwiseThen(thenBlock: WorkflowAST) extends ConditionThen
    
    sealed trait Condition
    case class Equals(factName: String, factValue: String) extends Condition
```

`WorkflowToken` наследуеТ неявное преобразование из `WorkflowToken` в `Parser[WorkflowToken]`. 
Это полезно для анализа безпараметрических токенов, таких как `EXIT`, `CALLSERVICE` и т.д. 
Для `IDENTIFIER` и `LITERAL` мы можем сопоставлять совпадение этих токенов с методом `accept`.

<!-- code -->
```scala
    private def identifier: Parser[IDENTIFIER] = {
      accept("identifier", { case id @ IDENTIFIER(name) => id })
    }
    
    private def literal: Parser[LITERAL] = {
      accept("string literal", { case lit @ LITERAL(name) => lit })
    }
```

Правила грамматики могут быть реализованы подобным образом:

<!-- code -->
```scala
    def condition: Parser[Equals] = {
      (identifier ~ EQUALS ~ literal) ^^ { case id ~ eq ~ lit => Equals(id, lit) }
    }
```

Это похоже на то, что мы делали ранее для создания токенов: мы сопоставили результат анализа 
(состав результатов `identifier`, `EQUALS` и `literal`) на экземпляр `Equals`. 
Обратите внимание на то, как совпадение шаблонов может быть использовано для экспрессивной распаковки результата композиции 
парсера путем секвенирования (т.е. оператора `~`).

Реализация остальных правил очень похожа на описанную выше грамматику:

<!-- code -->
```scala
    def program: Parser[WorkflowAST] = {
      phrase(block)
    }
    
    def block: Parser[WorkflowAST] = {
      rep1(statement) ^^ { case stmtList => stmtList reduceRight AndThen }
    }
    
    def statement: Parser[WorkflowAST] = {
      val exit = EXIT ^^ (_ => Exit)
      val readInput = READINPUT ~ rep(identifier ~ COMMA) ~ identifier ^^ {
        case read ~ inputs ~ IDENTIFIER(lastInput) => ReadInput(inputs.map(_._1.str) ++ List(lastInput))
      }
      val callService = CALLSERVICE ~ literal ^^ {
        case call ~ LITERAL(serviceName) => CallService(serviceName)
      }
      val switch = SWITCH ~ COLON ~ INDENT ~ rep1(ifThen) ~ opt(otherwiseThen) ~ DEDENT ^^ {
        case _ ~ _ ~ _ ~ ifs ~ otherwise ~ _ => Choice(ifs ++ otherwise)
      }
      exit | readInput | callService | switch
    }
    
    def ifThen: Parser[IfThen] = {
      (condition ~ ARROW ~ INDENT ~ block ~ DEDENT) ^^ {
        case cond ~ _ ~ _ ~ block ~ _ => IfThen(cond, block)
      }
    }
    
    def otherwiseThen: Parser[OtherwiseThen] = {
      (OTHERWISE ~ ARROW ~ INDENT ~ block ~ DEDENT) ^^ {
        case _ ~ _ ~ _ ~ block ~ _ => OtherwiseThen(block)
      }
    }
```

Как и в случае с lexer, мы также определяем метод `apply` у монады, который мы можем использовать позже, чтобы выразить конвейер операций:

<!-- code -->
```scala
    case class WorkflowParserError(msg: String) extends WorkflowCompilationError
```

<!-- code -->
```scala
    object WorkflowParser extends RegexParsers {
      ...
    
      def apply(tokens: Seq[WorkflowToken]): Either[WorkflowParserError, WorkflowAST] = {
        val reader = new WorkflowTokenReader(tokens)
        program(reader) match {
          case NoSuccess(msg, next) => Left(WorkflowParserError(msg))
          case Success(result, next) => Right(result)
        }
      }
    }
```


>НАВИГАЦИЯ:

* => [Построение цепочки парсеров](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p03-Pipelining.md)

* <= [Создание лексического анализатора](https://github.com/steklopod/LexerParser/blob/master/src/main/resources/docs/p01-Building_Lexer.md)

<== [Начало](https://github.com/steklopod/LexerParser/blob/master/README.md)


