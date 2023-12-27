package dz;

import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;
import dzaima.ui.node.types.FrameNode;
import dzaima.ui.node.utils.ListUtils;

public class DividableNode extends FrameNode {
  public DividableNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  
  private int pad;
  public void propsUpd() {
    super.propsUpd();
    pad = gc.lenD(this, "pad", 0);
  }
  
  public int fillW() { return ListUtils.hMinW(ch); }
  public int fillH(int w) { return ListUtils.hMinH(ch, w); }
  
  protected void resized() {
    int n = ch.size();
    int parts = Math.max(n, 32);
    int inc = parts/n;
    int totW = w+pad;
    for (int i = 0; i < n; i++) {
      int s = totW*inc*i    /parts;
      int e = totW*inc*(i+1)/parts - pad;
      ch.get(i).resize(e-s, h, s, 0);
    }
  }
}
