package dz;

import dzaima.utils.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.regex.*;

public abstract class Executer {
  protected SiPlayground r;
  protected String bqnExe, siExe;
  protected String[] siArgs;
  
  protected AtomicBoolean canceled = new AtomicBoolean(false);
  protected String code;
  
  private final Path tmpDir;
  private final Runnable onDone;
  private final Vec<Process> processes = new Vec<>();
  private final Lock l = new ReentrantLock();
  private Thread thread;
  
  protected Executer(SiPlayground r, String code, Runnable onDone) {
    this.r = r;
    this.tmpDir = r.exec;
    this.bqnExe = r.bqn;
    this.siExe = r.singeliPath.toAbsolutePath().toString();
    this.siArgs = r.singeliArgs;
    this.code = code;
    this.onDone = onDone;
  }
  
  public void start() {
    thread = Tools.thread(() -> {
      try {
        status("writing file...");
        onThread();
      } catch (ExpException e) {
        if (e.getMessage()!=null) note(e.getMessage());
      } catch (Throwable e) {
        note("Error:\n");
        note(e.getMessage());
        Log.stacktrace("runner", e);
      }
      status(null);
      finishFiles();
      r.toRun.add(onDone);
    });
  }
  public void cancel() {
    thread.interrupt();
    l.lock();
    canceled.set(true);
    for (Process c : processes) c.destroyForcibly();
    l.unlock();
    finishFiles();
  }
  public void finishFiles() {
    for (Path p : files) {
      try {
        Path dest = p.getParent().resolve(p.getFileName().toString().replaceAll("^tmp_.*\\.", "last."));
        if (Files.exists(p)) Files.move(p, dest, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        Log.stacktrace("cleanup", e);
      }
    }
  }
  
  
  
  protected abstract void onThread() throws Exception;
  
  
  
  protected static class Preprocessed {
    public final String c, siMain, siREPL;
    public final Vec<String> vars;
    protected Preprocessed(String c, String siMain, String siREPL, Vec<String> vars) {
      this.c=c; this.siMain=siMain; this.siREPL=siREPL;
      this.vars = vars;
    }
  }
  
  private static final Pattern P_ASGN = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*) *(:?) *([←↩])|'[^']*'");
  
  protected String siQuote(String s) {
    return "'"+s+"'";
  }
  protected Preprocessed preprocess(String siCode, Vec<Pair<String,SiType>> prevVars) {
    HashSet<String> prevMap = new HashSet<>();
    for (Pair<String,SiType> c : prevVars) prevMap.add(c.a);
    Vec<String> newVars = prevVars.map(c -> c.a);
    HashMap<String,Integer> newMap = new HashMap<>();
    for (int i = 0; i < prevVars.sz; i++) newMap.put(prevVars.get(i).a, i);
    
    StringBuilder c = new StringBuilder();
    c.append("#include<stdio.h>\n");
    StringBuilder siMain = new StringBuilder();
    StringBuilder siREPL = new StringBuilder();
    boolean inREPL = false;
    for (String l : Tools.split(siCode, '\n')) {
      if (l.startsWith("cinit ")) {
        c.append(l.substring(6));
      } else if (l.equals("⍎")) {
        if (inREPL) throw new ExpException("Multiple ⍎s found");
        inREPL = true;
        siREPL.append("include ").append(siQuote(Tools.RES_DIR.resolve("replPrep").toAbsolutePath().toString())).append("; main:i32 = {");
        for (Pair<String, SiType> p : prevVars) {
          siREPL.append(p.a).append(":=_playground_load{").append(p.b.toSingeli()).append("};");
        }
      } else if (!inREPL) {
        siMain.append(l);
      } else {
        Matcher m = P_ASGN.matcher(l);
        while (m.find()) {
          String name = m.group(1);
          if (name==null) {
            m.appendReplacement(siREPL, m.group());
            continue;
          }
          boolean onlyFirst = m.group(2).equals(":");
          boolean mod = m.group(3).equals("↩");
          if (onlyFirst && mod) throw new ExpException("Cannot use 'name :↩ expr'");
          boolean isNew = !prevMap.contains(name);
          
          newMap.computeIfAbsent(name, n1 -> {
            newVars.add(name);
            return newMap.size();
          });
          
          m.appendReplacement(siREPL,
            onlyFirst && !isNew? "@_playground_dontRun("+name+") " :
            mod || !isNew? "@_playground_varUpd(tup{"+name+","+siQuote(name)+"}) " :
            name+" := @_playground_varSet("+siQuote(name)+") "
          );
        }
        m.appendTail(siREPL);
      }
      (inREPL? siREPL : siMain).append('\n');
    }
    siREPL.append("0}");
    
    return new Preprocessed(c.toString(), siMain.toString(), siREPL.toString(), newVars);
  }
  
  
  
  protected void notCanceled() {
    if (canceled.get()) throw new Tools.QInterruptedException();
  }
  
  protected Process exec(String[] args) throws IOException {
    l.lock();
    notCanceled();
    Process r = Runtime.getRuntime().exec(args);
    processes.add(r);
    l.unlock();
    return r;
  }
  
  
  
  private String status = null;
  private final StringBuilder notes = new StringBuilder();
  private void updNoteNode() { // executed on main thread
    if (canceled.get()) return;
    r.noteNode.removeAll();
    if (status!=null) r.noteNode.append(status+"\n");
    r.noteNode.append(notes.toString());
    r.noteNode.um.clear();
  }
  protected void status(String s) {
    notCanceled();
    this.status = s;
    r.toRun.add(this::updNoteNode);
  }
  protected void note(String s) {
    notCanceled();
    notes.append(s);
    r.toRun.add(this::updNoteNode);
  }
  
  
  
  private static final AtomicInteger CTR = new AtomicInteger();
  private final Vec<Path> files = new Vec<>();
  protected Path tmpFile(String name) {
    l.lock();
    notCanceled();
    char[] chars = Integer.toString(CTR.getAndIncrement()).toCharArray();
    for (int i = 0; i < chars.length; i++) chars[i] = (char) (chars[i]-'0'+'a');
    Path p = tmpDir.resolve("tmp_" + new String(chars) + name).toAbsolutePath();
    files.add(p);
    l.unlock();
    return p;
  }
  
  protected static class Executed {
    public int code;
    public String out, err, custom;
    public byte[] outBytes, errBytes;
  }
  protected Executed execCollect(String[] cmd, byte[] stdin) throws Exception {
    Process p = exec(cmd);
    OutputStream o = p.getOutputStream();
    o.write(stdin);
    o.flush();
    Executed r = new Executed();
    r.outBytes = p.getInputStream().readAllBytes();
    r.errBytes = p.getErrorStream().readAllBytes();
    r.out = new String(r.outBytes, StandardCharsets.UTF_8);
    r.err = new String(r.errBytes, StandardCharsets.UTF_8);
    r.code = p.waitFor();
    return r;
  }
  
  protected Executed compileSingeli(String src, boolean ir) throws Exception { // throws if failed
    status("Compiling singeli...");
    Path tmpIn = tmpFile(".singeli");
    Path tmpOut = tmpFile(".c");
    Tools.writeFile(tmpIn, src);
    
    Vec<String> cmd = new Vec<>();
    cmd.add(bqnExe);
    cmd.add(siExe);
    for (String c : siArgs) cmd.add(c);
    cmd.add("-o");
    cmd.add(tmpOut.toString());
    if (ir) {
      cmd.add("-t");
      cmd.add("ir");
    }
    cmd.add(tmpIn.toString());
    Executed r = execCollect(cmd.toArray(new String[0]), new byte[0]);
    if (r.code!=0) {
      note("Failed to compile Singeli code:\n");
      note(r.out);
      note(r.err);
      throw new ExpException();
    }
    r.custom = Tools.readFile(tmpOut);
    return r;
  }
  
  protected String compileSingeliMain(String src, boolean ir) throws Exception {
    Preprocessed pre = preprocess(src, new Vec<>());
    Executed o = compileSingeli(pre.siMain, ir);
    note(o.out);
    note(o.err);
    return ir? o.custom : pre.c + "\n" + o.custom;
  }
  
  protected void compileC(String[] init, String... more) throws Exception {
    String[] tot = new String[init.length+more.length];
    System.arraycopy(init, 0, tot, 0, init.length);
    System.arraycopy(more, 0, tot, init.length, more.length);
    compileC(tot);
  }
  protected void compileC(String... cmd) throws Exception {
    status("Compiling C...");
    Process cc = exec(cmd);
    cc.getInputStream().close();
    byte[] err = cc.getErrorStream().readAllBytes();
    
    int exit = cc.waitFor();
    if (exit!=0 || err.length>0) {
      note("C compilation "+(exit==0? "warnings" : "errors")+":\n");
      note(new String(err, StandardCharsets.UTF_8));
      if (exit!=0) throw new ExpException();
    }
  }
  
  
  protected static class ExpException extends RuntimeException {
    public ExpException(String msg) { super(msg); }
    public ExpException() { super(); }
  }
}
