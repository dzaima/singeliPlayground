# Singeli playground

### Usage

1. `./build.py` (`./build.py i` for incremental compilation)
2. `./run cbqn path/to/Singeli`

Code before `⍎` appears outside any scope, thus can be imports/exports/generators. After `⍎`, the code will be placed inside a function that reads stored variables and stores updated ones.