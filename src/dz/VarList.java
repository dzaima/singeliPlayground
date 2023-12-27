package dz;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.ReorderableNode;
import dzaima.utils.Vec;

public class VarList extends ReorderableNode {
  public SiPlayground r;
  public VarList(Ctx ctx, Props props) {
    super(ctx, props);
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
}