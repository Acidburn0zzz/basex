package org.basex;

import static org.basex.core.Text.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.basex.core.Session;
import org.basex.core.LocalSession;
import org.basex.core.Main;
import org.basex.core.Prop;
import org.basex.core.proc.IntStop;
import org.basex.server.ClientSession;
import org.basex.server.Semaphore;
import org.basex.server.ServerSession;
import org.basex.util.Args;

/**
 * This is the starter class for the database server.
 * It handles concurrent requests from multiple users.
 * Add the '-h' option to get a list on all available command-line
 * arguments.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Andreas Weiler
 * @author Christian Gruen
 */
public final class BaseXServer extends Main implements Runnable {
  /** Server socket. */
  ServerSocket server;
  /** Flag for server activity. */
  boolean running = true;
  /** Verbose mode. */
  boolean info;
  /** Semaphore for managing processes. */
  public final Semaphore sem;

  /**
   * Main method, launching the server process. Command-line arguments can be
   * listed with the <code>-h</code> argument.
   * @param args command-line arguments
   */
  public static void main(final String... args) {
    new BaseXServer(args);
  }

  /**
   * Constructor.
   * @param args command line arguments
   */
  public BaseXServer(final String... args) {
    super(args);
    sem = new Semaphore();
    if(!success) return;
    try {
      server = new ServerSocket(context.prop.num(Prop.SERVERPORT));
      new Thread(this).start();

      outln(CONSOLE, SERVERMODE, console ? CONSOLE2 : SERVERSTART);
      if(console) quit(console());
    } catch(final Exception ex) {
      error(ex, true);
    }
  }

  /**
   * Server thread.
   */
  public void run() {
    while(running) {
      try {
        final Socket s = server.accept();
        context.add(new ServerSession(s, this, info));
      } catch(final IOException ex) {
        // socket was closed..
        break;
      }
    }
  }
  
  @Override
  public synchronized void quit(final boolean user) {
    if(!running) return;
    running = false;
    super.quit(user);

    try {
      // close input streams
      if(console) System.in.close();
      server.close();
    } catch(final IOException ex) {
      error(ex, false);
    }

    console = false;
    context.close();
  }

  @Override
  protected Session session() {
    if(session == null) session = new LocalSession(context);
    return session;
  }

  @Override
  protected void parseArguments(final String[] args) {
    final Args arg = new Args(args);
    success = true;
    while(arg.more() && success) {
      if(arg.dash()) {
        final char c = arg.next();
        if(c == 'd') {
          // activate debug mode
          context.prop.set(Prop.DEBUG, true);
        } else if(c == 'i') {
          // activate interactive mode
          console = true;
        } else if(c == 'p') {
          // parse server port
          context.prop.set(Prop.SERVERPORT, arg.num());
        } else if(c == 'v') {
          // show process info
          info = true;
        } else {
          success = false;
        }
      } else {
        success = false;
        if(arg.string().equals("stop")) {
          try {
            new ClientSession("localhost", context.prop.num(Prop.PORT),
                ADMIN, ADMIN).execute(new IntStop(), null);
            outln(SERVERSTOPPED);
          } catch(final IOException ex) {
            error(ex, true);
          }
          return;
        }
      }
    }
    if(!success) outln(SERVERINFO);
  }
}
