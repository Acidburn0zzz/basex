package org.basex.query.expr;

import static org.basex.query.QueryText.*;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Empty;
import org.basex.query.item.Item;
import org.basex.query.item.Nod;
import org.basex.query.item.Type;
import org.basex.query.iter.Iter;
import org.basex.query.iter.NodIter;
import org.basex.query.iter.NodeIter;
import org.basex.query.util.Err;
import org.basex.util.Array;
import org.basex.util.InputInfo;

/**
 * Union expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class Union extends Set {
  /**
   * Constructor.
   * @param ii input info
   * @param e expression list
   */
  public Union(final InputInfo ii, final Expr... e) {
    super(ii, e);
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    super.comp(ctx);

    for(int e = 0; e != expr.length; ++e) {
      // remove empty operands
      if(expr[e].empty()) {
        expr = Array.delete(expr, e--);
        ctx.compInfo(OPTREMOVE, desc(), Type.EMP);
      }
    }
    
    // as union is required to always returns sorted results,
    // a single, non-sorted argument must be evaluated as well
    return expr.length == 0 ? Empty.SEQ :
      expr.length == 1 && !dupl ? expr[0] : this;
  }

  @Override
  protected NodeIter eval(final Iter[] iter) throws QueryException {
    final NodIter ni = new NodIter().random();
    for(final Iter ir : iter) {
      Item it;
      while((it = ir.next()) != null) {
        if(!it.node()) Err.type(this, Type.NOD, it);
        ni.add((Nod) it);
      }
    }
    return ni.sort();
  }

  @Override
  protected NodeIter iter(final Iter[] iter) {
    return new NodeIter() {
      Nod[] items;

      @Override
      public Nod next() throws QueryException {
        if(items == null) {
          items = new Nod[iter.length];
          for(int i = 0; i != iter.length; ++i) next(i);
        }

        int m = -1;
        for(int i = 0; i != items.length; ++i) {
          if(items[i] == null) continue;
          final int d = m == -1 ? 1 : items[m].diff(items[i]);
          if(d == 0) {
            next(i--);
          } else if(d > 0) {
            m = i;
          }
        }
        if(m == -1) return null;

        final Nod it = items[m];
        next(m);
        return it;
      }

      private void next(final int i) throws QueryException {
        final Item it = iter[i].next();
        if(it != null && !it.node()) Err.type(Union.this, Type.NOD, it);
        items[i] = (Nod) it;
      }
    };
  }

  @Override
  public String toString() {
    return "(" + toString(" | ") + ")";
  }
}
