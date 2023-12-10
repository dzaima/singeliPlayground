package dz.tabs;

import dz.*;
import dzaima.ui.node.Node;

public class VarsTab extends SiExecTab {
  private final Node node;
  public final VarList varsNode;
  
  public VarsTab(SiPlayground r) {
    super(r);
    node = ctx.make(r.gc.getProp("si.varsUI").gr());
    varsNode = (VarList) node.ctx.id("vars");
    varsNode.r = r;
  }
  
  public Node show() {
    return node;
  }
  
  public String name() {
    return "variables";
  }
  
  public int mode() {
    return 0;
  }
}
