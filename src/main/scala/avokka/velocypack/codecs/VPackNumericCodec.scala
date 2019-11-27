package avokka.velocypack.codecs

trait VPackNumericCodec[T] {

  def smallEncode(implicit num: Numeric[T]): PartialFunction[T, Int] = {
    case 0 => 0x30
    case 1 => 0x31
    case 2 => 0x32
    case 3 => 0x33
    case 4 => 0x34
    case 5 => 0x35
    case 6 => 0x36
    case 7 => 0x37
    case 8 => 0x38
    case 9 => 0x39
    case -6 => 0x3a
    case -5 => 0x3b
    case -4 => 0x3c
    case -3 => 0x3d
    case -2 => 0x3e
    case -1 => 0x3f
  }

  def smallDecode(implicit num: Numeric[T]): PartialFunction[Int, T] = {
    case 0x30 => num.fromInt(0)
    case 0x31 => num.fromInt(1)
    case 0x32 => num.fromInt(2)
    case 0x33 => num.fromInt(3)
    case 0x34 => num.fromInt(4)
    case 0x35 => num.fromInt(5)
    case 0x36 => num.fromInt(6)
    case 0x37 => num.fromInt(7)
    case 0x38 => num.fromInt(8)
    case 0x39 => num.fromInt(9)
    case 0x3a => num.fromInt(-6)
    case 0x3b => num.fromInt(-5)
    case 0x3c => num.fromInt(-4)
    case 0x3d => num.fromInt(-3)
    case 0x3e => num.fromInt(-2)
    case 0x3f => num.fromInt(-1)
  }

}
