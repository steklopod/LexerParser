package ru.steklopod.lexer

import scala.util.parsing.combinator.RegexParsers

object WorkflowLexer extends RegexParsers {

  override def skipWhitespace: Boolean = true

  override val whiteSpace = "[ \t\r\f]+".r

  def identifier: Parser[IDENTIFIER] = positioned {
    "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => IDENTIFIER(str) }
  }

  /**
    * [^ ]	- соответствует единичному символу из числа тех, которых нет в скобках.
    * Например, [^abc] соответствует любому символу, кроме «a», «b» или «c».
    * [^a-z] соответствует любому символу, кроме символов нижнего регистра в латинском алфавите.
    * */
  def literal: Parser[LITERAL] = positioned {
    """"[^"]*"""".r ^^ { str =>
      val content = str.substring(1, str.length - 1)
      LITERAL(content)
    }
  }

  def indentation: Parser[INDENTATION] = positioned {
    "\n[ ]*".r ^^ { whitespace =>
      val nSpaces = whitespace.length - 1
      INDENTATION(nSpaces)
    }
  }
}
