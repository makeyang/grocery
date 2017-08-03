package com.jd.jes.client.demo;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SpanNearQueryBuilder;
import org.elasticsearch.index.query.SpanQueryBuilder;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class ClientTest {
    /**
     * curl -XPUT 'ip:port/spanneartest' -d'
     {
     "settings" : {
     "number_of_shards" : 1,
     "number_of_replicas": "0"
     },
     "mappings" : {
     "spanneartest" : {
     "properties" : {
     "content" : { "type" : "text", "analyzer" : "whitespace" }
     }
     }
     }
     }
     '


     curl -XPUT 'ip:port/spanneartest/spanneartest/1' -d'
     {
     "content":"大概 明天 有 一辆 从 北京 开往 上海 的 火车 将 提速 10%"
     }
     '

     curl -XPUT 'ip:port/spanneartest/spanneartest/2' -d'
     {
     "content":"大概 明天 有 一辆 从 上海 开往 北京 的 火车 将 提速 10%"
     }
     '

     curl -XPUT 'ip:port/spanneartest/spanneartest/3' -d'
     {
     "content":"大概 明天 上午 八点 左右 有 一辆 从 上海 开往 北京 的 混合 动力 的 火车 将 提速 10%"
     }
     '
     */
    private static Client client = null;
    public static void main(String[] args) {

        try {
            //用集群名字，集群节点地址构建es client
            client = getClient("name", "ip", 9300);
            searchWithSpanNearQuery("spanneartest", "spanneartest");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public static Client getClient(String clusterName, String nodeIp, int nodePort) throws UnknownHostException {
        //设置集群的名字
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put("client.transport.ping_timeout", "1000s")
                .build();
        Collection<Class<? extends Plugin>> plugins = Collections.emptyList();
        //创建集群client并添加集群节点地址
        TransportClient client = new PreBuiltTransportClient(settings, plugins)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(nodeIp), nodePort));

        return client;
    }

    private static void searchWithSpanNearQuery(String indexName, String typeName) {
        try {
            //search result get source
            SpanQueryBuilder st1 = QueryBuilders.spanTermQuery("content", "明天");
            SpanQueryBuilder st2 = QueryBuilders.spanTermQuery("content", "北京");
            SpanQueryBuilder st3 = QueryBuilders.spanTermQuery("content", "上海");
            SpanQueryBuilder st4 = QueryBuilders.spanTermQuery("content", "火车");
            SpanNearQueryBuilder sqb = QueryBuilders.spanNearQuery(st1, 12);
            sqb.addClause(st2);
            sqb.addClause(st3);
            sqb.addClause(st4);
            sqb.inOrder(true);
            SearchResponse sResponse = client.prepareSearch(indexName)
                    .setTypes(typeName)
                    .setSearchType(SearchType.QUERY_THEN_FETCH)
                    .setQuery(sqb)
                    .setFrom(0).setSize(60)
                    .setTimeout(TimeValue.timeValueHours(10))
                    .execute()
                    .actionGet();
            SearchHits hits = sResponse.getHits();
            long count = hits.getTotalHits();
            SearchHit[] hitArray = hits.getHits();
            for(int i = 0; i < count; i++) {
                System.out.println("==================================");
                SearchHit hit = hitArray[i];
                String source = hit.getSourceAsString();
                System.out.println(source);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


}
