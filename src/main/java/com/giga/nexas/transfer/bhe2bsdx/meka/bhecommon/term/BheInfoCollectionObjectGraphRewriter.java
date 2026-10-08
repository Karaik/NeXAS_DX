package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 在 BSDX 形状对象图中原地重编译 InfoCollection。
 *
 * <p>BHE -> BSDX 的格式转换阶段把字段名换成 BSDX DTO，但 collection 内部仍保留 BHE term 索引空间。
 * 这个 rewriter 专门扫出这些 collection，并交给 {@link BheInfoCollectionTermConverter} 做语义重编译。</p>
 */
public class BheInfoCollectionObjectGraphRewriter {

    private final BheInfoCollectionTermConverter converter = new BheInfoCollectionTermConverter();

    public void rewrite(Object root, String context) {
        visit(root, context, new IdentityHashMap<>(), AuxConsumerPurpose.EXPRESSION);
    }

    private void visit(Object node, String path, IdentityHashMap<Object, Boolean> visited, AuxConsumerPurpose purpose) {
        if (node == null || isLeafValue(node)) {
            return;
        }
        if (visited.put(node, Boolean.TRUE) != null) {
            return;
        }

        if (node instanceof BsdxInfoCollection collection) {
            converter.rewriteCopiedBsdxCollection(collection, path, purpose);
            return;
        }

        if (node instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                visit(list.get(i), path + "[" + i + "]", visited, purpose);
            }
            return;
        }

        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                visit(entry.getValue(), path + "." + entry.getKey(), visited, purpose);
            }
            return;
        }

        if (!node.getClass().getName().startsWith("com.giga.nexas.")) {
            return;
        }

        for (Field field : getAllFields(node.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            try {
                visit(field.get(node), path + "." + field.getName(), visited, consumerOf(node, field, purpose));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("term 重编译读取字段失败: " + path + "." + field.getName(), e);
            }
        }
    }

    private AuxConsumerPurpose consumerOf(Object host, Field field, AuxConsumerPurpose inherited) {
        if (host instanceof com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent) {
            return AuxConsumerPurpose.OBJECT_REFERENCE;
        }
        if (host instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm term) {
            Integer type = term.getTypeId();
            if (type == null) return AuxConsumerPurpose.UNKNOWN;
            return switch (type) {
                case 32, -1 -> AuxConsumerPurpose.UNUSED;
                case 5 -> AuxConsumerPurpose.POSITION;
                case 6 -> AuxConsumerPurpose.OBJECT_REFERENCE;
                case 9, 10 -> AuxConsumerPurpose.ANGLE;
                case 11 -> AuxConsumerPurpose.PARAMETER;
                default -> AuxConsumerPurpose.UNKNOWN;
            };
        }
        if (host instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventMove) {
            return AuxConsumerPurpose.POSITION;
        }
        if (host instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange) {
            return field.getName().equals("bsdxInfoCollectionList2")
                    ? AuxConsumerPurpose.UNUSED : AuxConsumerPurpose.OBJECT_REFERENCE;
        }
        return inherited;
    }

    public List<BheInfoCollectionTermConverter.AuxAudit> getAuxAudit() { return converter.getAuxAudit(); }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private boolean isLeafValue(Object value) {
        Class<?> type = value.getClass();
        return type.isPrimitive()
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || type.isArray()
                || type.getName().startsWith("java.time.");
    }
}
