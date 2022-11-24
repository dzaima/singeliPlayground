package dz;

public enum VTy {
  SIGNED("i"), UNSIGNED("u"), HEX("x"), BIN("b"), FLOAT("f");
  
  public final String btnName;
  VTy(String btnName) {
    this.btnName = btnName;
  }
  
  public boolean f() {
    return this == FLOAT;
  }
}
