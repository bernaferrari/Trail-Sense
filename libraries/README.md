# Shared libraries

Sol, Andromeda, and Luna are included as Git subtrees. Trail Sense builds against these directories directly. Each subtree retains its upstream history and license.

To bring in new upstream changes, start with a clean working tree and run the relevant command from the repository root:

```sh
git subtree pull --prefix=libraries/sol https://github.com/kylecorry31/sol.git main
git subtree pull --prefix=libraries/andromeda https://github.com/kylecorry31/andromeda.git main
git subtree pull --prefix=libraries/luna https://github.com/kylecorry31/luna.git main
```

Resolve conflicts against the multiplatform code and source-set layout, then build Trail Sense and run the relevant library tests. Upstream code in JVM or Android source sets may need to move to `commonMain` or a shared JVM source set. The three subtrees are independent: sync only the libraries whose changes you intend to integrate.
