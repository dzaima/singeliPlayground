package dz;

public enum TyRepr {
  SIGNED("i", "i"),
  UNSIGNED("u", "u"),
  HEX("x", "u"),
  BIN("b", "u"),
  MASK("m", "u"),
  FLOAT("f", "f");
  
  public final String fmt, qual;
  TyRepr(String fmt, String qual) {
    this.fmt = fmt;
    this.qual = qual;
  }
  
  public boolean f() { return this == FLOAT; }
  public boolean bits() {
    return this==BIN || this==MASK;
  }
}
