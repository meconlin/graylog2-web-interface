/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import lib.ApiClient;
import models.api.responses.system.indices.*;
import org.joda.time.DateTime;
import play.Logger;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Index {

    public interface Factory {
        Index fromRangeResponse(IndexRangeSummary ir);
    }

    private final ApiClient api;

    private final Range range;
    private Info indexInfo;
    private final String name;

    @AssistedInject
    public Index(ApiClient api, @Assisted IndexRangeSummary ir) {
        this.api = api;

        this.range = new Range(ir);
        this.name = ir.index;
    }

    public Range getRange() {
        return range;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        try {
            return Integer.parseInt(getName().substring(getName().lastIndexOf("_") + 1));
        } catch(Exception e) {
            Logger.error("Could not get number of index [{}].", getName(), e);
            return -1;
        }
    }

    public Info getInfo() {
        if (indexInfo == null) {
            loadIndexInfo();
        }

        return indexInfo;
    }

    private void loadIndexInfo() {
        try {
            this.indexInfo = new Info(api.get(IndexSummaryResponse.class).path("/system/indexer/indices/{0}", getName()).execute());
        } catch (Exception e) {
           Logger.error("Could not get index information for index [{}]", getName(), e);
        }
    }

    public class Info {

        private final int openSearchContexts;
        private final long storeSizeBytes;
        private final long segments;

        private final ShardDocumentsResponse documents;
        private final ShardMeter primaryShards;
        private final ShardMeter allShards;
        private final List<ShardRoutingResponse> shardRouting;

        public Info(IndexSummaryResponse i) {
            this.primaryShards = new ShardMeter(i.primaryShards);
            this.allShards = new ShardMeter(i.allShards);

            IndexShardsResponse primaries = i.primaryShards;
            this.openSearchContexts = primaries.openSearchContexts;
            this.storeSizeBytes = primaries.storeSizeBytes;
            this.segments = primaries.segments;
            this.documents = primaries.documents;

            this.shardRouting = i.routing;
        }

        public int getOpenSearchContexts() {
            return openSearchContexts;
        }

        public long getStoreSizeBytes() {
            return storeSizeBytes;
        }

        public long getSegments() {
            return segments;
        }

        public List<ShardRoutingResponse> getShardRouting() {
            return shardRouting;
        }

        public ShardDocumentsResponse getDocuments() {
            return documents;
        }

        public ShardMeter getPrimaryShards() {
            return primaryShards;
        }

        public ShardMeter getAllShards() {
            return allShards;
        }

        public class ShardMeter {

            private final ShardMeterResponse indexMeter;
            private final ShardMeterResponse flushMeter;
            private final ShardMeterResponse getMeter;
            private final ShardMeterResponse mergeMeter;
            private final ShardMeterResponse searchFetchMeter;
            private final ShardMeterResponse searchQueryMeter;
            private final ShardMeterResponse refreshMeter;

            public ShardMeter(IndexShardsResponse shards) {
                this.indexMeter = shards.index;
                this.flushMeter = shards.flush;
                this.getMeter = shards.get;
                this.mergeMeter = shards.merge;
                this.searchFetchMeter = shards.searchFetch;
                this.searchQueryMeter = shards.searchQuery;
                this.refreshMeter = shards.refresh;
            }

            public ShardMeterResponse getIndexMeter() {
                return indexMeter;
            }

            public ShardMeterResponse getFlushMeter() {
                return flushMeter;
            }

            public ShardMeterResponse getGetMeter() {
                return getMeter;
            }

            public ShardMeterResponse getMergeMeter() {
                return mergeMeter;
            }

            public ShardMeterResponse getSearchFetchMeter() {
                return searchFetchMeter;
            }

            public ShardMeterResponse getSearchQueryMeter() {
                return searchQueryMeter;
            }

            public ShardMeterResponse getRefreshMeter() {
                return refreshMeter;
            }

        }

    }

    public class Range {

        private final DateTime starts;
        private final boolean providesCalculationInfo;

        private long calculationTookMs = 0;
        private DateTime calculatedAt = null;

        public Range(IndexRangeSummary ir) {
            this.starts = new DateTime(ir.starts);

            if (ir.calculatedAt != null && !ir.calculatedAt.isEmpty()  && ir.calculationTookMs >= 0) {
                this.providesCalculationInfo = true;
                this.calculationTookMs = ir.calculationTookMs;
                this.calculatedAt = new DateTime(ir.calculatedAt);
            } else {
                this.providesCalculationInfo = false;
            }
        }

        public DateTime getStarts() {
            return starts;
        }

        public boolean isProvidesCalculationInfo() {
            return providesCalculationInfo;
        }

        public long getCalculationTookMs() {
            return calculationTookMs;
        }

        public DateTime getCalculatedAt() {
            return calculatedAt;
        }
    }

}
