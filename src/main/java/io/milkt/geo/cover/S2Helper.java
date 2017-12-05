package io.milkt.geo.cover;

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leewind on 2017/12/3.
 *
 * @author leewind (leewind19841209@gmail.com)
 * @version v0.0.1 2017.12.2
 */
public class S2Helper {

  public static void showRect(S2CellUnion s2CellUnion){
    showRect(s2CellUnion.cellIds());
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

}
