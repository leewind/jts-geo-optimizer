package io.milkt.geo.cover;

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2LatLng;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

/**
 * Created by leewind on 2017/12/8.
 *
 * @author leewind (leewind19841209@gmail.com)
 * @version v0.1
 */
public class JTSHelper {

  public static Geometry s2CellConvertToGeometry(S2Cell s2Cell) {

    GeometryFactory factory = new GeometryFactory();

    Coordinate[] coordinatesForCell = new Coordinate[5];
    for (int j = 0; j < 4; j++) {
      S2LatLng latLng = new S2LatLng(s2Cell.getVertex(j));
      coordinatesForCell[j] = new Coordinate(latLng.lng().degrees(), latLng.lat().degrees());
    }

    coordinatesForCell[4] = coordinatesForCell[0];

    LinearRing linearRing = factory.createLinearRing(coordinatesForCell);
    return factory.createPolygon(linearRing);
  }

}
