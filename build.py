#!/usr/bin/env python3
import importlib, subprocess, os

def git_lib(path):
  if os.path.exists(path): return path
  path2 = path+"Clone"
  print("using "+path2+" submodule; link custom path to "+path+" to override")
  subprocess.check_call(["git","submodule","update","--init",path2])
  return path2

uiPath = git_lib("UI")
b = importlib.import_module(uiPath+".build")

cp = b.build_ui_lib(uiPath)
b.jar("playground.jar", cp)
b.make_run("run", cp+":playground.jar", "dz.SiPlayground", "-ea")