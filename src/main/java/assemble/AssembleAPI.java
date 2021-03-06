package assemble;

import app.error;
import assemble.bean.*;
import domainTopic.DomainTopicDAO;
import domainTopic.bean.DomainTopic;
import facet.FacetDAO;
import facet.bean.FacetRelation;
import facet.bean.FacetSimple;
import io.swagger.annotations.*;
import utils.Log;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**  
 * 碎片装配
 * @author 郑元浩 
 */
@Path("/AssembleAPI")
@Api(value = "AssembleAPI")
public class AssembleAPI {

    @GET
    @Path("/getTreeByDomain")
    @ApiOperation(value = "输入领域名，获得实例化主题分面树的数据", notes = "输入领域名，获得实例化主题分面树的数据，碎片包含文本和图片")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 402, message = "MySql数据库  查询成功，不存在该实例化主题分面树"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getTreeByDomain(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("ClassName") String className) {

        List<DomainTopic> domainTopics = DomainTopicDAO.getDomainTopics(className);
        if (domainTopics.size() == 0) {
            return Response.status(401).entity(new error("获取课程下主题失败~")).build();
        }
        String topicName = domainTopics.get(0).getTermName();
        return getTreeByTopicForFragment(className, topicName, true);
    }

    @GET
    @Path("/getTreeByTopicForFragmentPure")
    @ApiOperation(value = "获得实例化主题分面树的数据，碎片没有标签", notes = "输入领域名和知识主题，获得实例化主题分面树的数据，碎片包含文本和图片且没有标签")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 402, message = "MySql数据库  查询成功，不存在该实例化主题分面树"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getTreeByTopicForFragmentPure(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("ClassName") String className,
            @DefaultValue("抽象资料型别") @ApiParam(value = "主题名", required = true) @QueryParam("TermName") String topicName
    ) {

        Response response = null;
        /**
         * 返回该主题一级/二级分面信息
         */
        List<FacetSimple> facetSimpleList1 = FacetDAO.getFacet(className, topicName, 1);
        List<FacetRelation> facetRelationList = FacetDAO.getFacetRelation(className, topicName, 1, 2);
        List<FacetRelation> facetRelationList2 = FacetDAO.getFacetRelation(className, topicName, 2, 3);


        /**
         * 一个主题返回一个实例化主题分面树
         */
        Tree tree = new Tree();
        int totalbranchlevel = 2;
        int branchnum = facetSimpleList1.size();
        int term_id = DomainTopicDAO.getDomainTopic(className, topicName);
        String name = topicName;
        List<Branch> children = new ArrayList<Branch>();

        /**
         * 得到主题分面树每个一级分枝上的数据
         */
        for (int i = 0; i < facetSimpleList1.size(); i++) {
            /**
             * 判断一级分面是否存在二级分面，不存在直接返回BranchSimple为树的节点，存在返回BranchComplex为树的节点
             */
            FacetSimple facetSimple = facetSimpleList1.get(i);
            List<FacetSimple> secondFacetList = FacetDAO.getChildFacet(facetSimple, facetRelationList);

            /**
             * 判断是否存在二级分面
             */
            if (secondFacetList.size() == 0) {

                /**
                 * 不存在二级分面，使用BranchSimple继承Branch
                 */
                int totalbranchlevel2 = 0;
                String facet_name = facetSimple.getFacetName();
                int totalbranchnum = 0;
                String type = "branch";
                /**
                 * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                 */
                List<Leaf> leafFragmentList = AssembleDAO.getFragmentByFacetPure(className, topicName, facet_name);
                int totalleafnum = leafFragmentList.size();
                BranchSimple branchSimple = new BranchSimple(totalbranchlevel2, facet_name, totalbranchnum, type, leafFragmentList, totalleafnum);
                children.add(branchSimple);

            } else {

                /**
                 * 存在二级分面，使用BranchComplex继承Branch
                 */
                int totalbranchlevel2 = 1;
                String facet_name = facetSimple.getFacetName();
                int totalbranchnum = secondFacetList.size();
                String type = "branch";

                /**
                 * 设置二级分枝的子分枝
                 */
                List<BranchSimple> branchSimpleList = new ArrayList<BranchSimple>();
                for (int j = 0; j < secondFacetList.size(); j++) {

                    /**
                     * 遍历每一个二级分面，设置每个二级分面的
                     */
                    FacetSimple secondFacet = secondFacetList.get(j);
                    List<FacetSimple> thirdFacetList = FacetDAO.getChildFacet(secondFacet, facetRelationList2);

                    /**
                     * 判断是否存在三级分面
                     */
                    if (thirdFacetList.size() == 0) {

                        /**
                         * 不存在三级分面，将BranchSimple添加到对应的父亲branchSimpleList中，文本碎片
                         */
                        int totalbranchlevel3 = 0;
                        String secondFacetName = secondFacet.getFacetName();
                        int totalbranchnum3 = 0;
                        String type3 = "branch";
                        /**
                         * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                         */
                        List<Leaf> leafFragmentList = AssembleDAO.getFragmentByFacetPure(className, topicName, secondFacetName);
                        int totalleafnum = leafFragmentList.size();
                        BranchSimple branchSimple = new BranchSimple(totalbranchlevel3, secondFacetName, totalbranchnum3, type3, leafFragmentList, totalleafnum);
                        branchSimpleList.add(branchSimple);

                    } else {
                        Log.log(className + "--->" + topicName + "--->" + secondFacet.getFacetName() + ", 该二级分面存在三级分面，三级分面待开发");
                        /**
                         * 存在三级分面，将三级分面的碎片内容全部挂载到二级分面上去
                         */
                        List<Leaf> leafAllList = new ArrayList<Leaf>();
                        for (int k = 0; k < thirdFacetList.size(); k++) {
                            FacetSimple thirdFacet = thirdFacetList.get(k);
                            String thirdFacetName = thirdFacet.getFacetName();

                            /**
                             * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                             */
                            List<Leaf> leafFragmentList = AssembleDAO.getFragmentByFacetPure(className, topicName, thirdFacetName);
                            leafAllList.addAll(leafFragmentList);
                        }
                        int totalbranchlevel3 = 0;
                        String secondFacetName = secondFacet.getFacetName();
                        int totalbranchnum3 = 0;
                        String type3 = "branch";
                        int totalleafnum = leafAllList.size();
                        BranchSimple branchSimple = new BranchSimple(totalbranchlevel3, secondFacetName, totalbranchnum3, type3, leafAllList, totalleafnum);
                        branchSimpleList.add(branchSimple);
                    }
                }
                int totalleafnum = secondFacetList.size();
                /**
                 * 将其添加到树的分枝中
                 */
                BranchComplex branchComplex = new BranchComplex(totalbranchlevel2, facet_name, totalbranchnum, type, branchSimpleList, totalleafnum);
                children.add(branchComplex);
            }

        }

        /**
         * 设置实例化分面树的值
         */
        tree.setTotalbranchlevel(totalbranchlevel);
        tree.setBranchnum(branchnum);
        tree.setTerm_id(term_id);
        tree.setName(name);
        tree.setChildren(children);

        /**
         * 返回实例化分面树数据
         */
        response = Response.status(200).entity(tree).build();
        return response;
    }

