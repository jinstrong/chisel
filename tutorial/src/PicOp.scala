package Tutorial

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.io.Source
import java.io.FileOutputStream
import java.io.File

class PicOp extends Component {
  val io = new Bundle {
    val in  = Bits(8, INPUT)
    val out = Bits(8, OUTPUT)
  }

  io.out := io.in << UFix(1)
}

object Image {
  val MagicNumber  = 0x59a66a95
  val NoColorMap   = 0
  val StandardType = 1
  def dwordFromString(s: String, off: Int): Int = 
    (s(off+0) << 24)|(s(off+1) << 16)|(s(off+2) << 8)|(s(off+3) << 0)
  def dwordToString(d: Int) = {
    var res = new Array[Byte](4)
    res(0) = ((d >> 24)&255).toByte
    res(1) = ((d >> 16)&255).toByte
    res(2) = ((d >>  8)&255).toByte
    res(3) = ((d >>  0)&255).toByte
    res
  }
  def apply(filename: String): Image = {
    val file = Source.fromFile(filename)(scala.io.Codec.ISO8859)
    var buf = new StringBuilder();
    file.foreach(c => buf += c)
    val rawData = buf.result()
    file.close()
    val magicNumber    = dwordFromString(rawData,  0)
    if (magicNumber != MagicNumber) println("BAD MAGIC NUMBER")
    val w              = dwordFromString(rawData,  4)
    val h              = dwordFromString(rawData,  8)
    val d              = dwordFromString(rawData, 12)
    // println(w + "x" + h + "x" + d)
    val len            = dwordFromString(rawData, 16)
    val itype          = dwordFromString(rawData, 20)
    val colorMapType   = dwordFromString(rawData, 24)
    val colorMapLength = dwordFromString(rawData, 28)
    val data           = new Array[Byte](rawData.length - 32)
    for (i <- 32 until rawData.length) {
      data(i-32) = rawData(i).toByte
    }
    new Image(w, h, d, data)
  }
  def apply(w: Int, h: Int, d: Int): Image = {
    var dat = new Array[Byte](w*h*d/8)
    println("CREATING IMAGE OF LEN " + dat.length)
    new Image(w, h, d, dat)
  }
}

import Image._

class Image(val w: Int, val h: Int, val d: Int, val data: Array[Byte]) {
  def write(filename: String) = {
    val writer = new FileOutputStream(new File(filename))
    // println("WRITING " + filename + " DATA LEN " + data.length)
    writer.write(dwordToString(MagicNumber))
    writer.write(dwordToString(w))
    writer.write(dwordToString(h))
    writer.write(dwordToString(d))
    writer.write(dwordToString(data.length))
    writer.write(dwordToString(StandardType))
    writer.write(dwordToString(NoColorMap))
    writer.write(dwordToString(0))
    writer.write(data)
    writer.close()
  }
}

class PicOpTests(c: PicOp, val infilename: String, val outfilename: String) extends Tester(c, Array(c.io)) {  
  defTests {
    val svars = new HashMap[Node, Node]()
    val ovars = new HashMap[Node, Node]()

    val inPic  = Image(infilename)
    val outPic = Image(inPic.w, inPic.h, inPic.d)
    step(svars, ovars, false)
    for (i <- 0 until inPic.data.length) {
      val rin = inPic.data(i)
      val  in = if (rin < 0) 256 + rin else rin
      svars(c.io.in) = Bits(in)
      step(svars, ovars, false)
      val out = ovars(c.io.out).litValue()
      outPic.data(i) = out.toByte
    }
    outPic.write(outfilename)
    true
  }
}
