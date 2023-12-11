package dz.tabs;

import dz.*;

public abstract class SiExecTab extends SiTab {
  protected SiExecTab(SiPlayground p) {
    super(p);
  }
  
  protected void onShow() {
    p.openTabs.add(this);
  }
  public void hide() {
    p.openTabs.remove(this);
  }
  
  public abstract Executer prep(String src, Runnable onDone);
}
