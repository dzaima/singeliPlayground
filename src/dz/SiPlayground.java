package dz;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.utils.*;
import io.github.humbleui.skija.Surface;

import java.nio.file.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SiPlayground extends NodeWindow {
  public static final Path LOCAL_CFG = Paths.get("local.dzcfg");
  public Path file = Paths.get("current.singeli");
  
  public String bqn;
  public Path singeliPath;
  public Path exec = Paths.get("exec/");
  
  private final CodeAreaNode code;
  public final CodeAreaNode noteNode;
  public final Node tabPlace;
  
  public final Node varTab;
  private final VarList varsNode;
  public final Vec<Var> vars = new Vec<>();
  
  public final Vec<Tab> tabs = new Vec<>();
  public Tab cTab;
  
  public final ConcurrentLinkedQueue<Runnable> toRun = new ConcurrentLinkedQueue<>();
  
  public SiPlayground(GConfig gc, Ctx pctx, PNodeGroup g, String bqn, Path singeliPath) {
    super(gc, pctx, g, new WindowInit("Singeli playground"));
    this.bqn = bqn;
    this.singeliPath = Files.isDirectory(singeliPath)? singeliPath.resolve("singeli") : singeliPath;
    code = (CodeAreaNode) base.ctx.id("code");
    noteNode = (CodeAreaNode) base.ctx.id("note");
    code.propsUpd();
    code.setLang(gc.langs().fromName("singeli"));
    int i = code.um.pushIgnore();
    if (Files.exists(file)) {
      code.append(Tools.readFile(file));
    } else {
      String s = "";
      s+= "include 'skin/c'\n";
      s+= "include 'arch/c'\n";
      s+= "def isintv{T} = isint{eltype{T}}\n";
      s+= "def isf64v{T} = f64==eltype{T}\n";
      s+= "def load {a:T, n    & width{eltype{T}}==256 & isintv{eltype{T}}} = emit{eltype{T}, '_mm256_loadu_si256',          emit{T, 'op +', a, n}}\n";
      s+= "def load {a:T, n    & width{eltype{T}}==256 & isf64v{eltype{T}}} = emit{eltype{T}, '_mm256_loadu_pd', cast_p{f64, emit{T, 'op +', a, n}}}\n";
      s+= "def store{a:T, n, v & width{eltype{T}}==256 & isintv{eltype{T}}} = emit{void, '_mm256_storeu_si256',          emit{T, 'op +', a, n}, v}\n";
      s+= "def store{a:T, n, v & width{eltype{T}}==256 & isf64v{eltype{T}}} = emit{void, '_mm256_storeu_pd', cast_p{f64, emit{T, 'op +', a, n}}, v}\n";
      s+= "\n";
      s+= "fn(x:*[8]i32) = load{x,10}+emit{[8]i32, '_mm256_set1_epi32', 1}\n";
      s+= "'fn' = fn\n";
      s+= "\n???\n\n";
      s+= "# 'name ??? expression' to update watch\n";
      s+= "# 'name:??? expression' to set only if name doesn't already exist\n";
      s+= "# variables from a watch can be read as regular variables\n";
      s+= "c:??? cast{i32, 1}\n";
      s+= "c = c+1\n";
      code.append(s+"\na ??? emit{[8]i32, '_mm256_set1_epi32', 1}");
    }
    try {
      Files.createDirectories(exec);
    } catch (Exception e) { throw new RuntimeException(e); }
    code.setFn(value -> {
      if (value!=0) run();
      return value!=0;
    });
    code.um.popIgnore(i);
    
    varTab = ctx.make(gc.getProp("si.varsUI").gr());
    varsNode = (VarList) varTab.ctx.id("vars");
    varsNode.r = this;
    
    
    tabPlace = base.ctx.id("tabPlace");
    
    new VarsTab(this).addTab("variables");
    new AsmTab(this).addTab("assembly 1");
    new AsmTab(this).addTab("assembly 2");
    new AsmTab(this).addTab("assembly 3");
    new IRTab(this).addTab("IR");
    
    tabs.get(0).open();
    
    updVars();
  }
  
  SiExecute prev;
  public void run() {
    if (prev!=null) {
      prev.cancel();
      prev = null;
    }
    SiExecute x = new SiExecute(this, file, cTab);
    prev = x;
    String src = code.getAll();
    x.start(src);
  }
  
  public void tick() {
    super.tick();
    while (true) {
      Runnable v = toRun.poll();
      if (v==null) break;
      v.run();
    }
  }
  
  public void updVars() {
    varsNode.stopReorder();
    varsNode.clearCh();
    for (int i = 0; i < vars.sz; i++) {
      varsNode.add(vars.get(i).n);
    }
  }
  
  public static void main(String[] args) {
    // Windows.setManager(Windows.Manager.JWM);
    if (args.length!=2) {
      System.out.println("Usage: ./run cbqn path/to/Singeli");
      return;
    }
    Windows.start(mgr -> {
      GConfig gc = GConfig.newConfig(gc0 -> {
        gc0.addCfg(() -> Tools.readRes("siPlayground.dzcfg"));
        gc0.addCfg(() -> {
          if (Files.exists(LOCAL_CFG)) return Tools.readFile(LOCAL_CFG);
          return "";
        });
      });
      gc.langs().addLang("asm", AsmLang::new, "s");
      
      BaseCtx ctx = Ctx.newCtx();
      ctx.put("varfield", VarField::new);
      ctx.put("varlist", VarList::new);
      
      SiPlayground w = new SiPlayground(gc, ctx, gc.getProp("si.ui").gr(), args[0], Paths.get(args[1]));
      mgr.start(w);
    });
  }
  
  public void resized(Surface s) {
    super.resized(s);
    if (varsNode!=null) varsNode.mResize();
  }
  
  public boolean key(Key key, int scancode, KeyAction a) {
    if (a.press) {
      if (key.k_enter() && !key.plain()  ||  key.k_s() && key.onlyCtrl()) {
        run();
        return true;
      }
      if (key.k_f12()) {
        createTools();
        return true;
      }
      if (key.onlyCtrl() && key.k_add()   && key.onKeypad()) { gc.setEM(gc.em+1); return true; }
      if (key.onlyCtrl() && key.k_minus() && key.onKeypad()) { gc.setEM(gc.em-1); return true; }
    }
    return super.key(key, scancode, a);
  }
}