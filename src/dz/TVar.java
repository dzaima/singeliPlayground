package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.utils.*;

public class TVar {
  public final Var v;
  public final Node n;
  public final int elBits;
  public final int count;
  public TyRepr qual;
  public final Vec<VarField> fs = new Vec<>();
  
  private final Node nextText;
  
  public TVar(Var v, int elBits, TyRepr q) {
    this.v = v;
    this.elBits = elBits;
    this.qual = q;
    n = v.n.ctx.make(v.r.gc.getProp("si.hlUI").gr());
    Node vl = n.ctx.id("list");
    count = v.type.widthBits()/elBits;
    int hc = 32 / (elBits/8);
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
    boolean binOpt = v.scalar || v.type.elBits()==1;
    next.setFn(b -> {
      if (qual != TyRepr.FLOAT) {
        switch (qual) {
          case SIGNED:   qual = TyRepr.UNSIGNED; break;
          case UNSIGNED: qual = TyRepr.HEX; break;
          case HEX:      qual = binOpt? TyRepr.BIN : TyRepr.SIGNED; break;
          case BIN:      qual =                      TyRepr.SIGNED; break;
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
    nextText.add(new StringNode(nextText.ctx, qual.repr));
    long[] vs = v.read(elBits);
    for (int i = 0; i < vs.length; i++) {
      EditNode n = fs.get(i);
      if (n == v.r.focusNode()) continue;
      long c = vs[i];
      String nv;
      switch (qual) {
        case FLOAT:
          nv = elBits==32? Float.toString(Float.intBitsToFloat((int) c)) : Double.toString(Double.longBitsToDouble(c));
          break;
        case SIGNED:
          int pad = 64-elBits;
          nv = Long.toString((c<<pad)>>pad);
          break;
        case UNSIGNED:
          nv = Long.toUnsignedString(c);
          break;
        case HEX: {
          String s = Long.toHexString(c).toUpperCase();
          nv = Tools.repeat('0', elBits/4 - s.length()) + s;
          break;
        }
        case BIN: {
          String s = Long.toBinaryString(c);
          s = Tools.repeat('0', elBits-s.length()) + s;
          StringBuilder b = new StringBuilder();
          for (int j = 0; j < s.length(); j+= 8) {
            if (j!=0) b.append("_");
            b.append(s.substring(j, Math.min(s.length(), j+8)));
          }
          if (v.scalar) nv = b.toString();
          else nv = "m"+b.reverse();
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
