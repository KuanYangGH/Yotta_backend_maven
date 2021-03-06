package spider.spiders.wikien;

import app.Config;
import domain.bean.Domain;
import domainTopic.bean.LayerRelation;
import domainTopic.bean.Term;
import utils.Log;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * 爬取英文维基的领域术语
 *
 * @author 张铎
 * @date 2018年3月10日
 */

public class TopicEnCrawler {

    /**
     * 根据领域名存储领域
     *
     * @param domain 课程
     * @return true 表示已经爬取
     */
    public static void storeDomain(Domain domain) {
        List<Domain> list = new ArrayList<>();
        list.add(domain);
        if (!MysqlReadWriteDAO.judgeByClass(Config.DOMAIN_TABLE, domain.getClassName())) {
            MysqlReadWriteDAO.storeDomain(list);
        }
    }

    /**
     * 获取三层领域术语和知识主题（某门课程）
     *
     * @param domain 课程
     * @throws Exception
     */
    public static void storeTopic(Domain domain) throws Exception {
        /**
         * 领域术语采集：爬虫爬取
         * 读取domain表格，获取所有领域名
         * 将所有领域术语存储到damain_layer表格中
         */
        String domainName = domain.getClassName();
        /**
         * 判断该领域是否已经爬取
         */
        Boolean existLayer = MysqlReadWriteDAO.judgeByClass(Config.DOMAIN_LAYER_TABLE, domainName);
        if (!existLayer) {
            Log.log(domain + "：正在爬取领域术语");
            layerExtract(domainName);
        } else {
            Log.log(domain + "：领域术语已经爬取");
        }
        /**
         * 判断该领域是否已经爬取
         */
        Boolean existTopic = MysqlReadWriteDAO.judgeByClass(Config.DOMAIN_TOPIC_TABLE, domainName);
        if (!existTopic) {
            Log.log(domain + "：正在处理知识主题");
            topicExtract(domainName);
        } else {
            Log.log(domain + "：知识主题已经爬取");
        }
    }

    /**
     * 获取三层领域术语（某门所有课程）
     *
     * @param domainName 课程名
     * @throws Exception
     */
    public static void layerExtract(String domainName) throws Exception {

        /**
         * 领域术语采集：单门课程
         */
        /**
         * 第一层
         */
        String domain_url = "https://en.wikipedia.org/wiki/Category:" + URLEncoder.encode(domainName, "UTF-8");
        int firstLayer = 1;
        List<Term> topicFirst = TopicEnCrawlerDAO.topic(domain_url); // 得到第一层领域术语（不含子主题）
        MysqlReadWriteDAO.storeDomainLayer(topicFirst, domainName, firstLayer); // 存储第一层领域术语（不含子主题）
        MysqlReadWriteDAO.storeDomainLayerFuzhu(topicFirst, domainName, firstLayer, 0); // 存储第一层领域术语（不含子主题）

        // 构造一个主题作为没有子主题的一级主题的父主题
        List<Term> terms = new ArrayList<>();
        Term term = new Term(domainName + " Introduction", domain_url);
        terms.add(term);
        MysqlReadWriteDAO.storeLayerRelation(domainName, 0, terms, 0, domainName); // 第一层主题与领域名构成上下位关系
        MysqlReadWriteDAO.storeLayerRelation(term.getTermName(), 0, topicFirst, firstLayer, domainName); // 第一层主题与领域名构成上下位关系
        /**
         * 第二层
         */
        int secondLayer = 2;
        List<Term> layerSecond = TopicEnCrawlerDAO.layer(domain_url); // 获取第一层领域术语（含子主题）
        MysqlReadWriteDAO.storeDomainLayerFuzhu(layerSecond, domainName, firstLayer, 1); // 存储第一层领域术语（含子主题）
        MysqlReadWriteDAO.storeLayerRelation(domainName, 0, layerSecond, firstLayer, domainName); // 第一层主题与领域名构成上下位关系
        List<Term> topicSecondAll = new ArrayList<Term>(); // 保存所有第二层的领域术语
        if (layerSecond.size() != 0) {
            for (int i = 0; i < layerSecond.size(); i++) {
                Term layer = layerSecond.get(i);
                String url = layer.getTermUrl();
                List<Term> topicSecond = TopicEnCrawlerDAO.topic(url); // 得到第二层领域术语（不含子主题）
                MysqlReadWriteDAO.storeDomainLayer(topicSecond, domainName, secondLayer); // 存储第二层领域术语（不含子主题）
                MysqlReadWriteDAO.storeDomainLayerFuzhu(topicSecond, domainName, secondLayer, 0); // 存储第二层领域术语（不含子主题）
                MysqlReadWriteDAO.storeLayerRelation(layer.getTermName(), firstLayer, topicSecond, secondLayer, domainName); // 存储领域术语的上下位关系
                topicSecondAll.addAll(topicSecond); // 合并所有第二层领域术语

                int thirdLayer = 3;
                List<Term> layerThird = TopicEnCrawlerDAO.layer(url); // 得到第二层领域术语（含子主题）
                MysqlReadWriteDAO.storeDomainLayerFuzhu(layerThird, domainName, secondLayer, 1); // 存储第二层领域术语（含子主题）
                MysqlReadWriteDAO.storeLayerRelation(layer.getTermName(), firstLayer, layerThird, secondLayer, domainName); // 存储领域术语的上下位关系
                List<Term> topicThirdAll = new ArrayList<Term>(); // 保存所有第三层的领域术语
                if (layerThird.size() != 0) {
                    for (int j = 0; j < layerThird.size(); j++) {
                        Term layer2 = layerThird.get(j);
                        String url2 = layer2.getTermUrl();
                        List<Term> topicThird = TopicEnCrawlerDAO.topic(url2); // 得到第三层领域术语（不含子主题）
                        MysqlReadWriteDAO.storeDomainLayer(topicThird, domainName, thirdLayer); // 存储第三层领域术语（不含子主题）
                        MysqlReadWriteDAO.storeDomainLayerFuzhu(topicThird, domainName, thirdLayer, 0); // 存储第三层领域术语（不含子主题）
                        MysqlReadWriteDAO.storeLayerRelation(layer2.getTermName(), secondLayer, topicThird, thirdLayer, domainName); // 存储领域术语的上下位关系
                        topicThirdAll.addAll(topicThird); // 合并所有第三层领域术语

                        List<Term> layerThird2 = TopicEnCrawlerDAO.layer(url); // 得到第二层领域术语（含子主题）
                        MysqlReadWriteDAO.storeDomainLayerFuzhu(layerThird, domainName, secondLayer, 1); // 存储第二层领域术语（含子主题）
                        MysqlReadWriteDAO.storeLayerRelation(layer.getTermName(), firstLayer, layerThird, secondLayer, domainName); // 存储领域术语的上下位关系
                    }
                } else {
//                    Log.log("不存在第三层领域术语源链接....");
                }
            }
        } else {
//            Log.log("不存在第二层领域术语源链接...");
        }
    }

