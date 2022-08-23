/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.conda.AuthTokens;
import com.artipie.http.Headers;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DeleteTokenSlice}.
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DeleteTokenSliceTest {

    @Test
    void removesToken() {
        MatcherAssert.assertThat(
            "Incorrect response status, 201 CREATED is expected",
            new DeleteTokenSlice(new FakeTokens(true)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.DELETE, "/authentications$"),
                new Headers.From(new Authorization.Token("abc123")),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsBadRequestIfTokenIsNotFound() {
        MatcherAssert.assertThat(
            "Incorrect response status, BAD_REQUEST is expected",
            new DeleteTokenSlice(new FakeTokens(false)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/authentications$"),
                new Headers.From(new Authorization.Token("any")),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsUnauthorizedIfHeaderIsNotPresent() {
        MatcherAssert.assertThat(
            "Incorrect response status, BAD_REQUEST is expected",
            new DeleteTokenSlice(new FakeTokens(false)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.UNAUTHORIZED),
                new RequestLine(RqMethod.DELETE, "/authentications$"),
                Headers.EMPTY,
                Content.EMPTY
            )
        );
    }

    /**
     * Fake test implementation of {@link AuthTokens}.
     * @since 0.3
     * @checkstyle JavadocVariableCheck (500 lines)
     */
    private static final class FakeTokens implements AuthTokens {

        private final boolean removed;

        private FakeTokens(final boolean removed) {
            this.removed = removed;
        }

        @Override
        public CompletionStage<Optional<TokenItem>> get(final String token) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public CompletionStage<Optional<TokenItem>> find(final String username) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public CompletionStage<TokenItem> generate(final String name, final Duration ttl) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public CompletionStage<Boolean> remove(final String token) {
            return CompletableFuture.completedFuture(this.removed);
        }
    }

}
