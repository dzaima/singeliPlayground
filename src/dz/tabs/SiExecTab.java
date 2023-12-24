package dz.tabs;

import dz.*;

public abstract class SiExecTab extends SiTab implements SiPlayground.ExecuterKey {
  protected SiExecTab(SiPlayground p) {
    super(p);
  }
  
  public void onShown() {
    p.openTabs.add(this);
  }
  public void onHidden() {
    p.openTabs.remove(this);
  }
  
  public abstract Executer prep(String src, Runnable onDone);
}
