package org.basex.api.rest;

import static org.basex.api.rest.RESTText.*;
import static org.basex.data.DataText.*;
import static org.basex.query.func.Function.*;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.basex.core.Prop;
import org.basex.core.Text;
import org.basex.core.cmd.Get;
import org.basex.core.cmd.Set;
import org.basex.io.in.ArrayInput;
import org.basex.io.serial.SerializerProp;
import org.basex.server.Query;
import org.basex.server.Session;
import org.basex.util.TokenBuilder;
import org.basex.util.Util;

/**
 * REST-based evaluation of XQuery expressions.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
class RESTQuery extends RESTCode {
  /** External variables. */
  protected Map<String, String[]> variables;
  /** Query input. */
  protected final String input;
  /** Optional context item. */
  protected final byte[] item;

  /**
   * Constructor.
   * @param in query to be executed
   * @param vars external variables
   * @param it context item
   */
  RESTQuery(final String in, final Map<String, String[]> vars,
      final byte[] it) {
    input = in;
    variables = vars;
    item = it;
  }

  @Override
  void run(final RESTContext ctx) throws RESTException, IOException {
    query(input, ctx);
  }

  /**
   * Evaluates the specified query.
   * @param in query input
   * @param ctx REST context
   * @throws RESTException REST exception
   * @throws IOException I/O exception
   */
  protected void query(final String in, final RESTContext ctx)
      throws RESTException, IOException {

    if(item != null) {
      // create main memory instance of the context item
      final boolean mm = ctx.session.execute(
          new Get(Prop.MAINMEM)).split(Text.COLS)[1].equals(Text.TRUE);
      ctx.session.execute(new Set(Prop.MAINMEM, true));
      ctx.session.create(Util.name(RESTQuery.class), new ArrayInput(item));
      if(!mm) ctx.session.execute(new Set(Prop.MAINMEM, false));
    } else {
      // try to open addressed database
      open(ctx);
    }

    // set specified serialization options
    final Session session = ctx.session;
    session.execute(new Set(Prop.SERIALIZER, serial(ctx)));

    // check if path points to a raw file
    String query = in.isEmpty() ? "." : in;
    if(query.equals(".") && ctx.depth() > 1) {
      // retrieve binary contents if no query is specified
      final String raw = DBISRAW.args(ctx.db(), ctx.dbpath());
      if(session.query(raw).execute().equals(Text.TRUE))
        query = "declare option output:method '" + M_RAW + "';" +
            DBRETRIEVE.args(ctx.db(), ctx.dbpath());
    }

    // redirect output stream
    session.setOutputStream(ctx.out);

    // create query instance
    final Query qu = session.query(query);
    // bind external variables
    for(final Entry<String, String[]> e : variables.entrySet()) {
      final String[] val = e.getValue();
      if(val.length == 2) qu.bind(e.getKey(), val[0], val[1]);
      if(val.length == 1) qu.bind(e.getKey(), val[0]);
    }

    // initializes the output
    initOutput(new SerializerProp(qu.options()), ctx);

    // run query
    qu.execute();
  }

  /**
   * Returns the serialization options.
   * @param ctx REST context
   * @return serialization options
   */
  String serial(final RESTContext ctx) {
    final TokenBuilder ser = new TokenBuilder(ctx.serialization);
    if(ctx.wrapping) {
      if(ser.size() != 0) ser.add(',');
      ser.addExt(SerializerProp.S_WRAP_PREFIX[0]).add('=').add(REST).add(',');
      ser.addExt(SerializerProp.S_WRAP_URI[0]).add('=').add(RESTURI);
    }
    return ser.toString();
  }
}
