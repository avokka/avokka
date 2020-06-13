package avokka.velocypack

import scodec.Err

sealed trait VPackError extends Exception with Product with Serializable {
  def history: List[String]
  def historyAdd(e: String): VPackError
}

object VPackError {

  final case class Overflow(l: Long, history: List[String] = List.empty)
      extends Exception(s"long overflow for $l")
      with VPackError {
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  final case class WrongType(v: VPack, history: List[String] = List.empty)
      extends Exception(s"wrong type ${v.name}")
      with VPackError {
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  final case class Conversion(exception: Throwable, history: List[String] = List.empty)
      extends Exception("type conversion", exception)
      with VPackError {
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  final case class Codec(err: Err) extends Exception(err.message) with VPackError {
    override def history: List[String] = err.context
    override def historyAdd(e: String): VPackError = copy(err = err.pushContext(e))
  }

  final case class NotEnoughElements(history: List[String] = List.empty)
      extends Exception("not enough elements")
      with VPackError {
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  final case class ObjectFieldAbsent(name: String, history: List[String] = List.empty)
      extends Exception(s"object field absent $name")
      with VPackError {
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  final case class IllegalValue(message: String, history: List[String] = List.empty)
      extends Exception(message)
      with VPackError {
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }
}
