package com.campuseateries.dao;

import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.Restaurant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RestaurantDao {

    private final DbConnectionManager db = DbConnectionManager.getInstance();

    public List<Restaurant> findActiveOrdered() throws Exception {
        List<Restaurant> list = new ArrayList<>();
        String sql = """
                SELECT restaurant_id,name,description,campus_zone,is_active
                FROM restaurants
                WHERE is_active=1
                ORDER BY campus_zone,name
                """;

        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Restaurant> find(long id) throws Exception {
        String sql = """
                SELECT restaurant_id,name,description,campus_zone,is_active
                FROM restaurants WHERE restaurant_id=?
                """;
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static Restaurant map(ResultSet rs) throws Exception {
        return new Restaurant(
                rs.getLong("restaurant_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("campus_zone"),
                rs.getBoolean("is_active"));
    }
}
