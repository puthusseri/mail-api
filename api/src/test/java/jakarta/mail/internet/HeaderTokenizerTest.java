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
 */
public class HeaderTokenizerTest {

    private static boolean gen_test_input = false; // output good for input to -p
    private static boolean parse_mail = false;     // parse input in mail format
    private static boolean return_comments = false; // return comments as tokens
    private static boolean mime = false;           // use MIME specials
    static int errors = 0;

    static boolean junit;
    static List<Object[]> testData;

    /**
     * Provides test data for the parameterized test.
     */
    public static Collection<Object[]> data() throws IOException {
        junit = true;
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
    public static void main(String[] argv) throws Exception {
        int optind;
        for (optind = 0; optind < argv.length; optind++) {
            if (argv[optind].equals("-")) {
                // ignore
            } else if (argv[optind].equals("-g")) {
                gen_test_input = true;
            } else if (argv[optind].equals("-p")) {
                parse_mail = true;
            } else if (argv[optind].equals("-c")) {
                return_comments = true;
            } else if (argv[optind].equals("-m")) {
                mime = true;
            } else if (argv[optind].equals("--")) {
                optind++;
                break;
            } else if (argv[optind].startsWith("-")) {
                System.out.println(
                        "Usage: tokenizertest [-g] [-p] [-c] [-m] [-] [header ...]");
                System.exit(1);
            } else {
                break;
            }
        }

        /*
         * If there's any args left on the command line,
         * concatenate them into a string and test that.
         */
        if (optind < argv.length) {
            StringBuffer sb = new StringBuffer();
            for (int i = optind; i < argv.length; i++) {
                sb.append(argv[i]);
                sb.append(" ");
            }
            performTest("To", sb.toString(), null);
        } else {
            // read from stdin
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(System.in));
            String s;

            if (parse_mail)
                parse(in, new ArrayList<>());
            else {
                while ((s = in.readLine()) != null)
                    performTest("To", s, null);
            }
        }
        System.exit(errors);

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
                try {
                    if (junit)
                        testData.add(new Object[]{
                                header.substring(0, i),
                                header.substring(i + 2),
                                expect});
                    else
                        performTest(header.substring(0, i), header.substring(i + 2),
                                expect);
                } catch (StringIndexOutOfBoundsException e) {
                    // ignore
                }
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
        else if (!junit)
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
                if (junit) {
                    assertEquals(expect.length, toklist.size(), "Number of tokens");
                } else {
                    out.println("Got " + toklist.size() + " tokens:");
                    if (expect != null && toklist.size() != expect.length) {
                        out.println("Expected " + expect.length + " tokens");
                        errors++;
                    }
                }
            }


            for (int i = 0; i < toklist.size(); i++) {
                tok = toklist.elementAt(i);
                if (gen_test_input) {
                    out.println("\t" + type(tok.getType()) +
                            "\t" + tok.getValue());
                } else {

                    if (!junit)
                        out.println("\t[" + (i + 1) + "] " + type(tok.getType()) +
                            "\t" + tok.getValue());
                    if (expect != null && i < expect.length) {
                        HeaderTokenizer.Token expectedToken = makeToken(expect[i]);
                        if (junit) {
                            assertEquals(expectedToken.getType(), tok.getType(), "Token type mismatch at index " + i);
                            assertEquals(expectedToken.getValue(), tok.getValue(), "Token value mismatch at index " + i);
                        } else {
                            if (expectedToken.getType() != tok.getType() ||
                                    !expectedToken.getValue().equals(tok.getValue())) {
                                out.println("\tExpected:\t" +
                                        type(expectedToken.getType()) + "\t" + expectedToken.getValue());
                                errors++;
                            }
                        }
                    }
                }
            }
        } catch (ParseException e) {
            if (gen_test_input)
                out.println("Expect: Exception " + e);
            else {
                if (junit) {
                    assertTrue(expect.length == 1 && "Exception".equals(expect[0]), "Expected exception");
                } else {
                    out.println("Got Exception: " + e);
                    if (expect != null &&
                            (expect.length != 1 || !expect[0].equals("Exception"))) {
                        out.println("Expected " + expect.length + " tokens");
                        for (int i = 0; i < expect.length; i++)
                            out.println("\tExpected:\t" + expect[i]);
                        errors++;
                    }
                }
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

    private static final String n(String s) {
        return s == null ? "<null>" : s;
    }
}
