# Singeli playground

![screenshot](https://github.com/dzaima/dzaima.github.io/blob/master/images/singeliPlayground.png)

Evalutes [Singeli](https://github.com/mlochbaum/Singeli) code in an interactive environment with viewable and modifiable vector variable watches.

### Usage
1. Clone [Singeli](https://github.com/mlochbaum/Singeli)
2. `./build.py`
3. `./run cbqn path/to/Singeli`

Watches can be drag&drop reordered and tabs can be reordered & the layout changed (right-click a tab).

Run with ctrl+s or ctrl+enter (or alt+enter or shift+enter). Open external files with ctrl+o.

Assembly & IR tabs automatically recompile upon being opened, but will require manual runs afterwards.

ctrl+plus/ctrl+minus to change interface scale.

### Flags

`--file path/to/file.singeli` changes the primary file being edited and ran.

`--layout path/to/layout.dzcfg` changes the file that stores the layout & configuration of tabs. `--read-layout` can be used to read the layout, but not update it.

Flags `-a`/`--arch`, `-i`/`--infer`, `-l`/`--lib`, `-c`/`--config`, `-p`/`--pre`, `-n`/`--name` are passed on to Singeli invocations.

To configure for running non-native code, the `-a` flag must be specified, and `--runner path/to/runner` must be used.

The runner is invoked as `path/to/runner path/to/input.c path/to/compiler.log path/to/temp/file`, and is expected to:

1. Compile the `input.c` code, writing compiler warnings/errors to `compiler.log`. This can output to the provided temporary file location is desired.
2. Run the compiled code, passing through stdin/stdout/stderr.
3. Provide a non-zero exit code if either failed, or `0` if ran successfully.

Example configurations using QEMU and clang cross-compilation:
```sh
./run --layout layout_aarch64.dzcfg --runner res/qemu_aarch64 -a aarch64 ... # aarch64
./run --layout layout_riscv64.dzcfg --runner res/qemu_rvv128 -a RVV ... # RISC-V RVV, configured with 128-bit vectors
./run --layout layout_riscv64.dzcfg --runner res/qemu_rvv256 -a RVV ... # RISC-V RVV, configured with 256-bit vectors
```