package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.*;
import dzaima.utils.Vec;

import java.util.HashSet;

public class Var {
  public final SiPlayground r;
  public Node n;
  public final boolean scalar;
  public final String name;
  public final byte[] data;
  public final Vec<TVar> types = new Vec<>();
  public final SiType type;
  
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
    
    types.add(new TVar(this, type.isBitVec()? Math.min(64, type.widthBits()) : type.elBits(), type.repr));
    
    btns = new Vec<>();
    Node btnList = n.ctx.id("btns");
    for (String btnName : new String[]{"8","16","32","64","f32","f64","X"}) {
      if (btnName.startsWith("f") && type.isBitVec()) continue;
      if (!btnName.equals("X")) {
        int cw = Integer.parseInt(btnName.startsWith("f")? btnName.substring(1) : btnName);
        if (cw > type.elBits()) continue;
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
            if (ct.elBits==aw && ct.qual.f()==at.f()) {
              types.remove(ct);
              updList();
              return;
            }
          }
          types.add(new TVar(this, aw, at));
          updList();
        }
      });
    }
    
    updList();
  }
  
  public void updTitle() {
    nameNode.clearCh();
    nameNode.add(new StringNode(n.ctx, name+" : "+type(true)));
  }
  public void updList() {
    updTitle();
    n.remove(1, n.ch.sz);
    HashSet<String> tyNames = new HashSet<>();
    for (TVar c : types) {
      n.add(c.n);
      tyNames.add(c.qual==TyRepr.FLOAT? "f"+c.elBits : ""+c.elBits);
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
    int by = elBits/8;
    long[] res = new long[data.length/by];
    for (int i = 0; i < res.length; i++) {
      long c = 0;
      for (int j = 0; j < by; j++) c|= ((long) (data[i*by+j]&0xff)) << (8*j);
      res[i] = c;
    }
    return res;
  }
  
  public void store(long[] vs) {
    int by = data.length/vs.length;
    for (int i = 0; i < vs.length; i++) {
      long c = vs[i];
      for (int j = 0; j < by; j++) {
        data[i*by + j] = (byte) (c&0xff);
        c>>= 8;
      }
    }
    updData();
  }
  
  public String type() {
    return type(false);
  }
  public String type(boolean includeScale) {
    String elt = (type.repr==TyRepr.SIGNED?"i":type.repr==TyRepr.FLOAT?"f":"u")+type.elBits();
    if (scalar) return elt;
    String sc = includeScale && type.scale!=-1? type.scale+"×" : "";
    return sc+"["+type.unscaledCount()+"]"+elt;
  }
  
  public Var copy() {
    return new Var(r, name, data, type);
  }
}
