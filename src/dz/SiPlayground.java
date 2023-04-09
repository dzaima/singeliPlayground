package dz;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.ui.node.types.tabs.TabbedNode;
import dzaima.utils.*;
import io.github.humbleui.skija.Surface;

import java.nio.file.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SiPlayground extends NodeWindow {
  public static final Path LOCAL_CFG = Paths.get("local.dzcfg");
  public Path file = Paths.get("current.singeli");
  
  public String bqn;
  public Path singeliPath;
  public final String[] singeliArgs;
  public final String externalRunner;
  public Path exec = Paths.get("exec/");
  
  private final CodeAreaNode code;
  public final CodeAreaNode noteNode;
  
  public final Node varTab;
  private final VarList varsNode;
  public final Vec<Var> vars = new Vec<>();
  
  TabbedNode tabsRight;
  public final Vec<SiTab> tabs = new Vec<>();
  
  public final ConcurrentLinkedQueue<Runnable> toRun = new ConcurrentLinkedQueue<>();
  
  public SiPlayground(GConfig gc, Ctx pctx, PNodeGroup g, String bqn, Path singeliPath, String[] singeliArgs, String externalRunner) {
    super(gc, pctx, g, new WindowInit("Singeli playground"));
    this.bqn = bqn;
    this.singeliPath = Files.isDirectory(singeliPath)? singeliPath.resolve("singeli") : singeliPath;
    this.externalRunner = externalRunner;
    this.singeliArgs = singeliArgs;
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
  
  
    tabsRight = (TabbedNode) base.ctx.id("tabsRight");
    tabsRight.addSelectedTab(new VarsTab(this, "variables"));
    tabsRight.addTab(new AsmTab(this, "assembly 1",    "cc -O3 -masm=intel -march=native"));
    tabsRight.addTab(new AsmTab(this, "assembly 2", "clang -O3 -masm=intel"));
    tabsRight.addTab(new AsmTab(this, "assembly 3", "clang -O3 -masm=intel -march=native"));
    tabsRight.addTab(new AsmTab(this, "assembly 4",   "gcc -O3 -masm=intel"));
    tabsRight.addTab(new AsmTab(this, "assembly 5",   "gcc -O3 -masm=intel -march=native"));
    tabsRight.addTab(new IRTab(this, false, "IR"));
    tabsRight.addTab(new IRTab(this, true, "C"));
    
    updVars();
  }
  
  SiExecute prev;
  public void run() {
    if (prev!=null) {
      prev.cancel();
      prev = null;
    }
    SiExecute x = new SiExecute(this, file, (SiTab) tabsRight.cTab());
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
    Windows.setManager(Windows.Manager.JWM);
    if (args.length<2) {
      System.out.println("Usage: ./run [singeli args] [--runner path/to/runner] cbqn path/to/Singeli");
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
      
      BaseCtx ctx = Ctx.newCtx();
      ctx.put("varfield", VarField::new);
      ctx.put("varlist", VarList::new);
      
      String runner = null;
      Vec<String> singeliArgs = new Vec<>();
      for (int i = 0; i < args.length-2; ) {
        String c = args[i++];
        if (c.equals("-a") || c.equals("-b")) {
          singeliArgs.add(c);
          singeliArgs.add(args[i++]);
        } else if (c.equals("--runner")) {
          runner = args[i++];
        }
      }
      
      SiPlayground w = new SiPlayground(gc, ctx, gc.getProp("si.ui").gr(), args[args.length-2], Paths.get(args[args.length-1]), singeliArgs.toArray(new String[0]), runner);
      mgr.start(w);
    });
  }
  
  public void resized(Surface s) {
    super.resized(s);
    if (varsNode!=null) varsNode.mResize();
  }
  
  public boolean key(Key key, int scancode, KeyAction a) {
    switch (gc.keymap(key, a, "si")) {
      case "devtools": createTools(); return true;
      case "run": run(); return true;
      case "fontPlus":  gc.setEM(gc.em+1); return true;
      case "fontMinus": gc.setEM(gc.em-1); return true;
    }
    return super.key(key, scancode, a);
  }
}