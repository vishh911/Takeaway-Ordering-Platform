package com.campuseateries;

import com.campuseateries.controller.MvcShellController;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** CLI entry bridging users to JDBC-backed MVC stack. */
public final class CampusEateriesApp {

    public static void main(String[] args) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            new MvcShellController(scanner).run();
        }
    }
}
