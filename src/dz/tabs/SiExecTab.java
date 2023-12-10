package dz.tabs;

import dz.SiPlayground;

public abstract class SiExecTab extends SiTab {
  protected SiExecTab(SiPlayground r) {
    super(r);
  }
  
  public abstract int mode();
}
