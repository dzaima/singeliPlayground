package dz.tabs;

import dz.SiPlayground;
import dzaima.ui.node.types.tabs.Tab;

public abstract class SiTab extends Tab {
  public final SiPlayground r;
  private final String title;
  
  protected SiTab(SiPlayground r, String title) {
    super(r.ctx);
    this.title = title;
    this.r = r;
  }
  
  public String name() {
    return title;
  }
  
  public abstract int mode();
}
