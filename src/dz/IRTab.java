package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.code.CodeAreaNode;

public class IRTab extends Tab {
  
  private final Node irTab;
  public final CodeAreaNode irArea;
  
  public IRTab(SiPlayground r) {
    super(r);
    irTab = r.ctx.make(r.gc.getProp("si.irUI").gr());
    irArea = (CodeAreaNode) irTab.ctx.id("ir");
    irArea.setLang(r.gc.langs().fromName("singeli"));
  }
  
  public void opened() {
    r.tabPlace.replace(0, irTab);
    irArea.removeAll();
    r.run();
  }
  
  public void setContents(String s) {
    AsmTab.setContents(irArea, s);
  }
  
  public int mode() {
    return 2;
  }
  
  public Node mainNode() {
    return irTab;
  }
}
