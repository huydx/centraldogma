/*
 * Copyright 2017 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.centraldogma.server.internal.storage.repository.cache;

import static com.linecorp.centraldogma.common.Author.SYSTEM;
import static com.linecorp.centraldogma.common.Revision.HEAD;
import static com.linecorp.centraldogma.common.Revision.INIT;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.linecorp.centraldogma.common.Change;
import com.linecorp.centraldogma.common.Commit;
import com.linecorp.centraldogma.common.Entry;
import com.linecorp.centraldogma.common.EntryType;
import com.linecorp.centraldogma.common.Markup;
import com.linecorp.centraldogma.common.Query;
import com.linecorp.centraldogma.common.QueryResult;
import com.linecorp.centraldogma.common.Revision;
import com.linecorp.centraldogma.server.internal.storage.project.Project;
import com.linecorp.centraldogma.server.internal.storage.repository.Repository;

public class CachingRepositoryTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private Repository delegateRepo;

    @Test
    @SuppressWarnings("unchecked")
    public void query() {
        final Repository repo = setMockNames(newCachingRepo(10));
        final Query<Object> query = Query.identity("/baz.txt");
        final QueryResult<Object> queryResult = new QueryResult<>(new Revision(10), EntryType.TEXT, "qux");

        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(new Revision(10));
        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(HEAD);

        // Uncached
        when(delegateRepo.getOrElse(any(), any(Query.class), any())).thenReturn(completedFuture(queryResult));
        assertThat(repo.get(HEAD, query).join()).isEqualTo(queryResult);
        verify(delegateRepo).getOrElse(new Revision(10), query, CacheableQueryCall.EMPTY);
        verifyNoMoreInteractions(delegateRepo);

        // Cached
        clearInvocations(delegateRepo);
        assertThat(repo.get(HEAD, query).join()).isEqualTo(queryResult);
        assertThat(repo.get(new Revision(10), query).join()).isEqualTo(queryResult);
        verify(delegateRepo, never()).getOrElse(any(), any(Query.class), any());
        verifyNoMoreInteractions(delegateRepo);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void queryMissingEntry() {
        final Repository repo = setMockNames(newCachingRepo(10));
        final Query<Object> query = Query.identity("/baz.txt");
        final QueryResult<Object> queryResult = CacheableQueryCall.EMPTY;

        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(new Revision(10));
        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(HEAD);

        // Uncached
        when(delegateRepo.getOrElse(any(), any(Query.class), any()))
                .thenReturn(completedFuture(CacheableQueryCall.EMPTY));
        assertThat(repo.getOrElse(HEAD, query, null).join()).isNull();
        verify(delegateRepo).getOrElse(new Revision(10), query, CacheableQueryCall.EMPTY);
        verifyNoMoreInteractions(delegateRepo);

        // Cached
        clearInvocations(delegateRepo);
        assertThat(repo.getOrElse(HEAD, query, null).join()).isNull();
        assertThat(repo.getOrElse(new Revision(10), query, null).join()).isNull();
        verify(delegateRepo, never()).getOrElse(any(), any(Query.class), any());
        verifyNoMoreInteractions(delegateRepo);
    }

    @Test
    public void find() {
        final Repository repo = setMockNames(newCachingRepo(10));
        final Map<String, Entry<?>> entries = ImmutableMap.of("/baz.txt", Entry.ofText("/baz.txt", "qux"));

        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(new Revision(10));
        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(HEAD);

        // Uncached
        when(delegateRepo.find(any(), any(), any())).thenReturn(completedFuture(entries));
        assertThat(repo.find(HEAD, "/**", ImmutableMap.of()).join()).isEqualTo(entries);
        verify(delegateRepo).find(new Revision(10), "/**", ImmutableMap.of());
        verifyNoMoreInteractions(delegateRepo);

        // Cached
        clearInvocations(delegateRepo);
        assertThat(repo.find(HEAD, "/**", ImmutableMap.of()).join()).isEqualTo(entries);
        assertThat(repo.find(new Revision(10), "/**", ImmutableMap.of()).join()).isEqualTo(entries);
        verify(delegateRepo, never()).find(any(), any(), any());
        verifyNoMoreInteractions(delegateRepo);
    }

    @Test
    public void history() {
        final Repository repo = setMockNames(newCachingRepo(3));
        final List<Commit> commits = ImmutableList.of(
                new Commit(new Revision(3), SYSTEM, "third",  "", Markup.MARKDOWN),
                new Commit(new Revision(3), SYSTEM, "second", "", Markup.MARKDOWN),
                new Commit(new Revision(3), SYSTEM, "first",  "", Markup.MARKDOWN));

        doReturn(completedFuture(new Revision(3))).when(delegateRepo).normalize(new Revision(3));
        doReturn(completedFuture(new Revision(3))).when(delegateRepo).normalize(HEAD);
        doReturn(completedFuture(INIT)).when(delegateRepo).normalize(INIT);
        doReturn(completedFuture(INIT)).when(delegateRepo).normalize(new Revision(-3));

        // Uncached
        when(delegateRepo.history(any(), any(), any(), anyInt())).thenReturn(completedFuture(commits));
        assertThat(repo.history(HEAD, INIT, "/**", Integer.MAX_VALUE).join()).isEqualTo(commits);
        verify(delegateRepo).history(new Revision(3), INIT, "/**", 3);
        verifyNoMoreInteractions(delegateRepo);

        // Cached
        clearInvocations(delegateRepo);
        assertThat(repo.history(HEAD, new Revision(-3), "/**", 3).join()).isEqualTo(commits);
        assertThat(repo.history(HEAD, INIT, "/**", 4).join()).isEqualTo(commits);
        assertThat(repo.history(new Revision(3), new Revision(-3), "/**", 5).join()).isEqualTo(commits);
        assertThat(repo.history(new Revision(3), INIT, "/**", 6).join()).isEqualTo(commits);
        verify(delegateRepo, never()).history(any(), any(), any(), anyInt());
        verifyNoMoreInteractions(delegateRepo);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void singleDiff() {
        final Repository repo = setMockNames(newCachingRepo(10));
        final Query query = Query.identity("/foo.txt");
        final Change change = Change.ofTextUpsert(query.path(), "bar");

        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(new Revision(10));
        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(HEAD);
        doReturn(completedFuture(INIT)).when(delegateRepo).normalize(INIT);
        doReturn(completedFuture(INIT)).when(delegateRepo).normalize(new Revision(-10));

        // Uncached
        when(delegateRepo.diff(any(), any(), any(Query.class))).thenReturn(completedFuture(change));
        assertThat(repo.diff(HEAD, INIT, query).join()).isEqualTo(change);
        verify(delegateRepo).diff(new Revision(10), INIT, query);
        verifyNoMoreInteractions(delegateRepo);

        // Cached
        clearInvocations(delegateRepo);
        assertThat(repo.diff(HEAD, new Revision(-10), query).join()).isEqualTo(change);
        assertThat(repo.diff(HEAD, INIT, query).join()).isEqualTo(change);
        assertThat(repo.diff(new Revision(10), new Revision(-10), query).join()).isEqualTo(change);
        assertThat(repo.diff(new Revision(10), INIT, query).join()).isEqualTo(change);
        verify(delegateRepo, never()).diff(any(), any(), any(Query.class));
        verifyNoMoreInteractions(delegateRepo);
    }

    @Test
    public void multiDiff() {
        final Repository repo = setMockNames(newCachingRepo(10));
        final Map<String, Change<?>> changes = ImmutableMap.of(
                "/foo.txt", Change.ofTextUpsert("/foo.txt", "bar"));

        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(new Revision(10));
        doReturn(completedFuture(new Revision(10))).when(delegateRepo).normalize(HEAD);
        doReturn(completedFuture(INIT)).when(delegateRepo).normalize(INIT);
        doReturn(completedFuture(INIT)).when(delegateRepo).normalize(new Revision(-10));

        // Uncached
        when(delegateRepo.diff(any(), any(), any(String.class))).thenReturn(completedFuture(changes));
        assertThat(repo.diff(HEAD, INIT, "/**").join()).isEqualTo(changes);
        verify(delegateRepo).diff(new Revision(10), INIT, "/**");
        verifyNoMoreInteractions(delegateRepo);

        // Cached
        clearInvocations(delegateRepo);
        assertThat(repo.diff(HEAD, new Revision(-10), "/**").join()).isEqualTo(changes);
        assertThat(repo.diff(HEAD, INIT, "/**").join()).isEqualTo(changes);
        assertThat(repo.diff(new Revision(10), new Revision(-10), "/**").join()).isEqualTo(changes);
        assertThat(repo.diff(new Revision(10), INIT, "/**").join()).isEqualTo(changes);
        verify(delegateRepo, never()).diff(any(), any(), any(Query.class));
        verifyNoMoreInteractions(delegateRepo);
    }

    private Repository newCachingRepo(int headRevision) {
        when(delegateRepo.normalize(HEAD)).thenReturn(completedFuture(new Revision(headRevision)));

        Repository cachingRepo = new CachingRepository(delegateRepo, new RepositoryCache("maximumSize=1000"));

        verify(delegateRepo, times(1)).normalize(HEAD);
        verifyNoMoreInteractions(delegateRepo);
        clearInvocations(delegateRepo);

        return cachingRepo;
    }

    private static Repository setMockNames(Repository mockRepo) {
        Project project = mock(Project.class);
        when(mockRepo.parent()).thenReturn(project);
        when(project.name()).thenReturn("mock_proj");
        when(mockRepo.name()).thenReturn("mock_repo");
        return mockRepo;
    }
}
