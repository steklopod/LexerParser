package ru.steklopod.lexer

import ru.steklopod.compiler.{Location, WorkflowLexerError}
import scala.util.parsing.combinator.RegexParsers

object WorkflowLexer extends RegexParsers {
  override def skipWhitespace: Boolean = true
  override val whiteSpace = "[ \t\r\f]+".r    /**  \t - табуляция; \r - возврат каретки; \f - конец (разрыв) страницы. */

  def apply(code: String): Either[WorkflowLexerError, List[WorkflowToken]] = {
    parse(tokens, code) match {
      case NoSuccess(msg, next) => Left(WorkflowLexerError(Location(next.pos.line, next.pos.column), msg))
      case Success(result, next) => Right(result)
    }
  }

  def tokens: Parser[List[WorkflowToken]] = {
    phrase(rep1(exit | readInput | callService | switch | otherwise | colon | arrow
      | equals | comma | literal | identifier | indentation)) ^^ { rawTokens =>
      processIndentations(rawTokens)
    }
  }

  private def processIndentations(tokens: List[WorkflowToken], indents: List[Int] = List(0)): List[WorkflowToken] = {
    tokens.headOption match {

      // если есть увеличение (increase) уровня отступов, мы добавляем этот новый уровень в стек и создаем INDENT
      case Some(INDENTATION(spaces)) if spaces > indents.head =>
        INDENT() :: processIndentations(tokens.tail, spaces :: indents)

      // если снижение (decrease), мы выходим из стека, пока не достигнем нового уровня и
      // мы производим DEDENT для каждого взятия из стека (`pop`-операции)
      case Some(INDENTATION(spaces)) if spaces < indents.head =>
        val (dropped, kept) = indents.partition(_ > spaces)
        (dropped map (_ => DEDENT())) ::: processIndentations(tokens.tail, kept)

      // если уровень отступов остается неизменным, токены не создаются
      case Some(INDENTATION(spaces)) if spaces == indents.head =>
        processIndentations(tokens.tail, indents)

      // другие токены игнорируются
      case Some(token) =>
        token :: processIndentations(tokens.tail, indents)

      // последний шаг - создать DEDENT для каждого оставшегося уровня отступов, таким образом
      // «закрывая» оставшиеся открытые объекты
      case None =>
        indents.filter(_ > 0).map(_ => DEDENT())
    }
  }

  def identifier: Parser[IDENTIFIER] = positioned {
    "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => IDENTIFIER(str) }
  }

  /**
    * [^ ]	- соответствует единичному символу из числа тех, которых нет в скобках,
    * например, [^abc] соответствует любому символу, кроме «a», «b» или «c».
    * [^a-z] соответствует любому символу, кроме символов нижнего регистра в латинском алфавите.
    **/
  def literal: Parser[LITERAL] = positioned {
    """"[^"]*"""".r ^^ { str =>
      val content = str.substring(1, str.length - 1)
      LITERAL(content)
    }
  }

  /**
    * \n[] - перенос строки, затем пробелы являются классом INDENTATION
    */
  def indentation: Parser[INDENTATION] = positioned {
    "\n[ ]*".r ^^ { whitespace =>
      val nSpaces = whitespace.length - 1
      INDENTATION(nSpaces)
    }
  }

  def exit             = positioned {    "exit" ^^ (_ => EXIT())  }
  def readInput   = positioned {    "read input" ^^ (_ => READINPUT())  }
  def callService= positioned {    "call service" ^^ (_ => CALLSERVICE())  }
  def switch         = positioned {    "switch" ^^ (_ => SWITCH())  }
  def otherwise   = positioned {    "otherwise" ^^ (_ => OTHERWISE())  }
  def colon          = positioned {    ":" ^^ (_ => COLON())  }
  def arrow         = positioned {    "->" ^^ (_ => ARROW())  }
  def equals        = positioned {    "==" ^^ (_ => EQUALS())  }
  def comma         = positioned {    "," ^^ (_ => COMMA())  }
}
