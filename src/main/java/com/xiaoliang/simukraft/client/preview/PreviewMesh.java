package com.xiaoliang.simukraft.client.preview;

import com.mojang.blaze3d.vertex.VertexBuffer;

import java.util.Collections;
import java.util.List;

/**
 * 预览建筑的预构建网格，包含三种渲染层的 VBO 和需要逐帧渲染的实体方块列表。
 * VBO 顶点位于构建时的世界坐标，渲染时通过偏移补偿位移。
 */
public class PreviewMesh implements AutoCloseable {

    public static final PreviewMesh EMPTY = new PreviewMesh(null, null, null, Collections.emptyList());

    private VertexBuffer solidBuffer;
    private VertexBuffer cutoutBuffer;
    private VertexBuffer translucentBuffer;
    private final List<SchematicBlockData> entityBlocks;

    public PreviewMesh(VertexBuffer solidBuffer, VertexBuffer cutoutBuffer,
                       VertexBuffer translucentBuffer, List<SchematicBlockData> entityBlocks) {
        this.solidBuffer = solidBuffer;
        this.cutoutBuffer = cutoutBuffer;
        this.translucentBuffer = translucentBuffer;
        this.entityBlocks = entityBlocks;
    }

    public VertexBuffer solidBuffer() { return solidBuffer; }

    public VertexBuffer cutoutBuffer() { return cutoutBuffer; }

    public VertexBuffer translucentBuffer() { return translucentBuffer; }

    public List<SchematicBlockData> entityBlocks() { return entityBlocks; }

    public boolean isEmpty() {
        return solidBuffer == null && cutoutBuffer == null
                && translucentBuffer == null && entityBlocks.isEmpty();
    }

    @Override
    public void close() {
        if (solidBuffer != null) { solidBuffer.close(); solidBuffer = null; }
        if (cutoutBuffer != null) { cutoutBuffer.close(); cutoutBuffer = null; }
        if (translucentBuffer != null) { translucentBuffer.close(); translucentBuffer = null; }
    }
}
