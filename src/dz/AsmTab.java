package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.editable.code.CodeAreaNode;

public class AsmTab extends Tab {
  
  private final Node asmTab;
  public final CodeAreaNode asmArea;
  public final EditNode asmCCFlags;
  
  public AsmTab(SiPlayground r) {
    super(r);
    asmTab = r.ctx.make(r.gc.getProp("si.asmUI").gr());
    asmArea = (CodeAreaNode) asmTab.ctx.id("asm");
    asmCCFlags = (EditNode) asmTab.ctx.id("ccFlags");
    asmArea.setLang(r.gc.langs().fromName("asm"));
  }
  
  public void opened() {
    r.tabPlace.replace(0, asmTab);
    asmArea.removeAll();
    r.run();
  }
  
  public int mode() {
    return 1;
  }
  
  public Node mainNode() {
    return asmTab;
  }
}
