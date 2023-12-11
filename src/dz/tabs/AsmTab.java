package dz.tabs;

import dz.*;
import dzaima.ui.gui.PartialMenu;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.ui.node.types.tabs.*;
import dzaima.utils.*;

import java.nio.file.Path;

public class AsmTab extends SiExecTab {
  private final Node node;
  public String title;
  public final CodeAreaNode asmArea;
  public final EditNode command;
  
  public AsmTab(SiPlayground p, String title, String flags) {
    super(p);
    this.title = title;
    node = p.ctx.make(p.gc.getProp("si.asmUI").gr());
    asmArea = (CodeAreaNode) node.ctx.id("asm");
    command = (EditNode) node.ctx.id("ccFlags");
    asmArea.setLang(p.gc.langs().fromName("Assembly"));
    command.append(flags);
  }
  
  public Node show() {
    onShow();
    asmArea.removeAll();
    p.run(this);
    return node;
  }
  
  public String name() {
    return title;
  }
  
  public String serializeName() { return "assembly"; }
  public String serialize() { return "name="+JSON.quote(title)+" cmd="+JSON.quote(command.getAll()); }
  
  public boolean closable() {
    Vec<Tab> tabs = new Vec<>();
    collectRec(tabs, p.layoutPlace.ch.get(0));
    tabs.filterInplace(c -> c instanceof AsmTab);
    return tabs.sz>1;
  }
  private void collectRec(Vec<Tab> tabs, Node n) {
    if (n instanceof WindowSplitNode) {
      for (Node c : n.ch) collectRec(tabs, c);
    } else if (n instanceof TabbedNode) {
      tabs.addAll(Vec.of(((TabbedNode) n).getTabs()));
    }
  }
  
  public void addMenuBarOptions(PartialMenu m) {
    m.add("duplicate", () -> w.o.addTab(new AsmTab(p, title, command.getAll())));
  }
  
  public void preMenuOptions(PartialMenu m) {
    m.addField(title, s -> {
      title = s;
      nameUpdated();
    });
  }
  
  public Executer prep(String src, Runnable onDone) {
    String[] cmd = Tools.split(command.getAll(), ' ');
    return new Executer(this, p, src, onDone) {
      protected void onThread() throws Exception {
        Path c = tmpFile(".c");
        Path asm = tmpFile(".asm");
        
        String cSrc = compileSingeliMain(src, false);
        
        Tools.writeFile(c, cSrc);
        compileC(cmd, "-o", asm.toString(), "-S", c.toString());
        String asmSrc = Tools.readFile(asm);
        
        String fmt = AsmFormatter.formatAsm(cSrc, asmSrc);
        p.toRun.add(() -> TextOutTab.setContents(asmArea, fmt));
      }
    };
  }
}
