package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import java.util.Objects;

public class LocationPoint {

    private String locCode;
    private Integer floor;

    public LocationPoint(String locCode, Integer floor) {
        this.locCode = locCode;
        this.floor = floor;
    }

    public String getLocCode() {
        return locCode;
    }

    public Integer getFloor() {
        return floor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationPoint)) return false;
        LocationPoint that = (LocationPoint) o;
        return Objects.equals(locCode, that.locCode)
                && Objects.equals(floor, that.floor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locCode, floor);
    }

    @Override
    public String toString() {
        return "LocationPoint{" +
                "locCode='" + locCode + '\'' +
                ", floor=" + floor +
                '}';
    }
}