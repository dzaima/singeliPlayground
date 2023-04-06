package dz;

import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.ui.node.types.ScrollNode;
import dzaima.utils.XY;

public class AsmTab extends SiTab {
  
  private final Node asmTab;
  public final CodeAreaNode asmArea;
  public final EditNode asmCCFlags;
  
  public AsmTab(SiPlayground r, String title, String flags) {
    super(r, title);
    asmTab = r.ctx.make(r.gc.getProp("si.asmUI").gr());
    asmArea = (CodeAreaNode) asmTab.ctx.id("asm");
    asmCCFlags = (EditNode) asmTab.ctx.id("ccFlags");
    asmArea.setLang(r.gc.langs().fromName("Assembly"));
    asmCCFlags.append(flags);
  }
  
  public void setContents(String s) {
    setContents(asmArea, s);
  }
  public static void setContents(EditNode e, String s) {
    ScrollNode sc = ScrollNode.nearestScrollNode(e);
    XY rel = e.relPos(sc);
    e.removeAll();
    e.append(s);
    ScrollNode.scrollTo(e, ScrollNode.Mode.INSTANT, ScrollNode.Mode.INSTANT,
      (sc.clipSX+sc.clipEX)/2-rel.x,
      (sc.clipSY+sc.clipEY)/2-rel.y);
  }
  
  public int mode() {
    return 1;
  }
  
  public Node show() {
    asmArea.removeAll();
    r.run();
    return asmTab;
  }
}
