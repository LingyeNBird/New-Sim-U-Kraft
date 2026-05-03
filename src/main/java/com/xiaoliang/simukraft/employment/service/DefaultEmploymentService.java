package com.xiaoliang.simukraft.employment.service;

import com.xiaoliang.simukraft.employment.domain.EmploymentAssignment;
import com.xiaoliang.simukraft.employment.domain.EmploymentStatus;
import com.xiaoliang.simukraft.employment.domain.JobType;
import com.xiaoliang.simukraft.employment.persistence.EmploymentRepository;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DefaultEmploymentService implements EmploymentService, EmploymentQueryService {
    private final EmploymentRepository repository;

    public DefaultEmploymentService(EmploymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public synchronized EmploymentResult hire(EmploymentCommands.HireCommand command) {
        if (command == null || command.npcUuid() == null || command.workplacePos() == null || command.workBlockType() == null || command.jobType() == null) {
            return EmploymentResult.error(EmploymentErrorCode.INVALID_COMMAND, "Invalid hire command");
        }

        List<EmploymentAssignment> all = new ArrayList<>(repository.loadAll());
        Optional<EmploymentAssignment> npcAssigned = findAssignedByNpc(all, command.npcUuid());
        Optional<EmploymentAssignment> workplaceAssigned = findAssignedByWorkplaceSlot(all, command.dimensionId(), command.workplacePos(), command.workBlockType(), command.jobType());

        if (npcAssigned.isPresent()) {
            EmploymentAssignment current = npcAssigned.get();
            boolean sameTarget = current.workplacePos().equals(command.workplacePos())
                    && safeEquals(current.dimensionId(), command.dimensionId())
                    && current.jobType() == command.jobType();
            if (sameTarget) {
                return EmploymentResult.ok("Already hired for target", current);
            }
            return EmploymentResult.error(EmploymentErrorCode.NPC_ALREADY_ASSIGNED, "NPC already assigned");
        }

        if (workplaceAssigned.isPresent()) {
            return EmploymentResult.error(EmploymentErrorCode.WORKPLACE_ALREADY_OCCUPIED, "Workplace already occupied");
        }

        // 再次检查NPC是否已被雇佣（防止并发问题）
        if (hasAnyAssignedByNpc(all, command.npcUuid())) {
            return EmploymentResult.error(EmploymentErrorCode.NPC_ALREADY_ASSIGNED, "NPC already assigned (double check)");
        }

        long now = System.currentTimeMillis();
        long version = nextVersion(all);

        EmploymentAssignment created = new EmploymentAssignment(
                command.npcUuid(),
                normalizeDimension(command.dimensionId()),
                command.workplacePos(),
                command.workBlockType(),
                command.jobType(),
                EmploymentStatus.ASSIGNED,
                version,
                now
        );
        all.add(created);

        if (!repository.saveAll(all)) {
            return EmploymentResult.error(EmploymentErrorCode.PERSISTENCE_ERROR, "Failed to persist assignment");
        }

        // 保存成功后，重新加载并验证NPC确实只有一条ASSIGNED记录
        List<EmploymentAssignment> verifyAll = new ArrayList<>(repository.loadAll());
        long assignedCount = verifyAll.stream()
                .filter(EmploymentAssignment::isAssigned)
                .filter(a -> a.npcUuid().equals(command.npcUuid()))
                .count();
        if (assignedCount > 1) {
            // 数据不一致，删除刚添加的记录
            System.err.println("[DefaultEmploymentService] 数据不一致：NPC " + command.npcUuid().toString().substring(0, 8)
                + " 有 " + assignedCount + " 条ASSIGNED记录，回滚本次雇佣");
            all.remove(created);
            repository.saveAll(all);
            return EmploymentResult.error(EmploymentErrorCode.NPC_ALREADY_ASSIGNED, "NPC already has multiple assignments");
        }

        return EmploymentResult.ok("Hired", created);
    }

    @Override
    public synchronized EmploymentResult fireByNpc(EmploymentCommands.FireByNpcCommand command) {
        if (command == null || command.npcUuid() == null) {
            return EmploymentResult.error(EmploymentErrorCode.INVALID_COMMAND, "Invalid fireByNpc command");
        }

        List<EmploymentAssignment> all = new ArrayList<>(repository.loadAll());

        // 查找该NPC的所有ASSIGNED记录（可能有多个，比如同时被雇佣为建筑师和规划师）
        List<EmploymentAssignment> existingList = all.stream()
                .filter(EmploymentAssignment::isAssigned)
                .filter(a -> a.npcUuid().equals(command.npcUuid()))
                .toList();

        if (existingList.isEmpty()) {
            // 检查是否有RELEASED状态的记录（可能之前已经被解雇过，但副作用未执行）
            Optional<EmploymentAssignment> releasedRecord = findAnyByNpc(all, command.npcUuid());
            if (releasedRecord.isPresent()) {
                // 返回RELEASED记录，让调用方执行副作用
                return EmploymentResult.ok("NPC already released, applying side effects", releasedRecord.get());
            }
            return EmploymentResult.error(EmploymentErrorCode.NOT_FOUND, "NPC assignment not found");
        }

        // 解雇所有ASSIGNED记录
        EmploymentAssignment lastReleased = null;
        for (EmploymentAssignment existing : existingList) {
            lastReleased = releaseAssignment(all, existing);
        }

        if (!repository.saveAll(all)) {
            return EmploymentResult.error(EmploymentErrorCode.PERSISTENCE_ERROR, "Failed to persist release");
        }

        return EmploymentResult.ok("Fired by NPC", lastReleased);
    }

    @Override
    public synchronized EmploymentResult fireByWorkplace(EmploymentCommands.FireByWorkplaceCommand command) {
        if (command == null || command.workplacePos() == null) {
            return EmploymentResult.error(EmploymentErrorCode.INVALID_COMMAND, "Invalid fireByWorkplace command");
        }

        List<EmploymentAssignment> all = new ArrayList<>(repository.loadAll());
        Optional<EmploymentAssignment> existing = findAssignedByWorkplace(all, command.dimensionId(), command.workplacePos());
        if (existing.isEmpty()) {
            return EmploymentResult.error(EmploymentErrorCode.NOT_FOUND, "Workplace assignment not found");
        }

        EmploymentAssignment released = releaseAssignment(all, existing.get());
        if (!repository.saveAll(all)) {
            return EmploymentResult.error(EmploymentErrorCode.PERSISTENCE_ERROR, "Failed to persist release");
        }

        return EmploymentResult.ok("Fired by workplace", released);
    }

    @Override
    public synchronized EmploymentResult onWorkBlockRemoved(EmploymentCommands.WorkBlockRemovedCommand command) {
        if (command == null || command.workplacePos() == null) {
            return EmploymentResult.error(EmploymentErrorCode.INVALID_COMMAND, "Invalid workBlockRemoved command");
        }
        return fireByWorkplace(new EmploymentCommands.FireByWorkplaceCommand(command.dimensionId(), command.workplacePos()));
    }

    @Override
    public synchronized Optional<EmploymentAssignment> findByNpc(UUID npcUuid) {
        if (npcUuid == null) {
            return Optional.empty();
        }
        return findAssignedByNpc(repository.loadAll(), npcUuid);
    }

    @Override
    public synchronized Optional<EmploymentAssignment> findByWorkplace(String dimensionId, BlockPos pos) {
        if (pos == null) {
            return Optional.empty();
        }
        return findAssignedByWorkplace(repository.loadAll(), dimensionId, pos);
    }

    @Override
    public synchronized Optional<EmploymentAssignment> findByWorkplaceAndJob(String dimensionId, BlockPos pos, JobType jobType) {
        if (pos == null || jobType == null) {
            return Optional.empty();
        }
        return repository.loadAll().stream()
                .filter(EmploymentAssignment::isAssigned)
                .filter(a -> a.workplacePos().equals(pos)
                        && safeEquals(a.dimensionId(), dimensionId)
                        && a.jobType() == jobType)
                .max(Comparator.comparingLong(EmploymentAssignment::version));
    }

    @Override
    public synchronized List<EmploymentAssignment> listAssignedByWorkplace(String dimensionId, BlockPos pos) {
        if (pos == null) {
            return List.of();
        }
        String normalized = EmploymentSlotKey.normalizeDimension(dimensionId);
        return repository.loadAll().stream()
                .filter(EmploymentAssignment::isAssigned)
                .filter(a -> a.workplacePos().equals(pos) && safeEquals(a.dimensionId(), normalized))
                .sorted(Comparator.comparingLong(EmploymentAssignment::version))
                .toList();
    }

    @Override
    public synchronized List<EmploymentAssignment> listByCity(UUID cityId) {
        // Phase 1 keeps city ownership lookup in legacy path.
        return repository.loadAll().stream().filter(EmploymentAssignment::isAssigned).toList();
    }

    private EmploymentAssignment releaseAssignment(List<EmploymentAssignment> all, EmploymentAssignment existing) {
        all.remove(existing);
        EmploymentAssignment released = new EmploymentAssignment(
                existing.npcUuid(),
                existing.dimensionId(),
                existing.workplacePos(),
                existing.workBlockType(),
                existing.jobType(),
                EmploymentStatus.RELEASED,
                nextVersion(all),
                System.currentTimeMillis()
        );
        all.add(released);
        return released;
    }

    private Optional<EmploymentAssignment> findAssignedByNpc(List<EmploymentAssignment> all, UUID npcUuid) {
        // 一个NPC只能有一条ASSIGNED记录，返回任意一条（这里按version取最新的）
        return all.stream()
                .filter(EmploymentAssignment::isAssigned)
                .filter(a -> a.npcUuid().equals(npcUuid))
                .max(Comparator.comparingLong(EmploymentAssignment::version));
    }

    /**
     * 检查NPC是否已经有任何ASSIGNED状态的雇佣记录（任何职业）
     */
    private boolean hasAnyAssignedByNpc(List<EmploymentAssignment> all, UUID npcUuid) {
        return all.stream()
                .filter(EmploymentAssignment::isAssigned)
                .anyMatch(a -> a.npcUuid().equals(npcUuid));
    }

    private Optional<EmploymentAssignment> findAnyByNpc(List<EmploymentAssignment> all, UUID npcUuid) {
        return all.stream()
                .filter(a -> a.npcUuid().equals(npcUuid))
                .max(Comparator.comparingLong(EmploymentAssignment::version));
    }

    private Optional<EmploymentAssignment> findAssignedByWorkplace(List<EmploymentAssignment> all, String dimensionId, BlockPos pos) {
        String normalized = normalizeDimension(dimensionId);
        return all.stream()
                .filter(EmploymentAssignment::isAssigned)
                .filter(a -> a.workplacePos().equals(pos) && safeEquals(a.dimensionId(), normalized))
                .max(Comparator.comparingLong(EmploymentAssignment::version));
    }

    private Optional<EmploymentAssignment> findAssignedByWorkplaceSlot(List<EmploymentAssignment> all, String dimensionId, BlockPos pos, com.xiaoliang.simukraft.employment.domain.WorkBlockType workBlockType, JobType jobType) {
        String normalized = normalizeDimension(dimensionId);
        String targetSlot = EmploymentSlotKey.workplaceSlotKey(normalized, pos, workBlockType, jobType);
        return all.stream()
                .filter(EmploymentAssignment::isAssigned)
                .filter(a -> EmploymentSlotKey.workplaceSlotKey(a.dimensionId(), a.workplacePos(), a.workBlockType(), a.jobType()).equals(targetSlot))
                .max(Comparator.comparingLong(EmploymentAssignment::version));
    }


    private long nextVersion(List<EmploymentAssignment> all) {
        return all.stream().mapToLong(EmploymentAssignment::version).max().orElse(0L) + 1L;
    }

    private boolean safeEquals(String left, String right) {
        String l = normalizeDimension(left);
        String r = normalizeDimension(right);
        return l.equals(r);
    }

    private String normalizeDimension(String dimensionId) {
        return EmploymentSlotKey.normalizeDimension(dimensionId);
    }
}

