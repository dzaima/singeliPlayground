package dz.tabs;

import dz.*;
import dzaima.ui.node.Node;
import dzaima.utils.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class VarsTab extends SiExecTab {
  private final Node node;
  public final VarList varsNode;
  
  public VarsTab(SiPlayground r) {
    super(r);
    node = ctx.make(r.gc.getProp("si.varsUI").gr());
    varsNode = (VarList) node.ctx.id("vars");
    varsNode.r = r;
  }
  
  public Node show() {
    return node;
  }
  
  public String name() {
    return "variables";
  }
  
  
  public Executer prep(String src, Runnable onDone) {
    Vec<Var> vars = p.vars.map(Var::copy);
    
    return new Executer(p, src, onDone) {
      protected void onThread() throws Exception {
        Preprocessed pre = preprocess(code, vars.map(c -> new Pair<>(c.name, c.type)));
        Executed o = compileSingeli(pre.siMain + pre.siREPL, false);
        note(o.out);
        note(o.err);
        
        Path tc = tmpFile(".c");
        Tools.writeFile(tc, pre.c+"\n"+o.custom);
        Path exe = tmpFile(".out");
        compileC("cc", "-march=native", "-O1", "-o", exe.toString(), tc.toString());
        
        status("Running binary...");
        ByteVec in = new ByteVec();
        for (Var v : vars) in.addAll(v.data);
        Executed t = execCollect(new String[]{exe.toString()}, in.get());
        
        status("Processing output...");
        String begin = "<VAR_SET_BEGIN>";
        byte[] bs = t.errBytes;
        int progress = 0;
        ByteVec realErr = new ByteVec();
        int i = 0;
        
        Vec<Var> sets = new Vec<>();
        while (i < bs.length) {
          byte c = bs[i++];
          realErr.add(c);
          if (c != begin.charAt(progress)) progress = 0;
          else if (begin.length() == ++progress) {
            realErr.remove(realErr.sz-progress, realErr.sz);
            progress = 0;
            int i0 = i;
            for (int j = 0; j < 4; j++) {
              while (bs[i]!=';') i++;
              i++;
            }
            String[] parts = Tools.split(new String(Arrays.copyOfRange(bs, i0, i-1), StandardCharsets.UTF_8), ';');
            String type = parts[0];
            int scale = Integer.parseInt(parts[1]);
            String name = parts[2];
            int bytes = Integer.parseInt(parts[3]);
            byte[] data = Arrays.copyOfRange(bs, i, i + bytes);
            sets.add(new Var(r, name, data, SiType.from(type, scale)));
            i+= bytes;
          }
        }
        
        note(t.out);
        note(new String(realErr.get(), StandardCharsets.UTF_8));
        
        p.toRun.add(() -> {
          HashMap<String, Integer> m = new HashMap<>();
          Vec<Var> vars = p.vars;
          for (int j = 0; j < vars.sz; j++) m.put(vars.get(j).name, j);
          for (Var c : sets) {
            if (m.containsKey(c.name)) {
              vars.set(m.get(c.name), c);
            } else {
              m.put(c.name, vars.size());
              vars.add(c);
            }
          }
          if (sets.sz>0) p.updVars();
        });
      }
    };
  }
}
