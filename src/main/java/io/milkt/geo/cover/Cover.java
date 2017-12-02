package io.milkt.geo.cover;

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2Polygon;
import com.google.common.geometry.S2RegionCoverer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by leewind on 2017/12/1.
 *
 * 通过Google S2 Library中的贪婪算法对区域面积进行cover
 */
public class Cover {

  private final static String CONNECTION_URL = "jdbc:mysql://localhost:3306/isochrone?user=root&password=ilove1388&useUnicode=true&characterEncoding=UTF8";

  public static void main(String[] args) throws Exception {

    Class.forName("com.mysql.jdbc.Driver");
    Connection conn = DriverManager.getConnection(CONNECTION_URL);

    Statement statement = conn.createStatement();

    String sentence = "select * from grid_border";
    ResultSet rs = statement.executeQuery(sentence);

    List<S2Point> s2Points = new ArrayList<>();
    while (rs.next()) {
      S2LatLng s2LatLng = S2LatLng.fromDegrees(rs.getDouble(3), rs.getDouble(4));
      s2Points.add(s2LatLng.toPoint());
    }

    Collections.reverse(s2Points);

    S2Loop s2Loop = new S2Loop(s2Points);
    S2Polygon s2Polygon = new S2Polygon(s2Loop);

    S2RegionCoverer s2RegionCoverer = new S2RegionCoverer();
    s2RegionCoverer.setMinLevel(8);
    s2RegionCoverer.setMaxLevel(20);
    s2RegionCoverer.setMaxCells(500);

    //    调查模式的具体含义
    //    s2RegionCoverer.setLevelMod(3);

    S2CellUnion s2CellUnion = s2RegionCoverer.getCovering(s2Loop);
    System.out.println(s2CellUnion.cellIds().size());

    for (S2CellId s2cellid : s2CellUnion.cellIds()) {
      S2Cell s2Cell = new S2Cell(s2cellid);
      List<String> pointStrs = new ArrayList<>();
      for (int j = 0; j < 4; j++) {
        S2LatLng latLng = new S2LatLng(s2Cell.getVertex(j));
        pointStrs.add("[" + latLng.lng().degrees()+ "," + latLng.lat().degrees() + "]");
      }
      System.out.println("[" + String.join(",", pointStrs) +"],");
    }

  }
}