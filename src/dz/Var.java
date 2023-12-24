package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.*;
import dzaima.utils.Vec;

import java.util.*;

public class Var {
  public final SiPlayground r;
  public final boolean scalar;
  public final String name;
  public final SiType type;
  public final byte[] data;
  
  public final Node n;
  public final Vec<TVar> types = new Vec<>();
  private final Vec<BtnNode> btns;
  private final Node nameNode;
  private static final String[] BTN_KS = new String[]{"bg", "borderL"};
  private static final Prop[] BTN_VS = new Prop[]{new ColProp(0), new ColProp(0)};
  
  public Var(SiPlayground r, String name, byte[] data, SiType type) {
    this.r = r;
    this.name = name;
    this.data = data;
    this.type = type;
    this.scalar = type.scalar();
    
    
    n = r.base.ctx.make(r.gc.getProp("si.vlUI").gr());
    nameNode = n.ctx.id("name");
    
    types.add(new TVar(this,
      type.isBitVec()? type.reinterpretScale1(TyRepr.MASK, Math.min(64, type.widthBits()))
      :                type.reinterpretScale1(type.repr, type.elBits())
    ));
    
    btns = new Vec<>();
    Node btnList = n.ctx.id("btns");
    for (String btnName : new String[]{"8","16","32","64","f32","f64","X"}) {
      if (btnName.startsWith("f") && type.isBitVec()) continue;
      if (!btnName.equals("X")) {
        int cw = Integer.parseInt(btnName.startsWith("f")? btnName.substring(1) : btnName);
        if (cw > type.widthBits()) continue;
      }
      BtnNode b = new BtnNode(btnList.ctx, BTN_KS, BTN_VS);
      btnList.add(b);
      b.add(new StringNode(b.ctx, btnName));
      btns.add(b);
      b.setFn(n -> {
        int aw = -1;
        TyRepr at = TyRepr.HEX;
        switch (btnName) {
          case "8": aw=8; break;
          case "16": aw=16; break;
          case "32": aw=32; break;
          case "64": aw=64; break;
          case "f32": aw=32; at=TyRepr.FLOAT; break;
          case "f64": aw=64; at=TyRepr.FLOAT; break;
          case "X": r.vars.remove(this); r.updVars(); break;
        }
        if (aw!=-1) {
          for (TVar ct : types) {
            if (ct.type.elBits()==aw && ct.type.repr.f()==at.f()) {
              types.remove(ct);
              updList();
              return;
            }
          }
          types.add(new TVar(this, type.reinterpretScale1(at, aw)));
          updList();
        }
      });
    }
    
    updList();
  }
  
  public void updTitle() {
    nameNode.clearCh();
    nameNode.add(new StringNode(n.ctx, name+" : "+type()));
  }
  public void updList() {
    updTitle();
    n.remove(1, n.ch.sz);
    HashSet<String> tyNames = new HashSet<>();
    for (TVar c : types) {
      n.add(c.n);
      tyNames.add(c.type.repr.f()? "f"+c.type.elBits() : ""+c.type.elBits());
    }
    Prop bgOff = n.gc.getProp("coloredBtn.bgDef");
    Prop bgOn = n.gc.getProp("coloredBtn.bgGreen");
    Prop borderOff = n.gc.getProp("coloredBtn.borderLDef");
    Prop borderOn = n.gc.getProp("coloredBtn.borderLGreen");
    for (Node c : btns) {
      boolean has = tyNames.contains(((StringNode) c.ch.get(0)).s);
      c.set(c.id("bg"), has? bgOn : bgOff);
      c.set(c.id("borderL"), has? borderOn : borderOff);
    }
  }
  public void updData() {
    for (TVar c : types) c.updData();
  }
  
  public long[] read(int elBits) { // read as elBits-sized unsigned integers 
    int by = Math.max(elBits/8, 1);
    int bm = elBits>=8? 0xff : (1<<elBits)-1;
    long[] res = new long[type.widthBits()/elBits];
    for (int i = 0; i < res.length; i++) {
      long c = 0;
      for (int j = 0; j < by; j++) c|= ((long) (data[i*by+j]&bm)) << (8*j);
      res[i] = c;
    }
    return res;
  }
  
  public void store(SiType type, long[] vs) {
    int bits = type.elBits();
    int bytes = (bits+7) / 8;
    assert bytes*8 == bits || vs.length==1;
    for (int i = 0; i < vs.length; i++) {
      long c = vs[i];
      for (int j = 0; j < bytes; j++) {
        data[i*bytes + j] = (byte) (c&0xff);
        c>>= 8;
      }
    }
    updData();
  }
  
  public String type() {
    String elt = type.repr.qual+type.elBits();
    if (scalar) return elt;
    String sc = type.scale>1? type.scale+"×" : "";
    return sc+"["+type.unscaledCount()+"]"+elt;
  }
  
  public Var copy() {
    return new Var(r, name, Arrays.copyOf(data, data.length), type);
  }
  
  public Var updatedBy(Var v) {
    if (v.type.equals(type)) {
      System.arraycopy(v.data, 0, data, 0, v.data.length);
      updData();
      return this;
    }
    return v;
  }
}
