/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link AstoMergedJson}.
 * @since 0.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoMergedJsonTest {

    /**
     * Test key.
     */
    private static final Key.From KEY = new Key.From("repodata.json");

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void addsItemsWhenInputIsPresent() throws JSONException {
        new TestResource("MergedJsonTest/mp1_input.json")
            .saveTo(this.asto, AstoMergedJsonTest.KEY);
        new AstoMergedJson(this.asto, AstoMergedJsonTest.KEY).merge(
            new MapOf<String, JsonObject>(
                this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json")
            )
        ).toCompletableFuture().join();
        JSONAssert.assertEquals(
            this.getRepodata(),
            new String(
                new TestResource("AstoMergedJsonTest/addsItemsWhenInputIsPresent.json")
                    .asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void addsItemsWhenInputIsAbsent() throws JSONException {
        new AstoMergedJson(this.asto, AstoMergedJsonTest.KEY).merge(
            new MapOf<String, JsonObject>(
                this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json")
            )
        ).toCompletableFuture().join();
        JSONAssert.assertEquals(
            this.getRepodata(),
            new String(
                new TestResource("AstoMergedJsonTest/addsItemsWhenInputIsAbsent.json")
                    .asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    private String getRepodata() {
        return new PublisherAs(
            this.asto.value(AstoMergedJsonTest.KEY).toCompletableFuture().join()
        ).asciiString().toCompletableFuture().join();
    }

    private MapEntry<String, JsonObject> packageItem(final String filename,
        final String resourse) {
        return new MapEntry<>(
            filename,
            Json.createReader(
                new TestResource(String.format("MergedJsonTest/%s", resourse)).asInputStream()
            ).readObject()
        );
    }

}
