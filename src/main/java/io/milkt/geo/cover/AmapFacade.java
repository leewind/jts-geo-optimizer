package io.milkt.geo.cover;

import com.vividsolutions.jts.geom.Coordinate;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.fluent.Request;

/**
 * Created by leewind on 2017/12/8.
 */
public class AmapFacade {

  private static final String AMAP_KEY = "87165763f132e3a8580d9ad41a08e53b";
  private static final String AMAP_REGEO = "http://restapi.amap.com/v3/geocode/regeo";

  private static String request(Map<String, String> mapping, String address) {
    String response = null;
    try {

      // 拼接Query参数
      StringBuilder params = new StringBuilder();
      for (Map.Entry<String, String> entry : mapping.entrySet()) {
        params.append(entry.getKey())
            .append("=")
            .append(URLEncoder.encode(entry.getValue(), "utf-8"))
            .append("&");
      }

      // 发起请求获取返回
      response = Request.Get(address + "?" + params.toString()).execute().returnContent()
          .toString();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return response;
  }


  public static String getCellInfo(Coordinate coordinate) {
    Map<String, String> mapping = new HashMap<String, String>();

    mapping.put("key", AMAP_KEY);
    mapping.put("radius", "100");
    mapping.put("location",
        String.valueOf(coordinate.x) + ',' + String.valueOf(coordinate.y));

    String response = request(mapping, AMAP_REGEO);
//    JSONObject responseJson = JSON.parseObject(response);

    System.out.println(response);
    return response;
  }
}
