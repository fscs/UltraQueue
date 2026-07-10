package de.hhu.fscs.ultraqueue.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Common {
    public static UUID uuidFromFields(Object... fields) {
        var out = new java.io.ByteArrayOutputStream();
        try {
            for (Object f : fields) {
                if (f != null) {
                    out.write(f.toString().getBytes(StandardCharsets.UTF_8));
                }
                out.write(0);
            }
        } catch (IOException ignored) {
        }
        return UUID.nameUUIDFromBytes(out.toByteArray());
    }
}
