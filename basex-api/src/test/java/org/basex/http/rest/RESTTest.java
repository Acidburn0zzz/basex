package org.basex.http.rest;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;

import org.basex.http.*;
import org.basex.io.*;
import org.junit.*;

/**
 * This class tests the embedded REST API.
 *
 * @author BaseX Team 2005-15, BSD License
 * @author Christian Gruen
 */
public abstract class RESTTest extends HTTPTest {
  /** REST URI. */
  static final String URI = RESTText.REST_URI;
  /** Input file. */
  static final String FILE = "src/test/resources/input.xml";

  // INITIALIZERS =============================================================

  /**
   * Start server.
   * @throws Exception exception
   */
  @BeforeClass
  public static void start() throws Exception {
    init(REST_ROOT, true);
  }

  /**
   * Compares two byte arrays for equality.
   * @param string full string
   * @param prefix prefix
   */
  protected static void assertStartsWith(final String string, final String prefix) {
    assertTrue('\'' + string + "' does not start with '" + prefix + '\'',
        string.startsWith(prefix));
  }

  /**
   * Checks if a string is contained in another string.
   * @param str string
   * @param sub sub string
   */
  protected static void assertContains(final String str, final String sub) {
    if(!str.contains(sub)) fail('\'' + sub + "' not contained in '" + str + "'.");
  }

  /**
   * Executes the specified GET request and returns the content type.
   * @param query request
   * @return string result, or {@code null} for a failure.
   * @throws IOException I/O exception
   */
  protected static String contentType(final String query) throws IOException {
    final IOUrl url = new IOUrl(REST_ROOT + query);
    final HttpURLConnection conn = (HttpURLConnection) url.connection();
    try {
      read(conn.getInputStream());
      return conn.getContentType();
    } catch(final IOException ex) {
      throw error(conn, ex);
    } finally {
      conn.disconnect();
    }
  }
}
