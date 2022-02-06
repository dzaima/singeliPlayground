package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.editable.*;
import dzaima.utils.*;

public class TVar {
  public final Var v;
  public final Node n;
  public final int width;
  public final int count;
  public VTy type;
  public final Vec<VarField> fs = new Vec<>();
  
  private final Node nextText;
  
  public TVar(Var v, int width, VTy t0) {
    this.v = v;
    this.width = width;
    this.type = t0;
    n = v.n.ctx.make(v.r.gc.getProp("si.hlUI").gr());
    Node l = n.ctx.id("list");
    count = v.data.length*8/width;
    for (int i = 0; i < count; i++) {
      Node fn = l.ctx.make(l.gc.getProp("si.numUI").gr());
      VarField n = (VarField) fn.ctx.id("field");
      n.tvar = this;
      fs.add(n);
      l.add(fn);
    }
    BtnNode next = (BtnNode) n.ctx.id("ty");
    next.setFn(b -> {
      if (type !=VTy.FLOAT) {
        type = type==VTy.SIGNED? VTy.UNSIGNED : type==VTy.UNSIGNED? VTy.HEX : VTy.SIGNED;
        v.updTitle();
        updData();
      }
    });
    nextText = n.ctx.id("tyTxt");
    updData();
  }
  
  public void updData() {
    nextText.clearCh();
    nextText.add(new StringNode(nextText.ctx, type==VTy.FLOAT?"f":type==VTy.HEX?"x":type==VTy.SIGNED?"s":"u"));
    long[] vs = v.read(width);
    for (int i = 0; i < vs.length; i++) {
      EditNode n = fs.get(i);
      if (n == v.r.focusNode) continue;
      long c = vs[i];
      String nv;
      switch (type) {
        case FLOAT:
          nv = width==32? Float.toString(Float.intBitsToFloat((int) c)) : Double.toString(Double.longBitsToDouble(c));
          break;
        case SIGNED:
          int pad = 64-width;
          nv = Long.toString((c<<pad)>>pad);
          break;
        case UNSIGNED:
          nv = Long.toUnsignedString(c);
          break;
        case HEX:
          String s = Long.toUnsignedString(c, 16).toUpperCase();
          nv = Tools.repeat('0', width/4 - s.length()) + s;
          break;
        default:
          nv = "0";
          break;
      }
      if (!nv.equals(n.getAll())) {
        n.removeAll();
        n.append(nv);
        n.um.clear();
      }
    }
  }
}
