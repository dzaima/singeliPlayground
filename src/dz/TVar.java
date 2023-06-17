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
    Node vl = n.ctx.id("list");
    count = v.data.length*8/width;
    int hc = 32 / (width/8);
    hc = Math.min(count, hc);
    for (int i = 0; i < count/hc; i++) {
      Node hl = v.n.ctx.make(v.r.gc.getProp("si.hlPart").gr());
      for (int j = 0; j < hc; j++) {
        Node fn = vl.ctx.make(vl.gc.getProp("si.numUI").gr());
        VarField n = (VarField) fn.ctx.id("field");
        n.tvar = this;
        fs.add(n);
        hl.add(fn);
      }
      vl.add(hl);
    }
    BtnNode next = (BtnNode) n.ctx.id("ty");
    boolean binOpt = v.scalar || v.typeWidth==1;
    next.setFn(b -> {
      if (type!=VTy.FLOAT) {
        switch (type) {
          case SIGNED:   type = VTy.UNSIGNED; break;
          case UNSIGNED: type = VTy.HEX; break;
          case HEX:      type = binOpt? VTy.BIN : VTy.SIGNED; break;
          case BIN:      type =                   VTy.SIGNED; break;
        }
        v.updTitle();
        updData();
      }
    });
    nextText = n.ctx.id("tyTxt");
    updData();
  }
  
  public void updData() {
    nextText.clearCh();
    nextText.add(new StringNode(nextText.ctx, type.btnName));
    long[] vs = v.read(width);
    for (int i = 0; i < vs.length; i++) {
      EditNode n = fs.get(i);
      if (n == v.r.focusNode()) continue;
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
        case HEX: {
          String s = Long.toHexString(c).toUpperCase();
          nv = Tools.repeat('0', width/4 - s.length()) + s;
          break;
        }
        case BIN: {
          String s = Long.toBinaryString(c);
          s = Tools.repeat('0', width-s.length()) + s;
          StringBuilder b = new StringBuilder();
          for (int j = 0; j < s.length(); j+= 8) {
            if (j!=0) b.append("_");
            b.append(s.substring(j, Math.min(s.length(), j+8)));
          }
          if (v.scalar) nv = b.toString();
          else nv = "m"+b.reverse().toString();
          break;
        }
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
