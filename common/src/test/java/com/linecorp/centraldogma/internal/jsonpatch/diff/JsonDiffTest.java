/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: https://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.linecorp.centraldogma.internal.jsonpatch.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Equivalence;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import com.linecorp.centraldogma.internal.jsonpatch.JsonPatch;
import com.linecorp.centraldogma.internal.jsonpatch.JsonPatchException;
import com.linecorp.centraldogma.internal.jsonpatch.utils.JsonNumEquals;

public final class JsonDiffTest
{
    private static final Equivalence<JsonNode> EQUIVALENCE
        = JsonNumEquals.getInstance();

    private final JsonNode testData;

    public JsonDiffTest()
        throws IOException
    {
        final String resource = "/jsonpatch/diff/diff.json";
        URL url = this.getClass().getResource(resource);
        ObjectMapper objectMapper = new ObjectMapper();
        testData = objectMapper.readTree(url);
    }

    @DataProvider
    public Iterator<Object[]> getPatchesOnly()
    {
        final List<Object[]> list = Lists.newArrayList();

        for (final JsonNode node: testData)
            list.add(new Object[] { node.get("first"), node.get("second") });

        return list.iterator();
    }

    @Test(dataProvider = "getPatchesOnly")
    public void generatedPatchAppliesCleanly(final JsonNode first,
        final JsonNode second)
            throws JsonPatchException
    {
        final JsonPatch patch = JsonDiff.asJsonPatch(first, second);
        final Predicate<JsonNode> predicate = EQUIVALENCE.equivalentTo(second);
        final JsonNode actual = patch.apply(first);

        assertThat(predicate.apply(actual)).overridingErrorMessage(
            "Generated patch failed to apply\nexpected: %s\nactual: %s",
            second, actual
        ).isTrue();
    }

    @DataProvider
    public Iterator<Object[]> getLiteralPatches()
    {
        final List<Object[]> list = Lists.newArrayList();

        for (final JsonNode node: testData) {
            if (!node.has("patch"))
                continue;
            list.add(new Object[] {
                node.get("message").textValue(), node.get("first"),
                node.get("second"), node.get("patch")
            });
        }

        return list.iterator();
    }

    @Test(
        dataProvider = "getLiteralPatches",
        dependsOnMethods = "generatedPatchAppliesCleanly"
    )
    public void generatedPatchesAreWhatIsExpected(final String message,
        final JsonNode first, final JsonNode second, final JsonNode expected)
    {
        final JsonNode actual = JsonDiff.asJson(first, second);
        final Predicate<JsonNode> predicate
            = EQUIVALENCE.equivalentTo(expected);

        assertThat(predicate.apply(actual)).overridingErrorMessage(
            "patch is not what was expected\nscenario: %s\n"
            + "expected: %s\nactual: %s\n", message, expected, actual
        ).isTrue();
    }
}
