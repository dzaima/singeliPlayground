package dz;

public class SiType {
  public final TyRepr repr;
  public final int primWidth;
  public final int scale;
  private final int count; // -1 for scalar; scaled up
  
  public SiType(TyRepr repr, int count, int primWidth, int scale) {
    this.repr = repr;
    this.count = count;
    this.primWidth = primWidth;
    this.scale = count==-1? 1 : scale;
  }
  
  public int widthBits() {
    return count==-1? primWidth : count*primWidth;
  }
  public int widthBytes() {
    return (widthBits()+7)/8;
  }
  public int elBits() {
    return primWidth;
  }
  
  public int count() { // scaled up
    return isVector()? count : 1;
  }
  public int unscaledCount() {
    return count()/scale;
  }
  
  public boolean scalar() { return count==-1; }
  public boolean isVector() { return count>=0; }
  public boolean isBitVec() { return isVector() && elBits()==1; }
  
  public static SiType from(String str, int scale) {
    int count = -1;
    if (str.startsWith("[")) {
      int e = str.indexOf(']');
      count = Integer.parseInt(str.substring(1, e)) * scale;
      str = str.substring(e+1);
    }
    TyRepr repr;
    switch (str.charAt(0)) {
      default: throw new RuntimeException("Bad type '"+str+"'");
      case 'i': repr = TyRepr.SIGNED; break;
      case 'u': repr = TyRepr.UNSIGNED; break;
      case 'f': repr = TyRepr.FLOAT; break;
    }
    return new SiType(repr, count, Integer.parseInt(str.substring(1)), scale);
  }
  
  public String toSingeli() {
    String e = repr.repr + primWidth;
    return scalar()? e : "["+unscaledCount()+"]"+e;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof SiType)) return false;
    SiType t = (SiType) o;
    return primWidth==t.primWidth && scale==t.scale && count==t.count && repr==t.repr;
  }
}
