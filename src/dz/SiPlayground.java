package dz;

import dz.tabs.*;
import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.tabs.*;
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
  public final int scalableCount;
  public final boolean hasScalable;
  public Path exec = Paths.get("exec/");
  
  public OutputTab output;
  public SourceTab source;
  public EditNode noteNode;
  
  public final Node varTab;
  private final VarList varsNode;
  public final Vec<Var> vars = new Vec<>();
  
  TabbedNode tabsRight;
  public final Vec<SiTab> tabs = new Vec<>();
  
  public final ConcurrentLinkedQueue<Runnable> toRun = new ConcurrentLinkedQueue<>();
  
  public SiPlayground(GConfig gc, Ctx pctx, PNodeGroup g, String bqn, Path singeliPath, String[] singeliArgs, String externalRunner, int scalableCount) {
    super(gc, pctx, g, new WindowInit("Singeli playground"));
    gc.langs().addLang("number", NumLang::new);
    this.bqn = bqn;
    this.singeliPath = Files.isDirectory(singeliPath)? singeliPath.resolve("singeli") : singeliPath;
    this.externalRunner = externalRunner;
    this.hasScalable = scalableCount!=-1;
    this.scalableCount = hasScalable? scalableCount : 1;
    this.singeliArgs = singeliArgs;
    
    output = new OutputTab(this);
    ((TabbedNode) base.ctx.id("outputTab")).addSelectedTab(output);
    noteNode = output.area;
    
    source = new SourceTab(this);
    ((TabbedNode) base.ctx.id("sourceTab")).addSelectedTab(source);
    source.load(file);
    
    
    try {
      Files.createDirectories(exec);
    } catch (Exception e) { throw new RuntimeException(e); }
    
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
    x.start(source.code.getAll());
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
      ctx.put("dividable", DividableNode::new);
      ctx.put("windowSplit", WindowSplitNode::new);
      
      String runner = null;
      int scalableCount = -1;
      Vec<String> singeliArgs = new Vec<>();
      for (int i = 0; i < args.length-2; ) {
        String c = args[i++];
        switch (c) {
          default: throw new IllegalStateException("Unexpected argument "+c);
          case "-a":
          case "-b":
            singeliArgs.add(c);
            singeliArgs.add(args[i++]);
            break;
          case "--runner":
            runner = args[i++];
            break;
          case "--scale":
            scalableCount = Integer.parseInt(args[i++]);
            break;
        }
      }
      
      SiPlayground w = new SiPlayground(gc, ctx, gc.getProp("si.ui").gr(), args[args.length-2], Paths.get(args[args.length-1]), singeliArgs.toArray(new String[0]), runner, scalableCount);
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