package dz.tabs;

import dz.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.code.CodeAreaNode;

public class IRTab extends SiExecTab {
  
  private final Node irTab;
  private final boolean c;
  public final CodeAreaNode irArea;
  
  public IRTab(SiPlayground r, boolean c) {
    super(r);
    this.c = c;
    irTab = r.ctx.make(r.gc.getProp("si.irUI").gr());
    irArea = (CodeAreaNode) irTab.ctx.id("ir");
    irArea.setLang(r.gc.langs().fromName(c? "C" : "singeli"));
  }
  
  public void setContents(String s) {
    AsmTab.setContents(irArea, s);
  }
  
  public int mode() {
    return c? 3 : 2;
  }
  
  public Node show() {
    irArea.removeAll();
    p.run();
    return irTab;
  }
  
  public String name() {
    return c? "C" : "IR";
  }
}
