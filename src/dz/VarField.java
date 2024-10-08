package dz;

import dzaima.ui.gui.io.*;
import dzaima.ui.gui.undo.UndoFrame;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.editable.code.CodeFieldNode;
import dzaima.utils.Vec;

public class VarField extends CodeFieldNode {
  public TVar tvar;
  
  public VarField(Ctx ctx, Props props) {
    super(ctx, props);
    setLang(gc.langs().fromName("number"));
  }
  
  UndoFrame pFrame;
  boolean pFocused;
  public void tickC() {
    super.tickC();
    if (um.us.peek()!=pFrame || isFocused!=pFocused) {
      pFrame = um.us.peek();
      action(EditAction.CUSTOM1, 0);
      pFocused = isFocused;
    }
  }
  
  public boolean action(EditAction a, int mod) {
    long[] vs = new long[tvar.type.count()];
    for (int j = 0; j < tvar.type.count(); j++) {
      String s = tvar.fs.get(j).getAll().replace("_","");
      long c;
      try {
        TyRepr parseType = tvar.type.repr;
        if (s.startsWith("0x")) { s=s.substring(2); parseType=TyRepr.HEX; }
        if (s.startsWith("0b")) { s=s.substring(2); parseType=TyRepr.BIN; }
        if (s.startsWith("m"))  { s=s.substring(1); parseType=TyRepr.BIN; s = new StringBuilder(s).reverse().toString(); }
        switch (parseType) {
          case BIN: c = Long.parseUnsignedLong(s, 2); break;
          case HEX: c = Long.parseUnsignedLong(s, 16); break;
          case UNSIGNED: c = Long.parseUnsignedLong(s); break;
          case SIGNED: c = Long.parseLong(s); break;
          case FLOAT:
            c = tvar.type.elBits()==32? Float.floatToIntBits(Float.parseFloat(s))&0xffffffffL
              : Double.doubleToRawLongBits(Double.parseDouble(s));
            break;
          default: c = -1;
        }
      } catch (NumberFormatException e) { c = 0; }
      vs[j] = c;
    }
    tvar.v.store(tvar.type, vs);
    return true;
  }
  
  public int action(Key key, KeyAction a) {
    if ((key.k_tab() || key.k_space()) && a.press) {
      Vec<VarField> v = tvar.fs;
      int i = v.indexOf(this);
      i+= key.hasShift()? -1 : 1;
      if (i>=0 && i<v.sz) {
        um.pushU("clear selection");
        collapseCursors(true);
        cs.get(0).mv(0, 0);
        um.pop();
        
        VarField n = v.get(i);
        ctx.win().focus(n);
        n.um.pushU("select all");
        n.collapseCursors(true);
        n.cs.get(0).mv(0, 0, n.lns.get(0).sz(), 0);
        n.um.pop();
      }
      return 1;
    }
    return super.action(key, a);
  }
  
  public void typed(int codepoint) {
    if (codepoint==' ') return;
    super.typed(codepoint);
  }  
}
