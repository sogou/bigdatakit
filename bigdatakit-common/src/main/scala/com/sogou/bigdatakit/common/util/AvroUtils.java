package com.sogou.bigdatakit.common.util;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Tao Li on 2016/3/15.
 */
public class AvroUtils {
  public static <T extends GenericRecord> byte[] avroObjectToBytes(T object) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
    DatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(object.getSchema());
    try {
      writer.write(object, encoder);
      encoder.flush();
      return output.toByteArray();
    } finally {
      output.close();
    }
  }

  public static <T extends GenericRecord> T avroBytesToObject(byte[] bytes, Schema schema) throws IOException {
    DatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>(schema);
    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
    return (T) reader.read(null, decoder);
  }
}
