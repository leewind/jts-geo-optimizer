package io.milkt.geo.cover;

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2RegionCoverer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by leewind on 2017/12/2.
 */
public class OrderArea {

  private final static String CONNECTION_URL = "jdbc:mysql://localhost:3306/isochrone?user=root&password=ilove1388&useUnicode=true&characterEncoding=UTF8";

  public static void main(String[] args) throws Exception {
    Class.forName("com.mysql.jdbc.Driver");
    Connection conn = DriverManager.getConnection(CONNECTION_URL);

    Statement statement = conn.createStatement();

    String sentence = "select lat,lon,yxwcd from delivery_order";
    ResultSet rs = statement.executeQuery(sentence);

    List<S2Point> s2Points = new ArrayList<>();
    HashMap<String, OrderS2CellId> orderS2CellIdHashMap = new HashMap<>();
    while (rs.next()) {
      S2LatLng s2LatLng = S2LatLng.fromDegrees(rs.getDouble(1), rs.getDouble(2));
      S2Point s2Point = s2LatLng.toPoint();
      s2Points.add(s2Point);
      S2CellId s2CellId = S2CellId.fromLatLng(s2LatLng);
      s2CellId = s2CellId.parent(15);
      String token = s2CellId.toToken();

      OrderS2CellId orderS2CellId;
      if (orderS2CellIdHashMap.containsKey(token)) {
        orderS2CellId = orderS2CellIdHashMap.get(token);
        orderS2CellId.count += rs.getInt(3);
      } else {
        orderS2CellId = new OrderS2CellId();
        orderS2CellId.count = rs.getInt(3);
        orderS2CellId.s2CellId = s2CellId;
      }

      orderS2CellIdHashMap.put(token, orderS2CellId);
    }

    for (String token: orderS2CellIdHashMap.keySet()) {
      OrderS2CellId orderS2CellId = orderS2CellIdHashMap.get(token);
//      System.out.println(orderS2CellId.count);

      S2Cell s2Cell = new S2Cell(orderS2CellId.s2CellId);
      List<String> pointStrs = new ArrayList<>();
      for (int j = 0; j < 4; j++) {
        S2LatLng latLng = new S2LatLng(s2Cell.getVertex(j));
        pointStrs.add("[" + latLng.lng().degrees()+ "," + latLng.lat().degrees() + "]");
      }
      System.out.println("{area: [" + String.join(",", pointStrs) +"], count: " + orderS2CellId.count + "},");
    }

//    使用等大的cell进行覆盖
    ArrayList<S2CellId> result = new ArrayList<>();
    S2RegionCoverer.getSimpleCovering(new S2Loop(s2Points), s2Points.get(0), 18, result);
    S2Helper.showRect(result);

//    使用贪婪算法最快速度的覆盖
//    S2RegionCoverer s2RegionCoverer = new S2RegionCoverer();
//    s2RegionCoverer.setMinLevel(16);
//    s2RegionCoverer.setMaxLevel(22);
//    s2RegionCoverer.setMaxCells(500);
//    s2RegionCoverer.setLevelMod(2);
//
//    S2CellUnion s2CellUnion = s2RegionCoverer.getCovering(new S2Loop(s2Points));
//    System.out.println(s2CellUnion.cellIds().size());
//
//    S2Helper.showRect(s2CellUnion);
  }
}
