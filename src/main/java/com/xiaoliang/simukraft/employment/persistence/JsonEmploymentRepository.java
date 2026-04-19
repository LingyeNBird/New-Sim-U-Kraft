package com.xiaoliang.simukraft.employment.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.EmploymentStatus;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.domain.WorkBlockType;
import com.xiaoliang.simukraft.utils.FileUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("null")
public class JsonEmploymentRepository implements EmploymentRepository {
    private static final String FILE_NAME = "employment_assignments_v2.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final MinecraftServer server;

    public JsonEmploymentRepository(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public List<EmploymentAssignment> loadAll() {
        List<EmploymentAssignment> result = new ArrayList<>();
        try {
            Path dataFile = resolveDataFile();
            if (!Files.exists(dataFile) || Files.size(dataFile) == 0L) {
                return result;
            }

            EmploymentStoreDocument doc;
            try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
                doc = GSON.fromJson(reader, EmploymentStoreDocument.class);
            }
            if (doc == null || doc.assignments == null) {
                return result;
            }

            for (EmploymentStoreDocument.StoredAssignment stored : doc.assignments) {
                try {
                    EmploymentAssignment assignment = new EmploymentAssignment(
                            UUID.fromString(stored.npcUuid),
                            stored.dimensionId,
                            new BlockPos(stored.x, stored.y, stored.z),
                            WorkBlockType.valueOf(stored.workBlockType),
                            JobType.valueOf(stored.jobType),
                            EmploymentStatus.valueOf(stored.status),
                            stored.version,
                            stored.updatedAtEpochMs
                    );
                    result.add(assignment);
                } catch (Exception ignored) {
                    // Ignore malformed row and continue.
                }
            }
        } catch (JsonSyntaxException e) {
            System.err.println("[Employment] Failed to parse v2 repository JSON: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Employment] Failed to load v2 repository: " + e.getMessage());
        }
        return result;
    }

    @Override
    public boolean saveAll(List<EmploymentAssignment> assignments) {
        try {
            Path dataFile = resolveDataFile();
            Path dir = dataFile.getParent();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            EmploymentStoreDocument doc = new EmploymentStoreDocument();
            for (EmploymentAssignment assignment : assignments) {
                EmploymentStoreDocument.StoredAssignment stored = new EmploymentStoreDocument.StoredAssignment();
                stored.npcUuid = assignment.npcUuid().toString();
                stored.dimensionId = assignment.dimensionId();
                stored.x = assignment.workplacePos().getX();
                stored.y = assignment.workplacePos().getY();
                stored.z = assignment.workplacePos().getZ();
                stored.workBlockType = assignment.workBlockType().name();
                stored.jobType = assignment.jobType().name();
                stored.status = assignment.status().name();
                stored.version = assignment.version();
                stored.updatedAtEpochMs = assignment.updatedAtEpochMs();
                doc.assignments.add(stored);
            }

            try (Writer writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8)) {
                GSON.toJson(doc, writer);
            }
            return true;
        } catch (Exception e) {
            System.err.println("[Employment] Failed to save v2 repository: " + e.getMessage());
            return false;
        }
    }

    private Path resolveDataFile() {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        return worldDir.resolve(FileUtils.MODE_DIR).resolve(FILE_NAME);
    }
}
