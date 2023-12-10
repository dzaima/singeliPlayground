package dz.tabs;

import dz.SiPlayground;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.tabs.Tab;

public class OutputTab extends Tab {
  public final SiPlayground t;
  private final Node node;
  public EditNode area;
  
  public OutputTab(SiPlayground t) {
    super(t.ctx);
    this.t = t;
    node = t.ctx.make(t.gc.getProp("si.outputTab").gr());
    area = (EditNode) node.ctx.id("area");
  }
  
  public Node show() {
    return node;
  }
  
  public String name() {
    return "output";
  }
}
