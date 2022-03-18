package dz;

import dzaima.utils.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.*;

public class SiExecute {
  public final SiPlayground r;
  public final Vec<Var> vars;
  public final Path singeliPath, srcPath, execDir;
  public final String bqnExe;
  public final String ccExe = "cc";
  public final String[] ccFlags;
  int mode;
  
  SiExecute(SiPlayground r, Path src, int mode) {
    this.r = r;
    this.vars = r.vars.map(x->x);
    
    singeliPath = r.singeliPath;
    srcPath = src;
    execDir = r.exec;
    bqnExe = r.bqn;
    ccFlags = Tools.split(r.asmCCFlags.getAll(), ' ');
    this.mode = mode;
  }
  
  
  
  AtomicBoolean canceled = new AtomicBoolean(false);
  Thread thread;
  public void start(String code) {
    r.noteNode.removeAll();
    thread = Tools.thread(() -> {
      try {
        status("writing file...");
        Tools.writeFile(srcPath, code);
        exec(code);
      } catch (Throwable e) {
        note("Failed:\n");
        note(e.getMessage());
        e.printStackTrace();
      }
      status(null);
    });
  }
  public void cancel() {
    thread.interrupt();
    canceled.set(true);
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
  public void note(String s) {
    if (canceled.get()) throw new Tools.QInterruptedException();
    notes.append(s);
    r.toRun.add(this::updNoteNode);
  }
  public void status(String s) {
    if (canceled.get()) throw new Tools.QInterruptedException();
    this.status = s;
    r.toRun.add(this::updNoteNode);
  }
  
  public void exec(String code) throws Throwable {
    StringBuilder defsC = new StringBuilder();
    StringBuilder defsSi = new StringBuilder();
    StringBuilder initSi = new StringBuilder();
    StringBuilder bodySi = new StringBuilder();
    StringBuilder curr = initSi;
    defsC.append("#include <stdint.h>\n");
    defsC.append("#include <stdio.h>\n");
    defsC.append("#include <string.h>\n");
    defsC.append("#define EXEC_G(N) (void*)(&exec_global_##N)\n");
    for (String l : Tools.split(code, '\n')) {
      if (l.startsWith("cinit ")) {
        defsC.append(l.substring(6)).append('\n');
      } else if (l.startsWith("defarr ")) {
        if (l.length()<9) { note("Bad '#arr' definition"); return; }
        int szEnd = 7;
        while (szEnd+1<l.length() && l.charAt(szEnd)!=' ') szEnd++;
        String tSi = l.substring(7, szEnd);
        String tC = siToC.get(tSi);
        if (tC==null) { note("Bad 'defarr' type"); return; }
        int nEnd = szEnd+1;
        while (nEnd<l.length() && Character.isUnicodeIdentifierPart(l.charAt(nEnd))) nEnd++;
        String name = l.substring(szEnd+1, nEnd);
        defsC.append(tC).append(' ').append(name).append("[] = {").append(l.substring(nEnd)).append("};\n");
        defsSi.append("  def ").append(name).append(" = emit{*").append(tSi).append(", '', '").append(name).append("'}\n");
      } else if (l.equals("⍎")) {
        curr = bodySi;
      } else {
        curr.append(l).append('\n');
      }
    }
    if (mode==0) execVars(defsC.toString(), defsSi.toString(), initSi.toString(), bodySi.toString());
    else if (mode==1) execAsm(defsC.toString(), defsSi.toString(), initSi.toString(), bodySi.toString());
    else note("Unknown mode!");
  }
  
  public static Pattern labelPattern = Pattern.compile("^(.?[a-zA-Z0-9_$]+):");
  public static Pattern ignored = Pattern.compile("^(#|\t\\.(section|p2align|cfi_startproc|text|type|size|globl|addrsig|addrsig_sym|intel_syntax|file|cfi_def_cfa_offset|cfi_offset)\\b|\\s*# kill:)");
  public void execAsm(String defsC, String defsSi, String init, String body) throws Exception {
    status("generating C...");
    String[] siOut = runSi(init, false);
    if (!siOut[0].equals("0")) {
      note("Failed to build Singeli:\n");
      note(siOut[1]);
      return;
    }
    note(siOut[1]);
    
    status("compiling C...");
    Path cFile = execDir.resolve("c.c");
    Path outFile = execDir.resolve("c.s");
    Tools.writeFile(cFile, defsC+siOut[2]);
    String[] cmd = new String[5+ccFlags.length];
    cmd[0] = ccExe;
    cmd[1] = "-S";
    cmd[2] = "-o";
    cmd[3] = outFile.toString();
    System.arraycopy(ccFlags, 0, cmd, 4, ccFlags.length);
    cmd[cmd.length-1] = cFile.toString();
    if (!cc(cmd)) return;
    
    HashMap<String, String> nameMap = new HashMap<>();
    HashSet<String> siExports = new HashSet<>();
    for (String l : Tools.split(siOut[2], '\n')) {
      if (!l.startsWith(" ") && l.contains("(*")) {
        String[] ps = l.split("\\*const |\\) = |\\)|;");
        nameMap.put(ps[3], ps[1]);
        siExports.add(ps[1]);
      }
    }
    
    boolean output = false;
    StringBuilder res = new StringBuilder();
    for (String l : Tools.split(Tools.readFile(outFile), '\n')) {
      // replace generated labels with exported ones
      Matcher lm = labelPattern.matcher(l);
      if (lm.find()) {
        output = true;
        String g1 = lm.group(1);
        String m = nameMap.get(g1);
        if (m!=null) {
          if (res.length()>0) res.append('\n');
          l = m+": ################################";
        } else if (siExports.contains(g1)) { // don't show singeli-generated exports
          output = false;
        }
      }
      
      if (ignored.matcher(l).find()) continue;
      if (l.startsWith(".Lfunc_end")) output = false; // asm boilerplate
      
      if (!output) continue;
      // expand tabs to spaces
      String[] ps = Tools.split(l, '\t');
      int len = 0;
      for (int i = 0; i < ps.length; i++) {
        if (i>0) {
          int added = Math.max(1, 10*i-4 - len);
          len+= added;
          res.append(" ".repeat(added));
        }
        res.append(ps[i]);
        len+= ps[i].length();
      }
      res.append('\n');
    }
    String resStr = res.toString();
    r.toRun.add(() -> {
      r.asmArea.removeAll();
      r.asmArea.append(resStr);
    });
    
  }
  
  public void execVars(String defsC, String defsSi, String init, String body) throws Throwable {
    status("generating source...");
    String sep = "<singeli playground stdout separator>";
    
    StringBuilder mainCode = new StringBuilder("   \n");
    HashSet<String> varSet = new HashSet<>();
    Vec<String> newVarList = new Vec<>();
    StringBuilder showType = new StringBuilder();
    StringBuilder siRead = new StringBuilder();
    StringBuilder cInit = new StringBuilder();
    cInit.append(defsC);
    for (Var v : vars) {
      String name = v.name;
      varSet.add(name);
      showType.append("show{type{").append(name).append("}};");
      
      String ln = "  "+name+":= load{emit{*"+v.type()+", 'EXEC_G', '"+name+"'}, 0}";
      
      siRead.append(ln).append('\n');
      
    }
    
    // build the main singeli code
    mainCode.append(defsSi);
    for (String c : Tools.split(body, '\n')) {
      int pE = c.indexOf('←');
      String expr = c;
      def: if (pE!=-1) {
        int pS = pE;
        boolean onlyInit = pS>0 && c.charAt(pS-1)==':';
        if (onlyInit) pS--;
        while (pS>0 && c.charAt(pS-1)==' ') pS--;
        String name = c.substring(0, pS);
        for (int j = 0; j < name.length(); j++) if (!Character.isUnicodeIdentifierPart(name.charAt(j))) break def;
        boolean had = varSet.contains(name);
        if (had && onlyInit) continue;
        if (!had) {
          varSet.add(newVarList.add(name));
          showType.append("show{type{").append(name).append("}};");
        }
        
        expr = name+(had?"=":":=")+c.substring(pE+1);
      }
      mainCode.append("  ").append(expr).append('\n');
    }
    String codeStart = init+"\n__exec_fn() : void = {\n"+siRead+mainCode+"\n";
    
    // parse out the types of the variables
    status("generating IR...");
    String[] siIROut = runSi(codeStart+"  show{'"+sep+"'};"+showType+"\n  \n}", true);
    // System.out.println(siIROut[0]);
    // System.out.println(siIROut[1]);
    // System.out.println(siIROut[2]);
    String out0 = siIROut[1];
    if (!siIROut[0].equals("0")) {
      note("Failed to build Singeli:\n");
      note(out0);
      return;
    }
    String[] out0Parts = out0.split(sep+"\n", -1);
    note(out0Parts[0]);
    String[] ts = Tools.split(out0Parts[1], '\n');
    for (int j = 0; j < newVarList.sz; j++) {
      String name = newVarList.get(j);
      String ty = ts[ts.length-1 - newVarList.sz + j];
      String[] ps = Tools.split(ty, ']');
      
      int count;
      boolean scalar;
      String elt;
      if (ps.length == 1) {
        scalar = true;
        count = 1;
        elt = ps[0];
      } else {
        scalar = false;
        count = Integer.parseInt(ps[0].substring(1));
        elt = ps[1];
      }
      char tchr = elt.charAt(0);
      VTy vty = elt.equals("u1")? VTy.HEX : tchr=='i'? VTy.SIGNED : tchr=='f'? VTy.FLOAT : VTy.HEX;
      int width = Integer.parseInt(elt.substring(1));
      vars.add(new Var(r, name, new byte[count*width/8], width, vty, scalar));
    }
    
    // generate variable I/O
    StringBuilder cWrite = new StringBuilder("  printf(\""+sep+"\");\n");
    StringBuilder siWrites = new StringBuilder();
    siWrites.append("  def store_n{p,n,v} = emit{void, 'memcpy', p, v, n}\n");
    boolean first = true;
    for (Var v : vars) {
      String cName = "exec_global_"+v.name;
      
      cInit.append("unsigned char ").append(cName).append('[').append(v.data.length).append("] = {");
      for (int j = 0; j < v.data.length; j++) {
        if (j!=0) cInit.append(",");
        cInit.append(v.data[j]);
      }
      cInit.append("};\n");
      
      String ln;
      if (v.scalar) {
        ln = "  store_n{emit{*u8, 'EXEC_G', '"+v.name+"'}, "+v.data.length+", emit{*void, '&', "+v.name+"}}";
      } else {
        ln = "  store{emit{*"+v.byteType()+", 'EXEC_G', '"+v.name+"'}, 0, emit{"+v.byteType()+", '', "+v.name+"}}";
      }
      
      cWrite.append("  for (int i = 0; i < ").append(v.data.length).append("; i++)")
        .append("printf(\"%d \", (signed char)").append(cName).append("[i]);\n");
      cWrite.append("  putchar('\\n');\n");
      
      siWrites.append(ln).append('\n');
      if (first) first=false;
    }
    
    status("generating C...");
    String[] siCOut = runSi(codeStart+siWrites + "  \n}\n'exec_run'=__exec_fn", false);
    if (!siCOut[0].equals("0")) { note("Failed second singeli build:\n"); note(siCOut[1]+"\n"); note(siCOut[3]+"\n"); return; }
    cInit.append(siCOut[2]);
    cInit.append("int main() {\n").append("\n  exec_run();\n").append(cWrite).append("\n}\n");
    Path cFile = execDir.resolve("c.c");
    String outFile = execDir.resolve("a.out").toString();
    Tools.writeFile(cFile, cInit.toString());
    
    status("compiling C...");
    // invoke cc
    if (!cc(new String[]{ccExe, "-march=native", "-o", outFile, cFile.toString()})) return;
  
    status("executing C...");
    // execute actual thing & read results
    Process exec = Runtime.getRuntime().exec(new String[]{outFile});
    String out = new String(exec.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String[] outParts = out.split(sep,-1);
    if (outParts.length!=2) {
      note("Bad stdout:\n");
      note(out);
      return;
    }
    String[] outVars = Tools.split(outParts[1], '\n');
    int ec = exec.waitFor();
    if (ec!=0) {
      note("Execution stopped with exit code "+ec+"\n");
      note(out+"\n");
      note(new String(exec.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
      return;
    }
    r.toRun.add(() -> {
      for (int j = 0; j < outVars.length-1; j++) {
        Var v = vars.get(j);
        String[] ps = Tools.split(outVars[j], ' ');
        if (ps.length!=v.data.length+1) System.err.println("bad lengths: "+v.data.length+" vs "+ps.length);
        for (int k = 0; k < v.data.length; k++) {
          v.data[k] = Byte.parseByte(ps[k]);
        }
        v.updData();
      }
      r.vars.clear();
      r.vars.addAll(0, vars);
      r.updVars();
    });
    note(outParts[0]);
  }
  
  private boolean cc(String[] cmd) throws IOException, InterruptedException {
    Process cc = Runtime.getRuntime().exec(cmd);
    cc.getInputStream().close();
    byte[] err = cc.getErrorStream().readAllBytes();
    int exit = cc.waitFor();
    if (exit!=0 || err.length>0) {
      note("C compilation "+(exit==0? "warnings" : "errors")+":\n");
      note(new String(err, StandardCharsets.UTF_8));
      return exit==0;
    }
    return true;
  }
  
  
  public String[] runSi(String src, boolean ir) throws Exception {
    String siFile = singeliPath.toAbsolutePath().toString();
    Path tmpIn = execDir.resolve("tmp.singeli");
    Path tmpOut = execDir.resolve("tmp.out");
    Tools.writeFile(tmpIn, src);
    Process p = Runtime.getRuntime().exec(ir? new String[]{bqnExe, siFile, "-o", tmpOut.toString(), "-t", "ir", tmpIn.toString()}
      : new String[]{bqnExe, siFile, "-o", tmpOut.toString(),             tmpIn.toString()});
    String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    return new String[]{Integer.toString(p.waitFor()), out, Tools.readFile(tmpOut), err};
  }
  
  
  static HashMap<String,String> siToC = new HashMap<>();
  static {
    siToC.put("i8", "int8_t" );siToC.put("u8", "uint8_t" );
    siToC.put("i16","int16_t");siToC.put("u16","uint16_t");
    siToC.put("i32","int32_t");siToC.put("u32","uint32_t");
    siToC.put("i64","int64_t");siToC.put("u64","uint64_t");
    siToC.put("f32","float"  );siToC.put("f64","double"  );
  }
}
