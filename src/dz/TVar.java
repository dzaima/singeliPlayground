package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.utils.*;

public class TVar {
  public final Var v;
  public final Node n;
  public final SiType type;
  public final Vec<VarField> fs = new Vec<>();
  
  private final Node nextText;
  
  public TVar(Var v, SiType t0) {
    this.v = v;
    this.type = t0;
    n = v.n.ctx.make(v.r.gc.getProp("si.hlUI").gr());
    Node vl = n.ctx.id("list");
    int hc = (t0.repr.bits()? 64 : 256) / t0.elBits(); // horizontal count
    hc = Math.max(hc, 1);
    hc = Math.min(type.count(), hc);
    for (int i = 0; i < type.count()/hc; i++) {
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
      TyRepr r = type.repr;
      if (r != TyRepr.FLOAT) {
        switch (r) {
          case SIGNED:   r = TyRepr.UNSIGNED; break;
          case UNSIGNED: r = v.type.widthBits()==1? TyRepr.SIGNED : TyRepr.HEX; break;
          case HEX:      r = binOpt? TyRepr.BIN : TyRepr.SIGNED; break;
          case BIN:      r = TyRepr.MASK; break;
          case MASK:     r = TyRepr.SIGNED; break;
        }
        v.types.set(v.types.indexOf(this), new TVar(v, type.withRepr(r)));
        v.updList();
      }
    });
    nextText = n.ctx.id("tyTxt");
    updData();
  }
  
  public void updData() {
    nextText.clearCh();
    nextText.add(new StringNode(nextText.ctx, type.repr.fmt));
    long[] vs = v.read(type.elBits());
    for (int i = 0; i < vs.length; i++) {
      EditNode n = fs.get(i);
      if (n == v.r.focusNode()) continue;
      long c = vs[i];
      String nv;
      switch (type.repr) {
        case FLOAT:
          nv = type.elBits()==32? Float.toString(Float.intBitsToFloat((int) c)) : Double.toString(Double.longBitsToDouble(c));
          break;
        case SIGNED:
          int pad = 64-type.elBits();
          nv = Long.toString((c<<pad)>>pad);
          break;
        case UNSIGNED:
          nv = Long.toUnsignedString(c);
          break;
        case HEX: {
          String s = Long.toHexString(c).toUpperCase();
          nv = Tools.repeat('0', (type.elBits()+3)/4 - s.length()) + s;
          break;
        }
        case BIN: case MASK: {
          String s = Long.toBinaryString(c);
          s = Tools.repeat('0', type.elBits()-s.length()) + s;
          StringBuilder b = new StringBuilder();
          for (int j = 0; j < s.length(); j+= 8) {
            if (j!=0) b.append("_");
            b.append(s, j, Math.min(s.length(), j+8));
          }
          if (type.repr==TyRepr.BIN) nv = b.toString();
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
