package org.basex.examples.jaxrx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.basex.api.jaxrx.JaxRxServer;

/**
 * This class is a simple Java client to demonstrate the JAX-RX implementation.
 * It shows the add function of the HTTP POST method.
 *
 * @author BaseX Team 2005-11, BSD License
 */
public final class JaxRxPOSTAdd {
  /**
   * Runs the example.
   * @throws IOException I/O exception
   */
  static void run() throws IOException {
    System.out.println("=== POST: add a document to a database ===");

    // The java URL connection to the resource
    URL url = new URL("http://localhost:8984/basex/jax-rx/factbook/input.xml");
    System.out.println("\n* URL: " + url);

    // File to be sent to the server
    String doc = "etc/xml/input.xml";
    System.out.println("\n* Document: " + doc);

    // Establish the connection to the URL
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    // Set an output connection
    conn.setDoOutput(true);
    // Set as PUT request
    conn.setRequestMethod("POST");
    // Specify content type
    conn.setRequestProperty("Content-Type", "text/xml");

    // Get and cache output stream
    OutputStream out = new BufferedOutputStream(conn.getOutputStream());
    // Create and cache file input stream
    InputStream in = new BufferedInputStream(new FileInputStream(doc));

    // Send document to server
    System.out.println("\n* Send document...");
    for(int i; (i = in.read()) != -1;) out.write(i);
    in.close();
    out.close();

    // Print the HTTP response code
    System.out.println("\n* HTTP response: " + conn.getResponseCode() +
        " (" + conn.getResponseMessage() + ")");

    // Close connection
    conn.disconnect();
  }

  /**
   * This method demonstrates the POST method. In this example, a new XML
   * document is added to a database.
   * @param args (ignored) command-line arguments
   * @throws Exception exception
   */
  public static void main(final String... args) throws Exception {
    // Start servers
    JaxRxServer jaxrx = new JaxRxServer();
    // Run example
    run();
    // Stop servers
    jaxrx.stop();
  }
}
