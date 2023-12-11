package dz.tabs;

import dz.SiPlayground;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.EditNode;

public class OutputTab extends SiTab {
  private final Node node;
  public EditNode area;
  
  public OutputTab(SiPlayground p) {
    super(p);
    node = p.ctx.make(p.gc.getProp("si.outputTab").gr());
    area = (EditNode) node.ctx.id("area");
  }
  
  public Node show() {
    return node;
  }
  
  public String name() {
    return "output";
  }
  
  public String serializeName() {
    return "output";
  }
}
