/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package apitest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import jdk.codetools.apidiff.Main;
import jdk.codetools.apidiff.Main.Result;
import jdk.codetools.apidiff.Messages;
import jdk.codetools.apidiff.Options;

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for the main program.
 */
public class MainTest extends APITester {

    /**
     * Tests the {@code --help} option.
     *
     * The test verifies that all the necessary resources are defined
     * and that the corresponding value appears in the output generated
     * by the option.
     */
    @Test
    public void testHelp() {
        for (String help : Options.Option.HELP.getNames()) {
            Map<OutputKind, String> outMap = run(List.of(help));
            String out = outMap.get(OutputKind.OUT);
            for (Options.Option option : Options.Option.values()) {
                option.getNames().forEach(name -> assertTrue(out.contains(name)));
            }
            assertFalse(out.contains("opt.desc."));
            assertFalse(out.contains("opt.arg."));
            Messages messages = Messages.instance("jdk.codetools.apidiff.resources.help");
            for (String key : messages.getKeys()) {
                if (key.startsWith("opt.arg")) {
                    assertTrue(out.contains(messages.getString(key)));
                }
                if (key.startsWith("opt.desc")) {
                    messages.getString(key).lines().forEach(line ->
                            assertTrue(line.contains("{0}") || out.contains(line)));
                }
            }
        }
    }

    @Test
    public void testBadOption() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            new ModuleBuilder(tb, false, "mA")
                    .exports("p")
                    .classes("package p; public class C { }\n")
                    .write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "mA/**",
                "-notAnOption",
                "--help",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options, EnumSet.of(Main.Result.BAD_ARGS));

        String err = outMap.get(OutputKind.ERR);
        assertTrue(err.contains("unknown option"));

        // verify processing stopped after detecting a bad option
        String out = outMap.get(OutputKind.OUT);
        assertFalse(out.contains("help"));
        assertFalse(out.contains("Completed comparison:"));
    }

    @Test
    public void testMakeOutputDirectory() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            new ModuleBuilder(tb, false, "mA")
                    .exports("p")
                    .classes("package p; public class C { }\n")
                    .write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "mA/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);

        assertTrue(Files.exists(base.resolve("out")));
    }

    @Test
    public void testSyntaxError() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            new ModuleBuilder(tb, false, "mA")
                    .exports("p")
                    .classes("package p; public class C " + (a == 0 ? "{ }" : "") +"\n")
                    .write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "mA/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options, EnumSet.of(Result.FAULT));

        String err = outMap.get(OutputKind.ERR);
        assertTrue(err.contains("1 error"));

        String out = outMap.get(OutputKind.ERR);
        assertFalse(out.contains("Completed comparison:"));
    }
}
