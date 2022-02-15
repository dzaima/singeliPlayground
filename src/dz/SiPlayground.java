package dz;

import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.types.editable.code.*;
import dzaima.utils.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class SiPlayground extends NodeWindow {
  public Path file = Paths.get("current.singeli");
  public String bqn;
  public Path singeliPath;
  public Path exec = Paths.get("exec/");
  
  private final CodeAreaNode code;
  private final VarList varsNode;
  private final CodeAreaNode noteNode;
  public final Vec<Var> vars = new Vec<>();
  
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
      s+= "\n⍎\n\n";
      s+= "# 'name ← expression' to update watch\n";
      s+= "# 'name:← expression' to set only if name doesn't already exist\n";
      s+= "# variables from a watch can be read as regular variables\n";
      code.append(s+"\na ← emit{[8]i32, '_mm256_set1_epi32', 1}");
    }
    try {
      Files.createDirectories(exec);
    } catch (Exception e) { throw new RuntimeException(e); }
    code.setFn(value -> {
      if (value!=0) run();
      return value!=0;
    });
    code.um.popIgnore(i);
    varsNode = (VarList) base.ctx.id("vars");
    updVars();
  }
  
  public void run() {
    String all = code.getAll();
    try {
      Tools.writeFile(file, all);
    } catch (Exception e) {
      System.err.println("Failed to save file:");
      e.printStackTrace();
    }
    StringBuilder notes = new StringBuilder();
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
          if (c.length()<7) { notes.append("Bad '#arr' definition"); break build; }
          int szEnd = 5;
          while (szEnd+1<c.length() && c.charAt(szEnd)!=' ') szEnd++;
          String tSi = c.substring(5, szEnd);
          String tC = siToC.get(tSi);
          if (tC==null) { notes.append("Bad '#arr' type"); break build; }
          int nEnd = szEnd+1;
          while(nEnd<c.length() && Character.isUnicodeIdentifierPart(c.charAt(nEnd))) nEnd++;
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
      String[] siIROut = runSi(codeStart+"  __res:=tup{"+tupRes+"}\n  \n}", true);
      System.out.println(siIROut[0]);
      System.out.println(siIROut[1]);
      System.out.println(siIROut[2]);
      if (!siIROut[0].equals("0")) {
        notes.append("Failed to build Singeli:\n");
        notes.append(siIROut[1]);
        break build;
      }
      notes.append(siIROut[1]);
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
        vars.add(new Var(this, name, new byte[count*width/8], width, vty));
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
      
      String[] siCOut = runSi(codeStart+siWrites + "  \n}\n'exec_run'=__exec_fn", false);
      if (!siCOut[0].equals("0")) { notes.append("Failed second singeli build:\n"); notes.append(siCOut[1]); break build; }
      cInit.append(siCOut[2]);
      cInit.append("int main() {\n").append("\n  exec_run();\n").append(cWrite).append("\n}\n");
      Path cFile = exec.resolve("c.c");
      String outFile = exec.resolve("a.out").toString();
      Tools.writeFile(cFile, cInit.toString());
      
      // invoke cc
      Process cc = Runtime.getRuntime().exec(new String[]{"cc", "-march=native", "-o", outFile, cFile.toString()});
      cc.getInputStream().close();
      byte[] err = cc.getErrorStream().readAllBytes();
      int exit = cc.waitFor();
      if (exit!=0 || err.length>0) {
        notes.append("C compilation ").append(exit==0? "warnings" : "errors").append(":\n");
        notes.append(new String(err, StandardCharsets.UTF_8));
        if (exit!=0) break build;
      }
      
      // execute actual thing & read results
      Process exec = Runtime.getRuntime().exec(new String[]{outFile});
      String out = new String(exec.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      String[] outParts = out.split(sep);
      if (outParts.length!=2) {
        notes.append("Bad stdout:\n");
        notes.append(out);
        break build;
      }
      String[] outVars = Tools.split(outParts[1], '\n');
      int ec = exec.waitFor();
      if (ec!=0) {
        notes.append("Execution stopped with exit code ").append(ec).append("\n");
        notes.append(out).append('\n');
        notes.append(new String(exec.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        break build;
      }
      for (int j = 0; j < outVars.length-1; j++) {
        Var v = vars.get(j);
        String[] ps = Tools.split(outVars[j], ' ');
        if (ps.length!=v.data.length+1) System.err.println("bad lengths: "+v.data.length+" vs "+ps.length);
        for (int k = 0; k < v.data.length; k++) {
          v.data[k] = Byte.parseByte(ps[k]);
        }
        v.updData();
      }
      updVars();
      notes.append(outParts[0]);
      
    } catch (Exception e) {
      notes.append("Failed building:\n");
      notes.append(e.getMessage());
      e.printStackTrace();
    }
    
    noteNode.removeAll();
    noteNode.append(notes.toString());
    noteNode.um.clear();
  }
  
  public String[] runSi(String src, boolean ir) throws Exception {
    String siFile = singeliPath.toAbsolutePath().toString();
    Path tmpIn = exec.resolve("tmp.singeli");
    Path tmpOut = exec.resolve("tmp.out");
    Tools.writeFile(tmpIn, src);
    Process p = Runtime.getRuntime().exec(ir? new String[]{bqn, siFile, "-o", tmpOut.toString(), "-t", "ir", tmpIn.toString()}
                                            : new String[]{bqn, siFile, "-o", tmpOut.toString(),             tmpIn.toString()});
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
  
  public void updVars() {
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
      GConfig gc = GConfig.newConfig();
      gc.addCfg(() -> Tools.readRes("siPlayground.dzcfg"));
      BaseCtx ctx = Ctx.newCtx();
      ctx.put("varfield", VarField::new);
      ctx.put("varlist", VarList::new);
      SiPlayground w = new SiPlayground(gc, ctx, gc.getProp("si.ui").gr(), args[0], Paths.get(args[1]));
      mgr.start(w);
    });
  }
  
  public void resized() {
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