package dz.tabs;

import dz.*;
import dzaima.ui.gui.PartialMenu;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.ui.node.types.ScrollNode;
import dzaima.utils.XY;

public class AsmTab extends SiExecTab {
  private final Node node;
  public String title;
  public final CodeAreaNode asmArea;
  public final EditNode command;
  
  public AsmTab(SiPlayground r, String title, String flags) {
    super(r);
    this.title = title;
    node = r.ctx.make(r.gc.getProp("si.asmUI").gr());
    asmArea = (CodeAreaNode) node.ctx.id("asm");
    command = (EditNode) node.ctx.id("ccFlags");
    asmArea.setLang(r.gc.langs().fromName("Assembly"));
    command.append(flags);
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
    p.run();
    return node;
  }
  
  public String name() {
    return title;
  }
  
  public void preMenuOptions(PartialMenu m) {
    m.addField(title, s -> {
      title = s;
      nameUpdated();
    });
  }
}
