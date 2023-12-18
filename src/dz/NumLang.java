package dz;


import dzaima.ui.gui.Font;
import dzaima.ui.node.types.editable.code.*;
import io.github.humbleui.skija.paragraph.TextStyle;

import java.util.Arrays;

public class NumLang extends Lang {
  public static int[] cols = new int[]{
    0xffD2D2D2, // 0 default
    0xff777777, // 1 leading zero
  };
  public NumLang() {
    super(new NumState());
  }
  protected TextStyle[] genStyles(Font f) {
    return Lang.colors(cols, f);
  }
  
  private static class NumState extends LangState<NumState> {
    public NumState after(int sz, char[] p, byte[] b) {
      if (sz==0) return this;
      boolean isFloat = false;
      for (int i = 0; i < sz; i++) {
        if (p[i] == '.') { isFloat = true; break; }
      }
      if (isFloat) {
        Arrays.fill(b, 0, sz, (byte) 0);
      } else {
        int i = 0;
        if (sz>=2 && p[0]=='0' && (p[1]=='b' || p[1]=='x')) i+= 2;
        byte c = 1;
        while (i < sz) {
          if (p[i]!='0' && p[i]!='_') c = 0;
          b[i] = c;
          i++;
        }
      }
      return this;
    }
    public boolean equals(Object obj) { return this==obj; }
    public int hashCode() { return 0; }
  }
}
