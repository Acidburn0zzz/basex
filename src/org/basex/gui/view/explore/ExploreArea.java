package org.basex.gui.view.explore;

import static org.basex.Text.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import org.basex.BaseX;
import org.basex.core.proc.Find;
import org.basex.data.Data;
import org.basex.data.DataText;
import org.basex.data.StatsKey;
import org.basex.gui.GUIConstants;
import org.basex.gui.GUIProp;
import org.basex.gui.GUIConstants.Fill;
import org.basex.gui.layout.BaseXBack;
import org.basex.gui.layout.BaseXCombo;
import org.basex.gui.layout.BaseXDSlider;
import org.basex.gui.layout.BaseXLabel;
import org.basex.gui.layout.BaseXLayout;
import org.basex.gui.layout.BaseXPanel;
import org.basex.gui.layout.BaseXTextField;
import org.basex.gui.layout.TableLayout;
import org.basex.index.Names;
import org.basex.util.StringList;
import org.basex.util.Token;
import org.basex.util.TokenBuilder;
import org.basex.util.TokenList;

/**
 * This is a simple user search panel.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
final class ExploreArea extends BaseXPanel implements ActionListener {
  /** Component width. */
  private static final int COMPW = 150;
  /** Exact search pattern. */
  private static final String PATEX = "[% = \"%\"]";
  /** Substring search pattern. */
  private static final String PATSUB = "[% ftcontains \"%\"]";
  /** Numeric search pattern. */
  private static final String PATNUM = "[% >= % and % <= %]";
  /** Simple search pattern. */
  private static final String PATSIMPLE = "[%]";

  /** Main panel. */
  final ExploreView main;

  /** Main panel. */
  final BaseXBack panel;
  /** Query field. */
  final BaseXTextField all;
  /** Last Query. */
  String last = "";

  /**
   * Default constructor.
   * @param m main panel
   */
  ExploreArea(final ExploreView m) {
    super(null, m.gui);
    main = m;

    setLayout(new BorderLayout(0, 5));
    setMode(Fill.NONE);

    all = new BaseXTextField(null, gui);
    all.addKeyListener(main);
    all.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        query(false);
      }
    });
    add(all, BorderLayout.NORTH);

    panel = new BaseXBack(GUIConstants.Fill.NONE);
    panel.setLayout(new TableLayout(20, 2, 10, 5));
    add(panel, BorderLayout.CENTER);

    final BaseXBack p = new BaseXBack(GUIConstants.Fill.NONE);
    p.setLayout(new BorderLayout());
  }

  /**
   * Initializes the panel.
   */
  void init() {
    panel.removeAll();
    panel.revalidate();
    panel.repaint();
  }

  @Override
  public void paintComponent(final Graphics g) {
    super.paintComponent(g);
    final Data data = gui.context.data();
    if(!main.visible() || data == null) return;

    final boolean pi = data.meta.pathindex;
    if(panel.getComponentCount() != 0) {
      if(!pi) init();
      return;
    }
    if(!pi) return;

    all.help(data.fs != null ? HELPSEARCHFS : HELPSEARCHXML);
    addKeys(gui.context.data());
    panel.revalidate();
    panel.repaint();
  }

  /**
   * Adds a text field.
   * @param pos position
   */
  private void addInput(final int pos) {
    final BaseXTextField txt = new BaseXTextField(HELPCATINPUT, gui);
    BaseXLayout.setWidth(txt, COMPW);
    BaseXLayout.setHeight(txt, txt.getFont().getSize() + 11);
    txt.setMargin(new Insets(0, 0, 0, 10));
    txt.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        query(false);
      }
    });
    txt.addKeyListener(main);
    panel.add(txt, pos);
  }

  /**
   * Adds a category combobox.
   * @param data data reference
   */
  private void addKeys(final Data data) {
    final TokenList sl = new TokenList();
    final int cs = panel.getComponentCount();
    for(int c = 0; c < cs; c += 2) {
      final BaseXCombo combo = (BaseXCombo) panel.getComponent(c);
      if(combo.getSelectedIndex() == 0) continue;
      final String elem = combo.getSelectedItem().toString();
      if(!elem.startsWith("@")) sl.add(Token.token(elem));
    }

    final TokenList tmp = data.path.desc(sl, data, true, false);
    if(tmp.size() == 0) return;

    final String[] keys = entries(tmp.finish());
    final BaseXCombo cm = new BaseXCombo(keys, HELPSEARCHCAT, gui);
    cm.addActionListener(this);
    cm.addKeyListener(main);
    panel.add(cm);
    panel.add(new BaseXLabel(""));
  }

  /**
   * Adds a combobox.
   * @param values combobox values
   * @param pos position
   */
  private void addCombo(final String[] values, final int pos) {
    final BaseXCombo cm = new BaseXCombo(values, HELPCAT, gui);
    BaseXLayout.setWidth(cm, COMPW);
    cm.addActionListener(this);
    cm.addKeyListener(main);
    panel.add(cm, pos);
  }

  /**
   * Adds a combobox.
   * @param min minimum value
   * @param max maximum value
   * @param pos position
   * @param kb kilobyte flag
   * @param itr integer flag
   * @param date date flag
   */
  private void addSlider(final double min, final double max, final int pos,
      final boolean kb, final boolean date, final boolean itr) {
    final BaseXDSlider sl = new BaseXDSlider(gui, min, max, HELPDS, this);
    BaseXLayout.setWidth(sl, COMPW + BaseXDSlider.LABELW);
    sl.kb = kb;
    sl.date = date;
    sl.itr = itr;
    sl.addKeyListener(main);
    panel.add(sl, pos);
  }

  public void actionPerformed(final ActionEvent e) {
    if(e != null) {
      final Object source = e.getSource();

      // find modified component
      int cp = 0;
      final int cs = panel.getComponentCount();
      for(int c = 0; c < cs; c++) if(panel.getComponent(c) == source) cp = c;

      if((cp & 1) == 0) {
        // combo box with tags/attributes
        final BaseXCombo combo = (BaseXCombo) source;
        panel.remove(cp + 1);

        final Data data = gui.context.data();
        final boolean selected = combo.getSelectedIndex() != 0;
        if(selected) {
          final String item = combo.getSelectedItem().toString();
          final boolean att = item.startsWith("@");
          final Names names = att ? data.atts : data.tags;
          final byte[] key = Token.token(att ? item.substring(1) : item);
          final StatsKey stat = names.stat(names.id(key));
          switch(stat.kind) {
            case INT:
              addSlider(stat.min, stat.max, cp + 1,
                  item.equals("@" + DataText.S_SIZE),
                  item.equals("@" + DataText.S_MTIME), true);
              break;
            case DBL:
              addSlider(stat.min, stat.max, cp + 1, false, false, false);
              break;
            case CAT:
              addCombo(entries(stat.cats.keys()), cp + 1);
              break;
            case TEXT:
              addInput(cp + 1);
              break;
            case NONE:
              panel.add(new BaseXLabel(""), cp + 1);
              break;
          }
        } else {
          panel.add(new BaseXLabel(""), cp + 1);
        }
        while(cp + 2 < panel.getComponentCount()) {
          panel.remove(cp + 2);
          panel.remove(cp + 2);
        }
        if(selected) addKeys(data);
        panel.revalidate();
        panel.repaint();
      }
    }
    query(false);
  }

  /**
   * Runs a query.
   * @param force force the execution of a new query.
   */
  void query(final boolean force) {
    if(!force && !gui.prop.is(GUIProp.EXECRT)) return;

    final TokenBuilder tb = new TokenBuilder();
    final Data data = gui.context.data();

    final int cs = panel.getComponentCount();
    for(int c = 0; c < cs; c += 2) {
      final BaseXCombo com = (BaseXCombo) panel.getComponent(c);
      final int k = com.getSelectedIndex();
      if(k <= 0) continue;
      String key = com.getSelectedItem().toString();
      final boolean attr = key.startsWith("@");

      final Component comp = panel.getComponent(c + 1);
      String pattern = "";
      String val1 = null;
      String val2 = null;
      if(comp instanceof BaseXTextField) {
        val1 = ((BaseXTextField) comp).getText();
        if(val1.length() != 0) {
          if(val1.startsWith("\"")) {
            val1 = val1.replaceAll("\"", "");
            pattern = PATEX;
          } else {
            pattern = attr && data.meta.atvindex ||
              !attr && data.meta.txtindex ? PATEX : PATSUB;
          }
        }
      } else if(comp instanceof BaseXCombo) {
        final BaseXCombo combo = (BaseXCombo) comp;
        if(combo.getSelectedIndex() != 0) {
          val1 = combo.getSelectedItem().toString();
          pattern = PATEX;
        }
      } else if(comp instanceof BaseXDSlider) {
        final BaseXDSlider slider = (BaseXDSlider) comp;
        if(slider.min != slider.totMin || slider.max != slider.totMax) {
          final double m = slider.min;
          final double n = slider.max;
          val1 = (long) m == m ? Long.toString((long) m) : Double.toString(m);
          val2 = (long) n == n ? Long.toString((long) n) : Double.toString(n);
          pattern = PATNUM;
        }
      }

      if(attr) {
        if(tb.size() == 0) tb.add("//*");
        if(pattern.length() == 0) pattern = PATSIMPLE;
      } else {
        tb.add("//" + key);
        key = "text()";
      }
      tb.add(pattern, key, val1, key, val2);
    }

    String qu = tb.toString();
    final boolean root = gui.context.root();
    final boolean rt = gui.prop.is(GUIProp.FILTERRT);
    if(qu.length() != 0 && !rt && !root) qu = "." + qu;

    String simple = all.getText().trim();
    if(simple.length() != 0) {
      simple = Find.find(simple, gui.context, rt);
      qu = qu.length() != 0 ? simple + " | " + qu : simple;
    }

    if(qu.length() == 0) qu = rt || root ? "/" : ".";

    if(!force && last.equals(qu)) return;
    last = qu;
    gui.xquery(qu, false);
  }

  /**
   * Returns the combo box selections
   * and the keys of the specified set.
   * @param key keys
   * @return key array
   */
  private String[] entries(final byte[][] key) {
    final StringList sl = new StringList();
    sl.add(BaseX.info(INFOENTRIES, key.length));
    for(final byte[] k : key) sl.add(Token.string(k));
    sl.sort(true);
    return sl.finish();
  }
}
