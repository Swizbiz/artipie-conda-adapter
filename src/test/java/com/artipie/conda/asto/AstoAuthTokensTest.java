/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.asto;

import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.conda.AuthTokens;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.json.Json;
import javax.json.JsonObject;
import org.cactoos.io.ReaderOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Test for {@link AstoAuthTokens}.
 * @since 0.5
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class AstoAuthTokensTest {

    /**
     * Test resource path.
     */
    private static final String TOKENS_JSON = "AstoAuthTokensTest/tokens.json";

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void returnsEmptyIfTokensDoNotExist() {
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto).get("000").toCompletableFuture()
                .join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsEmptyByUsernameIfTokensDoNotExist() {
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto).find("Any").toCompletableFuture().join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsTokenWhenFound() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        final String token = "abc123";
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto).get(token).toCompletableFuture().join().get(),
            new IsEqual<>(
                // @checkstyle MagicNumberCheck (1 line)
                new AuthTokens.TokenItem(token, "alice", Instant.ofEpochMilli(4_108_568_400_000L))
            )
        );
    }

    @Test
    void returnsTokenByUsernameWhenFound() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        final String name = "alice";
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto).find(name).toCompletableFuture().join().get(),
            new IsEqual<>(
                // @checkstyle MagicNumberCheck (1 line)
                new AuthTokens.TokenItem("abc123", name, Instant.ofEpochMilli(4_108_568_400_000L))
            )
        );
    }

    @Test
    void returnsEmptyWhenExpired() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto).get("xyz098").toCompletableFuture().join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsEmptyByUsernameWhenExpired() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto).find("John").toCompletableFuture().join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void generatesTokenWhenTokensExist() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        final AuthTokens.TokenItem token = new AstoAuthTokens(this.asto)
            .generate("Jane", Duration.ofDays(365)).toCompletableFuture().join();
        final JsonObject tokens = Json.createReader(
            new ReaderOf(
                new BlockingStorage(this.asto).value(AstoAuthTokens.TKNS), StandardCharsets.UTF_8
            )
        ).readObject().getJsonObject("tokens");
        MatcherAssert.assertThat(
            "Resulting json format is not as expected",
            tokens,
            Matchers.allOf(
                new JsonHas(
                    token.token(),
                    Matchers.allOf(
                        new JsonHas("name", new JsonValueIs("Jane")),
                        new JsonHas("expire", new JsonValueIs(token.validUntil().toEpochMilli()))
                    )
                ),
                new JsonHas(
                    "abc123",
                    Matchers.allOf(
                        new JsonHas("name", new JsonValueIs("alice")),
                        new JsonHas("expire", new JsonValueIs(4_108_568_400_000L))
                    )
                ),
                new JsonHas(
                    "xyz098",
                    Matchers.allOf(
                        new JsonHas("name", new JsonValueIs("John")),
                        new JsonHas("expire", new JsonValueIs(1_516_376_429_792L))
                    )
                )
            )
        );
    }

    @Test
    void generatesTokenWhenTokensDoNotExist() {
        final AuthTokens.TokenItem token = new AstoAuthTokens(this.asto)
            .generate("Jordan", Duration.ofDays(60)).toCompletableFuture().join();
        final JsonObject tokens = Json.createReader(
            new ReaderOf(
                new BlockingStorage(this.asto).value(AstoAuthTokens.TKNS), StandardCharsets.UTF_8
            )
        ).readObject().getJsonObject("tokens");
        MatcherAssert.assertThat(
            "Resulting json format is not as expected",
            tokens,
            new JsonHas(
                token.token(),
                Matchers.allOf(
                    new JsonHas("name", new JsonValueIs("Jordan")),
                    new JsonHas("expire", new JsonValueIs(token.validUntil().toEpochMilli()))
                )
            )
        );
    }

    @Test
    void removesTokenWhenPresent() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        MatcherAssert.assertThat(
            "Should return true when token removed",
            new AstoAuthTokens(this.asto).remove("abc123").toCompletableFuture().join(),
            new IsEqual<>(true)
        );
        final JsonObject tokens = Json.createReader(
            new ReaderOf(
                new BlockingStorage(this.asto).value(AstoAuthTokens.TKNS), StandardCharsets.UTF_8
            )
        ).readObject().getJsonObject("tokens");
        MatcherAssert.assertThat(
            "Resulting json is not as expected",
            tokens,
            new JsonHas(
                "xyz098",
                Matchers.allOf(
                    new JsonHas("name", new JsonValueIs("John")),
                    new JsonHas("expire", new JsonValueIs(1_516_376_429_792L))
                )
            )
        );
    }

    @Test
    void returnsFalseIfTokenNotFound() throws JSONException {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        MatcherAssert.assertThat(
            "Should return false when token not removed",
            new AstoAuthTokens(this.asto).remove("any").toCompletableFuture().join(),
            new IsEqual<>(false)
        );
        JSONAssert.assertEquals(
            "Should not change json token file",
            new String(
                new BlockingStorage(this.asto).value(AstoAuthTokens.TKNS), StandardCharsets.UTF_8
            ),
            new String(
                new TestResource(AstoAuthTokensTest.TOKENS_JSON).asBytes(), StandardCharsets.UTF_8
            ),
            true
        );
    }
}
