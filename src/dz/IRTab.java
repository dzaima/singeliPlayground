package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.code.CodeAreaNode;

public class IRTab extends SiTab {
  
  private final Node irTab;
  public final CodeAreaNode irArea;
  
  public IRTab(SiPlayground r, String title) {
    super(r, title);
    irTab = r.ctx.make(r.gc.getProp("si.irUI").gr());
    irArea = (CodeAreaNode) irTab.ctx.id("ir");
    irArea.setLang(r.gc.langs().fromName("singeli"));
  }
  
  public void setContents(String s) {
    AsmTab.setContents(irArea, s);
  }
  
  public int mode() {
    return 2;
  }
  
  public Node show() {
    irArea.removeAll();
    r.run();
    return irTab;
  }
}
