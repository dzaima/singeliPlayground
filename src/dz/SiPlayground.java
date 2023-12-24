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
import dzaima.ui.node.types.editable.code.langs.Lang;
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
  public final Path layoutPath;
  public final Path runnerPath;
  public boolean initialized = false;
  
  public String bqn;
  public Path singeliPath;
  public final String[] singeliArgList;
  public final Vec<Pair<String,String>> singeliArgs;
  public Path execTmp = Paths.get("exec/");
  public Lang asmLang;
  
  public OutputTab output;
  public SourceTab source;
  public EditNode noteNode;
  
  private final VarList varsNode;
  public final boolean layoutReadOnly;
  public final Node layoutPlace;
  public final Vec<Var> vars = new Vec<>();
  
  public final ConcurrentLinkedQueue<Runnable> toRun = new ConcurrentLinkedQueue<>();
  
  public SiPlayground(GConfig gc, Ctx pctx, PNodeGroup g, String bqn, Path singeliPath, Vec<Pair<String,String>> singeliArgs, Path savePath, Path layoutPath, boolean layoutReadOnly, Path runnerPath) {
    super(gc, pctx, g, new WindowInit("Singeli playground"));
    gc.langs().addLang("number", new NumLang());
    this.bqn = bqn;
    this.singeliPath = Files.isDirectory(singeliPath)? singeliPath.resolve("singeli") : singeliPath;
    this.runnerPath = runnerPath;
    this.layoutPath = layoutPath;
    this.singeliArgs = singeliArgs;
    this.asmLang = gc.langs().fromName("x86 assembly");
    this.layoutReadOnly = layoutReadOnly;
    
    Vec<String> argList = new Vec<>();
    for (Pair<String, String> c : singeliArgs) {
      argList.add(c.a);
      argList.add(c.b);
    }
    this.singeliArgList = argList.toArray(new String[0]);
    
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
    source = new SourceTab(this, savePath, true);
    output = new OutputTab(this);
    m.put("source", c -> source);
    m.put("externalSource", c -> new SourceTab(this, Paths.get(c.get("path").str()), false));
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
    
    Box<String> lang = new Box<>();
    run((code, onDone) -> new Executer(SiPlayground.this, "each{show,listarch{}}", () -> {
      onDone.run();
      asmLang = gc.langs().fromName(lang.get());
      for (SiTab tab : allTabs()) if (tab instanceof AsmTab) ((AsmTab) tab).asmArea.setLang(asmLang);
    }) {
      protected void onThread() throws Exception {
        status("detecting architecture...");
        Executed e = compileSingeli(code, true);
        note(e.err);
        for (String l : Tools.split(e.out, '\n')) {
          if (l.length() > 2) {
            switch (l.substring(1, l.length()-1)) {
              case "X86_64":  lang.set("x86 assembly"); break;
              case "AARCH64": lang.set("aarch64 assembly"); break;
              case "RV64":    lang.set("risc-v assembly"); break;
            }
          }
        }
      }
    });
  }
  
  public final LinkedHashSet<SiExecTab> openTabs = new LinkedHashSet<>();
  
  
  public interface ExecuterKey {
    Executer prep(String code, Runnable onDone);
  }
  private final LinkedHashMap<ExecuterKey, Executer> queue = new LinkedHashMap<>(); // first entry is the active one
  public void save() {
    if (!initialized) return;
    for (SiTab t : allTabs()) {
      if (t instanceof SourceTab) ((SourceTab) t).save();
    }
    if (!layoutReadOnly) Tools.writeFile(layoutPath, SerializableTab.serializeTree(layoutPlace.ch.get(0)));
  }
  public void run(ExecuterKey t) {
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
  
  public Vec<SiTab> allTabs() {
    Vec<SiTab> r = new Vec<>();
    collectRec(r, layoutPlace.ch.get(0));
    return r;
  }
  private void collectRec(Vec<SiTab> tabs, Node n) {
    if (n instanceof WindowSplitNode) {
      for (Node c : n.ch) collectRec(tabs, c);
    } else if (n instanceof TabbedNode) {
      for (Tab t : Vec.of(((TabbedNode) n).getTabs())) tabs.add((SiTab)t);
    }
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
      boolean layoutReadOnly = false;
      Vec<Pair<String,String>> singeliArgs = new Vec<>();
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
            singeliArgs.add(new Pair<>(c, args[i++]));
            break;
          case "--file": save = Paths.get(args[i++]); break;
          case "--runner": runner = Paths.get(args[i++]); break;
          case "--read-layout":
            layoutReadOnly = true;
            // fallthrough
          case "--layout": layout = Paths.get(args[i++]); break;
        }
      }
      
      SiPlayground w = new SiPlayground(
        gc, ctx, gc.getProp("si.ui").gr(),
        args[args.length-2], Paths.get(args[args.length-1]), singeliArgs,
        save, layout, layoutReadOnly, runner);
      mgr.start(w);
    });
  }
  
  public void resized(Surface s) {
    super.resized(s);
    if (varsNode!=null) varsNode.mResize();
  }
  
  private Path prevOpen;
  public boolean key(Key key, int scancode, KeyAction a) {
    switch (gc.keymap(key, a, "si")) {
      case "devtools": createTools(); return true;
      case "run": runAll(); return true;
      case "fontPlus":  gc.setEM(gc.em+1); return true;
      case "fontMinus": gc.setEM(gc.em-1); return true;
      case "open":
        openFile(null, prevOpen, r -> {
          if (r==null) return;
          prevOpen = r;
          SiTab t = allTabs().linearFind(c -> c instanceof SourceTab && ((SourceTab) c).main);
          t.w.o.addSelectedTab(new SourceTab(this, r, false));
        });
        return true;
    }
    return super.key(key, scancode, a);
  }
}