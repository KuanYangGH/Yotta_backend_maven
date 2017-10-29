package spider;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import spider.bean.Count;
import spider.bean.Image;
import spider.bean.Text;
import utils.FragmentSplit;
import utils.Log;
import utils.mysqlUtils;
import app.Config;
import app.error;
import app.success;
import domainTopic.DomainTopicDAO;

/**
 * 碎片化知识采集
 * @author 郑元浩
 */
@Path("/SpiderAPI")
@Api(value = "SpiderAPI")
public class SpiderAPI {

    @GET
    @Path("/getFragmentByTopicArray")
    @ApiOperation(value = "根据课程名和主题数组，获取主题下的碎片数据", notes = "根据课程名和主题数组，获取主题下的碎片数据")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败", response = String.class),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getFragmentByTopicArray(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("className") String className,
            @DefaultValue("树状数组,图论术语") @ApiParam(value = "主题名字符串", required = true) @QueryParam("topicNames")
                    String topicNames) {
        Response response = null;
        List<Text> textList = new ArrayList<Text>();
        String[] topicNameArray = topicNames.split(",");

        /**
         * 循环所有主题
         */
        for (int i = 0; i < topicNameArray.length; i++) {

            /**
             * 读取spider_fragment，获得主题碎片
             */
            String topicName = topicNameArray[i];
            mysqlUtils mysql = new mysqlUtils();
            String sql = "select * from " + Config.ASSEMBLE_FRAGMENT_TABLE + " where ClassName=? and TermName=?";
            List<Object> params = new ArrayList<Object>();
            params.add(className);
            params.add(topicName);
            try {
                List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
                for (int j = 0; j < results.size(); j++) {
                    Map<String, Object> map = results.get(j);
                    int FragmentID = Integer.parseInt(map.get("FragmentID").toString());
                    String FragmentContent = map.get("FragmentContent").toString();
                    String FragmentScratchTime = map.get("FragmentScratchTime").toString();
                    int TermID = Integer.parseInt(map.get("TermID").toString());
                    String TermName = map.get("TermName").toString();
                    String ClassName = map.get("ClassName").toString();
                    Text text = new Text(FragmentID, FragmentContent, "", "", FragmentScratchTime, TermID, TermName, ClassName);
                    textList.add(text);
                }
            } catch (Exception e) {
                e.printStackTrace();
                response = Response.status(401).entity(new error(e.toString())).build();
            } finally {
                mysql.closeconnection();
            }
        }
        response = Response.status(200).entity(textList).build();

        return response;
    }

    @GET
    @Path("/getTextByDomain")
    @ApiOperation(value = "获得某门课程的文本信息", notes = "输入领域名，获得某门课程的文本信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getTextByDomain(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("ClassName") String className) {
        Response response = null;
        List<Text> textList = new ArrayList<Text>();

        /**
         * 读取spider_text，获得知识点的文本碎片
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.SPIDER_TEXT_TABLE + " where ClassName=?";
        List<Object> params = new ArrayList<Object>();
        params.add(className);
        try {
            List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> map = results.get(i);
                int FragmentID = Integer.parseInt(map.get("FragmentID").toString());
                String FragmentContent = map.get("FragmentContent").toString();
                String FragmentUrl = map.get("FragmentUrl").toString();
                String FragmentPostTime = map.get("FragmentPostTime").toString();
                String FragmentScratchTime = map.get("FragmentScratchTime").toString();
                int TermID = Integer.parseInt(map.get("TermID").toString());
                String TermName = map.get("TermName").toString();
                String ClassName = map.get("ClassName").toString();
                Text text = new Text(FragmentID, FragmentContent, FragmentUrl, FragmentPostTime, FragmentScratchTime, TermID, TermName, ClassName);
                textList.add(text);
            }
            response = Response.status(200).entity(textList).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }

        return response;
    }

    @GET
    @Path("/getTextByTopicArray")
    @ApiOperation(value = "获得主题数组", notes = "输入领域名和知识主题数组，获得主题数组的文本信息集合")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败", response = String.class),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getTextByTopicArray(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("className") String className,
            @DefaultValue("树状数组,图论术语") @ApiParam(value = "主题名字符串", required = true) @QueryParam("topicNames")
                    String topicNames) {
        Response response = null;
        List<Text> textList = new ArrayList<Text>();
        String[] topicNameArray = topicNames.split(",");

        /**
         * 循环所有主题
         */
        for (int i = 0; i < topicNameArray.length; i++) {

            /**
             * 读取spider_text，获得知识点的文本碎片
             */
            String topicName = topicNameArray[i];
            mysqlUtils mysql = new mysqlUtils();
            String sql = "select * from " + Config.SPIDER_TEXT_TABLE + " where ClassName=? and TermName=?";
            List<Object> params = new ArrayList<Object>();
            params.add(className);
            params.add(topicName);
            try {
                List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
                for (int j = 0; j < results.size(); j++) {
                    Map<String, Object> map = results.get(j);
                    int FragmentID = Integer.parseInt(map.get("FragmentID").toString());
                    String FragmentContent = map.get("FragmentContent").toString();
                    String FragmentUrl = map.get("FragmentUrl").toString();
                    String FragmentPostTime = map.get("FragmentPostTime").toString();
                    String FragmentScratchTime = map.get("FragmentScratchTime").toString();
                    int TermID = Integer.parseInt(map.get("TermID").toString());
                    String TermName = map.get("TermName").toString();
                    String ClassName = map.get("ClassName").toString();
                    Text text = new Text(FragmentID, FragmentContent, FragmentUrl, FragmentPostTime, FragmentScratchTime, TermID, TermName, ClassName);
                    textList.add(text);
                }
            } catch (Exception e) {
                e.printStackTrace();
                response = Response.status(401).entity(new error(e.toString())).build();
            } finally {
                mysql.closeconnection();
            }
        }
        response = Response.status(200).entity(textList).build();

        return response;
    }


    @GET
    @Path("/getImageByTopicArray")
    @ApiOperation(value = "获得多个知识主题的图片信息", notes = "输入领域名和多个知识主题，获得多个知识主题的图片信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getImageByTopicArray(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("ClassName") String className,
            @DefaultValue("树状数组,图论术语") @ApiParam(value = "主题名数组", required = true) @QueryParam("topicNames")
                    String topicNames) {
        Response response = null;
        List<Image> imageList = new ArrayList<Image>();
        String[] topicNameList = topicNames.split(",");

        /**
         * 循环所有主题
         */
        for (int i = 0; i < topicNameList.length; i++) {

            /**
             * 读取spider_image，获得知识点的图片碎片
             */
            String topicName = topicNameList[i];
            mysqlUtils mysql = new mysqlUtils();
            String sql = "select * from " + Config.SPIDER_IMAGE_TABLE + " where ClassName=? and TermName=?";
            List<Object> params = new ArrayList<Object>();
            params.add(className);
            params.add(topicName);
            try {
                List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
                for (int j = 0; j < results.size(); j++) {
                    Map<String, Object> map = results.get(j);
                    int ImageID = Integer.parseInt(map.get("ImageID").toString());
                    String ImageUrl = map.get("ImageUrl").toString();
                    int ImageWidth = 100;
                    if (!map.get("ImageWidth").toString().equals("")) {
                        ImageWidth = Integer.parseInt(map.get("ImageWidth").toString());
                    }
                    int ImageHeight = 100;
                    if (!map.get("ImageHeight").toString().equals("")) {
                        ImageHeight = Integer.parseInt(map.get("ImageHeight").toString());
                    }
                    int TermID = Integer.parseInt(map.get("TermID").toString());
                    String TermName = map.get("TermName").toString();
                    String TermUrl = map.get("TermUrl").toString();
                    String ClassName = map.get("ClassName").toString();
                    String ImageScratchTime = map.get("ImageScratchTime").toString();
                    Image image = new Image(ImageID, ImageUrl, ImageWidth, ImageHeight, TermID, TermName, TermUrl, ClassName, ImageScratchTime);
                    imageList.add(image);
                }
            } catch (Exception e) {
                e.printStackTrace();
                response = Response.status(401).entity(new error(e.toString())).build();
            } finally {
                mysql.closeconnection();
            }
        }
        response = Response.status(200).entity(imageList).build();

        return response;
    }


    @GET
    @Path("/getCountByDomain2")
    @ApiOperation(value = "获得某门课程的文本和图片数量", notes = "输入领域名，获得某门课程的文本和图片数量")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getCountByDomain2(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("ClassName") String className) {
        Response response = null;
        List<Count> countList = new ArrayList<Count>();
        int textSize = 0;
        int imageSize = 0;

        /**
         * 统计每个主题的文本碎片数量
         */
        List<String> topicList = DomainTopicDAO.getDomainTopicList(className);
        for (int i = 0; i < topicList.size(); i++) {
            String topicName = topicList.get(i);
            /**
             * 读取spider_text，获得文本数量
             */
            mysqlUtils mysql = new mysqlUtils();
            String sql = "select * from " + Config.SPIDER_TEXT_TABLE + " where ClassName=? and TermName=?";
            List<Object> params = new ArrayList<Object>();
            params.add(className);
            params.add(topicName);
            try {
                List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
                textSize += results.size();
            } catch (Exception e) {
                e.printStackTrace();
                response = Response.status(401).entity(new error(e.toString())).build();
            } finally {
                mysql.closeconnection();
            }

            /**
             * 读取spider_image，获得文本数量
             */
            mysqlUtils mysqlImage = new mysqlUtils();
            String sqlImage = "select * from " + Config.SPIDER_IMAGE_TABLE + " where ClassName=? and TermName=?";
            List<Object> paramsImage = new ArrayList<Object>();
            paramsImage.add(className);
            paramsImage.add(topicName);
            try {
                List<Map<String, Object>> results = mysqlImage.returnMultipleResult(sqlImage, paramsImage);
                imageSize += results.size();
            } catch (Exception e) {
                e.printStackTrace();
                response = Response.status(401).entity(new error(e.toString())).build();
            } finally {
                mysqlImage.closeconnection();
            }
        }

        countList.add(new Count("text", textSize));
        countList.add(new Count("image", imageSize));
        Log.log("text: " + textSize);
        Log.log("image: " + imageSize);
        response = Response.status(200).entity(countList).build();

        return response;
    }


    @GET
    @Path("/getCountByTopic")
    @ApiOperation(value = "获得某门课程下主题的文本和图片数量", notes = "输入领域名和主题，获得某门课程下主题的文本和图片数量")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getCountByTopic(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("ClassName") String className,
            @DefaultValue("抽象资料型别") @ApiParam(value = "主题名", required = true) @QueryParam("TermName") String topicName) {
        Response response = null;
        List<Count> countList = new ArrayList<Count>();

        /**
         * 读取spider_text，获得文本数量
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.SPIDER_TEXT_TABLE + " where ClassName=? and TermName=?";
        List<Object> params = new ArrayList<Object>();
        params.add(className);
        params.add(topicName);
        try {
            List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
            int textSize = results.size();
            countList.add(new Count("text", textSize));
            //			response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }

        /**
         * 读取spider_image，获得文本数量
         */
        mysqlUtils mysqlImage = new mysqlUtils();
        String sqlImage = "select * from " + Config.SPIDER_IMAGE_TABLE + " where ClassName=? and TermName=?";
        List<Object> paramsImage = new ArrayList<Object>();
        paramsImage.add(className);
        paramsImage.add(topicName);
        try {
            List<Map<String, Object>> results = mysqlImage.returnMultipleResult(sqlImage, paramsImage);
            int imageSize = results.size();
            countList.add(new Count("image", imageSize));
            //			response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysqlImage.closeconnection();
        }

        response = Response.status(200).entity(countList).build();

        return response;
    }


    @GET
    @Path("/getDomainTerm")
    @ApiOperation(value = "获得指定领域下主题的信息", notes = "获得指定领域下主题的信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getDomainTerm(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName) {
        Response response = null;
        /**
         * 根据指定领域，获得该领域下的所有主题信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.DOMAIN_TOPIC_TABLE + " where ClassName=?";
        List<Object> params = new ArrayList<Object>();
        params.add(ClassName);
        try {
            List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }

    @GET
    @Path("/getDomainTermFacet1")
    @ApiOperation(value = "获得指定领域下指定主题的一级分面信息", notes = "获得指定领域下指定主题的一级分面信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getDomainTermFacet1(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName, @ApiParam(value = "主题名字", required = true) @QueryParam("TermName") String TermName) {
        Response response = null;
        /**
         * 根据指定领域和指定主题，获得该主题下的所有一级分面信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.FACET_TABLE + " where ClassName=? and TermName=? and FacetLayer='1'";
        List<Object> params = new ArrayList<Object>();
        params.add(ClassName);
        params.add(TermName);
        try {
            List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }

    @GET
    @Path("/getDomainTermFacet2")
    @ApiOperation(value = "获得指定领域下指定主题一级分面下的二级分面信息", notes = "获得指定领域下指定主题一级分面下的二级分面信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getDomainTermFacet2(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName, @ApiParam(value = "主题名字", required = true) @QueryParam("TermName") String TermName, @ApiParam(value = "一级分面名字", required = true) @QueryParam("Facet1Name") String Facet1Name) {
        Response response = null;
        /**
         * 获得指定领域下指定主题一级分面下的二级分面信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.FACET_RELATION_TABLE + " where ClassName=? and TermName=? and ParentFacet=? and ParentLayer='1'";
        List<Object> params = new ArrayList<Object>();
        params.add(ClassName);
        params.add(TermName);
        params.add(Facet1Name);
        try {
            List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }


    @GET
    @Path("/getDomainTermFacet3")
    @ApiOperation(value = "获得指定领域下指定主题二级分面下的三级分面信息", notes = "获得指定领域下指定主题二级分面下的三级分面信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getDomainTermFacet3(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName, @ApiParam(value = "主题名字", required = true) @QueryParam("TermName") String TermName, @ApiParam(value = "二级分面名字", required = true) @QueryParam("Facet2Name") String Facet2Name) {
        Response response = null;
        /**
         * 获得指定领域下指定主题二级分面下的三级分面信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.FACET_RELATION_TABLE + " where ClassName=? and TermName=? and ParentFacet=? and ParentLayer='2'";
        List<Object> params = new ArrayList<Object>();
        params.add(ClassName);
        params.add(TermName);
        params.add(Facet2Name);
        try {
            List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }


    @GET
    @Path("/getDomainTermFragment")
    @ApiOperation(value = "获得指定领域下指定主题的碎片信息", notes = "获得指定领域下指定主题的碎片信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getDomainTermFragment(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName, @ApiParam(value = "主题名字", required = true) @QueryParam("TermName") String TermName) {
        Response response = null;
        /**
         * 根据指定领域和指定主题，获得该主题下的所有碎片信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.ASSEMBLE_FRAGMENT_TABLE + " where ClassName=? and TermName=?";
        List<Object> params = new ArrayList<Object>();
        params.add(ClassName);
        params.add(TermName);
        try {
            List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }


    @GET
    @Path("/getDomainTermFacet1Fragment")
    @ApiOperation(value = "获得指定领域下指定主题一级分面的碎片信息", notes = "获得指定领域下指定主题一级分面的碎片信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getDomainTermFacet1Fragment(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName, @ApiParam(value = "主题名字", required = true) @QueryParam("TermName") String TermName, @ApiParam(value = "分面名字", required = true) @QueryParam("FacetName") String FacetName) {
        Response response = null;
        /**
         * 根据指定领域和指定主题，获得该主题下一级分面的碎片信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.ASSEMBLE_FRAGMENT_TABLE + " where ClassName=? and TermName=? and FacetName=? and FacetLayer=?";


        String sql_facet2 = "select * from " + Config.FACET_RELATION_TABLE + " where ClassName=? and TermName=? and ParentFacet=? and ParentLayer='1'";
        String sql_facet3 = "select * from " + Config.FACET_RELATION_TABLE + " where ClassName=? and TermName=? and ParentFacet=? and ParentLayer='2'";
        List<Object> params_facet2 = new ArrayList<Object>();
        params_facet2.add(ClassName);
        params_facet2.add(TermName);
        params_facet2.add(FacetName);
        try {
            List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> results_facet2 = mysql.returnMultipleResult(sql_facet2, params_facet2);
            List<Map<String, Object>> results_finalfacet = new ArrayList<Map<String, Object>>();
            Map<String, Object> facet1 = new HashMap<String, Object>();
            facet1.put("ClassName", ClassName);
            facet1.put("TermName", TermName);
            facet1.put("FacetName", FacetName);
            facet1.put("FacetLayer", 1);
            results_finalfacet.add(facet1);
            for (int i = 0; i < results_facet2.size(); i++) {
                Map<String, Object> facet2 = new HashMap<String, Object>();
                facet2.put("ClassName", ClassName);
                facet2.put("TermName", TermName);
                facet2.put("FacetName", results_facet2.get(i).get("ChildFacet"));
                facet2.put("FacetLayer", 2);
                results_finalfacet.add(facet2);

                List<Object> params_facet3 = new ArrayList<Object>();
                params_facet3.add(ClassName);
                params_facet3.add(TermName);
                params_facet3.add(results_facet2.get(i).get("ChildFacet"));
                try {
                    List<Map<String, Object>> results_facet3 = mysql.returnMultipleResult(sql_facet3, params_facet3);
                    for (int j = 0; j < results_facet3.size(); j++) {
                        Map<String, Object> facet3 = new HashMap<String, Object>();
                        facet3.put("ClassName", ClassName);
                        facet3.put("TermName", TermName);
                        facet3.put("FacetName", results_facet3.get(j).get("ChildFacet"));
                        facet3.put("FacetLayer", 3);
                        results_finalfacet.add(facet3);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (int k = 0; k < results_finalfacet.size(); k++) {
                List<Object> params_fragment = new ArrayList<Object>();
                params_fragment.add(results_finalfacet.get(k).get("ClassName"));
                params_fragment.add(results_finalfacet.get(k).get("TermName"));
                params_fragment.add(results_finalfacet.get(k).get("FacetName"));
                params_fragment.add(results_finalfacet.get(k).get("FacetLayer"));
                try {
                    results.addAll(mysql.returnMultipleResult(sql, params_fragment));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }


    @GET
    @Path("/getDomainTermFacet2Fragment")
    @ApiOperation(value = "获得指定领域下指定主题二级分面的碎片信息", notes = "获得指定领域下指定主题二级分面的碎片信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getDomainTermFacet2Fragment(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName, @ApiParam(value = "主题名字", required = true) @QueryParam("TermName") String TermName, @ApiParam(value = "分面名字", required = true) @QueryParam("FacetName") String FacetName) {
        Response response = null;
        /**
         * 根据指定领域和指定主题，获得该主题下二级分面的碎片信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.ASSEMBLE_FRAGMENT_TABLE + " where ClassName=? and TermName=? and FacetName=? and FacetLayer=?";

        String sql_facet3 = "select * from " + Config.FACET_RELATION_TABLE + " where ClassName=? and TermName=? and ParentFacet=? and ParentLayer='2'";
        List<Object> params_facet3 = new ArrayList<Object>();
        params_facet3.add(ClassName);
        params_facet3.add(TermName);
        params_facet3.add(FacetName);
        try {
            List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> results_facet3 = mysql.returnMultipleResult(sql_facet3, params_facet3);
            List<Map<String, Object>> results_finalfacet = new ArrayList<Map<String, Object>>();
            Map<String, Object> facet2 = new HashMap<String, Object>();
            facet2.put("ClassName", ClassName);
            facet2.put("TermName", TermName);
            facet2.put("FacetName", FacetName);
            facet2.put("FacetLayer", 2);
            results_finalfacet.add(facet2);
            for (int i = 0; i < results_facet3.size(); i++) {
                Map<String, Object> facet3 = new HashMap<String, Object>();
                facet3.put("ClassName", ClassName);
                facet3.put("TermName", TermName);
                facet3.put("FacetName", results_facet3.get(i).get("ChildFacet"));
                facet3.put("FacetLayer", 3);
                results_finalfacet.add(facet3);
            }
            for (int k = 0; k < results_finalfacet.size(); k++) {
                List<Object> params_fragment = new ArrayList<Object>();
                params_fragment.add(results_finalfacet.get(k).get("ClassName"));
                params_fragment.add(results_finalfacet.get(k).get("TermName"));
                params_fragment.add(results_finalfacet.get(k).get("FacetName"));
                params_fragment.add(results_finalfacet.get(k).get("FacetLayer"));
                try {
                    results.addAll(mysql.returnMultipleResult(sql, params_fragment));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }


    @GET
    @Path("/getDomainTermFacet3Fragment")
    @ApiOperation(value = "获得指定领域下指定主题三级分面的碎片信息", notes = "获得指定领域下指定主题三级分面的碎片信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getDomainTermFacet3Fragment(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName, @ApiParam(value = "主题名字", required = true) @QueryParam("TermName") String TermName, @ApiParam(value = "分面名字", required = true) @QueryParam("FacetName") String FacetName) {
        Response response = null;
        /**
         * 根据指定领域和指定主题，获得该主题下三级分面的碎片信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.ASSEMBLE_FRAGMENT_TABLE + " where ClassName=? and TermName=? and FacetName=? and FacetLayer=?";
        List<Object> params_fragment = new ArrayList<Object>();
        params_fragment.add(ClassName);
        params_fragment.add(TermName);
        params_fragment.add(FacetName);
        params_fragment.add(3);
        try {
            List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
            results = mysql.returnMultipleResult(sql, params_fragment);
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }

    @GET
    @Path("/createFragment")
    @ApiOperation(value = "创建碎片", notes = "创建碎片")
    @ApiResponses(value = {
            @ApiResponse(code = 402, message = "数据库错误", response = error.class),
            @ApiResponse(code = 200, message = "正常返回结果", response = success.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response createFragment(@ApiParam(value = "FragmentContent", required = true) @QueryParam("FragmentContent") String FragmentContent) {
//		Response response = null;
        /**
         * 创建碎片
         */
        try {
            boolean result = false;
            mysqlUtils mysql = new mysqlUtils();
            String sql = "insert into " + Config.FRAGMENT + "(FragmentContent,FragmentScratchTime) values(?,?);";
            Date d = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            List<Object> params = new ArrayList<Object>();
            params.add(FragmentContent);
            params.add(sdf.format(d));
            try {
                result = mysql.addDeleteModify(sql, params);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mysql.closeconnection();
            }
            if (result) {
                return Response.status(200).entity(new success("碎片创建成功~")).build();
            } else {
                return Response.status(401).entity(new error("碎片创建失败~")).build();
            }
        } catch (Exception e) {
            return Response.status(402).entity(new error(e.toString())).build();
        }

    }

    @GET
    @Path("/getFragment")
    @ApiOperation(value = "获得碎片信息", notes = "获得碎片信息")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getFragment() {
        Response response = null;
        /**
         * 获得碎片信息
         */
        mysqlUtils mysql = new mysqlUtils();
        String sql = "select * from " + Config.FRAGMENT;
        List<Object> params = new ArrayList<Object>();
        try {
            List<Map<String, Object>> results = mysql.returnMultipleResult(sql, params);
            response = Response.status(200).entity(results).build();
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(401).entity(new error(e.toString())).build();
        } finally {
            mysql.closeconnection();
        }
        return response;
    }


    @POST
    @Path("/createImageFragment")
    @ApiOperation(value = "插入图片", notes = "插入图片")
    @ApiResponses(value = {
            @ApiResponse(code = 402, message = "数据库错误", response = error.class),
            @ApiResponse(code = 200, message = "正常返回结果", response = success.class)})
    @Consumes(MediaType.MULTIPART_FORM_DATA + ";charset=" + "UTF-8")
    @Produces(MediaType.TEXT_PLAIN + ";charset=" + "UTF-8")
    public static Response createImageFragment(
            @FormDataParam("imageContent") FormDataContentDisposition disposition,
            @FormDataParam("imageContent") InputStream imageContent) {

        Response response = null;
        mysqlUtils mysql = new mysqlUtils();

        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


//		String sqlFragment="select * from "+Config.UNADD_IMAGE+" where ImageUrl=?";
        String sqlAdd = "insert into " + Config.UNADD_IMAGE + "(ImageUrl,ImageContent,ImageAPI, ImageScratchTime) values (?, ?, ?,?)";
        String sqlImageID = "select * from " + Config.UNADD_IMAGE + " where ImageUrl=?";
        String sqlApi = "update " + Config.UNADD_IMAGE + " set ImageAPI=? where ImageUrl=?";
        List<Object> paramsAdd = new ArrayList<Object>();
        List<Object> paramsImageID = new ArrayList<Object>();
        List<Object> paramsApi = new ArrayList<Object>();
        paramsAdd.add("http://image.baidu.com/" + disposition.getFileName());
        paramsAdd.add(imageContent);
        paramsAdd.add("");
        paramsAdd.add(sdf.format(d));
        paramsImageID.add("http://image.baidu.com/" + disposition.getFileName());
//		paramsApi.add(e);
//		paramsApi.add("http://image.baidu.com/" + disposition.getFileName());
        List<Map<String, Object>> resultFragment = new ArrayList<Map<String, Object>>();

        try {
            resultFragment = mysql.returnMultipleResult(sqlImageID, paramsImageID);
            if (resultFragment.size() == 0) {
                try {
                    mysql.addDeleteModify(sqlAdd, paramsAdd);
                    try {
                        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
                        result = mysql.returnMultipleResult(sqlImageID, paramsImageID);
                        paramsApi.add(Config.IP2 + "/SpiderAPI/getUnaddImage?imageID=" + result.get(0).get("ImageID"));
                        paramsApi.add("http://image.baidu.com/" + disposition.getFileName());
                        try {
                            mysql.addDeleteModify(sqlApi, paramsApi);
                            response = Response.status(200).entity(paramsApi.get(0)).build();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                } catch (Exception e0) {
                    e0.printStackTrace();
                }
            } else {
                response = Response.status(200).entity(resultFragment.get(0).get("ImageAPI")).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            response = Response.status(402).entity(new error("MySql数据库  更新失败")).build();
        } finally {
            mysql.closeconnection();
        }
        //System.out.println(response.getEntity());
        return response;
    }

    @GET
    @Path("/getUnaddImage")
    @ApiOperation(value = "读取图片数据表中数据到成API", notes = "输入图片ID，得到对应API")
    @ApiResponses(value = {@ApiResponse(code = 401, message = "MySql数据库  图片内容查询失败"),
            @ApiResponse(code = 200, message = "MySql数据库  图片数据表检查处理完成", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_OCTET_STREAM + ";charset=" + "UTF-8")
    public static Response getImage(@ApiParam(value = "图片ID", required = true) @QueryParam("imageID") int imageID) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {
            mysqlUtils mysql = new mysqlUtils();
            String sql = "select * from " + Config.UNADD_IMAGE + " where ImageID=?";
            List<Object> params = new ArrayList<Object>();
            params.add(imageID);
            try {
                result = mysql.returnMultipleResult(sql, params);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mysql.closeconnection();
            }
            String imageUrl = (String) result.get(0).get("ImageUrl");
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Object imageContent = result.get(0).get("ImageContent");
            return Response.status(200).header("Content-disposition", "attachment; " + "filename=" + filename).entity(imageContent).build();
        } catch (Exception e) {
            return Response.status(402).entity(new error(e.toString())).build();
        }
    }


    @GET
    @Path("/addFacetFragment")
    @ApiOperation(value = "向分面添加碎片", notes = "向分面添加碎片")
    @ApiResponses(value = {
            @ApiResponse(code = 402, message = "数据库错误", response = error.class),
            @ApiResponse(code = 200, message = "正常返回结果", response = success.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response addFacetFragment(@ApiParam(value = "课程名字", required = true) @QueryParam("ClassName") String ClassName, @ApiParam(value = "主题名字", required = true) @QueryParam("TermName") String TermName, @ApiParam(value = "分面名字", required = true) @QueryParam("FacetName") String FacetName, @ApiParam(value = "分面级数", required = true) @QueryParam("FacetLayer") String FacetLayer, @ApiParam(value = "FragmentID", required = true) @QueryParam("FragmentID") String FragmentID) {
//		Response response = null;
        /**
         * 向分面添加碎片
         */
        try {
            boolean result = false;
            mysqlUtils mysql = new mysqlUtils();
            String sql_term = "select * from " + Config.DOMAIN_TOPIC_TABLE + " where ClassName=? and TermName=?";
            String sql_query = "select * from " + Config.FRAGMENT + " where FragmentID=?";
            String sql_delete = "delete from " + Config.FRAGMENT + " where FragmentID=?";
            String sql_add = "insert into " + Config.ASSEMBLE_FRAGMENT_TABLE + "(FragmentContent,FragmentScratchTime,TermID,TermName,FacetName,FacetLayer,ClassName) values(?,?,?,?,?,?,?);";

            String sql_addImage = "insert into " + Config.ASSEMBLE_IMAGE_TABLE + "(ImageUrl,TermID,TermName,FacetLayer,FacetName,ClassName,ImageScratchTime) values(?,?,?,?,?,?,?);";
            String sql_addText = "insert into " + Config.ASSEMBLE_TEXT_TABLE + "(FragmentContent,FragmentScratchTime,TermID,TermName,FacetName,FacetLayer,ClassName) values(?,?,?,?,?,?,?);";

            String sql_addImageSpider = "insert into " + Config.SPIDER_IMAGE_TABLE + "(ImageUrl,TermID,TermName,ClassName,ImageScratchTime) values(?,?,?,?,?);";
            String sql_addTextSpider = "insert into " + Config.SPIDER_TEXT_TABLE + "(FragmentContent,FragmentScratchTime,TermID,TermName,ClassName) values(?,?,?,?,?);";


            List<Object> params_term = new ArrayList<Object>();
            params_term.add(ClassName);
            params_term.add(TermName);
            List<Object> params_fragment = new ArrayList<Object>();
            params_fragment.add(FragmentID);
            try {
                List<Map<String, Object>> results_term = mysql.returnMultipleResult(sql_term, params_term);
                List<Map<String, Object>> fragmentinfo = mysql.returnMultipleResult(sql_query, params_fragment);
                List<Object> params_add = new ArrayList<Object>();
                params_add.add(fragmentinfo.get(0).get("FragmentContent"));
                params_add.add(fragmentinfo.get(0).get("FragmentScratchTime"));
                params_add.add(results_term.get(0).get("TermID"));
                params_add.add(TermName);
                params_add.add(FacetName);
                params_add.add(FacetLayer);
                params_add.add(ClassName);
                result = mysql.addDeleteModify(sql_add, params_add);
                if (result) {
                    try {
                        mysql.addDeleteModify(sql_delete, params_fragment);

                        // 将碎片写到assemble_text表
                        String Content = String.valueOf(fragmentinfo.get(0).get("FragmentContent"));
                        List<Object> params_addText = new ArrayList<Object>();
                        params_addText.add(FragmentSplit.getTextFromHtml(Content));
                        params_addText.add(fragmentinfo.get(0).get("FragmentScratchTime"));
                        params_addText.add(results_term.get(0).get("TermID"));
                        params_addText.add(TermName);
                        params_addText.add(FacetName);
                        params_addText.add(FacetLayer);
                        params_addText.add(ClassName);
                        try {
                            mysql.addDeleteModify(sql_addText, params_addText);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 将碎片写到assemble_image表
                        if (Content.indexOf("src") > 0) {
                            List<Object> params_addImage = new ArrayList<Object>();
                            params_addImage.add(FragmentSplit.getImageFromHtml(Content));
                            params_addImage.add(results_term.get(0).get("TermID"));
                            params_addImage.add(TermName);
                            params_addImage.add(FacetLayer);
                            params_addImage.add(FacetName);
                            params_addImage.add(ClassName);
                            params_addImage.add(fragmentinfo.get(0).get("FragmentScratchTime"));
                            try {
                                mysql.addDeleteModify(sql_addImage, params_addImage);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // 将碎片写到spider_text表
                        List<Object> params_addTextSpider = new ArrayList<Object>();
                        params_addTextSpider.add(FragmentSplit.getTextFromHtml(Content));
                        params_addTextSpider.add(fragmentinfo.get(0).get("FragmentScratchTime"));
                        params_addTextSpider.add(results_term.get(0).get("TermID"));
                        params_addTextSpider.add(TermName);
                        params_addTextSpider.add(ClassName);
                        try {
                            mysql.addDeleteModify(sql_addTextSpider, params_addTextSpider);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 将碎片写到spider_image表
                        if (Content.indexOf("src") > 0) {
                            List<Object> params_addImageSpider = new ArrayList<Object>();
                            params_addImageSpider.add(FragmentSplit.getImageFromHtml(Content));
                            params_addImageSpider.add(results_term.get(0).get("TermID"));
                            params_addImageSpider.add(TermName);
                            params_addImageSpider.add(ClassName);
                            params_addImageSpider.add(fragmentinfo.get(0).get("FragmentScratchTime"));
                            try {
                                mysql.addDeleteModify(sql_addImageSpider, params_addImageSpider);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mysql.closeconnection();
            }
            if (result) {
                return Response.status(200).entity(new success("碎片添加成功~")).build();
            } else {
                return Response.status(401).entity(new error("碎片添加失败~")).build();
            }
        } catch (Exception e) {
            return Response.status(402).entity(new error(e.toString())).build();
        }

    }


    @GET
    @Path("/deleteUnaddFragment")
    @ApiOperation(value = "删除未挂接的碎片", notes = "删除未挂接的碎片")
    @ApiResponses(value = {
            @ApiResponse(code = 402, message = "数据库错误", response = error.class),
            @ApiResponse(code = 200, message = "正常返回结果", response = success.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response deleteUnaddFragment(@ApiParam(value = "FragmentID", required = true) @QueryParam("FragmentID") String FragmentID) {
        /**
         * 删除未挂接的碎片
         */
        try {
            boolean result = false;
            mysqlUtils mysql = new mysqlUtils();
            String sql = "delete from " + Config.FRAGMENT + " where FragmentID=?;";
            List<Object> params = new ArrayList<Object>();
            params.add(FragmentID);
            try {
                result = mysql.addDeleteModify(sql, params);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mysql.closeconnection();
            }
            if (result) {
                return Response.status(200).entity(new success("碎片删除成功~")).build();
            } else {
                return Response.status(401).entity(new error("碎片删除失败~")).build();
            }
        } catch (Exception e) {
            return Response.status(402).entity(new error(e.toString())).build();
        }
    }


    @GET
    @Path("/deleteFragment")
    @ApiOperation(value = "删除碎片", notes = "删除碎片")
    @ApiResponses(value = {
            @ApiResponse(code = 402, message = "数据库错误", response = error.class),
            @ApiResponse(code = 200, message = "正常返回结果", response = success.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response deleteFragment(@ApiParam(value = "FragmentID", required = true) @QueryParam("FragmentID") String FragmentID) {
        /**
         * 删除碎片
         */
        try {
            boolean result = false;
            mysqlUtils mysql = new mysqlUtils();
            String sql = "delete from " + Config.ASSEMBLE_FRAGMENT_TABLE + " where FragmentID=?;";
            List<Object> params = new ArrayList<Object>();
            params.add(FragmentID);
            try {
                result = mysql.addDeleteModify(sql, params);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mysql.closeconnection();
            }
            if (result) {
                return Response.status(200).entity(new success("碎片删除成功~")).build();
            } else {
                return Response.status(401).entity(new error("碎片删除失败~")).build();
            }
        } catch (Exception e) {
            return Response.status(402).entity(new error(e.toString())).build();
        }
    }

}
