package ni.edu.uam.facturacionapp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import ni.edu.uam.facturacionapp.dao.CategoriaDao;
import ni.edu.uam.facturacionapp.model.Categoria;

import java.sql.SQLException;
import java.util.Optional;

public class CategoriaController {

    @FXML private TextField txtNombre;
    @FXML private CheckBox chkActivo;
    @FXML private TableView<Categoria> tblCategorias;
    @FXML private TableColumn<Categoria, Integer> colId;
    @FXML private TableColumn<Categoria, String> colNombre;
    @FXML private TableColumn<Categoria, Boolean> colActivo;
    @FXML private TextField txtBuscar;

    @FXML private Button btnEliminar;
    @FXML private Button btnActualizar;

    private final ObservableList<Categoria> categorias = FXCollections.observableArrayList();
    private final CategoriaDao categoriaDAO = new CategoriaDao();

    private Categoria categoriaEdicion = null;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activa"));

        tblCategorias.setItems(categorias);
        chkActivo.setSelected(true);

        cargarCategorias();
        Buscador();
    }

    private void cargarCategorias() {
        try {
            categorias.clear();
            categorias.addAll(categoriaDAO.listar());
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error al cargar categorías desde PostgreSQL: " + e.getMessage());
        }
    }

    private boolean validarCategoria() {
        String nombre = txtNombre.getText().trim();

        if (nombre.isEmpty()) {
            mostrarError("Validación", "El nombre de la categoría es obligatorio y no puede contener únicamente espacios.");
            txtNombre.requestFocus();
            return false;
        }

        try {
            if (categoriaEdicion == null) {
                // Modo creación: no debe existir otra categoría con el mismo nombre
                if (categoriaDAO.existeNombre(nombre)) {
                    mostrarError("Validación", "Ya existe una categoría registrada con el nombre '" + nombre + "'.");
                    txtNombre.requestFocus();
                    return false;
                }
            } else {
                // Modo edición: si cambió de nombre, verificar que el nuevo no exista en la BD
                if (!categoriaEdicion.getNombre().equalsIgnoreCase(nombre) && categoriaDAO.existeNombre(nombre)) {
                    mostrarError("Validación", "Ya existe otra categoría registrada con el nombre '" + nombre + "'.");
                    txtNombre.requestFocus();
                    return false;
                }
            }
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error de base de datos al validar duplicados: " + e.getMessage());
            return false;
        }

        return true;
    }

    @FXML
    private void editar() {
        Categoria seleccionada = tblCategorias.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Seleccione una categoría", "Debe seleccionar la categoría que desea actualizar.");
            return;
        }

        this.categoriaEdicion = seleccionada;
        txtNombre.setText(seleccionada.getNombre());
        chkActivo.setSelected(seleccionada.isActiva());
    }

    @FXML
    private void actualizar() {
        editar();
    }

    @FXML
    private void guardar() {
        if (!validarCategoria()) {
            return;
        }

        String nombreIngresado = txtNombre.getText().trim();

        try {
            if (categoriaEdicion == null) {
                // MODO: INSERTAR
                Categoria nuevaCategoria = new Categoria(
                        null,
                        nombreIngresado,
                        chkActivo.isSelected()
                );

                if (categoriaDAO.insertar(nuevaCategoria)) {
                    mensaje(Alert.AlertType.INFORMATION, "Categoría guardada con éxito.");
                    cargarCategorias();
                    limpiar();
                }
            } else {
                // MODO: ACTUALIZAR
                categoriaEdicion.setNombre(nombreIngresado);
                categoriaEdicion.setActiva(chkActivo.isSelected());

                if (categoriaDAO.actualizar(categoriaEdicion)) {
                    mensaje(Alert.AlertType.INFORMATION, "Categoría actualizada con éxito.");
                    cargarCategorias();
                    limpiar();
                } else {
                    mensaje(Alert.AlertType.ERROR, "No se pudo actualizar la categoría.");
                }
            }
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error de base de datos: " + e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        Categoria seleccionada = tblCategorias.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Validación", "Debe seleccionar la categoría que desea eliminar.");
            return;
        }

        try {
            // Validación previa: si tiene productos asociados, no continúa ni pide confirmación
            if (categoriaDAO.tieneProductos(seleccionada.getId())) {
                mostrarError("Operación Cancelada",
                        "No se puede eliminar la categoría '" + seleccionada.getNombre() +
                                "' porque existen productos asociados a ella.");
                return;
            }

            Alert confirmacion = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "¿Estás seguro de que deseas eliminar la categoría '" + seleccionada.getNombre() + "'?",
                    ButtonType.YES,
                    ButtonType.NO
            );

            Optional<ButtonType> respuesta = confirmacion.showAndWait();

            if (respuesta.isPresent() && respuesta.get() == ButtonType.YES) {
                if (categoriaDAO.eliminar(seleccionada.getId())) {
                    mensaje(Alert.AlertType.INFORMATION, "Categoría eliminada con éxito.");
                    cargarCategorias();
                    limpiar();
                } else {
                    mensaje(Alert.AlertType.ERROR, "No se pudo eliminar la categoría.");
                }
            }
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error de base de datos al intentar eliminar: " + e.getMessage());
        }
    }

    @FXML
    private void limpiar() {
        txtNombre.clear();
        chkActivo.setSelected(true);
        categoriaEdicion = null;
    }

    private void Buscador() {
        FilteredList<Categoria> categoriasFiltradas = new FilteredList<>(categorias, c -> true);

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            categoriasFiltradas.setPredicate(categoria -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String busqueda = newValue.toLowerCase().trim();

                if (categoria.getNombre().toLowerCase().contains(busqueda)) {
                    return true;
                } else if (String.valueOf(categoria.getId()).contains(busqueda)) {
                    return true;
                }

                return false;
            });
        });

        SortedList<Categoria> categoriasOrdenadas = new SortedList<>(categoriasFiltradas);
        categoriasOrdenadas.comparatorProperty().bind(tblCategorias.comparatorProperty());

        tblCategorias.setItems(categoriasOrdenadas);
    }

    @FXML
    private void cerrar() {
        ((Stage) txtNombre.getScene().getWindow()).close();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mensaje(Alert.AlertType tipo, String texto) {
        new Alert(tipo, texto, ButtonType.OK).showAndWait();
    }
}