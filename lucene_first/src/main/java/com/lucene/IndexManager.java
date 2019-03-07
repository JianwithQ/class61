package com.lucene;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;

/**
 * @author Jian.Z
 * @version v1.0
 * @date 2019/3/4 16:44
 * @description TODO
 **/
public class IndexManager {

    //1 创建索引 IndexWriter

    @Test
    public void testWriter() throws Exception {
        //准备一个存储索引文件的路径
        FSDirectory directory = FSDirectory.open(new File("D:\\IDEA文件\\itheima-JAVAEE\\index_repo"));

        //指定使用那种分词器 标准分词器
//        Analyzer analyzer = new StandardAnalyzer();

        Analyzer analyzer = new IKAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(Version.LATEST,analyzer);
        //创建IndexWriter对象
        IndexWriter indexWriter = new IndexWriter(directory,conf);

        //删除所有文件
        indexWriter.deleteAll();

        File filePaths = new File("F:\\就业班视频资料\\框架\\Lucene\\资料\\上课用的查询资料searchsource");
        File[] files = filePaths.listFiles();
        for (File file : files) {
            Document document = new Document();
            // p1:域的名称   p2:值     p3:是否存储
            //Field.Store.YES 是否存储原数据
            String fileName = file.getName();
            document.add(new TextField("filename",fileName, Field.Store.YES));

            /**
             * 竞价排名 filename设置 setBoost(Int) 越高权重越高 默认 1
             */

            String fileContent = FileUtils.readFileToString(file, "utf-8");
            document.add(new TextField("filecontent",fileContent, Field.Store.YES));
            String filePath = file.getPath();
            //StringField 不分词 用于路径 或者号码之类的
            document.add(new StringField("filepath",filePath, Field.Store.YES));
            long fileSize = FileUtils.sizeOf(file);
            //需要LongFiled 来接受
            document.add(new LongField("filesize",fileSize, Field.Store.YES));
            indexWriter.addDocument(document);



        }

        indexWriter.close();

    }

    /**
     * 分词器测试
     */
    @Test
    public void testAnalyzer() throws IOException {

//        Analyzer analyzer = new StandardAnalyzer();  //一个字一个字
//        CJKAnalyzer analyzer = new CJKAnalyzer();    //两个字两个字
//        Analyzer analyzer = new SmartChineseAnalyzer(); //对中文还行  英文会缺字母
        Analyzer analyzer = new IKAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("test","使用indexwriter对象他妈的将嘛document对象写入索引库，此过程进行索引创建");

        //设置引用 为了获取每个分词的结果
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

        //指针位置归0
        tokenStream.reset();
        while (tokenStream.incrementToken()){
            System.out.println(charTermAttribute);
            System.out.println(charTermAttribute);
        }
    }

    //2 从索引中查询

    @Test
    public void indexReader()throws Exception{

        Directory directory = FSDirectory.open(new File("D:\\IDEA文件\\itheima-JAVAEE\\index_repo"));
        //创建读取文件的对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建查询的对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        //按照term查询 域名:值 查询标题带apache的
        //term 根據name查詢
//        Query query = new TermQuery(new Term("filename","spring is a project"));//中文分词单个单个的 词语无法分词


        /**
         * 查询的时候讲term内容 分词
         * 引入依赖 queryParser
         *
         <dependency>
         <groupId>org.apache.lucene</groupId>
         <artifactId>lucene-queryparser</artifactId>
         <version>4.7.2</version>
         </dependency>
         */
//        QueryParser queryParser = new QueryParser(Version.LATEST,"filename",new IKAnalyzer());
//        Query query = queryParser.parse("spring is a project");

//---------------------------------------------------------------------------------------------------------

        /**
         * 多域查询 MultiFieldQueryParser
         * 参数 填数组
         */
        QueryParser queryParser = new MultiFieldQueryParser(Version.LATEST,new String[]{"filename","filecontent"},new IKAnalyzer());
        Query query = queryParser.parse("spring is a project");


        //MatchAllDocsQuery 查询所有
//        Query query = new MatchAllDocsQuery();

        //NumericRangeQuery.newLongRange long类型的查询
//        Query query = NumericRangeQuery.newLongRange("filesize",100l,1500l,true,true);

//        //组合Query 多条件查询
//        BooleanQuery query = new BooleanQuery();
//
//        Query query1 = new TermQuery(new Term("filename", "apache"));//根据term查询
//        Query query2 = NumericRangeQuery.newLongRange("filesize",100l,957l,true,false);
//        query.add(query1,BooleanClause.Occur.MUST); +
//        query.add(query2,BooleanClause.Occur.SHOULD); -

        //filename:apache filesize:[100 TO 957}
        System.out.println("查询方法"+query);


        TopDocs topDocs = indexSearcher.search(query, 20);

        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        for (ScoreDoc scoreDoc : scoreDocs) {
            int docId = scoreDoc.doc;
            Document document = indexSearcher.doc(docId);
            System.out.println("标题"+document.get("filename"));
//            System.out.println("内容"+document.get("filecontent"));
            System.out.println("路径"+document.get("filepath"));
            System.out.println("大小"+document.get("filesize"));
            System.out.println("----------------------------");
        }

    }
}
