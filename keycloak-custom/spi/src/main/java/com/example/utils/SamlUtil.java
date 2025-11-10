package com.example.utils;

import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SamlUtil {
    private static final Logger logger = Logger.getLogger(SamlUtil.class);

    private SamlUtil() {
        // Private constructor to prevent instantiation
    }

    public static Map<String, String> flatten(SAML2Object object) {
        var result = new HashMap<String, String>();
        if (object == null)
            return result;
        flattenObject("", object, result);
        return result;
    }

    public static Map<String, String> flatten(Document document) {
        var result = new HashMap<String, String>();
        if (document == null)
            return result;
        var root = document.getDocumentElement();
        flattenNode("", root, result);

        return result;
    }

    static void flattenObject(String prefix, Object obj, Map<String, String> result) {
        if (obj == null)
            return;

        for (var field : obj.getClass().getFields()) {
            try {
                if (!field.canAccess(obj)) {
                    field.setAccessible(true);
                }
                var value = field.get(obj);
                var key = prefix.isEmpty()
                        ? field.getName()
                        : prefix + "." + field.getName();

                if (value == null) {
                    continue;
                }
                if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                    result.put(key, String.valueOf(value));
                }
                if (value instanceof Collection<?> collection) {
                    var index = 0;
                    for (var item : collection) {
                        flattenObject(key + "[" + index + "]", item, result);
                        index++;
                    }
                } else {
                    flattenObject(key, value, result);
                }
            } catch (IllegalAccessException e) {
                logger.errorf(e, "failed to access field %s", field.getName());
            }
        }
    }

    static void flattenNode(String prefix, Node node, Map<String, String> result) {
        if (node == null) return;

        var nodeName = node.getNodeName();
        var currentPath = prefix.isEmpty() ? nodeName : prefix + "." + nodeName;

        // Handle attributes
        if (node.getAttributes() != null) {
            for (var i = 0; i < node.getAttributes().getLength(); i++) {
                var attr = node.getAttributes().item(i);
                result.put(currentPath + "." + attr.getNodeName(), attr.getNodeValue());
            }
        }

        // Handle text content
        var textContent = getDirectTextContent(node);
        if (!textContent.isEmpty()) {
            result.put(currentPath, textContent);
        }

        // Handle child nodes
        var children = node.getChildNodes();
        for (var i = 0; i < children.getLength(); i++) {
            var child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                flattenNode(currentPath, child, result);
            }
        }
    }

    static String getDirectTextContent(Node node) {
        var children = node.getChildNodes();
        var text = new StringBuilder();
        for (int i = 0; i < children.getLength(); i++) {
            var child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                text.append(child.getNodeValue().trim());
            }
        }

        return text.toString();
    }
}