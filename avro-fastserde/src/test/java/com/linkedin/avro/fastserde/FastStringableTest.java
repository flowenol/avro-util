package com.linkedin.avro.fastserde;

import com.linkedin.avro.compatibility.AvroCompatibilityHelper;
import com.linkedin.avro.fastserde.generated.avro.AnotherSubRecord;
import com.linkedin.avro.fastserde.generated.avro.StringableRecord;
import com.linkedin.avro.fastserde.generated.avro.StringableSubRecord;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.util.Utf8;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.avro.fastserde.FastSerdeTestsSupport.*;


public class FastStringableTest {
  private File tempDir;
  private ClassLoader classLoader;

  public FastStringableTest() {
  }

  @DataProvider(name = "SlowFastDeserializer")
  public static Object[][] deserializers() {
    return new Object[][]{{true}, {false}};
  }

  @BeforeTest(groups = {"deserializationTest", "serializationTest"})
  public void prepare() throws Exception {
    Path tempPath = Files.createTempDirectory("generated");
    tempDir = tempPath.toFile();

    classLoader = URLClassLoader.newInstance(new URL[]{tempDir.toURI().toURL()},
        FastDeserializerDefaultsTest.class.getClassLoader());
  }

  StringableRecord generateRecord(URL exampleURL, URI exampleURI, File exampleFile, BigDecimal exampleBigDecimal,
      BigInteger exampleBigInteger) {

    StringableSubRecord subRecord = new StringableSubRecord();
    if (Utils.isAvro14()) {
      subRecord.put(0, exampleURI.toString());
    } else {
      subRecord.put(0, exampleURI);
    }
    AnotherSubRecord anotherSubRecord = new AnotherSubRecord();
    anotherSubRecord.put(0, subRecord);

    StringableRecord record = new StringableRecord();
    if (Utils.isAvro14()) {
      // Avro-1.4 doesn't support stringable field
      record.put(0, exampleBigInteger.toString());
      record.put(1, exampleBigDecimal.toString());
      record.put(2, exampleURI.toString());
      record.put(3, exampleURL.toString());
      record.put(4, exampleFile.toString());
      record.put(5, Collections.singletonList(exampleURL.toString()));
      record.put(6, Collections.singletonMap(exampleURL.toString(), exampleBigInteger.toString()));
      record.put(7, subRecord);
      record.put(8, anotherSubRecord);
    } else {
      record.put(0, exampleBigInteger);
      record.put(1, exampleBigDecimal);
      record.put(2, exampleURI);
      record.put(3, exampleURL);
      record.put(4, exampleFile);
      record.put(5, Collections.singletonList(exampleURL));
      record.put(6, Collections.singletonMap(exampleURL, exampleBigInteger));
      record.put(7, subRecord);
      record.put(8, anotherSubRecord);
    }
    return record;
  }

  @Test(groups = {"serializationTest"})
  public void serializeStringableFields() throws URISyntaxException, MalformedURLException {
    // given
    BigInteger exampleBigInteger = new BigInteger(String.valueOf(Long.MAX_VALUE)).pow(16);
    BigDecimal exampleBigDecimal = new BigDecimal(Double.MIN_VALUE).pow(16);
    File exampleFile = new File("/tmp/test");
    URI exampleURI = new URI("urn:ISSN:1522-3611");
    URL exampleURL = new URL("http://www.example.com");

    if (Utils.isSupportedAvroVersionsForSerializer()) {
      StringableRecord record =
          generateRecord(exampleURL, exampleURI, exampleFile, exampleBigDecimal, exampleBigInteger);

      // when
      StringableRecord afterDecoding =
          specificDataFromDecoder(StringableRecord.SCHEMA$, writeWithFastAvro(record, StringableRecord.SCHEMA$));

      // then
      if (Utils.isAvro14()) {
        Assert.assertEquals(exampleBigDecimal.toString(), afterDecoding.bigdecimal.toString());
        Assert.assertEquals(exampleBigInteger.toString(), afterDecoding.biginteger.toString());
        Assert.assertEquals(exampleFile.toString(), afterDecoding.file.toString());
        Assert.assertEquals(Collections.singletonList(new Utf8(exampleURL.toString())), afterDecoding.urlArray);
        Assert.assertEquals(
            Collections.singletonMap(new Utf8(exampleURL.toString()), new Utf8(exampleBigInteger.toString())),
            afterDecoding.urlMap);
        Assert.assertNotNull(afterDecoding.subRecord);
        Assert.assertEquals(exampleURI.toString(), afterDecoding.subRecord.uriField.toString());
        Assert.assertNotNull(afterDecoding.subRecordWithSubRecord);
        Assert.assertNotNull(afterDecoding.subRecordWithSubRecord.subRecord);
        Assert.assertEquals(exampleURI.toString(), afterDecoding.subRecordWithSubRecord.subRecord.uriField.toString());
      } else {
        Assert.assertEquals(exampleBigDecimal, afterDecoding.bigdecimal);
        Assert.assertEquals(exampleBigInteger, afterDecoding.biginteger);
        Assert.assertEquals(exampleFile, afterDecoding.file);
        Assert.assertEquals(Collections.singletonList(exampleURL), afterDecoding.urlArray);
        Assert.assertEquals(Collections.singletonMap(exampleURL, exampleBigInteger), afterDecoding.urlMap);
        Assert.assertNotNull(afterDecoding.subRecord);
        Assert.assertEquals(exampleURI, afterDecoding.subRecord.uriField);
        Assert.assertNotNull(afterDecoding.subRecordWithSubRecord);
        Assert.assertNotNull(afterDecoding.subRecordWithSubRecord.subRecord);
        Assert.assertEquals(exampleURI, afterDecoding.subRecordWithSubRecord.subRecord.uriField);
      }
    }
  }

