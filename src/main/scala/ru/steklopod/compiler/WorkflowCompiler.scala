package ru.steklopod.compiler

import ru.steklopod.lexer.WorkflowLexer
import ru.steklopod.parser.{WorkflowAST, WorkflowParser}


object WorkflowCompiler {
  def apply(code: String): Either[WorkflowCompilationError, WorkflowAST] = {
    for {
      tokens <- WorkflowLexer(code).right
      ast <- WorkflowParser(tokens).right
    } yield ast
  }
}
