package dz.tabs;

import dz.SiPlayground;
import dzaima.ui.gui.PartialMenu;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.types.tabs.*;

public abstract class SiTab extends Tab {
  public final SiPlayground p;
  public SiTab(SiPlayground p) {
    super(p.ctx);
    this.p = p;
  }
  
  public void onRightClick(Click cl) {
    PartialMenu m = new PartialMenu(p.gc);
    preMenuOptions(m);
    WindowSplitNode.onTabRightClick(m, this);
    addMenuBarOptions(m);
    m.open(ctx, cl);
  }
  
  public /*open*/ void preMenuOptions(PartialMenu m) { }
}
