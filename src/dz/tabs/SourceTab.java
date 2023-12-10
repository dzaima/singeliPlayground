package dz.tabs;

import dz.SiPlayground;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.ui.node.types.tabs.Tab;
import dzaima.utils.Tools;

import java.nio.file.*;

public class SourceTab extends Tab {
  public final SiPlayground t;
  private final Node node;
  public CodeAreaNode code;
  
  public SourceTab(SiPlayground t) {
    super(t.ctx);
    this.t = t;
    node = t.ctx.make(t.gc.getProp("si.sourceTab").gr());
    code = (CodeAreaNode) node.ctx.id("code");
    
    code.propsUpd();
    code.setLang(t.gc.langs().fromName("singeli"));
    
    code.setFn(value -> {
      if (value!=0) t.run();
      return value!=0;
    });
  }
  
  public void load(Path file) {
    int i = code.um.pushIgnore();
    if (Files.exists(file)) {
      code.append(Tools.readFile(file));
    } else {
      String s = "";
      s+= "include 'skin/c'\n";
      s+= "include 'arch/c'\n";
      s+= "oper ~~ reinterpret infix right 55\n";
      s+= "def isintv{T} = isint{eltype{T}}\n";
      s+= "def isf64v{T} = f64==eltype{T}\n";
      s+= "def load {a:T, n    & width{eltype{T}}==256 & isintv{eltype{T}}} = emit{eltype{T}, '_mm256_loadu_si256',    emit{T, 'op +', a, n}}\n";
      s+= "def load {a:T, n    & width{eltype{T}}==256 & isf64v{eltype{T}}} = emit{eltype{T}, '_mm256_loadu_pd', *f64~~emit{T, 'op +', a, n}}\n";
      s+= "def store{a:T, n, v & width{eltype{T}}==256 & isintv{eltype{T}}} = emit{void, '_mm256_storeu_si256',        emit{T, 'op +', a, n}, v}\n";
      s+= "def store{a:T, n, v & width{eltype{T}}==256 & isf64v{eltype{T}}} = emit{void, '_mm256_storeu_pd',     *f64~~emit{T, 'op +', a, n}, v}\n";
      s+= "def __add{a:T,b:T & T==[8]i32} = emit{T, '_mm256_add_epi32', a, b}\n";
      s+= "\n";
      s+= "fn f(x:*[8]i32) = load{x,10}+emit{[8]i32, '_mm256_set1_epi32', 1}\n";
      s+= "export{'f', f}\n";
      s+= "\n";
      s+= "⍎\n";
      s+= "\n";
      s+= "# 'name ← expression' to update watch\n";
      s+= "# 'name:← expression' to set only if name doesn't already exist\n";
      s+= "# variables from a watch can be read as regular variables\n";
      s+= "c:← i32~~1\n";
      s+= "c = c+1\n";
      s+= "data:*i32 = tup{0,1,2,3,4,5,6,7}\n";
      s+= "\n";
      s+= "a ← emit{[8]i32, '_mm256_set1_epi32', 1000}\n";
      s+= "b ← a + load{*[8]i32~~data, 0}\n";
      
      code.append(s);
    }
    code.um.popIgnore(i);
  }
  
  public Node show() {
    return node;
  }
  
  public String name() {
    return "source";
  }
}
