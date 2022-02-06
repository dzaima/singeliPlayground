package dz;

public enum VTy {
  SIGNED, UNSIGNED, HEX, FLOAT;
  
  public boolean f() {
    return this == FLOAT;
  }
}
