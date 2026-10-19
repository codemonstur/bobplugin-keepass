package keepass;

import bobthebuildtool.pojos.buildfile.Project;
import bobthebuildtool.pojos.error.InvalidInput;
import bobthebuildtool.pojos.error.VersionTooOld;
import de.slackspace.openkeepass.KeePassDatabase;
import de.slackspace.openkeepass.domain.Entry;
import de.slackspace.openkeepass.domain.Group;
import de.slackspace.openkeepass.domain.KeePassFile;
import de.slackspace.openkeepass.domain.Meta;
import jcli.errors.InvalidCommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static bobthebuildtool.services.Functions.isNullOrEmpty;
import static bobthebuildtool.services.Update.requireBobVersion;
import static java.nio.file.Files.isRegularFile;
import static jcli.CliParserBuilder.newCliParser;

public enum BobPlugin {;

    public static void installPlugin(final Project project) throws VersionTooOld {
        requireBobVersion("7");
        project.addCommand("keepass", "Loads variables from a keepass archive", BobPlugin::loadKeepassArchive);
    }

    private static int loadKeepassArchive(final Project project, final Map<String, String> env, final String[] args)
            throws InvalidCommandLine, InvalidInput {
        final CliKeepass arguments = newCliParser(CliKeepass::new).parse(args);

        final Path archive = toArchivePath(project, arguments.file);
        if (!isRegularFile(archive)) throw new InvalidInput("Keepass archive " + archive + " does not exist.");

        final String password = isNullOrEmpty(arguments.password) ? env.get("KEEPASS_MASTER_PASSWORD") : arguments.password;
        if (isNullOrEmpty(password)) throw new InvalidInput("Could not find a password to use for the keepass archive");

        final KeePassFile database = KeePassDatabase
            .getInstance(archive.toFile())
            .openDatabase(password);

        loadEntries(database, env, arguments.uppercase);

        return 0;
    }

    // Loads every entry (recycle bin excluded) into the environment. Entries without a username
    // are skipped, and a missing password becomes an empty string rather than a null value.
    static void loadEntries(final KeePassFile database, final Map<String, String> env, final boolean uppercase) {
        for (final Entry entry : collectEntries(database)) {
            final String username = entry.getUsername();
            // Entries without a username cannot be mapped to an environment variable
            if (isNullOrEmpty(username)) continue;

            final String name = uppercase ? username.toUpperCase(Locale.ROOT) : username;
            final String value = entry.getPassword();
            env.put(name, value == null ? "" : value);
        }
    }

    // Resolves the archive path against the project root when it is relative, so the command
    // behaves the same no matter what directory bob was invoked from.
    static Path toArchivePath(final Project project, final String file) {
        final Path path = Paths.get(file);
        return (path.isAbsolute() ? path : project.parentDir.resolve(path)).normalize();
    }

    // Collects every entry in the database, skipping the recycle bin so deleted credentials
    // are not loaded into the environment.
    private static List<Entry> collectEntries(final KeePassFile database) {
        final List<Entry> entries = new ArrayList<>();
        collectEntries(database.getRoot(), recycleBinUuid(database), entries);
        return entries;
    }
    private static void collectEntries(final Group group, final UUID recycleBin, final List<Entry> entries) {
        if (group == null) return;
        if (recycleBin != null && recycleBin.equals(group.getUuid())) return;
        if (group.getEntries() != null) entries.addAll(group.getEntries());
        if (group.getGroups() != null)
            for (final Group child : group.getGroups()) collectEntries(child, recycleBin, entries);
    }
    private static UUID recycleBinUuid(final KeePassFile database) {
        final Meta meta = database.getMeta();
        if (meta == null || !meta.getRecycleBinEnabled()) return null;
        return meta.getRecycleBinUuid();
    }

}
