package com.sogou.bigdatakit.kafka.serializer

import java.io.ByteArrayInputStream
import java.util.Collections

import com.sogou.bigdatakit.common.util.CommonUtils._
import kafka.utils.VerifiableProperties
import org.apache.avro.io.{BinaryDecoder, DecoderFactory}
import org.apache.avro.specific.SpecificDatumReader
import org.apache.flume.Event
import org.apache.flume.event.{EventBuilder, SimpleEvent}
import org.apache.flume.source.avro.AvroFlumeEvent

/**
 * Created by Tao Li on 2015/8/20.
 */
class SerializedSimpleEvent(event: Event) extends SimpleEvent with Serializable {
  setBody(event.getBody)
  setHeaders(event.getHeaders)
}

// FIXME AvroFlumeEventDecoder is not available in yarn-client mode
class AvroFlumeEventDecoder(props: VerifiableProperties = null)
  extends kafka.serializer.Decoder[Event] {
  private var reader: Option[SpecificDatumReader[AvroFlumeEvent]] = None
  private var decoder: ThreadLocal[BinaryDecoder] = new ThreadLocal[BinaryDecoder] {
    override def initialValue(): BinaryDecoder = null
  }

  override def fromBytes(bytes: Array[Byte]): Event = {
    val in = new ByteArrayInputStream(bytes)
    decoder.set(DecoderFactory.get.directBinaryDecoder(in, decoder.get))
    if (!reader.isDefined) reader = Some(new SpecificDatumReader[AvroFlumeEvent](classOf[AvroFlumeEvent]))
    val event: AvroFlumeEvent = reader.get.read(null, decoder.get)
    // Event should be Serializable
    new SerializedSimpleEvent(EventBuilder.withBody(event.getBody.array, toStringJavaMap(event.getHeaders)))
  }
}

class AvroFlumeEventBodyDecoder(props: VerifiableProperties = null)
  extends kafka.serializer.Decoder[String] {
  val encoding = if (props == null) "UTF-8" else props.getString("flume.event.body.serializer.encoding", "UTF-8")

  private var reader: Option[SpecificDatumReader[AvroFlumeEvent]] = None
  private var decoder: ThreadLocal[BinaryDecoder] = new ThreadLocal[BinaryDecoder] {
    override def initialValue(): BinaryDecoder = null
  }

  override def fromBytes(bytes: Array[Byte]): String = {
    val in = new ByteArrayInputStream(bytes)
    decoder.set(DecoderFactory.get.directBinaryDecoder(in, decoder.get))
    if (!reader.isDefined) reader = Some(new SpecificDatumReader[AvroFlumeEvent](classOf[AvroFlumeEvent]))
    val event: AvroFlumeEvent = reader.get.read(null, decoder.get)
    new String(event.getBody.array, encoding)
  }
}

class SimpleFlumeEventDecoder(props: VerifiableProperties = null)
  extends kafka.serializer.Decoder[Event] {
  override def fromBytes(bytes: Array[Byte]): Event = {
    EventBuilder.withBody(bytes, Collections.emptyMap[String, String])
  }
}