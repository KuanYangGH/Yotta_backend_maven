package utils;

import app.Config;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuanhao
 * @date 2018/5/10 17:41
 */
public class ExcelUtils {

    public static void main(String[] args) {
        readCourse();
    }

    /**
     * 将网院的课程信息存到数据库表格中
     */
    public static void readCourse() {
        mysqlUtils mysql = new mysqlUtils();
        String sql = "insert into " + Config.COURSE_WANGYUAN_TABLE + " (courseId, courseName, courseWiki, courseCode) " +
                "values (?,?,?,?);";
        Map<String, String> map = new HashMap<>();
        String relationfilePath = "E:\\02-快盘\\郑元浩-2016-2017新Yotta系统设计\\105-网院集成\\3-开发资料\\CourseRelation.xls";
        String filePath = "E:\\02-快盘\\郑元浩-2016-2017新Yotta系统设计\\105-网院集成\\3-开发资料\\CourseWare.xls";
        try {
            // 读取网院课程和中文维基百科课程之间的对应关系
            Workbook workbookRelation = Workbook.getWorkbook(new File(relationfilePath));
            Sheet sheetRelation = workbookRelation.getSheet(0);
            int rowsRelation = sheetRelation.getRows();
            for (int i = 1; i < rowsRelation; i++) {
                String courseWangyuan = sheetRelation.getCell(0, i).getContents().toString();
                String courseWiki = sheetRelation.getCell(1, i).getContents().toString();
                map.put(courseWangyuan, courseWiki);
            }
            // 读取网院课程信息写到数据库中
            Workbook workbook = Workbook.getWorkbook(new File(filePath));
            Sheet sheet = workbook.getSheet(0);
            int rows = sheet.getRows();
            for (int i = 0; i < rows; i++) {
                int courseId = Integer.parseInt(sheet.getCell(0, i).getContents().toString());
                String courseName = sheet.getCell(1, i).getContents().toString();
                String courseWiki = map.get(courseName);
                String courseCode = sheet.getCell(4, i).getContents().toString();
                List<Object> params = new ArrayList<>();
                params.add(courseId);
                params.add(courseName);
                params.add(courseWiki);
                params.add(courseCode);
                mysql.addDeleteModify(sql, params);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            mysql.closeconnection();
        }
    }

}
