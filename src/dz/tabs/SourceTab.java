package dz.tabs;

import dz.SiPlayground;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.utils.*;

import java.nio.file.*;

public class SourceTab extends SiTab {
  public final SiPlayground t;
  public CodeAreaNode code;
  public final boolean main;
  public final Path path;
  private final Node node;
  
  public SourceTab(SiPlayground p, Path path, boolean main) {
    super(p);
    this.t = p;
    this.path = path;
    node = p.ctx.make(p.gc.getProp("si.sourceTab").gr());
    this.main = main;
    code = (CodeAreaNode) node.ctx.id("code");
    
    code.propsUpd();
    code.setLang(p.gc.langs().fromName("singeli"));
    
    code.setFn((a,m) -> {
      if (a.enter && m>0) p.runAll();
      return a.enter && m>0;
    });
    
    
    int i = code.um.pushIgnore();
    if (Files.exists(path)) {
      code.append(Tools.readFile(path));
    } else if (main) {
      code.append(Tools.readRes("default.singeli"));
    }
    code.um.popIgnore(i);
  }
  
  public Node show() { return node; }
  public String name() {
    return main? "source" : path.getFileName().toString();
  }
  public boolean closable() {
    return !main;
  }
  
  public String serializeName() {
    return main? "source" : "externalSource";
  }
  public String serialize() {
    return main? "" : "path="+JSON.quote(path.toString());
  }
  
  public void save() {
    Tools.writeFile(path, code.getAll());
  }
}
