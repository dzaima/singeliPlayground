package dz;

import dz.tabs.*;
import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.ui.node.types.tabs.*;
import dzaima.utils.*;
import io.github.humbleui.skija.Surface;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Stream;

public class SiPlayground extends NodeWindow {
  public static final Path LOCAL_CFG = Paths.get("local.dzcfg");
  public final Path savePath, layoutPath;
  public final Path runnerPath;
  public boolean initialized = false;
  
  public String bqn;
  public Path singeliPath;
  public final String[] singeliArgs;
  public Path execTmp = Paths.get("exec/");
  
  public OutputTab output;
  public SourceTab source;
  public EditNode noteNode;
  
  private final VarList varsNode;
  public final Node layoutPlace;
  public final Vec<Var> vars = new Vec<>();
  
  public final ConcurrentLinkedQueue<Runnable> toRun = new ConcurrentLinkedQueue<>();
  
  public SiPlayground(GConfig gc, Ctx pctx, PNodeGroup g, String bqn, Path singeliPath, String[] singeliArgs, Path savePath, Path layoutPath, Path runnerPath) {
    super(gc, pctx, g, new WindowInit("Singeli playground"));
    gc.langs().addLang("number", NumLang::new);
    this.bqn = bqn;
    this.singeliPath = Files.isDirectory(singeliPath)? singeliPath.resolve("singeli") : singeliPath;
    this.runnerPath = runnerPath;
    this.savePath = savePath;
    this.layoutPath = layoutPath;
    this.singeliArgs = singeliArgs;
    
    try {
      Files.createDirectories(execTmp);
    } catch (Exception e) { throw new RuntimeException(e); }
    
    String layoutSrc;
    if (Files.exists(layoutPath)) {
      layoutSrc = Tools.readFile(layoutPath);
    } else {
      layoutSrc = Tools.readRes("defaultTabs.dzcfg");
    }
    
    HashMap<String, Function<HashMap<String, Prop>, Tab>> m = new HashMap<>();
    VarsTab varsTab = new VarsTab(this);
    source = new SourceTab(this);
    source.load(savePath);
    output = new OutputTab(this);
    m.put("source", c -> source);
    m.put("output", c -> output);
    m.put("variables", c -> varsTab);
    m.put("assembly", c -> new AsmTab(this, c.get("name").str(), c.get("cmd").str()));
    m.put("ir", c -> new TextOutTab(this, false));
    m.put("c", c -> new TextOutTab(this, true));
    
    layoutPlace = base.ctx.id("place");
    layoutPlace.add(SerializableTab.deserializeTree(base.ctx, layoutSrc, m));
    
    noteNode = output.area;
    varsNode = varsTab.varsNode;
    
    updVars();
    initialized = true;
  }
  
  public final LinkedHashSet<SiExecTab> openTabs = new LinkedHashSet<>();
  
  private final LinkedHashMap<SiExecTab, Executer> queue = new LinkedHashMap<>(); // first entry is the active one
  public void save() {
    if (!initialized) return;
    Tools.writeFile(savePath, source.code.getAll());
    Tools.writeFile(layoutPath, SerializableTab.serializeTree(layoutPlace.ch.get(0)));
  }
  public void run(SiExecTab t) {
    if (!initialized) return;
    Executer ex = t.prep(source.code.getAll(), () -> {
      queue.remove(t);
      if (!queue.isEmpty()) {
        queue.values().iterator().next().start();
      } else {
        try (Stream<Path> stream = Files.list(execTmp)) {
          stream.forEach(c -> {
            if (c.getFileName().toString().startsWith("tmp_")) {
              try { Files.deleteIfExists(c); }
              catch (IOException e) { Log.stacktrace("cleanup", e); }
            }
          });
        } catch (IOException e) {
          Log.stacktrace("cleanup", e);
        }
      }
    });
    
    if (!queue.isEmpty() && queue.keySet().iterator().next() == t) queue.values().iterator().next().cancel();
    queue.put(t, ex);
    if (queue.values().iterator().next() == ex) ex.start();
  }
  public void runAll() {
    if (!initialized) return;
    save();
    for (SiExecTab c : openTabs) run(c);
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
      System.out.println("Usage: ./run [singeli args] [--file file.singeli] [--layout layout.dzcfg] [--runner runner.sh] cbqn path/to/Singeli");
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
      
      Path save = Paths.get("current.singeli");
      Path layout = Paths.get("layout_default.dzcfg");
      Path runner = null;
      Vec<String> singeliArgs = new Vec<>();
      for (int i = 0; i < args.length-2; ) {
        String c = args[i++];
        switch (c) {
          default: throw new IllegalStateException("Unexpected argument "+c);
            case "-a": case "--arch":
            case "-i": case "--infer":
            case "-l": case "--lib":
            case "-c": case "--config":
            case "-p": case "--pre":
            case "-n": case "--name":
            singeliArgs.add(c);
            singeliArgs.add(args[i++]);
            break;
          case "--file": save = Paths.get(args[i++]); break;
          case "--runner": runner = Paths.get(args[i++]); break;
          case "--layout": layout = Paths.get(args[i++]); break;
        }
      }
      
      SiPlayground w = new SiPlayground(
        gc, ctx, gc.getProp("si.ui").gr(),
        args[args.length-2], Paths.get(args[args.length-1]), singeliArgs.toArray(new String[0]),
        save, layout, runner);
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
      case "run": runAll(); return true;
      case "fontPlus":  gc.setEM(gc.em+1); return true;
      case "fontMinus": gc.setEM(gc.em-1); return true;
    }
    return super.key(key, scancode, a);
  }
}