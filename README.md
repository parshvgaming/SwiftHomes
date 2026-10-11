# SwiftHomes

Premium home GUI plugin for Paper 1.21.x / Java 21.

## Commands
- `/sethome <name>` — set/update a named home
- `/home` or `/homes` — open the homes GUI
- `/home <name>` — teleport directly to a home
- `/delhome <name>` — open the confirmation menu for a home
- `/swifthomes reload` — reload configuration (admin)

## LuckPerms permissions
- `swifthomes.use` — access homes commands and GUI
- `swifthomes.home.1` — one home
- `swifthomes.home.2` — two homes
- `swifthomes.home.3` — three homes, and so on
- `swifthomes.admin` — bypass limits and reload config

Example:
```
lp group default permission set swifthomes.use true
lp group default permission set swifthomes.home.1 true
lp group vip permission set swifthomes.home.3 true
lp group mvp permission set swifthomes.home.5 true
```

The plugin checks permission nodes up to `permission-check-limit` in `config.yml` and uses the highest granted number. Existing homes are saved in `plugins/SwiftHomes/homes.yml` by UUID.

## Build
Use Java 21 and Maven with Paper API 1.21.11. The GitHub Actions workflow builds in the cloud, so Maven does not need to be installed on your PC. Run `mvn package`, then copy `target/SwiftHomes-1.0.0.jar` to `plugins/`.


## Version target and testing status

This cloud-build package targets the Paper 1.21.11 API and Java 21, intended for Paper 1.21.x servers. It has not yet been built or tested in this environment; do not assume compatibility with every 1.21 patch until the GitHub Actions build succeeds and the resulting JAR is tested on a staging server. Right-clicking a saved bed opens the delete confirmation screen; left-clicking teleports.
