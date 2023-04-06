package dz;

public abstract class SiTab extends dzaima.ui.node.types.tabs.Tab {
  public final SiPlayground r;
  private final String title;
  
  protected SiTab(SiPlayground r, String title) {
    super(r.ctx);
    this.title = title;
    this.r = r;
  }
  
  public String name() {
    return title;
  }
  
  public abstract int mode();
}
