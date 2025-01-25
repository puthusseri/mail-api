/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.mail.internet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InternetAddressTest {

    private static boolean strict = false;        // enforce strict RFC822 syntax
    private static boolean gen_test_input = false;    // output good for input to -p
    private static boolean parse_mail = false;        // parse input in mail format
    private static boolean parse_header = false;    // use parseHeader method?
    private static boolean verbose;            // print progress?
    private static int errors = 0;            // number of errors detected

    /**
     * Provides test data for the parameterized test.
     */
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> testData = new ArrayList<>();
        parse(new BufferedReader(new InputStreamReader(
                InternetAddressTest.class.getResourceAsStream("addrlist"))), testData);
        return testData;
    }

    /**
     * Parses the test data from the input file.
     */
    private static void parse(BufferedReader in, List<Object[]> testData) throws IOException {
        String header = "";
        boolean doStrict = strict;
        boolean doParseHeader = parse_header;

        while (true) {
            String s = in.readLine();
            if (s != null && s.length() > 0) {
                char c = s.charAt(0);
                if (c == ' ' || c == '\t') {
                    // a continuation line, add it to the current header
                    header += '\n' + s;
                    continue;
                }
            }

            if (header.startsWith("Strict: ")) {
                doStrict = Boolean.parseBoolean(value(header));
            } else if (header.startsWith("Header: ")) {
                doParseHeader = Boolean.parseBoolean(value(header));
            } else if (header.startsWith("From: ") ||
                    header.startsWith("To: ") ||
                    header.startsWith("Cc: ")) {
                int i;
                String[] expect = null;
                if (s != null && s.startsWith("Expect: ")) {
                    try {
                        int nexpect = Integer.parseInt(s.substring(8));
                        expect = new String[nexpect];
                        for (i = 0; i < nexpect; i++)
                            expect[i] = readLine(in).trim();
                    } catch (NumberFormatException e) {
                        if (s.substring(8, 17).equals("Exception")) {
                            expect = new String[1];
                            expect[0] = "Exception";
                        }
                    }
                }
                i = header.indexOf(':');
                testData.add(new Object[]{
                        header.substring(0, i), header.substring(i + 2),
                        expect, doStrict, doParseHeader
                });
            }

            if (s == null)
                return; // EOF
            if (s.isEmpty()) {
                while ((s = in.readLine()) != null) {
                    if (s.startsWith("From "))
                        break;
                }
                if (s == null)
                    return;
            }
            header = s;
        }
    }

    private static String value(String header) {
        return header.substring(header.indexOf(':') + 1).trim();
    }

    /**
     * Read an "expected" line, handling continuations
     * (backslash at end of line). If line ends with two backslashes,
     * it's not a continuation, just a line that ends with a single backslash.
     */
    private static String readLine(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (!line.endsWith("\\"))
            return line;
        if (line.endsWith("\\\\"))
            return line.substring(0, line.length() - 1);
        StringBuilder sb = new StringBuilder(line);
        sb.setCharAt(sb.length() - 1, '\n');
        while (true) {
            line = in.readLine();
            sb.append(line);
            if (!line.endsWith("\\"))
                break;
            if (line.endsWith("\\\\")) {
                sb.setLength(sb.length() - 1);
                break;
            }
            sb.setCharAt(sb.length() - 1, '\n');
        }
        return sb.toString();
    }

    /**
     * Parameterized test to validate address parsing.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAddress(String headerName, String headerValue, String[] expected, boolean doStrict, boolean doParseHeader) {
        test(headerName, headerValue, expected, doStrict, doParseHeader);
    }

    /**
     * Test the header's value to see if we can parse it as expected.
     */
    public static void test(String header, String value, String[] expect,
                            boolean doStrict, boolean doParseHeader) {
        PrintStream out = System.out;
        if (gen_test_input)
            pr(header + ": " + value);
        else
            pr("Test: " + value);

        try {
            InternetAddress[] al;
            if (doParseHeader)
                al = InternetAddress.parseHeader(value, doStrict);
            else
                al = InternetAddress.parse(value, doStrict);

            if (gen_test_input) {
                pr("Expect: " + al.length);
            } else {
                assertEquals(expect.length, al.length, "Number of addresses mismatch");
            }

            for (int i = 0; i < al.length; i++) {
                if (gen_test_input) {
                    pr("\t" + al[i].getAddress());
                } else {
                    assertEquals(expect[i], al[i].getAddress(), "Address mismatch at index " + i);
                }
            }

        } catch (AddressException e) {
            if (gen_test_input)
                pr("Expect: Exception " + e);
            else {
                assertNotNull(expect, "Expected exception but got null");
                assertEquals("Exception", expect[0], "Expected Exception but got a different result");
            }
        }
    }

    private static void pr(String s) {
        if (verbose)
            System.out.println(s);
    }

    private static String n(String s) {
        return s == null ? "<null>" : s;
    }
}
