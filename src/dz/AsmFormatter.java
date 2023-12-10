package dz;

import dzaima.utils.Tools;

import java.util.*;
import java.util.regex.*;

public class AsmFormatter {
  public static Pattern labelPattern = Pattern.compile("^(.?[a-zA-Z0-9_$]+):");
  public static Pattern endProc = Pattern.compile("^(\\s*(\\.cfi_endproc|\\.Lfunc_end))");
  public static Pattern endAll = Pattern.compile("^(\\s*(\\.ident))");
  public static Pattern ignored = Pattern.compile("^(#|\t\\.(section|p2align|text|type|size|globl|addrsig|addrsig_sym|intel_syntax|file|cfi_.+)\\b|\\s*# kill:)");
  
  public static String formatAsm(String c, String asm) {
    HashMap<String, String> nameMap = new HashMap<>();
    HashSet<String> siExports = new HashSet<>();
    for (String l : Tools.split(c, '\n')) {
      if (!l.startsWith(" ") && l.contains("(*const")) {
        String[] ps = l.split("\\*const |\\) = |\\)|;");
        nameMap.put(ps[3], ps[1]);
        siExports.add(ps[1]);
      }
    }
    
    boolean output = false;
    StringBuilder res = new StringBuilder();
    for (String l : Tools.split(asm, '\n')) {
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
      
      if (endAll.matcher(l).find()) break;
      if (endProc.matcher(l).find()) output = false;
      if (!output || ignored.matcher(l).find()) continue;
      
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
    return res.toString();
  }
}
