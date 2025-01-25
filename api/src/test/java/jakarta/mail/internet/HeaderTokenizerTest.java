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
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test MIME HeaderTokenizer.
 * Converted to JUnit 5.
 */
public class HeaderTokenizerTest {

    private static boolean gen_test_input = false; // output good for input to -p
    private static boolean parse_mail = false;     // parse input in mail format
    private static boolean return_comments = false; // return comments as tokens
    private static boolean mime = false;           // use MIME specials

    /**
     * Provides test data for the parameterized test.
     */
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> testData = new ArrayList<>();
        parse(new BufferedReader(new InputStreamReader(
                HeaderTokenizerTest.class.getResourceAsStream("tokenlist"))), testData);
        return testData;
    }

    /**
     * Parameterized test for HeaderTokenizer.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void test(String header, String value, String[] expect) {
        performTest(header, value, expect);
    }

    /**
     * Parses the input file to generate test cases.
     */
    private static void parse(BufferedReader in, List<Object[]> testData) throws IOException {
        String header = "";

        for (;;) {
            String s = in.readLine();
            if (s != null && s.length() > 0) {
                char c = s.charAt(0);
                if (c == ' ' || c == '\t') {
                    // a continuation line, add it to the current header
                    header += '\n' + s;
                    continue;
                }
            }
            // "s" is the next header, "header" is the last complete header
            if (header.startsWith("From: ") ||
                    header.startsWith("To: ") ||
                    header.startsWith("Cc: ")) {
                int i;
                String[] expect = null;
                if (s != null && s.startsWith("Expect: ")) {
                    try {
                        int nexpect = Integer.parseInt(s.substring(8));
                        expect = new String[nexpect];
                        for (i = 0; i < nexpect; i++) {
                            expect[i] = in.readLine().trim();
                        }
                    } catch (NumberFormatException e) {
                        if (s.substring(8, 17).equals("Exception")) {
                            expect = new String[1];
                            expect[0] = "Exception";
                        }
                    }
                }
                i = header.indexOf(':');
                testData.add(new Object[]{
                        header.substring(0, i),
                        header.substring(i + 2),
                        expect
                });
            }
            if (s == null)
                return; // EOF
            if (s.length() == 0) {
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

    /**
     * Test the header's value to see if it can be tokenized as expected.
     */
    private static void performTest(String header, String value, String[] expect) {
        PrintStream out = System.out;
        if (gen_test_input)
            out.println(header + ": " + value);
        else
            out.println("Test: " + value);

        try {
            HeaderTokenizer ht = new HeaderTokenizer(value,
                    mime ? HeaderTokenizer.MIME : HeaderTokenizer.RFC822,
                    !return_comments);
            HeaderTokenizer.Token tok;
            Vector<HeaderTokenizer.Token> toklist = new Vector<>();
            while ((tok = ht.next()).getType() != HeaderTokenizer.Token.EOF)
                toklist.addElement(tok);
            if (gen_test_input) {
                out.println("Expect: " + toklist.size());
            } else {
                assertEquals(expect.length, toklist.size(), "Number of tokens");
            }

            for (int i = 0; i < toklist.size(); i++) {
                tok = toklist.elementAt(i);
                if (gen_test_input) {
                    out.println("\t" + type(tok.getType()) +
                            "\t" + tok.getValue());
                } else {
                    HeaderTokenizer.Token expectedToken = makeToken(expect[i]);
                    assertEquals(expectedToken.getType(), tok.getType(), "Token type mismatch at index " + i);
                    assertEquals(expectedToken.getValue(), tok.getValue(), "Token value mismatch at index " + i);
                }
            }
        } catch (ParseException e) {
            if (gen_test_input)
                out.println("Expect: Exception " + e);
            else {
                assertTrue(expect.length == 1 && "Exception".equals(expect[0]), "Expected exception");
            }
        }
    }

    private static String type(int t) {
        switch (t) {
            case HeaderTokenizer.Token.ATOM:
                return "ATOM";
            case HeaderTokenizer.Token.QUOTEDSTRING:
                return "QUOTEDSTRING";
            case HeaderTokenizer.Token.COMMENT:
                return "COMMENT";
            case HeaderTokenizer.Token.EOF:
                return "EOF";
            default:
                return t < 0 ? "UNKNOWN" : "SPECIAL";
        }
    }

    private static int type(String s) {
        switch (s) {
            case "ATOM":
                return HeaderTokenizer.Token.ATOM;
            case "QUOTEDSTRING":
                return HeaderTokenizer.Token.QUOTEDSTRING;
            case "COMMENT":
                return HeaderTokenizer.Token.COMMENT;
            case "EOF":
                return HeaderTokenizer.Token.EOF;
            default:
                return 0; // SPECIAL
        }
    }

    private static HeaderTokenizer.Token makeToken(String line) {
        int i = line.indexOf('\t');
        int t = type(line.substring(0, i));
        String value = line.substring(i + 1);
        return t == 0 ? new HeaderTokenizer.Token(value.charAt(0), value)
                : new HeaderTokenizer.Token(t, value);
    }
}
