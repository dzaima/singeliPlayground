package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.*;

public abstract class Tab {
  public final SiPlayground r;
  
  protected Tab(SiPlayground r) { this.r = r; }
  
  public abstract void opened();
  
  public abstract int mode();
  
  public abstract Node mainNode();
  
  public void open() {
    r.tabPlace.replace(0, mainNode());
    r.cTab = this;
    opened();
  }
  
  public void addTab(String title) {
    r.tabs.add(this);
    Node btn = r.ctx.make(r.gc.getProp("si.tabSelUI").gr());
    btn.ctx.id("name").add(new StringNode(btn.ctx, title));
    r.base.ctx.id("btnList").add(btn);
    ((BtnNode) btn).setFn(c -> open());
  }
}
