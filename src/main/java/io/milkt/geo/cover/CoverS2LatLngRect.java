package io.milkt.geo.cover;

import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2LatLngRect;
import com.google.common.geometry.S2RegionCoverer;

/**
 * Created by leewind on 2017/12/1.
 */
public class CoverS2LatLngRect {

  public static void main(String[] args) throws Exception {

    S2LatLngRect s2LatLngRect = S2LatLngRect.fromPointPair(
        S2LatLng.fromDegrees(31.21009827, 121.4421844),
        S2LatLng.fromDegrees(31.21559143, 121.4476776)
    );

    S2RegionCoverer s2RegionCoverer = new S2RegionCoverer();
    s2RegionCoverer.setMinLevel(8);
    s2RegionCoverer.setMaxLevel(15);
    s2RegionCoverer.setMaxCells(500);

    S2CellUnion s2CellUnion = s2RegionCoverer.getCovering(s2LatLngRect);
    System.out.println(s2CellUnion.cellIds().size());
  }

}
