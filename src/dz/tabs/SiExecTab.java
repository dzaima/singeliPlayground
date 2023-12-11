package dz.tabs;

import dz.*;

public abstract class SiExecTab extends SiTab {
  protected SiExecTab(SiPlayground p) {
    super(p);
  }
  
  protected void onShow() {
    p.tabs.add(this);
  }
  public void hide() {
    p.tabs.remove(this);
  }
  
  public abstract Executer prep(String src, Runnable onDone);
}
