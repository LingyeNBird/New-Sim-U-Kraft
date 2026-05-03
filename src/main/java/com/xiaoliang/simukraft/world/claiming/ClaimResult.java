package com.xiaoliang.simukraft.world.claiming;

import net.minecraft.network.chat.Component;

import java.util.Objects;
import net.minecraft.network.chat.MutableComponent;

/**
 * 区块认领操作的结果。
 * 统一表示 Simukraft 内部、FTB Chunks、Open Parties And Claims 的认领结果。
 */
public class ClaimResult {
    private final boolean success;
    private final Problem problem;
    private final String detail;

    private ClaimResult(boolean success, Problem problem, String detail) {
        this.success = success;
        this.problem = problem;
        this.detail = detail;
    }

    public static ClaimResult success() {
        return new ClaimResult(true, null, null);
    }

    public static ClaimResult fail(Problem problem) {
        return new ClaimResult(false, problem, null);
    }

    public static ClaimResult fail(Problem problem, String detail) {
        return new ClaimResult(false, problem, detail);
    }

    public boolean isSuccess() {
        return success;
    }

    public Problem getProblem() {
        return problem;
    }

    public String getDetail() {
        return detail;
    }

    public Component getMessage() {
        if (success) {
            return Objects.requireNonNull(Component.translatable("simukraft.claim.success"));
        }
        if (problem != null) {
            Component msg = problem.getMessage();
            if (detail != null) {
                MutableComponent fullMessage = Objects.requireNonNull(msg.copy());
                fullMessage.append(": ");
                fullMessage.append(Objects.requireNonNull(Component.literal(Objects.requireNonNull(detail))));
                return Objects.requireNonNull((Component) fullMessage);
            }
            return Objects.requireNonNull(msg);
        }
        return Objects.requireNonNull(Component.translatable("simukraft.claim.unknown_error"));
    }


    public enum Problem {
        /** 区块已被其他城市占据 */
        ALREADY_CLAIMED("simukraft.claim.already_claimed"),
        /** 不在城市范围内（不与已有区块相邻） */
        NOT_ADJACENT("simukraft.claim.not_adjacent"),
        /** 资金不足 */
        INSUFFICIENT_FUNDS("simukraft.claim.insufficient_funds"),
        /** 超出城市等级允许的区块数量 */
        MAX_CHUNKS_REACHED("simukraft.claim.max_chunks"),
        /** 没有权限（不是市长或官员） */
        NO_PERMISSION("simukraft.claim.no_permission"),
        /** 玩家没有城市 */
        NO_CITY("simukraft.claim.no_city"),
        /** FTB Chunks 认领失败 */
        FTB_CLAIM_FAILED("simukraft.claim.ftb_failed"),
        /** Open Parties And Claims 认领失败 */
        OPAC_CLAIM_FAILED("simukraft.claim.opac_failed"),
        /** 维度不支持 */
        WRONG_DIMENSION("simukraft.claim.wrong_dimension"),
        /** 区块被服务器保护 */
        PROTECTED("simukraft.claim.protected"),
        /** 内部错误 */
        INTERNAL_ERROR("simukraft.claim.internal_error");

        private final String translationKey;

        Problem(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getMessage() {
            return Objects.requireNonNull(Component.translatable(Objects.requireNonNull(translationKey)));
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }
}
