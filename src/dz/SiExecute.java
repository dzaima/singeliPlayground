package dz;

import dzaima.utils.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SiExecute {
  public final SiPlayground r;
  public final Vec<Var> vars;
  public final Path singeliPath, src, execDir;
  public final String bqnExe;
  
  SiExecute(SiPlayground r, Path src) {
    this.r = r;
    this.vars = r.vars.map(x->x);
    
    singeliPath = r.singeliPath;
    this.src = src;
    execDir = r.exec;
    bqnExe = r.bqn;
  }
  
  
  
  AtomicBoolean canceled = new AtomicBoolean(false);
  Thread thread;
  public void start(String code) {
    thread = Tools.thread(() -> {
      exec(code);
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
  
  public void exec(String all) {
    r.toRun.add(r.noteNode::removeAll);
    status("writing file...");
    try {
      Tools.writeFile(src, all);
    } catch (Exception e) {
      System.err.println("Failed to save file:");
      e.printStackTrace();
    }
    status("generating source...");
    build: try {
      int i = all.indexOf("\n⍎\n");
      String init = all.substring(0, i);
      String fn = all.substring(i+3);
      
      StringBuilder mainCode = new StringBuilder("   \n");
      HashSet<String> varSet = new HashSet<>();
      Vec<String> newVarList = new Vec<>();
      StringBuilder tupRes = new StringBuilder();
      StringBuilder siRead = new StringBuilder();
      StringBuilder cInit = new StringBuilder();
      cInit.append("#include <stdint.h>\n");
      cInit.append("#include <stdio.h>\n");
      cInit.append("#define EXEC_G(N) (void*)(&exec_global_##N)\n");
      cInit.append("#define LIT(N) N\n");
      for (Var v : vars) {
        varSet.add(v.name);
        if (tupRes.length()!=0) tupRes.append(", ");
        tupRes.append(v.name);
      
        String ln = "  "+v.name+":= load{emit{*"+v.type()+", 'EXEC_G', '"+v.name+"'}, 0}";
      
        siRead.append(ln).append('\n');
      
      }
      
      // build the main singeli code
      for (String c : Tools.split(fn, '\n')) {
        if (c.startsWith("#arr ")) {
          if (c.length()<7) { note("Bad '#arr' definition"); break build; }
          int szEnd = 5;
          while (szEnd+1<c.length() && c.charAt(szEnd)!=' ') szEnd++;
          String tSi = c.substring(5, szEnd);
          String tC = siToC.get(tSi);
          if (tC==null) { note("Bad '#arr' type"); break build; }
          int nEnd = szEnd+1;
          while (nEnd<c.length() && Character.isUnicodeIdentifierPart(c.charAt(nEnd))) nEnd++;
          String name = c.substring(szEnd+1, nEnd);
          cInit.append(tC).append(' ').append(name).append("[] = {").append(c.substring(nEnd)).append("};\n");
          mainCode.append("  def ").append(name).append(" = emit{*").append(tSi).append(", 'LIT', '").append(name).append("'}\n");
          continue;
        }
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
            if (tupRes.length()!=0) tupRes.append(", ");
            tupRes.append(name);
          }
        
          expr = name+(had?"=":":=")+c.substring(pE+1);
        }
        mainCode.append("  ").append(expr).append('\n');
      }
      String codeStart = init+"\n__exec_fn() : void = {\n"+siRead+mainCode+"\n";
    
      // parse out the types of the variables
      status("generating IR...");
      String[] siIROut = runSi(codeStart+"  __res:=tup{"+tupRes+"}\n  \n}", true);
      System.out.println(siIROut[0]);
      System.out.println(siIROut[1]);
      System.out.println(siIROut[2]);
      if (!siIROut[0].equals("0")) {
        note("Failed to build Singeli:\n");
        note(siIROut[1]);
        break build;
      }
      note(siIROut[1]);
      String[] irLns = Tools.split(siIROut[2], '\n');
      String t = Tools.split(irLns[irLns.length-4], ' ')[3];
      String[] ts = Tools.split(t.substring(1, t.length()-1), ',');
      for (int j = 0; j < newVarList.sz; j++) {
        String name = newVarList.get(j);
        String ty = ts[ts.length-newVarList.sz + j];
        String[] ps = Tools.split(ty.substring(1), ']');
      
        int count = Integer.parseInt(ps[0]);
        char tchr = ps[1].charAt(0);
        VTy vty = ps[1].equals("u1")? VTy.HEX : tchr=='i'? VTy.SIGNED : tchr=='f'? VTy.FLOAT : VTy.HEX;
        int width = Integer.parseInt(ps[1].substring(1));
        vars.add(new Var(r, name, new byte[count*width/8], width, vty));
      }
    
      // generate variable I/O
      String sep = "<singeli playground stdout separator>";
      StringBuilder cWrite = new StringBuilder("  printf(\""+sep+"\");\n");
      StringBuilder siWrites = new StringBuilder();
      boolean first = true;
      for (Var v : vars) {
        String cName = "exec_global_"+v.name;
      
        cInit.append("unsigned char ").append(cName).append('[').append(v.data.length).append("] = {");
        for (int j = 0; j < v.data.length; j++) {
          if (j!=0) cInit.append(",");
          cInit.append(v.data[j]);
        }
        cInit.append("};\n");
      
        String ln = "  store{emit{*"+v.byteType()+", 'EXEC_G', '"+v.name+"'}, 0, emit{"+v.byteType()+", '', "+v.name+"}}";
      
        cWrite.append("  for (int i = 0; i < ").append(v.data.length).append("; i++)")
          .append("printf(\"%d \", (signed char)").append(cName).append("[i]);\n");
        cWrite.append("  putchar('\\n');\n");
      
        siWrites.append(ln).append('\n');
        if (first) first=false;
      }
  
      status("generating C...");
      String[] siCOut = runSi(codeStart+siWrites + "  \n}\n'exec_run'=__exec_fn", false);
      if (!siCOut[0].equals("0")) { note("Failed second singeli build:\n"); note(siCOut[1]); break build; }
      cInit.append(siCOut[2]);
      cInit.append("int main() {\n").append("\n  exec_run();\n").append(cWrite).append("\n}\n");
      Path cFile = execDir.resolve("c.c");
      String outFile = execDir.resolve("a.out").toString();
      Tools.writeFile(cFile, cInit.toString());
  
      status("running C...");
      // invoke cc
      Process cc = Runtime.getRuntime().exec(new String[]{"cc", "-march=native", "-o", outFile, cFile.toString()});
      cc.getInputStream().close();
      byte[] err = cc.getErrorStream().readAllBytes();
      int exit = cc.waitFor();
      if (exit!=0 || err.length>0) {
        note("C compilation "+(exit==0? "warnings" : "errors")+":\n");
        note(new String(err, StandardCharsets.UTF_8));
        if (exit!=0) break build;
      }
  
      status("preparing results...");
      // execute actual thing & read results
      Process exec = Runtime.getRuntime().exec(new String[]{outFile});
      String out = new String(exec.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      String[] outParts = out.split(sep);
      if (outParts.length!=2) {
        note("Bad stdout:\n");
        note(out);
        break build;
      }
      String[] outVars = Tools.split(outParts[1], '\n');
      int ec = exec.waitFor();
      if (ec!=0) {
        note("Execution stopped with exit code "+ec+"\n");
        note(out+"\n");
        note(new String(exec.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        break build;
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
      
    } catch (Exception e) {
      note("Failed building:\n");
      note(e.getMessage());
      e.printStackTrace();
    }
    status(null);
  }
  
  
  public String[] runSi(String src, boolean ir) throws Exception {
    String siFile = singeliPath.toAbsolutePath().toString();
    Path tmpIn = execDir.resolve("tmp.singeli");
    Path tmpOut = execDir.resolve("tmp.out");
    Tools.writeFile(tmpIn, src);
    Process p = Runtime.getRuntime().exec(ir? new String[]{bqnExe, siFile, "-o", tmpOut.toString(), "-t", "ir", tmpIn.toString()}
      : new String[]{bqnExe, siFile, "-o", tmpOut.toString(),             tmpIn.toString()});
    p.getErrorStream().close();
    String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new String[]{Integer.toString(p.waitFor()), out, Tools.readFile(tmpOut)};
  }
  
  
  static HashMap<String,String> siToC = new HashMap<>();
  static {
    siToC.put("i8", "int8_t" );siToC.put("u8", "uint8_t" );
    siToC.put("i16","int16_t");siToC.put("u16","uint16_t");
    siToC.put("i32","int32_t");siToC.put("u32","uint32_t");
    siToC.put("f32","float"  );siToC.put("f64","double"  );
  }
}
