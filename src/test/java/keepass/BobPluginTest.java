package keepass;

import bobthebuildtool.pojos.buildfile.Project;
import de.slackspace.openkeepass.domain.Entry;
import de.slackspace.openkeepass.domain.EntryBuilder;
import de.slackspace.openkeepass.domain.Group;
import de.slackspace.openkeepass.domain.GroupBuilder;
import de.slackspace.openkeepass.domain.KeePassFile;
import de.slackspace.openkeepass.domain.KeePassFileBuilder;
import de.slackspace.openkeepass.domain.MetaBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BobPluginTest {

    private static Entry entry(final String username, final String password) {
        return new EntryBuilder().title(username == null ? "untitled" : username)
            .username(username).password(password).build();
    }

    private static Group group(final String name, final UUID uuid, final Entry... entries) {
        final GroupBuilder builder = uuid == null ? new GroupBuilder(name) : new GroupBuilder(uuid).name(name);
        for (final Entry entry : entries) builder.addEntry(entry);
        return builder.build();
    }

    private static KeePassFile database(final UUID recycleBinUuid, final Group... groups) {
        final MetaBuilder meta = new MetaBuilder("test").recycleBinEnabled(recycleBinUuid != null);
        if (recycleBinUuid != null) meta.recycleBinUuid(recycleBinUuid);
        return new KeePassFileBuilder(meta.build()).addTopGroups(groups).build();
    }

    @Test
    void loadsEntriesIntoEnvironment() {
        final KeePassFile db = database(null, group("Root", null, entry("user1", "pass1")));

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, false);

        assertEquals("pass1", env.get("user1"));
    }

    @Test
    void loadsEntriesNestedInSubgroups() {
        final Group inner = group("Inner", null, entry("nested", "deep"));
        final Group rootChild = new GroupBuilder("Root")
            .addEntry(entry("middle", "mid"))
            .addGroup(inner)
            .build();
        final KeePassFile db = database(null, rootChild);

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, false);

        assertEquals("mid", env.get("middle"));
        assertEquals("deep", env.get("nested"));
    }

    @Test
    void skipsEntriesWithNullUsername() {
        final KeePassFile db = database(null,
            group("Root", null, entry(null, "orphan"), entry("good", "val")));

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, false);

        assertFalse(env.containsKey(null));
        assertFalse(env.containsValue("orphan"));
        assertEquals("val", env.get("good"));
    }

    @Test
    void skipsEntriesWithEmptyUsername() {
        final KeePassFile db = database(null,
            group("Root", null, entry("", "orphan"), entry("good", "val")));

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, false);

        assertFalse(env.containsKey(""));
        assertEquals("val", env.get("good"));
    }

    @Test
    void nullPasswordBecomesEmptyString() {
        final KeePassFile db = database(null, group("Root", null, entry("user1", null)));

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, false);

        assertTrue(env.containsKey("user1"));
        assertEquals("", env.get("user1"));
    }

    @Test
    void uppercaseUsesRootLocale() {
        final KeePassFile db = database(null, group("Root", null, entry("user", "pass")));

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, true);

        assertEquals("pass", env.get("USER"));
    }

    @Test
    void recycleBinEntriesAreSkipped() {
        final UUID recycleBinId = UUID.randomUUID();
        final KeePassFile db = database(recycleBinId,
            group("Root", null, entry("live", "keep")),
            group("Recycle Bin", recycleBinId, entry("dead", "drop")));

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, false);

        assertEquals("keep", env.get("live"));
        assertFalse(env.containsKey("dead"));
    }

    @Test
    void recycleBinDisabledLoadsAllEntries() {
        final KeePassFile db = database(null,
            group("Root", null, entry("live", "keep")),
            group("Deleted", null, entry("dead", "drop")));

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, false);

        assertEquals("keep", env.get("live"));
        assertEquals("drop", env.get("dead"));
    }

    @Test
    void duplicateUsernamesLastWins() {
        final KeePassFile db = database(null,
            group("Root", null, entry("dup", "first"), entry("dup", "second")));

        final Map<String, String> env = new HashMap<>();
        BobPlugin.loadEntries(db, env, false);

        assertEquals("second", env.get("dup"));
    }

    @Test
    void relativePathResolvesAgainstProjectRoot() {
        final Project project = new Project(Paths.get("/tmp/project"), Paths.get("/tmp/project/bobfile"));

        assertEquals(Paths.get("/tmp/project/keys/db.kdbx"), BobPlugin.toArchivePath(project, "keys/db.kdbx"));
    }

    @Test
    void absolutePathIsPreserved() {
        final Project project = new Project(Paths.get("/tmp/project"), Paths.get("/tmp/project/bobfile"));

        assertEquals(Paths.get("/var/data/db.kdbx"), BobPlugin.toArchivePath(project, "/var/data/db.kdbx"));
    }

    @Test
    void pathIsNormalized() {
        final Project project = new Project(Paths.get("/tmp/project"), Paths.get("/tmp/project/bobfile"));

        final Path resolved = BobPlugin.toArchivePath(project, "../secrets/db.kdbx");
        assertEquals(Paths.get("/tmp/secrets/db.kdbx"), resolved);
    }
}