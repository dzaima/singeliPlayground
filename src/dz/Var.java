package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.*;
import dzaima.utils.Vec;

import java.util.HashSet;

public class Var {
  public final SiPlayground r;
  public Node n;
  public final String name;
  public final byte[] data;
  public final Vec<TVar> types = new Vec<>();
  
  private final Vec<BtnNode> btns;
  private final Node nameNode;
  private static final String[] BTN_KS = new String[]{"bg", "borderL"};
  private static final Prop[] BTN_VS = new Prop[]{new ColProp(0), new ColProp(0)};
  
  public Var(SiPlayground r, String name, byte[] data, int w0, VTy t0) {
    this.r = r;
    this.name = name;
    this.data = data;
    n = r.base.ctx.make(r.gc.getProp("si.vlUI").gr());
    types.add(new TVar(this, w0, t0));
    nameNode = n.ctx.id("name");
  
    btns = new Vec<>();
    Node btnList = n.ctx.id("btns");
    for (String btnName : new String[]{"8","16","32","64","f32","f64","X"}) {
      BtnNode b = new BtnNode(btnList.ctx, BTN_KS, BTN_VS);
      btnList.add(b);
      b.add(new StringNode(b.ctx, btnName));
      btns.add(b);
      b.setFn(n -> {
        int aw = -1;
        VTy at = VTy.HEX;
        switch (btnName) {
          case "8": aw=8; break;
          case "16": aw=16; break;
          case "32": aw=32; break;
          case "64": aw=64; break;
          case "f32": aw=32; at=VTy.FLOAT; break;
          case "f64": aw=64; at=VTy.FLOAT; break;
          case "X": r.vars.remove(this); r.updVars(); break;
        }
        if (aw!=-1) {
          for (TVar ct : types) {
            if (ct.width==aw && ct.type.f()==at.f()) {
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
    nameNode.add(new StringNode(n.ctx, name+" : "+type()));
  }
  public void updList() {
    updTitle();
    n.remove(1, n.ch.sz);
    HashSet<String> tyNames = new HashSet<>();
    for (TVar c : types) {
      n.add(c.n);
      tyNames.add(c.type==VTy.FLOAT? "f"+c.width : ""+c.width);
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
  
  public long[] read(int width) {
    int by = width/8;
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
    if (types.sz==0) return "["+(data.length/4)+"]i32";
    TVar t = types.get(0);
    return "["+(data.length/(t.width/8))+"]"+(t.type==VTy.SIGNED?"i":t.type==VTy.FLOAT?"f":"u")+t.width;
  }
  public String byteType() {
    return "["+data.length+"]u8";
  }
}
