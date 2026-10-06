package ni.edu.uam.facturacionapp.dao;

import ni.edu.uam.facturacionapp.model.Categoria;
import ni.edu.uam.facturacionapp.database.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDao {

    // Validar duplicados por nombre (LOWER para ignorar mayúsculas/minúsculas)
    public boolean existeNombre(String nombre) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM categoria
            WHERE LOWER(nombre) = LOWER(?)
            """;

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn;
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombre.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    // Validar si existen productos vinculados antes de eliminar
    // Reemplaza el método en CategoriaDao.java con este:
    public boolean tieneProductos(int categoriaId) throws SQLException {
        String sql = """
        SELECT COUNT(*)
        FROM producto
        WHERE id_categoria = ?
        """;

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn;
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoriaId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    public boolean insertar(Categoria c) throws SQLException {
        String sql = "INSERT INTO categoria (nombre, is_active) VALUES (?, ?)";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn;
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, c.getNombre());
            stmt.setBoolean(2, c.isActiva());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean actualizar(Categoria c) throws SQLException {
        String sql = "UPDATE categoria SET nombre = ?, is_active = ? WHERE id = ?";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn;
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, c.getNombre());
            stmt.setBoolean(2, c.isActiva());
            stmt.setInt(3, c.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM categoria WHERE id = ?";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn;
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            return stmt.executeUpdate() > 0;
        }
    }

    public List<Categoria> listar() throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, is_active FROM categoria";

        Connection conn = ConexionDB.conectar();

        if (conn == null) {
            throw new SQLException("No se pudo establecer la conexión a la base de datos.");
        }

        try (conn;
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Categoria c = new Categoria(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getBoolean("is_active")
                );
                lista.add(c);
            }
        }

        return lista;
    }
}