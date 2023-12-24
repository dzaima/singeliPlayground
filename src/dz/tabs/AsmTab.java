package dz.tabs;

import dz.*;
import dzaima.ui.gui.PartialMenu;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
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
    command = (EditNode) node.ctx.id("ccFlags");
    command.append(flags);
    
    asmArea = (CodeAreaNode) node.ctx.id("asm");
    asmArea.setLang(p.asmLang);
  }
  
  public Node show() {
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
    return p.allTabs().filter(c -> c instanceof AsmTab).sz>1;
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
    return new Executer(p, src, onDone) {
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
