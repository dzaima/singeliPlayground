package dz;

import dzaima.ui.gui.Font;
import dzaima.ui.node.types.editable.code.*;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;
import java.util.regex.Pattern;

public class AsmLang extends Lang {
  public static LangState.Keywords sizes = new LangState.Keywords(
    "ptr","byte","word","dword","qword","tword","mmword","xmmword","ymmword","zmmword"
  );
  
  public static Pattern regs = Pattern.compile("(([re]?(ip|ax|bx|cx|dx|si|di|sp|bp))|[abcd][hl]|(si|di|sp|bp)l|r(8|9|1[0-5])[dwb]?|[cdsefg]s|[xyz]mm([12]?[0-9]|3[01])|[cdt]r[0-9]+)");
  
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff888888, // 1 comment
    0xff7AAFDB, // 2 instruction - add, mov, etc
    0xffCC6666, // 3 QWORD PTR etc
    0xff3DC9B0, // 4 labels
    0xffB5CEA8, // 5 numbers
    0xffCE9178, // 6 strings
    0xff5B73B2, // 7 registers
  };
  public TextStyle[] styles;
  public TextStyle style(byte v) {
    return styles[v];
  }
  
  AsmLang.AsmState init;
  public AsmLang(Font f) {
    super(new AsmLang.AsmState());
    styles = Lang.colors(cols, f);
  }
  public Lang font(Font f) { return new AsmLang(f); }
  public LangState<?> init() { return init; }
  
  
  static class AsmState extends LangState<AsmLang.AsmState> {
    boolean mlc;
    
    public AsmLang.AsmState after(int sz, char[] p, byte[] b) {
      if (sz==0) return this;
      AsmLang.AsmState r = new AsmLang.AsmState();
      r.mlc = mlc;
      r.eval(sz, p, b);
      // r.depthDelta = Math.min(Math.max(Math.max(r.depthDelta, 0), Math.max(r.parenDelta, r.bracketDelta)), 1);
      return r;
    }
    
    public void eval(int sz, char[] s, byte[] r) {
      if (sz==0) return;
      Arrays.fill(r, (byte) -1); r[0] = 0;
      boolean firstWord = true;
      int i = 0;
      while (i < sz) {
        int li = i;
        int c = s[i];
        switch (c) {
          case '#': { r[li] = 1; return; }
          case '\'': case '"':
            r[i++] = 6;
            while (i<sz) {
              if (s[i]==c) break;
              i+= s[i]=='\\'? 2 : 1;
            }
            i++;
            break;
          case '%':
            r[i++] = 7;
            break;
          case '<':
            r[i++] = 6;
            while (i<sz && s[i-1]!='>') i++;
            break;
          case'0':case'1':case'2':case'3':case'4':case'5':case'6':case'7':case'8':case'9':case'.':
            if (c!='.' || i+1>=sz || dig(s[i+1])) {
              r[i++] = 5;
              while (i<sz && ((c!='.' && nameM(s[i])) || s[i]=='.')) i++;
              break;
            }
            // fallthrough
          default:
            if (nameS(c) || c=='.') {
              i++;
              while (i<sz && nameM(s[i])) i++;
              char[] val = Arrays.copyOfRange(s, li, i);
              String str = new String(val);
              byte t = (byte) (firstWord? 2 : 4);
              if (i<sz && s[i]==':') { t = 4; i++; }
              else if (sizes.has(str.toLowerCase().toCharArray())) t = 3;
              else if (regs.matcher(str).matches()) t = 7;
              r[li] = t;
              firstWord = false;
            } else {
              r[i++] = 0;
            }
            break;
        }
      }
    }
    
    public boolean equals(Object o) { return o instanceof AsmLang.AsmState && mlc==((AsmLang.AsmState) o).mlc; }
    public int hashCode() { return mlc?314159265:0; }
  }
  public static boolean ws(int c) { return c==' '|c=='\n'; }
  public static boolean dig(int c) { return c>='0' & c<='9'; }
  public static boolean nameS(int c) { return c>='a' & c<='z'  |  c>='A' & c<='Z'  |  c=='_'; }
  public static boolean nameM(int c) { return nameS(c) || dig(c); }
}
