package dz.tabs;

import dz.*;

public abstract class SiExecTab extends SiTab {
  protected SiExecTab(SiPlayground r) {
    super(r);
  }
  
  public abstract Executer prep(String src, Runnable onDone);
}
