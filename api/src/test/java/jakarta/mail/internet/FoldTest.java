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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class FoldTest {

    /**
     * Provides test data for the parameterized test.
     */
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> testData = new ArrayList<>();
        parse(new BufferedReader(new InputStreamReader(
                FoldTest.class.getResourceAsStream("folddata"))), testData);
        return testData;
    }

    /**
     * Parses the test data from the input file.
     */
    private static void parse(BufferedReader in, List<Object[]> testData) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("#") || line.isEmpty())
                continue;

            String orig = readString(in);
            if (line.equals("BOTH")) {
                testData.add(new Object[]{line, orig, null});
            } else {
                String e = in.readLine();
                if (!e.equals("EXPECT"))
                    throw new IOException("TEST DATA FORMAT ERROR");
                String expect = readString(in);
                testData.add(new Object[]{line, orig, expect});
            }
        }
    }

    /**
     * Reads a string that ends with '$', preserving all characters,
     * especially including CR and LF.
     */
    private static String readString(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != '$')
            sb.append((char) c);
        in.readLine(); // throw away the rest of the line
        return sb.toString();
    }

    /**
     * Parameterized test for folding and unfolding.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testFold(String direction, String orig, String expect) {
        if (direction.equals("BOTH")) {
            String fs = MimeUtility.fold(0, orig);
            String us = MimeUtility.unfold(fs);
            assertEquals(orig, us);
        } else if (direction.equals("FOLD")) {
            assertEquals(expect, MimeUtility.fold(0, orig), "Fold");
        } else if (direction.equals("UNFOLD")) {
            assertEquals(expect, MimeUtility.unfold(orig), "Unfold");
        } else {
            fail("Unknown direction: " + direction);
        }
    }
}
