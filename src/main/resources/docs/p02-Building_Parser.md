## Построение парсера

Теперь, когда мы позаботились о лексическом анализе, нам все еще не хватает этапа синтаксического анализа, т.е. 
преобразования последовательности токенов в `абстрактное синтаксическое дерево (АСД)` [abstract syntax tree (AST)]. 
В отличие от `RegexParsers`, которые генерируют синтаксические анализаторы строк, нам понадобится анализатор `WorkflowToken`.

<!-- code -->
```scala
    object WorkflowParser extends Parsers {
      override type Elem = WorkflowToken
```

We also need to define a Reader[WorkflowToken] which will be used by the parser to read from a sequence of WorkflowTokens. 
This is pretty straightforward:

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

Moving on with the parser implementation, the process is similar to the one used to build the lexer. 
We define simple parsers and compose them into more complex ones. 
Only this time around our parsers will be returning ASTs instead of tokens:

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



===========================================

* => [Далее - построение цепочки из парсеров](https://github.com/steklopod/LexerParser)

* <= [Назад - Создание лексического анализатора](https://github.com/steklopod/LexerParser)

* <== [Начало](https://github.com/steklopod/LexerParser)


[пример переведен мной отсюда](https://enear.github.io/2016/03/31/parser-combinators/)