    /**
     * 获取三层知识主题（某门所有课程）
     *
     * @throws Exception
     */
    public static void topicExtract(String domainName) throws Exception {

        List<Term> topicFirst = MysqlReadWriteDAO.getDomainLayer(domainName, 1);
        List<Term> topicSecond = MysqlReadWriteDAO.getDomainLayer(domainName, 2);
        List<Term> topicThird = MysqlReadWriteDAO.getDomainLayer(domainName, 3);

        List<Term> topicFirstFuzhu = MysqlReadWriteDAO.getDomainLayerFuzhu(domainName, 1, 0);
        List<Term> topicSecondFuzhu = MysqlReadWriteDAO.getDomainLayerFuzhu(domainName, 2, 0);
        List<Term> topicThirdFuzhu = MysqlReadWriteDAO.getDomainLayerFuzhu(domainName, 3, 0);

        List<Term> topicFirstFuzhu2 = MysqlReadWriteDAO.getDomainLayerFuzhu(domainName, 1, 1);
        List<Term> topicSecondFuzhu2 = MysqlReadWriteDAO.getDomainLayerFuzhu(domainName, 2, 1);
        List<Term> topicThirdFuzhu2 = MysqlReadWriteDAO.getDomainLayerFuzhu(domainName, 3, 1);

        List<LayerRelation> layerRelationList = MysqlReadWriteDAO.getDomainLayerRelation(domainName);

        /**
         * 知识主题筛选：抽取算法获取知识主题
         * 存储到 domain_topic表格中
         */
        // 从 domain_layer 删除重复主题(含子主题)保存到 domain_topic
        List<Set<Term>> topicList = TopicEnCrawlerDAO.getTopic(topicFirst, topicSecond, topicThird);
        for (int i = 0; i < topicList.size(); i++) {
            Set<Term> topic = topicList.get(i);
            int layer_ID = i + 1;
            MysqlReadWriteDAO.storeDomainTopic(topic, domainName, layer_ID); // 存储第三层领域术语
        }
        // 从 domain_layer_fuzhu 删除重复主题(不含子主题)保存到 domain_layer_fuzhu2
        List<Set<Term>> topicListFuzhu = TopicEnCrawlerDAO.getTopic(topicFirstFuzhu, topicSecondFuzhu, topicThirdFuzhu);
        for (int i = 0; i < topicListFuzhu.size(); i++) {
            Set<Term> topic = topicListFuzhu.get(i);
            int layer_ID = i + 1;
            MysqlReadWriteDAO.storeDomainTopicFuzhu(topic, domainName, layer_ID, 0);
        }
        // 从 domain_layer_fuzhu 删除重复主题(含子主题)保存到 domain_layer_fuzhu2
        List<Set<Term>> topicListFuzhu2 = TopicEnCrawlerDAO.getTopic(topicFirstFuzhu2, topicSecondFuzhu2, topicThirdFuzhu2);
        for (int i = 0; i < topicListFuzhu2.size(); i++) {
            Set<Term> topic = topicListFuzhu2.get(i);
            int layer_ID = i + 1;
            MysqlReadWriteDAO.storeDomainTopicFuzhu(topic, domainName, layer_ID, 1);
        }
        // 从 domain_layer_relation 删除重复主题关系保存到 domain_topic_relation
        Set<LayerRelation> layerRelationSet = new LinkedHashSet<LayerRelation>(layerRelationList);
//		MysqlReadWriteDAO.storeDomainLayerRelation(layerRelationSet); // 存储 domain_layer_relation2
        MysqlReadWriteDAO.storeDomainTopicRelation(layerRelationSet); // 存储 domain_topic_relation
    }

}
