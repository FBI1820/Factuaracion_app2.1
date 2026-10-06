package ni.edu.uam.facturacionapp.dao;

import ni.edu.uam.facturacionapp.model.Categoria;
import ni.edu.uam.facturacionapp.model.Producto;
import ni.edu.uam.facturacionapp.database.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDao {

    // Validar duplicados por código (evita duplicados con variaciones de mayúsculas)
    public boolean existeCodigo(String codigo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM producto WHERE LOWER(codigo) = LOWER(?)";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn; PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // Validar duplicados por nombre
    public boolean existeNombre(String nombre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM producto WHERE LOWER(nombre) = LOWER(?)";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn; PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombre.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto " +
                "(codigo, nombre, id_categoria, precio_venta, existencia, ruta_imagen, activo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn; PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getCodigo().trim());
            stmt.setString(2, p.getNombre().trim());
            stmt.setInt(3, p.getCategoria().getId());
            stmt.setBigDecimal(4, p.getPrecioVenta());
            stmt.setInt(5, p.getExistencia());
            stmt.setString(6, p.getRutaImagen());
            stmt.setBoolean(7, p.getActivo());

            return stmt.executeUpdate() > 0;
        }
    }

    public List<Producto> listar() throws SQLException {
        List<Producto> lista = new ArrayList<>();

        String sql = "SELECT p.*, " +
                "c.nombre AS nombre_categoria, " +
                "c.activa AS activa_cat " +
                "FROM producto p " +
                "INNER JOIN categoria c ON p.id_categoria = c.id " +
                "ORDER BY p.id DESC";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn;
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Categoria cat = new Categoria(
                        rs.getInt("id_categoria"),
                        rs.getString("nombre_categoria"),
                        rs.getBoolean("activa_cat")
                );

                Producto p = new Producto(
                        rs.getInt("id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        cat,
                        rs.getBigDecimal("precio_venta"),
                        rs.getInt("existencia"),
                        rs.getString("ruta_imagen"),
                        rs.getBoolean("activo")
                );

                lista.add(p);
            }
        }

        return lista;
    }

    public boolean actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET codigo = ?, nombre = ?, id_categoria = ?, " +
                "precio_venta = ?, existencia = ?, ruta_imagen = ?, activo = ? " +
                "WHERE id = ?";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn; PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getCodigo().trim());
            stmt.setString(2, p.getNombre().trim());
            stmt.setInt(3, p.getCategoria().getId());
            stmt.setBigDecimal(4, p.getPrecioVenta());
            stmt.setInt(5, p.getExistencia());
            stmt.setString(6, p.getRutaImagen());
            stmt.setBoolean(7, p.getActivo());
            stmt.setInt(8, p.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM producto WHERE id = ?";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn; PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
}