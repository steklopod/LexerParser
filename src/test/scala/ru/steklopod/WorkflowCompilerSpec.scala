package ru.steklopod

import org.scalatest.{FlatSpec, Matchers}
import ru.steklopod.compiler.{Location, WorkflowCompiler, WorkflowParserError}
import ru.steklopod.parser._

class WorkflowCompilerSpec extends FlatSpec with Matchers {

  val validCode =
    """
      |read input name, country
      |switch:
      |  country == "PT" ->
      |    call service "A"
      |    exit
      |  otherwise ->
      |    call service "B"
      |    switch:
      |      name == "unknown" ->
      |        exit
      |      otherwise ->
      |        call service "C"
      |        exit
    """.stripMargin.trim

  val invalidCode =
    """
      |read input name, country
      |switch:
      |  country == PT ->
      |    call service "A"
      |    exit
      |  otherwise ->
      |    call service "B"
      |    switch:
      |      name == "unknown" ->
      |        exit
      |      otherwise ->
      |        call service "C"
      |        exit
    """.stripMargin.trim

  val successfulAST = AndThen(
    ReadInput(List("name", "country")),
    Choice(List(
      IfThen( Equals("country", "PT"), AndThen(CallService("A"), Exit) ),
      OtherwiseThen(
        AndThen(
          CallService("B"),
          Choice(List(
            IfThen( Equals("name", "unknown"), Exit ),
            OtherwiseThen( AndThen(CallService("C"), Exit) )
          ))
        )
      )
    ))
  )

  val errorMsg = WorkflowParserError(Location(3,14), "string literal expected")




  "Workflow compiler" should "successfully parse a valid workflow" in {
    WorkflowCompiler(validCode) shouldBe Right(successfulAST)
  }

  it should "return an error with an invalid workflow" in {
    WorkflowCompiler(invalidCode) shouldBe Left(errorMsg)
  }

}
