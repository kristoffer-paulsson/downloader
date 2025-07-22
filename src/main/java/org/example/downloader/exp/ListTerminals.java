package org.example.downloader.exp;

import javax.smartcardio.*;

public class ListTerminals {
    public static void main(String[] args) throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        for (CardTerminal terminal : factory.terminals().list()) {
            System.out.println("Terminal: " + terminal.getName());
        }
    }
}