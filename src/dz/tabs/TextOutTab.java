package dz.tabs;

import dz.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.ScrollNode;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.utils.XY;

public class TextOutTab extends SiExecTab {
  private final Node node;
  private final boolean c;
  public final CodeAreaNode irArea;
  
  public TextOutTab(SiPlayground r, boolean c) {
    super(r);
    this.c = c;
    node = r.ctx.make(r.gc.getProp("si.irUI").gr());
    irArea = (CodeAreaNode) node.ctx.id("ir");
    irArea.setLang(r.gc.langs().fromName(c? "C" : "singeli"));
  }
  
  public void setContents(String s) {
    setContents(irArea, s);
  }
  
  public int mode() {
    return c? 3 : 2;
  }
  
  public Node show() {
    irArea.removeAll();
    p.run();
    return node;
  }
  
  public String name() {
    return c? "C" : "IR";
  }
  
  public Executer prep(String src, Runnable onDone) {
    return new Executer(p, src, onDone) {
      protected void onThread() throws Exception {
        String s = compileSingeliMain(src, !c);
        p.toRun.add(() -> setContents(s));
      }
    };
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
}
