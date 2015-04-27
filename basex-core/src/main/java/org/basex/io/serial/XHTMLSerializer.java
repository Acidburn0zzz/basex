package org.basex.io.serial;

import static org.basex.data.DataText.*;
import static org.basex.util.Token.*;

import java.io.*;

import org.basex.io.out.*;
import org.basex.query.value.item.*;

/**
 * This class serializes items as XHTML.
 *
 * @author BaseX Team 2005-15, BSD License
 * @author Christian Gruen
 */
final class XHTMLSerializer extends MarkupSerializer {
  /**
   * Constructor, specifying serialization options.
   * @param po print output
   * @param sopts serialization parameters
   * @throws IOException I/O exception
   */
  XHTMLSerializer(final PrintOutput po, final SerializerOptions sopts) throws IOException {
    super(po, sopts, V10, V11);
  }

  @Override
  protected void attribute(final byte[] name, final byte[] value, final boolean standalone)
      throws IOException {

    // escape URI attributes
    final byte[] nm = concat(lc(elem.local()), COLON, lc(name));
    final byte[] val = escuri && HTMLSerializer.URIS.contains(nm) ? escape(value) : value;
    super.attribute(name, val, standalone);
  }

  @Override
  protected void startOpen(final QNm value) throws IOException {
    super.startOpen(value);
    if(content && eq(lc(elem.local()), HEAD)) ct++;
  }

  @Override
  protected void finishOpen() throws IOException {
    super.finishOpen();
    ct(false, false);
  }

  @Override
  protected void finishEmpty() throws IOException {
    if(ct(true, false)) return;
    if((html5 ? HTMLSerializer.EMPTIES5 : HTMLSerializer.EMPTIES).contains(lc(elem.local()))) {
      out.print(' ');
      out.print(ELEM_SC);
    } else {
      out.print(ELEM_C);
      sep = false;
      finishClose();
    }
  }

  @Override
  protected boolean doctype(final QNm type) throws IOException {
    if(level != 0) return false;
    if(!super.doctype(type) && html5) {
      if(sep) indent();
      out.print(DOCTYPE);
      if(type == null) out.print(HTML);
      else out.print(type.local());
      out.print(ELEM_C);
      newline();
    }
    return true;
  }
}
