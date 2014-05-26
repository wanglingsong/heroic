package com.spotify.heroic.backend.list;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.spotify.heroic.aggregator.AggregatorGroup;
import com.spotify.heroic.async.Callback;
import com.spotify.heroic.async.ConcurrentCallback;
import com.spotify.heroic.backend.BackendManager.QueryMetricsResult;
import com.spotify.heroic.backend.MetricBackend;
import com.spotify.heroic.backend.model.FindRowGroups;
import com.spotify.heroic.cache.AggregationCache;

@Slf4j
@RequiredArgsConstructor
public class QueryGroup {
    private final List<MetricBackend> backends;
    private final AggregationCache cache;

    public Callback<QueryMetricsResult> execute(FindRowGroups criteria, final AggregatorGroup aggregator) {
        return findRowGroups(criteria).transform(buildTransformer(criteria, aggregator));
    }

    public RowGroupsHandle buildTransformer(FindRowGroups criteria, final AggregatorGroup aggregator) {
        return new RowGroupsHandle(cache, aggregator, criteria.getRange());
    }

    public Callback<RowGroups> findRowGroups(FindRowGroups criteria) {
        final List<Callback<FindRowGroups.Result>> queries = new ArrayList<Callback<FindRowGroups.Result>>();

        for (final MetricBackend backend : backends) {
            try {
                queries.add(backend.findRowGroups(criteria));
            } catch (final Exception e) {
                log.error("Failed to query backend", e);
            }
        }

        return ConcurrentCallback.newReduce(queries, new FindRowGroupsReducer());
    }
}
