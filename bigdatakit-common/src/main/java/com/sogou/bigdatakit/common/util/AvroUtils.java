package com.sogou.bigdatakit.common.util;

import org.apache.avro.Schema;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Tao Li on 2016/3/15.
 */
public class AvroUtils {
  public static <T extends SpecificRecordBase> byte[] avroObjectToBytes(T object)
      throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
    DatumWriter<SpecificRecordBase> writer =
        new SpecificDatumWriter<SpecificRecordBase>(object.getSchema());
    try {
      writer.write(object, encoder);
      encoder.flush();
      return output.toByteArray();
    } finally {
      output.close();
    }
  }

  public static <T extends SpecificRecordBase> T avroBytesToObject(byte[] bytes, Schema schema)
      throws IOException {
    DatumReader<SpecificRecordBase> reader = new SpecificDatumReader<SpecificRecordBase>(schema);
    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
    return (T) reader.read(null, decoder);
  }
}