    @POST
    @Path("/getTreeByTopicForFragment")
    @ApiOperation(value = "获得实例化主题分面树的数据", notes = "输入领域名和知识主题，获得实例化主题分面树的数据，碎片包含文本和图片")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 402, message = "MySql数据库  查询成功，不存在该实例化主题分面树"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getTreeByTopicForFragment(
            @FormParam("ClassName") String className,
            @FormParam("TermName") String topicName,
            @FormParam("HasFragment") boolean hasFragment
    ) {

        Response response = null;
        /**
         * 返回该主题一级/二级分面信息
         */
        List<FacetSimple> facetSimpleList1 = FacetDAO.getFacet(className, topicName, 1);
        List<FacetRelation> facetRelationList = FacetDAO.getFacetRelation(className, topicName, 1, 2);
        List<FacetRelation> facetRelationList2 = FacetDAO.getFacetRelation(className, topicName, 2, 3);


        /**
         * 一个主题返回一个实例化主题分面树
         */
        Tree tree = new Tree();
        int totalbranchlevel = 2;
        int branchnum = facetSimpleList1.size();
        int term_id = DomainTopicDAO.getDomainTopic(className, topicName);
        String name = topicName;
        List<Branch> children = new ArrayList<Branch>();

        /**
         * 得到主题分面树每个一级分枝上的数据
         */
        for (int i = 0; i < facetSimpleList1.size(); i++) {
            /**
             * 判断一级分面是否存在二级分面，不存在直接返回BranchSimple为树的节点，存在返回BranchComplex为树的节点
             */
            FacetSimple facetSimple = facetSimpleList1.get(i);
            List<FacetSimple> secondFacetList = FacetDAO.getChildFacet(facetSimple, facetRelationList);

            /**
             * 判断是否存在二级分面
             */
            if (secondFacetList.size() == 0) {

                /**
                 * 不存在二级分面，使用BranchSimple继承Branch
                 */
                int totalbranchlevel2 = 0;
                String facet_name = facetSimple.getFacetName();
                int totalbranchnum = 0;
                String type = "branch";

                List<Leaf> leafFragmentList = new ArrayList<>();
                if (hasFragment) {
                    /**
                     * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                     */
                    leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, facet_name);
                }
                int totalleafnum = leafFragmentList.size();
                BranchSimple branchSimple = new BranchSimple(totalbranchlevel2, facet_name, totalbranchnum, type, leafFragmentList, totalleafnum);
                children.add(branchSimple);

            } else {

                /**
                 * 存在二级分面，使用BranchComplex继承Branch
                 */
                int totalbranchlevel2 = 1;
                String facet_name = facetSimple.getFacetName();
                int totalbranchnum = secondFacetList.size();
                String type = "branch";

                /**
                 * 设置二级分枝的子分枝
                 */
                List<BranchSimple> branchSimpleList = new ArrayList<BranchSimple>();
                for (int j = 0; j < secondFacetList.size(); j++) {

                    /**
                     * 遍历每一个二级分面，设置每个二级分面的
                     */
                    FacetSimple secondFacet = secondFacetList.get(j);
                    List<FacetSimple> thirdFacetList = FacetDAO.getChildFacet(secondFacet, facetRelationList2);

                    /**
                     * 判断是否存在三级分面
                     */
                    if (thirdFacetList.size() == 0) {

                        /**
                         * 不存在三级分面，将BranchSimple添加到对应的父亲branchSimpleList中，文本碎片
                         */
                        int totalbranchlevel3 = 0;
                        String secondFacetName = secondFacet.getFacetName();
                        int totalbranchnum3 = 0;
                        String type3 = "branch";
                        List<Leaf> leafFragmentList = new ArrayList<>();
                        if (hasFragment) {
                            /**
                             * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                             */
                            leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, secondFacetName);
                        }
//                        /**
//                         * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
//                         */
//                        List<Leaf> leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, secondFacetName);
                        int totalleafnum = leafFragmentList.size();
                        BranchSimple branchSimple = new BranchSimple(totalbranchlevel3, secondFacetName, totalbranchnum3, type3, leafFragmentList, totalleafnum);
                        branchSimpleList.add(branchSimple);

                    } else {
                        Log.log(className + "--->" + topicName + "--->" + secondFacet.getFacetName() + ", 该二级分面存在三级分面，三级分面待开发");
                        /**
                         * 存在三级分面，将三级分面的碎片内容全部挂载到二级分面上去
                         */
                        List<Leaf> leafAllList = new ArrayList<Leaf>();
                        for (int k = 0; k < thirdFacetList.size(); k++) {
                            FacetSimple thirdFacet = thirdFacetList.get(k);
                            String thirdFacetName = thirdFacet.getFacetName();

                            List<Leaf> leafFragmentList = new ArrayList<>();
                            if (hasFragment) {
                                /**
                                 * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                                 */
                                leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, thirdFacetName);
                            }

//                            /**
//                             * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
//                             */
//                            List<Leaf> leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, thirdFacetName);
                            leafAllList.addAll(leafFragmentList);
                        }
                        int totalbranchlevel3 = 0;
                        String secondFacetName = secondFacet.getFacetName();
                        int totalbranchnum3 = 0;
                        String type3 = "branch";
                        int totalleafnum = leafAllList.size();
                        BranchSimple branchSimple = new BranchSimple(totalbranchlevel3, secondFacetName, totalbranchnum3, type3, leafAllList, totalleafnum);
                        branchSimpleList.add(branchSimple);
                    }
                }
                int totalleafnum = secondFacetList.size();
                /**
                 * 将其添加到树的分枝中
                 */
                BranchComplex branchComplex = new BranchComplex(totalbranchlevel2, facet_name, totalbranchnum, type, branchSimpleList, totalleafnum);
                children.add(branchComplex);
            }

        }

        /**
         * 设置实例化分面树的值
         */
        tree.setTotalbranchlevel(totalbranchlevel);
        tree.setBranchnum(branchnum);
        tree.setTerm_id(term_id);
        tree.setName(name);
        tree.setChildren(children);

        /**
         * 返回实例化分面树数据
         */
        response = Response.status(200).entity(tree).build();
        return response;
    }

    @GET
    @Path("/getTreeByTopicForFragment1")
    @ApiOperation(value = "获得实例化主题分面树的数据", notes = "输入领域名和知识主题，获得实例化主题分面树的数据，碎片包含文本和图片")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "MySql数据库  查询失败"),
            @ApiResponse(code = 402, message = "MySql数据库  查询成功，不存在该实例化主题分面树"),
            @ApiResponse(code = 200, message = "MySql数据库  查询成功", response = String.class)})
    @Consumes("application/x-www-form-urlencoded" + ";charset=" + "UTF-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=" + "UTF-8")
    public static Response getTreeByTopicForFragment1(
            @DefaultValue("数据结构") @ApiParam(value = "领域名", required = true) @QueryParam("ClassName") String className,
            @DefaultValue("抽象资料型别") @ApiParam(value = "主题名", required = true) @QueryParam("TermName") String topicName,
            @DefaultValue("false") @ApiParam(value = "是否有碎片", required = true) @QueryParam("HasFragment") boolean hasFragment
    ) {

        Response response = null;
        /**
         * 返回该主题一级/二级分面信息
         */
        List<FacetSimple> facetSimpleList1 = FacetDAO.getFacet(className, topicName, 1);
        List<FacetRelation> facetRelationList = FacetDAO.getFacetRelation(className, topicName, 1, 2);
        List<FacetRelation> facetRelationList2 = FacetDAO.getFacetRelation(className, topicName, 2, 3);


        /**
         * 一个主题返回一个实例化主题分面树
         */
        Tree tree = new Tree();
        int totalbranchlevel = 2;
        int branchnum = facetSimpleList1.size();
        int term_id = DomainTopicDAO.getDomainTopic(className, topicName);
        String name = topicName;
        List<Branch> children = new ArrayList<Branch>();

        /**
         * 得到主题分面树每个一级分枝上的数据
         */
        for (int i = 0; i < facetSimpleList1.size(); i++) {
            /**
             * 判断一级分面是否存在二级分面，不存在直接返回BranchSimple为树的节点，存在返回BranchComplex为树的节点
             */
            FacetSimple facetSimple = facetSimpleList1.get(i);
            List<FacetSimple> secondFacetList = FacetDAO.getChildFacet(facetSimple, facetRelationList);

            /**
             * 判断是否存在二级分面
             */
            if (secondFacetList.size() == 0) {

                /**
                 * 不存在二级分面，使用BranchSimple继承Branch
                 */
                int totalbranchlevel2 = 0;
                String facet_name = facetSimple.getFacetName();
                int totalbranchnum = 0;
                String type = "branch";

                List<Leaf> leafFragmentList = new ArrayList<>();
                if (hasFragment) {
                    /**
                     * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                     */
                    leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, facet_name);
                }
                int totalleafnum = leafFragmentList.size();
                BranchSimple branchSimple = new BranchSimple(totalbranchlevel2, facet_name, totalbranchnum, type, leafFragmentList, totalleafnum);
                children.add(branchSimple);

            } else {

                /**
                 * 存在二级分面，使用BranchComplex继承Branch
                 */
                int totalbranchlevel2 = 1;
                String facet_name = facetSimple.getFacetName();
                int totalbranchnum = secondFacetList.size();
                String type = "branch";

                /**
                 * 设置二级分枝的子分枝
                 */
                List<BranchSimple> branchSimpleList = new ArrayList<BranchSimple>();
                for (int j = 0; j < secondFacetList.size(); j++) {

                    /**
                     * 遍历每一个二级分面，设置每个二级分面的
                     */
                    FacetSimple secondFacet = secondFacetList.get(j);
                    List<FacetSimple> thirdFacetList = FacetDAO.getChildFacet(secondFacet, facetRelationList2);

                    /**
                     * 判断是否存在三级分面
                     */
                    if (thirdFacetList.size() == 0) {

                        /**
                         * 不存在三级分面，将BranchSimple添加到对应的父亲branchSimpleList中，文本碎片
                         */
                        int totalbranchlevel3 = 0;
                        String secondFacetName = secondFacet.getFacetName();
                        int totalbranchnum3 = 0;
                        String type3 = "branch";
                        List<Leaf> leafFragmentList = new ArrayList<>();
                        if (hasFragment) {
                            /**
                             * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                             */
                            leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, secondFacetName);
                        }
//                        /**
//                         * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
//                         */
//                        List<Leaf> leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, secondFacetName);
                        int totalleafnum = leafFragmentList.size();
                        BranchSimple branchSimple = new BranchSimple(totalbranchlevel3, secondFacetName, totalbranchnum3, type3, leafFragmentList, totalleafnum);
                        branchSimpleList.add(branchSimple);

                    } else {
                        Log.log(className + "--->" + topicName + "--->" + secondFacet.getFacetName() + ", 该二级分面存在三级分面，三级分面待开发");
                        /**
                         * 存在三级分面，将三级分面的碎片内容全部挂载到二级分面上去
                         */
                        List<Leaf> leafAllList = new ArrayList<Leaf>();
                        for (int k = 0; k < thirdFacetList.size(); k++) {
                            FacetSimple thirdFacet = thirdFacetList.get(k);
                            String thirdFacetName = thirdFacet.getFacetName();

                            List<Leaf> leafFragmentList = new ArrayList<>();
                            if (hasFragment) {
                                /**
                                 * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
                                 */
                                leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, thirdFacetName);
                            }

//                            /**
//                             * 树叶同时包含：文本碎片 + 图片碎片，新API读取 assemble_fragment 表格获取文本和图片数据
//                             */
//                            List<Leaf> leafFragmentList = AssembleDAO.getFragmentByFacet(className, topicName, thirdFacetName);
                            leafAllList.addAll(leafFragmentList);
                        }
                        int totalbranchlevel3 = 0;
                        String secondFacetName = secondFacet.getFacetName();
                        int totalbranchnum3 = 0;
                        String type3 = "branch";
                        int totalleafnum = leafAllList.size();
                        BranchSimple branchSimple = new BranchSimple(totalbranchlevel3, secondFacetName, totalbranchnum3, type3, leafAllList, totalleafnum);
                        branchSimpleList.add(branchSimple);
                    }
                }
                int totalleafnum = secondFacetList.size();
                /**
                 * 将其添加到树的分枝中
                 */
                BranchComplex branchComplex = new BranchComplex(totalbranchlevel2, facet_name, totalbranchnum, type, branchSimpleList, totalleafnum);
                children.add(branchComplex);
            }

        }

        /**
         * 设置实例化分面树的值
         */
        tree.setTotalbranchlevel(totalbranchlevel);
        tree.setBranchnum(branchnum);
        tree.setTerm_id(term_id);
        tree.setName(name);
        tree.setChildren(children);

        /**
         * 返回实例化分面树数据
         */
        response = Response.status(200).entity(tree).build();
        return response;
    }

}
