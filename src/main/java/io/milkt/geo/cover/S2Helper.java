package io.milkt.geo.cover;

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Point;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by leewind on 2017/12/3.
 *
 * @author leewind (leewind19841209@gmail.com)
 * @version v0.0.1 2017.12.2
 */
public class S2Helper {

  private final static String CONNECTION_URL = "jdbc:mysql://localhost:3306/isochrone?user=root&password=ilove1388&useUnicode=true&characterEncoding=UTF8";

  public static void showRect(S2CellUnion s2CellUnion){
    showRect(s2CellUnion.cellIds());
  }

  public static ResultSet search(int gridId) throws ClassNotFoundException, SQLException {
    Class.forName("com.mysql.jdbc.Driver");
    Connection conn = DriverManager.getConnection(CONNECTION_URL);

    Statement statement = conn.createStatement();

    String sentence = "select lat,lon,yxwcd from delivery_order where grid_id=" + gridId;
    return statement.executeQuery(sentence);
  }

  public static void showRect(List<S2CellId> s2CellIds){
    for (S2CellId s2cellid : s2CellIds) {
      S2Cell s2Cell = new S2Cell(s2cellid);
      List<String> pointStrs = new ArrayList<>();
      for (int j = 0; j < 4; j++) {
        S2LatLng latLng = new S2LatLng(s2Cell.getVertex(j));
        pointStrs.add("[" + latLng.lng().degrees()+ "," + latLng.lat().degrees() + "]");
      }
      System.out.println("[" + String.join(",", pointStrs) +"],");
    }
  }

  public static void showRect(HashMap<String, S2CellId> maps){
    for (String token : maps.keySet()) {
      S2Cell s2Cell = new S2Cell(maps.get(token));
      List<String> pointStrs = new ArrayList<>();
      for (int j = 0; j < 4; j++) {
        S2LatLng latLng = new S2LatLng(s2Cell.getVertex(j));
        pointStrs.add("[" + latLng.lng().degrees()+ "," + latLng.lat().degrees() + "]");
      }
      System.out.println("[" + String.join(",", pointStrs) +"],");
    }
  }

  public static List<S2Point> distribute(int gridId) throws ClassNotFoundException, SQLException {

    ResultSet rs = search(gridId);

    List<S2Point> s2Points = new ArrayList<>();
    HashMap<String, OrderS2CellId> orderS2CellIdHashMap = new HashMap<>();
    while (rs.next()) {
      S2LatLng s2LatLng = S2LatLng.fromDegrees(rs.getDouble(1), rs.getDouble(2));
      S2Point s2Point = s2LatLng.toPoint();
      s2Points.add(s2Point);
      S2CellId s2CellId = S2CellId.fromLatLng(s2LatLng);
      s2CellId = s2CellId.parent(16);
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

      S2Cell s2Cell = new S2Cell(orderS2CellId.s2CellId);
      for (int j = 0; j < 4; j++) {
        S2LatLng latLng = new S2LatLng(s2Cell.getVertex(j));
      }
    }

    return s2Points;
  }


  public static List<S2CellId> findOutside(ArrayList<S2CellId> result, HashMap<String, S2Cell> maps) {
    List<S2CellId> finalResults = new ArrayList<>();

    for (S2CellId s2CellId : result){

      List<S2CellId> out = new ArrayList<>();
      s2CellId.getAllNeighbors(16, out);

      boolean isOutside = false;
      for (S2CellId one: out) {
        isOutside = isOutside || !maps.containsKey(one.toToken());
      }

      if (isOutside){
        finalResults.add(s2CellId);
      }
    }

    return finalResults;
  }


}
