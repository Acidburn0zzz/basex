package org.basex.query.expr;

import static org.basex.query.QueryTokens.*;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.DBNode;
import org.basex.query.item.Empty;
import org.basex.query.item.Item;
import org.basex.query.item.Nod;
import org.basex.query.item.Type;
import org.basex.query.iter.Iter;
import org.basex.query.iter.NodIter;
import org.basex.query.iter.NodeIter;
import org.basex.query.util.Err;
import org.basex.util.InputInfo;

/**
 * Intersect expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class InterSect extends Set {
  /**
   * Constructor.
   * @param ii input info
   * @param l expression list
   */
  public InterSect(final InputInfo ii, final Expr[] l) {
    super(ii, l);
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    super.comp(ctx);
    return oneEmpty() ? optPre(Empty.SEQ, ctx) : this;
  }

  @Override
  protected NodIter eval(final Iter[] iter) throws QueryException {
    NodIter ni = new NodIter();

    Item it;
    while((it = iter[0].next()) != null) {
      if(!it.node()) Err.type(this, Type.NOD, it);
      ni.add((Nod) it);
    }
    final boolean db = ni.dbnodes();

    for(int e = 1; e != expr.length && ni.size() != 0; ++e) {
      final NodIter res = new NodIter().random();
      final Iter ir = iter[e];
      while((it = ir.next()) != null) {
        if(!it.node()) Err.type(this, Type.NOD, it);
        final Nod node = (Nod) it;

        if(db && node instanceof DBNode) {
          // optimization: use binary search for database nodes
          if(ni.contains((DBNode) node)) res.add(node);
        } else {
          for(int n = 0; n < ni.size(); ++n) {
            if(ni.get(n).is(node)) {
              res.add(node);
              break;
            }
          }
        }
      }
      ni = res;
    }
    return ni.sort();
  }

  @Override
  protected NodeIter iter(final Iter[] iter) {
    return new NodeIter() {
      final Nod[] items = new Nod[iter.length];

      @Override
      public Nod next() throws QueryException {
        for(int i = 0; i != iter.length; ++i) if(!next(i)) return null;

        for(int i = 1; i != items.length; ++i) {
          final int d = items[0].diff(items[i]);
          if(d < 0) {
            if(!next(0)) return null;
            i = 0;
          } else if(d > 0) {
            if(!next(i--)) return null;
          }
        }
        return items[0];
      }

      private boolean next(final int i) throws QueryException {
        final Item it = iter[i].next();
        if(it == null) return false;
        if(!it.node()) Err.type(InterSect.this, Type.NOD, it);
        items[i] = (Nod) it;
        return true;
      }
    };
  }

  @Override
  public String toString() {
    return "(" + toString(" " + INTERSECT + " ") + ")";
  }
}