  @Test(groups = {"deserializationTest"}, dataProvider = "SlowFastDeserializer")
  public void deserializeStringableFields(Boolean whetherUseFastDeserializer)
      throws URISyntaxException, MalformedURLException {
    // given
    BigInteger exampleBigInteger = new BigInteger(String.valueOf(Long.MAX_VALUE)).pow(16);
    BigDecimal exampleBigDecimal = new BigDecimal(Double.MIN_VALUE).pow(16);
    File exampleFile = new File("/tmp/test");
    URI exampleURI = new URI("urn:ISSN:1522-3611");
    URL exampleURL = new URL("http://www.example.com");

    StringableRecord record = generateRecord(exampleURL, exampleURI, exampleFile, exampleBigDecimal, exampleBigInteger);

    // when
    StringableRecord afterDecoding = null;
    if (whetherUseFastDeserializer) {
      afterDecoding =
          readWithFastAvro(StringableRecord.SCHEMA$, StringableRecord.SCHEMA$, specificDataAsDecoder(record));
    } else {
      afterDecoding =
          readWithSlowAvro(StringableRecord.SCHEMA$, StringableRecord.SCHEMA$, specificDataAsDecoder(record));
    }

    // then
    if (Utils.isAvro14()) {
      Assert.assertEquals(exampleBigDecimal.toString(), afterDecoding.bigdecimal.toString());
      Assert.assertEquals(exampleBigInteger.toString(), afterDecoding.biginteger.toString());
      Assert.assertEquals(exampleFile.toString(), afterDecoding.file.toString());
      Assert.assertEquals(Collections.singletonList(new Utf8(exampleURL.toString())), afterDecoding.urlArray);
      Assert.assertEquals(
          Collections.singletonMap(new Utf8(exampleURL.toString()), new Utf8(exampleBigInteger.toString())),
          afterDecoding.urlMap);
      Assert.assertNotNull(afterDecoding.subRecord);
      Assert.assertEquals(exampleURI.toString(), afterDecoding.subRecord.uriField.toString());
      Assert.assertNotNull(afterDecoding.subRecordWithSubRecord);
      Assert.assertNotNull(afterDecoding.subRecordWithSubRecord.subRecord);
      Assert.assertEquals(exampleURI.toString(), afterDecoding.subRecordWithSubRecord.subRecord.uriField.toString());
    } else {
      Assert.assertEquals(exampleBigDecimal, afterDecoding.bigdecimal);
      Assert.assertEquals(exampleBigInteger, afterDecoding.biginteger);
      Assert.assertEquals(exampleFile, afterDecoding.file);
      Assert.assertEquals(Collections.singletonList(exampleURL), afterDecoding.urlArray);
      Assert.assertEquals(Collections.singletonMap(exampleURL, exampleBigInteger), afterDecoding.urlMap);
      Assert.assertNotNull(afterDecoding.subRecord);
      Assert.assertEquals(exampleURI, afterDecoding.subRecord.uriField);
      Assert.assertNotNull(afterDecoding.subRecordWithSubRecord);
      Assert.assertNotNull(afterDecoding.subRecordWithSubRecord.subRecord);
      Assert.assertEquals(exampleURI, afterDecoding.subRecordWithSubRecord.subRecord.uriField);
    }
  }

  public <T> Decoder writeWithFastAvro(T data, Schema schema) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BinaryEncoder binaryEncoder = AvroCompatibilityHelper.newBinaryEncoder(baos);

    try {
      FastSpecificSerializerGenerator<T> fastSpecificSerializerGenerator =
          new FastSpecificSerializerGenerator<>(schema, tempDir, classLoader, null);
      FastSerializer<T> fastSerializer = fastSpecificSerializerGenerator.generateSerializer();
      fastSerializer.serialize(data, binaryEncoder);
      binaryEncoder.flush();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return DecoderFactory.defaultFactory().createBinaryDecoder(baos.toByteArray(), null);
  }

  public <T> T readWithFastAvro(Schema writerSchema, Schema readerSchema, Decoder decoder) {
    FastDeserializer<T> deserializer =
        new FastSpecificDeserializerGenerator<T>(writerSchema, readerSchema, tempDir, classLoader,
            null).generateDeserializer();
    try {
      return deserializer.deserialize(decoder);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T readWithSlowAvro(Schema readerSchema, Schema writerSchema, Decoder decoder) {
    org.apache.avro.io.DatumReader<T> datumReader = new SpecificDatumReader<>(writerSchema, readerSchema);
    try {
      return datumReader.read(null, decoder);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
