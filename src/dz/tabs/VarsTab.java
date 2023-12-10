package dz.tabs;

import dz.SiPlayground;
import dz.tabs.SiTab;
import dzaima.ui.node.Node;

public class VarsTab extends SiTab {
  protected VarsTab(SiPlayground r, String title) {
    super(r, title);
  }
  
  public Node show() {
    return r.varTab;
  }
  
  public int mode() {
    return 0;
  }
}
