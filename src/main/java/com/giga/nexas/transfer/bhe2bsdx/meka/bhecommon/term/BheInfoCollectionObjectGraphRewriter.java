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
        visit(root, context, new IdentityHashMap<>());
    }

    private void visit(Object node, String path, IdentityHashMap<Object, Boolean> visited) {
        if (node == null || isLeafValue(node)) {
            return;
        }
        if (visited.put(node, Boolean.TRUE) != null) {
            return;
        }

        if (node instanceof BsdxInfoCollection collection) {
            converter.rewriteCopiedBsdxCollection(collection, path);
            return;
        }

        if (node instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                visit(list.get(i), path + "[" + i + "]", visited);
            }
            return;
        }

        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                visit(entry.getValue(), path + "." + entry.getKey(), visited);
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
                visit(field.get(node), path + "." + field.getName(), visited);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("term 重编译读取字段失败: " + path + "." + field.getName(), e);
            }
        }
    }

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
