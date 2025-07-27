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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonParser {
    private BufferedReader reader;
    private String currentLine; // Current line being processed
    private int lineIndex; // Index within the current line
    private boolean hasNextLine; // Indicates if there are more lines to read
    private int currentChar; // Current character being processed (-1 for EOF or end of line)

    // Main method to parse JSON from a BufferedReader
    public Object parse(BufferedReader reader) throws IllegalArgumentException, IOException {
        this.reader = reader;
        this.currentLine = null;
        this.lineIndex = 0;
        this.hasNextLine = true;
        fetchNextLine();
        skipWhitespace();
        Object result = parseValue();
        skipWhitespace();
        if (hasNextLine || (currentLine != null && lineIndex < currentLine.length())) {
            throw new IllegalArgumentException("Unexpected characters after JSON");
        }
        return result;
    }

    // Fetch the next line from the BufferedReader
    private void fetchNextLine() throws IOException {
        currentLine = reader.readLine();
        lineIndex = 0;
        hasNextLine = currentLine != null;
        if (hasNextLine && currentLine.isEmpty()) {
            fetchNextLine(); // Skip empty lines
        }
    }

    // Read the next character from the current line or fetch a new line
    private int readChar() throws IOException {
        while (hasNextLine) {
            if (currentLine == null || lineIndex >= currentLine.length()) {
                fetchNextLine();
                if (!hasNextLine) {
                    return -1; // EOF
                }
            }
            return currentLine.charAt(lineIndex++);
        }
        return -1; // EOF if no more lines
    }

    // Advance to the next character
    private void nextChar() throws IOException {
        currentChar = readChar();
    }

    // Parse any JSON value (object, array, string, number, boolean, null)
    private Object parseValue() throws IOException {
        skipWhitespace();
        if (!hasNextLine && (currentLine == null || lineIndex >= currentLine.length())) {
            throw new IllegalArgumentException("Unexpected end of JSON");
        }

        if (currentChar == '{') {
            return parseObject();
        } else if (currentChar == '[') {
            return parseArray();
        } else if (currentChar == '"') {
            return parseString();
        } else if (Character.isDigit(currentChar) || currentChar == '-') {
            return parseNumber();
        } else if (currentChar == 't' || currentChar == 'f') {
            return parseBoolean();
        } else if (currentChar == 'n') {
            return parseNull();
        } else {
            throw new IllegalArgumentException("Invalid JSON character: " + (char) currentChar);
        }
    }

    // Parse a JSON object
    private Map<String, Object> parseObject() throws IOException {
        Map<String, Object> object = new HashMap<>();
        nextChar(); // Skip '{'
        skipWhitespace();

        if (currentChar == '}' && (hasNextLine || lineIndex < currentLine.length())) {
            nextChar(); // Empty object
            return object;
        }

        while (hasNextLine || (currentLine != null && lineIndex < currentLine.length())) {
            skipWhitespace();
            if (currentChar != '"') {
                throw new IllegalArgumentException("Expected string key, found: " + (char) currentChar);
            }
            String key = parseString();
            skipWhitespace();
            if (!hasNextLine && (currentLine == null || lineIndex >= currentLine.length()) || currentChar != ':') {
                throw new IllegalArgumentException("Expected ':'");
            }
            nextChar(); // Skip ':'
            skipWhitespace();
            Object value = parseValue();
            object.put(key, value);
            skipWhitespace();
            if (currentChar == '}' && (hasNextLine || lineIndex < currentLine.length())) {
                nextChar(); // Skip '}'
                break;
            }
            if (!hasNextLine && (currentLine == null || lineIndex >= currentLine.length()) || currentChar != ',') {
                throw new IllegalArgumentException("Expected ',' or '}'");
            }
            nextChar(); // Skip ','
        }
        return object;
    }

    // Parse a JSON array
    private List<Object> parseArray() throws IOException {
        List<Object> array = new ArrayList<>();
        nextChar(); // Skip '['
        skipWhitespace();

        if (currentChar == ']' && (hasNextLine || lineIndex < currentLine.length())) {
            nextChar(); // Empty array
            return array;
        }

        while (hasNextLine || (currentLine != null && lineIndex < currentLine.length())) {
            array.add(parseValue());
            skipWhitespace();
            if (currentChar == ']' && (hasNextLine || lineIndex < currentLine.length())) {
                nextChar(); // Skip ']'
                break;
            }
            if (!hasNextLine && (currentLine == null || lineIndex >= currentLine.length()) || currentChar != ',') {
                throw new IllegalArgumentException("Expected ',' or ']'");
            }
            nextChar(); // Skip ','
            skipWhitespace();
        }
        return array;
    }

    // Parse a JSON string
    private String parseString() throws IOException {
        nextChar(); // Skip opening quote
        StringBuilder result = new StringBuilder();
        while (hasNextLine || (currentLine != null && lineIndex < currentLine.length())) {
            if (currentChar == '"') {
                nextChar(); // Skip closing quote
                return result.toString();
            }
            if (currentChar == '\\') {
                nextChar();
                if (!hasNextLine && (currentLine == null || lineIndex >= currentLine.length())) {
                    throw new IllegalArgumentException("Unexpected end of string");
                }
                switch (currentChar) {
                    case '"':
                    case '\\':
                    case '/':
                        result.append((char) currentChar);
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
                        throw new IllegalArgumentException("Invalid escape character: " + (char) currentChar);
                }
            } else {
                result.append((char) currentChar);
            }
            nextChar();
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    // Parse a Unicode escape sequence
    private char parseUnicode() throws IOException {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            nextChar();
            if (!hasNextLine && (currentLine == null || lineIndex >= currentLine.length())) {
                throw new IllegalArgumentException("Invalid Unicode sequence");
            }
            hex.append((char) currentChar);
        }
        try {
            return (char) Integer.parseInt(hex.toString(), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Unicode sequence: " + hex);
        }
    }

    // Parse a JSON number
    private Number parseNumber() throws IOException {
        StringBuilder number = new StringBuilder();
        while ((hasNextLine || (currentLine != null && lineIndex < currentLine.length())) &&
                (Character.isDigit(currentChar) || currentChar == '-' || currentChar == '+' || currentChar == '.' || currentChar == 'e' || currentChar == 'E')) {
            number.append((char) currentChar);
            nextChar();
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
    private Boolean parseBoolean() throws IOException {
        StringBuilder bool = new StringBuilder();
        for (int i = 0; i < 5 && (hasNextLine || (currentLine != null && lineIndex < currentLine.length())); i++) {
            bool.append((char) currentChar);
            nextChar();
            if (bool.toString().equals("true")) {
                return true;
            }
            if (bool.toString().equals("false")) {
                return false;
            }
        }
        throw new IllegalArgumentException("Invalid boolean: " + bool);
    }

    // Parse a JSON null
    private Object parseNull() throws IOException {
        StringBuilder nullStr = new StringBuilder();
        for (int i = 0; i < 4 && (hasNextLine || (currentLine != null && lineIndex < currentLine.length())); i++) {
            nullStr.append((char) currentChar);
            nextChar();
        }
        if (nullStr.toString().equals("null")) {
            return null;
        }
        throw new IllegalArgumentException("Invalid null: " + nullStr);
    }

    // Skip whitespace characters
    private void skipWhitespace() throws IOException {
        while (hasNextLine || (currentLine != null && lineIndex < currentLine.length())) {
            if (Character.isWhitespace(currentChar)) {
                nextChar();
            } else {
                break;
            }
        }
    }

    // Example usage
    public static void main(String[] args) {
        String jsonString = "{\"name\": \"John\", \"age\": 30, \"isStudent\": false, \"grades\": [90, 85, 88], \"address\": {\"street\": \"123 Main St\", \"city\": \"Anytown\"}}";
        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(jsonString))) {
            JsonParser parser = new JsonParser();
            Object result = parser.parse(reader);
            System.out.println(result);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }
}
