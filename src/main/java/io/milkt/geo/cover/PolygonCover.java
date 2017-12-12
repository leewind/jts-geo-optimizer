package io.milkt.geo.cover;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2RegionCoverer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by leewind on 2017/12/7.
 *
 * @author leewind (leewind19841209@gmail.com)
 * @version v0.1 2017.12.8
 */
public class PolygonCover {

  private final static String CONNECTION_URL = "jdbc:mysql://localhost:3306/isochrone?user=root&password=ilove1388&useUnicode=true&characterEncoding=UTF8";

  public static void main(String[] args) throws Exception {

    Class.forName("com.mysql.jdbc.Driver");
    Connection conn = DriverManager.getConnection(CONNECTION_URL);

    List<S2Point> s2Points = S2Helper.distribute(6428);
    System.out.println("计算出点的数量是：" + s2Points.size());

//    使用等大的cell进行全面积的覆盖
    ArrayList<S2CellId> result = new ArrayList<>();
    S2RegionCoverer.getSimpleCovering(new S2Loop(s2Points), s2Points.get(0), 16, result);
    System.out.println("覆盖的cell的数量是：" + result.size());

    HashMap<String, S2CellId> maps = new HashMap<>();
    for (S2CellId s2CellId : result) {
      maps.put(s2CellId.toToken(), s2CellId);
    }

//    对每个S2Cell求相邻边同等级Cell，如果这个Cell不在Map中，并且Cell的相邻边同等级Cell都在Map中含有，
//    说明他是被包围住的中空的Cell需要被填补，对应的是一个被包围的空白Cell

    boolean isModified = true;
    while(isModified) {

      isModified = false;

      for (int i = 0; i < result.size(); i++) {

        List<S2CellId> out = new ArrayList<>();
        result.get(i).getAllNeighbors(16, out);

        for (S2CellId one : out) {
          if (maps.containsKey(one.toToken())) {
            continue;
          }

          boolean isEdgeInside = true;
          S2CellId[] nexts = new S2CellId[4];
          one.getEdgeNeighbors(nexts);

          for (S2CellId next : nexts) {
            isEdgeInside = isEdgeInside && maps.containsKey(next.toToken());
          }

          if (isEdgeInside) {
            isModified = true;
            System.out.println("edge inside");
            maps.put(one.toToken(), one);
            result.add(one);
          }

//        --------------------------------------------------       //

          boolean isVertexInside = true;
          List<S2CellId> list = new ArrayList<>();
          one.getVertexNeighbors(16, list);

          for (S2CellId next : list) {
            isVertexInside = isVertexInside && maps.containsKey(next.toToken());
          }

          if (isVertexInside) {
            isModified = true;
            System.out.println("vertex inside");
            maps.put(one.toToken(), one);
            result.add(one);
          }


//          ------------------------------------------------      //
//          局部最优解，一步步向里吃，需要在凹多边形进行验证

          List<S2CellId> neighbors = new ArrayList<>();
          one.getAllNeighbors(16, neighbors);

          int count = 0 ;
          for (S2CellId next : neighbors) {
            if (maps.containsKey(next.toToken())){
              count++;
            }
          }

          if (count >= 5){
            isModified = true;
            System.out.println("neighbor inside");
            maps.put(one.toToken(), one);
            result.add(one);
          }
        }
      }
    }

    S2Helper.showRect(result);

//    对每个cell取100m的范围获取高德的poi信息，并落库
//    从poi信息中获得，如果是路障poi信息进行清除

    int j = 0;
    List<String> removeList = new ArrayList<>();
    for(String token: maps.keySet()){

      S2CellId s2CellId = maps.get(token);

      String query = String
          .format("select detail from cell_geo_info where cellid=%d", s2CellId.id());
      Statement statement = conn.createStatement();
      ResultSet rs = statement.executeQuery(query);

      String response = null;
      if ( !rs.first()) {
        S2LatLng s2LatLng = s2CellId.toLatLng();
        Coordinate coordinate = new Coordinate(s2LatLng.lngDegrees(), s2LatLng.latDegrees());
        response = AmapFacade.getCellInfo(coordinate);

//        @TODO 判断一下如果返回OK, 存库
        String sentence = String.format(
            "insert into cell_geo_info (cellid, token, lng, lat, detail) values (%d, \'%s\', %f, %f, \'%s\')",
            s2CellId.id(), s2CellId.toToken(), s2LatLng.lngDegrees(), s2LatLng.latDegrees(),
            response);
        statement.execute(sentence);
      } else {
        response = rs.getString(1);
      }

      JSONObject ojson = JSON.parseObject(response);
      JSONObject building = ojson.getJSONObject("regeocode")
          .getJSONObject("addressComponent")
          .getJSONObject("building");

      if (building.getString("name").contains("黄浦江")
          && building.getString("type").contains("旅游景点")
          ) {
        removeList.add(token);
      }
    }

    for (String token : removeList) {
      maps.remove(token);
    }

//    S2Helper.showRect(maps);

//    对cell进行封装，把它做成geometry队列
    List<Geometry> geometries = new ArrayList<>();

    List<Long> ids = new ArrayList<>();
    for (String token: maps.keySet()) {
      ids.add(maps.get(token).id());
    }

    Collections.sort(ids);

    for (Long id: ids) {
      S2CellId s2CellId = new S2CellId(id);
      S2Cell s2Cell = new S2Cell(s2CellId);
      geometries.add(JTSHelper.s2CellConvertToGeometry(s2Cell));
    }


//    Set<String> keySet = maps.keySet();
//    Iterator<String> iterator = keySet.iterator();
//    while(iterator.hasNext()){
//      String token = iterator.next();
//      geometries.add(JTSHelper.s2CellConvertToGeometry(new S2Cell(maps.get(token))));
//    }

    showGeometry(geometries);
  }

  private static Polygon acquireMax(MultiPolygon multiPolygon) {
    Polygon polygon = null;
    for (int i=0;i < multiPolygon.getNumGeometries(); i++) {
      if (polygon == null || polygon.getArea() < (multiPolygon.getGeometryN(i)).getArea()) {
        polygon = (Polygon) multiPolygon.getGeometryN(i);
      }
    }

    return polygon;
  }

  private static void showGeometry(List<Geometry> geometries) {
    Geometry[] geometryArray = new Geometry[geometries.size()];
    geometryArray = geometries.toArray(geometryArray);

    GeometryFactory factory = new GeometryFactory();
    GeometryCollection geometryCollection = factory.createGeometryCollection(geometryArray);
    Geometry buffer = geometryCollection.union();
    buffer = buffer.union();
//    Geometry buffer = geometry.buffer(0);

    Polygon polygon = null;
    if (buffer instanceof MultiPolygon){
      MultiPolygon multiPolygon = (MultiPolygon) buffer;
      polygon = acquireMax(multiPolygon);
    }else if (buffer instanceof Polygon) {
      polygon = (Polygon) buffer;
    }

    List<String> pointStrs = new ArrayList<>();
    Coordinate[] outSidePoints = polygon.getCoordinates();

//        polygon.getEnvelope().getCoordinates();
    for (Coordinate coordinate : outSidePoints) {
      pointStrs.add("[" + coordinate.x + "," + coordinate.y + "]");
    }

    System.out.println("[" + String.join(",", pointStrs) + "],");
  }

}
