package dz;

public enum TyRepr {
  SIGNED("i"), UNSIGNED("u"), HEX("x"), BIN("b"), FLOAT("f");
  
  public final String repr;
  TyRepr(String repr) {
    this.repr = repr;
  }
  
  public boolean f() {
    return this == FLOAT;
  }
}
