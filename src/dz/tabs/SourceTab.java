package dz.tabs;

import dz.SiPlayground;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.utils.Tools;

import java.nio.file.*;

public class SourceTab extends SiTab {
  public final SiPlayground t;
  private final Node node;
  public CodeAreaNode code;
  
  public SourceTab(SiPlayground p) {
    super(p);
    this.t = p;
    node = p.ctx.make(p.gc.getProp("si.sourceTab").gr());
    code = (CodeAreaNode) node.ctx.id("code");
    
    code.propsUpd();
    code.setLang(p.gc.langs().fromName("singeli"));
    
    code.setFn(value -> {
      if (value!=0) p.runAll();
      return value!=0;
    });
  }
  
  public void load(Path file) {
    int i = code.um.pushIgnore();
    if (Files.exists(file)) {
      code.append(Tools.readFile(file));
    } else {
      code.append(Tools.readRes("default.singeli"));
    }
    code.um.popIgnore(i);
  }
  
  public Node show() {
    return node;
  }
  
  public String name() {
    return "source";
  }
  
  public String serializeName() {
    return "source";
  }
}
