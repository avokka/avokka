package avokka.velocypack

import scodec.Err

trait VPackError {
  def message: String
  def history: List[String]
  def historyAdd(e: String): VPackError
}

object VPackError {

  case class Overflow(l: Long, history: List[String] = List.empty) extends VPackError {
    override def message: String = s"long overflow for $l"
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  case class WrongType(v: VPack, history: List[String] = List.empty) extends VPackError {
    override def message: String = s"wrong type $v"
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  case class Conversion(exception: Throwable, history: List[String] = List.empty) extends VPackError {
    override def message: String = exception.getMessage
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  case class Codec(err: Err) extends VPackError {
    override def message: String = err.message
    override def history: List[String] = err.context
    override def historyAdd(e: String): VPackError = copy(err = err.pushContext(e))
  }

  case class NotEnoughElements(history: List[String] = List.empty) extends VPackError {
    override def message: String = "not enough elements"
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  case class ObjectFieldAbsent(name: String, history: List[String] = List.empty) extends VPackError {
    override def message: String = s"object field absent $name"
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }

  case class IllegalValue(message: String, history: List[String] = List.empty) extends VPackError {
    override def historyAdd(e: String): VPackError = copy(history = history :+ e)
  }
}
