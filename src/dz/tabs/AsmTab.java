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
  
  public AsmTab(SiPlayground r, String title, String flags) {
    super(r);
    this.title = title;
    node = r.ctx.make(r.gc.getProp("si.asmUI").gr());
    asmArea = (CodeAreaNode) node.ctx.id("asm");
    command = (EditNode) node.ctx.id("ccFlags");
    asmArea.setLang(r.gc.langs().fromName("Assembly"));
    command.append(flags);
  }
  
  public Node show() {
    asmArea.removeAll();
    p.run();
    return node;
  }
  
  public String name() {
    return title;
  }
  
  public String serializeName() { return "assembly"; }
  public String serialize() { return "name="+JSON.quote(title)+" cmd="+JSON.quote(command.getAll()); }
  
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
