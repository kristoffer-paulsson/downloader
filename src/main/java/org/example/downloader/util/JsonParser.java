/**
 * Copyright (c) 2025 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
 *
 * This software is available under the terms of the MIT license. Parts are licensed
 * under different terms if stated. The legal terms are attached to the LICENSE file
 * and are made available on:
 *
 *      https://opensource.org/licenses/MIT
 *
 * SPDX-License-Identifier: MIT
 *
 * Contributors:
 *      Kristoffer Paulsson - initial implementation
 */
package org.example.downloader.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonParser {
    private int index;
    private String json;

    // Main method to parse a JSON string
    public Object parse(String jsonString) throws IllegalArgumentException {
        this.json = jsonString;
        this.index = 0;
        skipWhitespace();
        Object result = parseValue();
        skipWhitespace();
        if (index < json.length()) {
            throw new IllegalArgumentException("Unexpected characters after JSON: " + json.substring(index));
        }
        return result;
    }

    // Parse any JSON value (object, array, string, number, boolean, null)
    private Object parseValue() {
        skipWhitespace();
        if (index >= json.length()) {
            throw new IllegalArgumentException("Unexpected end of JSON");
        }

        char c = json.charAt(index);
        if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == '"') {
            return parseString();
        } else if (Character.isDigit(c) || c == '-') {
            return parseNumber();
        } else if (c == 't' || c == 'f') {
            return parseBoolean();
        } else if (c == 'n') {
            return parseNull();
        } else {
            throw new IllegalArgumentException("Invalid JSON at position " + index + ": " + c);
        }
    }

    // Parse a JSON object
    private Map<String, Object> parseObject() {
        Map<String, Object> object = new HashMap<>();
        index++; // Skip '{'
        skipWhitespace();

        if (index < json.length() && json.charAt(index) == '}') {
            index++; // Empty object
            return object;
        }

        while (index < json.length()) {
            skipWhitespace();
            if (json.charAt(index) != '"') {
                throw new IllegalArgumentException("Expected string key at position " + index);
            }
            String key = parseString();
            skipWhitespace();
            if (index >= json.length() || json.charAt(index) != ':') {
                throw new IllegalArgumentException("Expected ':' at position " + index);
            }
            index++; // Skip ':'
            skipWhitespace();
            Object value = parseValue();
            object.put(key, value);
            skipWhitespace();
            if (index < json.length() && json.charAt(index) == '}') {
                index++; // Skip '}'
                break;
            }
            if (index >= json.length() || json.charAt(index) != ',') {
                throw new IllegalArgumentException("Expected ',' or '}' at position " + index);
            }
            index++; // Skip ','
        }
        return object;
    }

    // Parse a JSON array
    private List<Object> parseArray() {
        List<Object> array = new ArrayList<>();
        index++; // Skip '['
        skipWhitespace();

        if (index < json.length() && json.charAt(index) == ']') {
            index++; // Empty array
            return array;
        }

        while (index < json.length()) {
            array.add(parseValue());
            skipWhitespace();
            if (index < json.length() && json.charAt(index) == ']') {
                index++; // Skip ']'
                break;
            }
            if (index >= json.length() || json.charAt(index) != ',') {
                throw new IllegalArgumentException("Expected ',' or ']' at position " + index);
            }
            index++; // Skip ','
            skipWhitespace();
        }
        return array;
    }

    // Parse a JSON string
    private String parseString() {
        index++; // Skip opening quote
        StringBuilder result = new StringBuilder();
        while (index < json.length()) {
            char c = json.charAt(index);
            if (c == '"') {
                index++; // Skip closing quote
                return result.toString();
            }
            if (c == '\\') {
                index++;
                if (index >= json.length()) {
                    throw new IllegalArgumentException("Unexpected end of string");
                }
                c = json.charAt(index);
                switch (c) {
                    case '"':
                    case '\\':
                    case '/':
                        result.append(c);
                        break;
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'u':
                        result.append(parseUnicode());
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid escape character at position " + index);
                }
            } else {
                result.append(c);
            }
            index++;
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    // Parse a Unicode escape sequence
    private char parseUnicode() {
        if (index + 4 > json.length()) {
            throw new IllegalArgumentException("Invalid Unicode sequence");
        }
        String hex = json.substring(index + 1, index + 5);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Unicode sequence: " + hex);
        }
    }

    // Parse a JSON number
    private Number parseNumber() {
        StringBuilder number = new StringBuilder();
        while (index < json.length()) {
            char c = json.charAt(index);
            if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                number.append(c);
                index++;
            } else {
                break;
            }
        }
        String numStr = number.toString();
        try {
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return Double.parseDouble(numStr);
            } else {
                return Long.parseLong(numStr);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + numStr);
        }
    }

    // Parse a JSON boolean
    private Boolean parseBoolean() {
        if (json.startsWith("true", index)) {
            index += 4;
            return true;
        } else if (json.startsWith("false", index)) {
            index += 5;
            return false;
        } else {
            throw new IllegalArgumentException("Invalid boolean at position " + index);
        }
    }

    // Parse a JSON null
    private Object parseNull() {
        if (json.startsWith("null", index)) {
            index += 4;
            return null;
        } else {
            throw new IllegalArgumentException("Invalid null at position " + index);
        }
    }

    // Skip whitespace characters
    private void skipWhitespace() {
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
    }

    // Example usage
    public static void main(String[] args) {
        JsonParser parser = new JsonParser();
        String jsonString = "{\"name\": \"John\", \"age\": 30, \"isStudent\": false, \"grades\": [90, 85, 88], \"address\": {\"street\": \"123 Main St\", \"city\": \"Anytown\"}}";
        try {
            Object result = parser.parse(jsonString);
            System.out.println(result);
        } catch (IllegalArgumentException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }
}
