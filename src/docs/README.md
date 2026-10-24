# bobplugin-keepass

A [bob](https://github.com/codemonstur/bobthebuildtool) plugin that loads build
variables from a KeePass password archive, so secrets never live in your
repository.

## Features

- Reads every entry in a KeePass archive as a build variable.
- Recurses into groups; skips the recycle bin.
- Uses the entry **username as the variable name** and the **password as the
  value**.
- When the same name appears more than once, the last entry wins.
- Supports an uppercase transform for names.

## Requirements

- Java 11+
- bob 7+

## Installation

```
plugin codemonstur/keepass-bobplugin:<version>
```

## Usage

```
bob keepass [options]
```

| Option | Description |
| --- | --- |
| `-f`, `--file` | Path to the KeePass archive (`.kdbx`). |
| `-p`, `--password` | Master password. |
| `-u`, `--uppercase` | Uppercase the variable names. |

If `--password` is omitted, the plugin falls back to the
`KEEPASS_MASTER_PASSWORD` environment variable.

## Behaviour notes

- Groups are traversed recursively; the recycle bin is ignored.
- Duplicate names: last one wins.
- Entries without a username are skipped.

## Building

```sh
make build
```

## License

MIT
