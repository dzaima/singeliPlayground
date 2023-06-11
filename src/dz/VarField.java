package dz;

import dzaima.ui.gui.io.*;
import dzaima.ui.gui.undo.UndoFrame;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.editable.TextFieldNode;
import dzaima.utils.Vec;

public class VarField extends TextFieldNode {
  public TVar tvar;
  
  public VarField(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  UndoFrame pFrame;
  boolean pFocused;
  public void tickC() {
    super.tickC();
    if (um.us.peek()!=pFrame || isFocused!=pFocused) {
      pFrame = um.us.peek();
      enter(Key.M_CTRL);
      pFocused = isFocused;
    }
  }
  
  public boolean enter(int mod) {
    long[] vs = new long[tvar.count];
    for (int j = 0; j < tvar.count; j++) {
      String s = tvar.fs.get(j).getAll();
      long c;
      try {
        VTy parseType = tvar.type;
        if (s.startsWith("0x")) { s=s.substring(2); parseType=VTy.HEX; }
        if (s.startsWith("0b")) { s=s.substring(2); parseType=VTy.BIN; }
        switch (parseType) {
          case BIN: c = Long.parseUnsignedLong(s, 2); break;
          case HEX: c = Long.parseUnsignedLong(s, 16); break;
          case UNSIGNED: c = Long.parseUnsignedLong(s); break;
          case SIGNED: c = Long.parseLong(s); break;
          case FLOAT:
            c = tvar.width==32? Float.floatToIntBits(Float.parseFloat(s))&0xffffffffL
              : Double.doubleToRawLongBits(Double.parseDouble(s));
            break;
          default: c = -1;
        }
      } catch (NumberFormatException e) { c = 0; }
      vs[j] = c;
    }
    tvar.v.store(vs);
    return true;
  }
  
  public boolean keyF2(Key key, int scancode, KeyAction a) {
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
      return true;
    }
    return super.keyF2(key, scancode, a);
  }
  
  public void typed(int codepoint) {
    if (codepoint==' ') return;
    super.typed(codepoint);
  }
}
