package dz;

import dzaima.ui.node.Node;

public class VarsTab extends Tab {
  protected VarsTab(SiPlayground r) {
    super(r);
  }
  
  public void opened() {
    
  }
  
  public Node mainNode() {
    return r.varTab;
  }
  
  public int mode() {
    return 0;
  }
}
