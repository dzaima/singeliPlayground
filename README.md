# Singeli playground

![permute example](./files/premute.png)

Evalutes [Singeli](https://github.com/mlochbaum/Singeli) code in an interactive environment with viewable and modifiable vector variable watches

### Usage
1. Clone [Singeli](https://github.com/mlochbaum/Singeli)
2. `./build.py`
3. `./run cbqn path/to/Singeli`

Code before `⍎` appears outside any scope, thus can be imports/exports/generators. After `⍎`, the code will be placed inside a function that reads stored variables and stores updated ones.

ctrl+numpad-plus/ctrl+numpad-minus change interface scale.