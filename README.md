
[![GitHub Release](https://img.shields.io/github/release/codemonstur/bobplugin-keepass.svg)](https://github.com/codemonstur/bobplugin-keepass/releases) 
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

# keepass bob plugin

A plugin for [bob](https://github.com/codemonstur/bobthebuildtool) that reads the entries of a
[KeePass](https://keepass.info/) archive into the build environment, so your tasks and templates can
use secrets (passwords, API keys, tokens) without committing them to the repository.

Each archive entry becomes an environment variable: the entry's **username** is the variable name and
its **password** is the value. Those variables can then be referenced from any bob task.

### Features

- Loads every entry in the archive, including entries nested in groups
- Skips the recycle bin, so deleted credentials never end up in your environment
- Entries without a username are ignored; entries without a password become an empty string
- Optional `-u` flag uppercases usernames, producing conventional `SCREAMING_SNAKE_CASE` variable names
- Master password can be supplied directly or sourced from an environment variable
- Relative archive paths resolve against the project root, not the current working directory

### Requirements

- Java 11 or newer
- bob 7 or newer (the plugin calls `requireBobVersion("7")`)

### Installation

Add this line to the top of your bobfile:

```
plugin codemonstur/keepass-bobplugin:4
```

The `keepass` command is then available in all your bob tasks.

### Usage

The `keepass` command takes the following options:

| Option | Long form | Required | Description |
| ------ | --------- | -------- | ----------- |
| `-f`   | `--file`  | yes      | Path to the KeePass (`.kdbx`) archive. Relative paths resolve against the project root. |
| `-p`   | `--password` | no   | Master password for the archive. If omitted, the `KEEPASS_MASTER_PASSWORD` environment variable is used. |
| `-u`   | `--uppercase` | no  | Uppercase the usernames when creating environment variables. |

For example, given an archive with an entry whose username is `db_password` and password is `s3cret`,
load it and use it in a task:

```
deploy
    keepass -f secrets/deploy.kdbx -u
    echo "Deploying with password ${DB_PASSWORD}"
```

Because `-u` was passed, `db_password` becomes `DB_PASSWORD`. Without `-u` the variable would be
referenced as `${db_password}`.

#### Supplying the master password

Rather than passing `-p` on the command line (which can leak the password into shell history and
process listings), you can export it before running bob:

```
export KEEPASS_MASTER_PASSWORD='the-master-password'
bob deploy
```

The command fails with a clear error if no password is available from either source.

### Behaviour

- The archive must exist and be a regular file, otherwise the command errors out.
- Entries are collected recursively from all groups. The group identified as the recycle bin (via the
  archive metadata) is skipped entirely.
- Duplicate usernames are allowed; the last entry read wins.
- Loading happens once per invocation, populating the environment for the remainder of the task.

### Building

The project builds with [Maven](https://maven.apache.org/). From the project root:

```
mvn package
```

Or use the `Makefile`:

```
make build      # clean and build
make test       # run the unit tests
make coverage   # run tests and generate a JaCoCo coverage report
make validate   # check dependency versions, CVEs and licenses
```

Building from source uses bob and expects a checkout of
[bobthebuildtool](https://github.com/codemonstur/bobthebuildtool) as a sibling directory; it is used
as a `provided` dependency for the bob plugin integration.

### License

MIT — see [LICENSE](LICENSE).
