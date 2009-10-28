package org.basex.query.expr;

import static org.basex.query.QueryTokens.*;

import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Item;
import org.basex.query.iter.Iter;
import org.basex.query.util.Var;
import org.basex.util.Token;

/**
 * Case expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class Case extends Single {
  /** Variable. */
  final Var var;

  /**
   * Constructor.
   * @param v variable
   * @param r return expression
   */
  public Case(final Var v, final Expr r) {
    super(r);
    var = v;
  }

  @Override
  public Case comp(final QueryContext ctx) throws QueryException {
    return comp(ctx, null);
  }

  /**
   * Compiles the expression.
   * @param ctx query context
   * @param it item to be bound
   * @return resulting item
   * @throws QueryException query exception
   */
  public Case comp(final QueryContext ctx, final Item it)
      throws QueryException {
    if(var.name == null) {
      super.comp(ctx);
    } else {
      final int s = ctx.vars.size();
      ctx.vars.add(it == null ? var : var.bind(it, ctx).copy());
      super.comp(ctx);
      ctx.vars.reset(s);
    }
    return this;
  }

  @Override
  public boolean uses(final Use u, final QueryContext ctx) {
    return u == Use.VAR || super.uses(u, ctx);
  }

  @Override
  public Case remove(final Var v) {
    if(!v.eq(var)) expr = expr.remove(v);
    return this;
  }

  /**
   * Evaluates the expression.
   * @param ctx query context
   * @param seq sequence to be checked
   * @return resulting item
   * @throws QueryException query exception
   */
  Iter iter(final QueryContext ctx, final Iter seq) throws QueryException {
    if(var.type != null && !var.type.instance(seq)) return null;
    if(var.name == null) return ctx.iter(expr);

    final int s = ctx.vars.size();
    ctx.vars.add(var.bind(seq.finish(), ctx).copy());
    final Item im = ctx.iter(expr).finish();
    ctx.vars.reset(s);
    return im.iter();
  }

  @Override
  public Return returned(final QueryContext ctx) {
    return expr.returned(ctx);
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this, VAR, var.name != null ? var.name.str() : Token.EMPTY);
    expr.plan(ser);
    ser.closeElement();
  }

  @Override
  public String toString() {
    return (var.type == null ? DEFAULT : CASE + ' ' + var) +
        ' ' + RETURN + ' ' + expr;
  }
}
