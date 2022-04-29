package dz;

import dzaima.ui.node.Node;
import dzaima.ui.gui.Graphics;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.*;
import dzaima.utils.Vec;

public class VarList extends ReorderableNode {
  public SiPlayground r;
  public VarList(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public void drawC(Graphics g) {
    if (ch.sz==0) g.text("ctrl+s or ctrl+enter to run", gc.defFont, 0, gc.defFont.hi, 0xa0808080);
  }
  
  public void stopReorder() {
    stopReorder(false);
  }
  
  public void reorderEnded(int oi, int ni, Node n) {
    Vec<Var> vs = r.vars;
    Var o = vs.get(oi);
    vs.removeAt(oi);
    vs.insert(ni, o);
  }
  
  public int fillW() {
    int w = ctx.win().w/2;
    for (Node c : ch) {
      if (c.ch.sz > 1) {
        if (!(c.ch.get(1) instanceof HNode) || c.ch.get(1).ch.sz==0) continue;
        HNode l = (HNode) c.ch.get(1);
        int pad = l.vs[l.id("pad")].len();
        int btn = l.ch.get(0).minW();
        int tpad = btn+32*pad;
        w = (w-tpad)/32 * 32 + tpad;
        break;
      }
    }
    for (Node c : ch) w = Math.max(w, c.minW());
    return w;
  }
  
  protected void resized() {
    int w = minW();
    int y = 0;
    for (Node c : ch) {
      c.resize(w, c.minH(w), 0, y);
      y+= c.h+pad;
    }
  }
}