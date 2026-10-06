package ni.edu.uam.facturacionapp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ni.edu.uam.facturacionapp.dao.CategoriaDao;
import ni.edu.uam.facturacionapp.dao.ProductoDao;
import ni.edu.uam.facturacionapp.model.Categoria;
import ni.edu.uam.facturacionapp.model.Producto;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

public class ProductoController {

    // Componentes mapeados exactamente con los fx:id del FXML
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecioVenta;
    @FXML private TextField txtExistencia;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private CheckBox chkActivo;

    // Componentes de Búsqueda y Filtro en el FXML
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroEstado;

    // Imagen
    @FXML private ImageView imgProducto;

    // Tabla y Columnas
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, Integer> colExistencia;
    @FXML private TableColumn<Producto, Boolean> colActivo;

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private final ProductoDao productoDAO = new ProductoDao();
    private final CategoriaDao categoriaDAO = new CategoriaDao();
    private String rutaImagen;

    // Variable para controlar si se está creando o editando un producto
    private Producto productoEdicion = null;

    @FXML
    private void initialize() {

        if (colCodigo != null) colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        if (colNombre != null) colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        if (colCategoria != null) {
            colCategoria.setCellValueFactory(cellData -> {
                Categoria cat = cellData.getValue().getCategoria();
                return new SimpleStringProperty(cat != null ? cat.getNombre() : "");
            });
        }

        if (colPrecio != null) colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        if (colExistencia != null) colExistencia.setCellValueFactory(new PropertyValueFactory<>("existencia"));
        if (colActivo != null) colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        // Carga de categorías en el ComboBox
        if (cmbCategoria != null) {
            try {
                cmbCategoria.setItems(FXCollections.observableArrayList(categoriaDAO.listar()));
            } catch (SQLException e) {
                mensaje(Alert.AlertType.ERROR, "Error al cargar categorías: " + e.getMessage());
            }
        }

        if (tblProductos != null) {
            tblProductos.setItems(productos);
        }

        if (chkActivo != null) {
            chkActivo.setSelected(true);
        }

        cargarProductos();
    }

    private void cargarProductos() {
        try {
            productos.clear();
            productos.addAll(productoDAO.listar());
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error al cargar productos de PostgreSQL: " + e.getMessage());
        }
    }

    // === MÉTODO DE VALIDACIÓN COMPLETO SEGÚN LA TABLA DE REQUISITOS ===
    private boolean validarProducto() {
        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();
        Categoria categoria = cmbCategoria.getValue();

        // 1. Código: Obligatorio y no duplicado
        if (codigo.isEmpty()) {
            mostrarAdvertencia("Validación", "El código del producto es obligatorio.");
            txtCodigo.requestFocus();
            return false;
        }

        try {
            if (productoEdicion == null) {
                if (productoDAO.existeCodigo(codigo)) {
                    mostrarAdvertencia("Validación", "Ya existe un producto registrado con el código '" + codigo + "'.");
                    txtCodigo.requestFocus();
                    return false;
                }
            } else {
                if (!productoEdicion.getCodigo().equalsIgnoreCase(codigo) && productoDAO.existeCodigo(codigo)) {
                    mostrarAdvertencia("Validación", "Ya existe otro producto registrado con el código '" + codigo + "'.");
                    txtCodigo.requestFocus();
                    return false;
                }
            }
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error al validar el código en la base de datos: " + e.getMessage());
            return false;
        }

        // 2. Nombre: Obligatorio
        if (nombre.isEmpty()) {
            mostrarAdvertencia("Validación", "El nombre del producto es obligatorio.");
            txtNombre.requestFocus();
            return false;
        }

        // 3. Categoría: Debe seleccionarse una
        if (categoria == null) {
            mostrarAdvertencia("Validación", "Debe seleccionar una categoría para el producto.");
            cmbCategoria.requestFocus();
            return false;
        }

        // 4. Precio Venta: Numérico y mayor que cero
        try {
            BigDecimal precio = new BigDecimal(txtPrecioVenta.getText().trim());
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarAdvertencia("Validación", "El precio de venta debe ser un número mayor que cero.");
                txtPrecioVenta.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            mostrarAdvertencia("Validación", "Ingrese un precio de venta válido (ej. 25.50).");
            txtPrecioVenta.requestFocus();
            return false;
        }

        // 5. Existencia: Entero y no negativo (>= 0)
        try {
            int existencia = Integer.parseInt(txtExistencia.getText().trim());
            if (existencia < 0) {
                mostrarAdvertencia("Validación", "La existencia no puede ser un valor negativo.");
                txtExistencia.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            mostrarAdvertencia("Validación", "La existencia debe ser un número entero válido (ej. 10).");
            txtExistencia.requestFocus();
            return false;
        }

        // 6. Activo: Se obtiene del CheckBox directamente
        return true;
    }

    @FXML
    private void seleccionarImagen() {
        if (txtCodigo == null || txtCodigo.getScene() == null) return;
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File archivo = chooser.showOpenDialog(txtCodigo.getScene().getWindow());
        if (archivo != null) {
            rutaImagen = archivo.toURI().toString();
            if (imgProducto != null) {
                imgProducto.setImage(new Image(rutaImagen));
            }
        }
    }

    @FXML
    private void editar() {
        Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAdvertencia("Seleccione un producto", "Debe seleccionar un producto de la tabla para actualizar.");
            return;
        }
        this.productoEdicion = seleccionado;
        txtCodigo.setText(seleccionado.getCodigo());
        txtNombre.setText(seleccionado.getNombre());
        txtPrecioVenta.setText(seleccionado.getPrecioVenta() != null ? seleccionado.getPrecioVenta().toString() : "");
        txtExistencia.setText(String.valueOf(seleccionado.getExistencia()));
        chkActivo.setSelected(seleccionado.getActivo() != null && seleccionado.getActivo());

        if (seleccionado.getCategoria() != null) {
            cmbCategoria.setValue(seleccionado.getCategoria());
        }
    }

    @FXML
    private void guardar() {
        // Ejecución de la rutina de validaciones completas
        if (!validarProducto()) {
            return;
        }

        String codigoIngresado = txtCodigo.getText().trim();
        String nombreIngresado = txtNombre.getText().trim();
        BigDecimal precio = new BigDecimal(txtPrecioVenta.getText().trim());
        int existencia = Integer.parseInt(txtExistencia.getText().trim());

        try {
            if (productoEdicion == null) {
                // MODO: INSERTAR NUEVO
                Producto nuevoProducto = new Producto(
                        null,
                        codigoIngresado,
                        nombreIngresado,
                        cmbCategoria.getValue(),
                        precio,
                        existencia,
                        rutaImagen,
                        chkActivo.isSelected()
                );

                if (productoDAO.insertar(nuevoProducto)) {
                    mensaje(Alert.AlertType.INFORMATION, "Producto guardado con éxito.");
                    cargarProductos();
                    limpiar();
                }
            } else {
                // MODO: ACTUALIZAR EXISTENTE
                productoEdicion.setCodigo(codigoIngresado);
                productoEdicion.setNombre(nombreIngresado);
                productoEdicion.setCategoria(cmbCategoria.getValue());
                productoEdicion.setPrecioVenta(precio);
                productoEdicion.setExistencia(existencia);
                productoEdicion.setRutaImagen(rutaImagen);
                productoEdicion.setActivo(chkActivo.isSelected());

                if (productoDAO.actualizar(productoEdicion)) {
                    mensaje(Alert.AlertType.INFORMATION, "Producto actualizado con éxito.");
                    cargarProductos();
                    limpiar();
                } else {
                    mensaje(Alert.AlertType.ERROR, "No se pudo actualizar el producto.");
                }
            }

        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error de base de datos: " + e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        if (tblProductos == null) return;
        Producto productoSeleccionado = tblProductos.getSelectionModel().getSelectedItem();

        if (productoSeleccionado == null) {
            mostrarAdvertencia("Seleccione un producto", "Debe seleccionar un producto de la tabla para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Estás seguro de que deseas eliminar el producto '" + productoSeleccionado.getNombre() + "'?",
                ButtonType.YES,
                ButtonType.NO
        );

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent() && respuesta.get() == ButtonType.YES) {
            try {
                if (productoDAO.eliminar(productoSeleccionado.getId())) {
                    mensaje(Alert.AlertType.INFORMATION, "Producto eliminado con éxito.");
                    cargarProductos();
                    limpiar();
                } else {
                    mensaje(Alert.AlertType.ERROR, "No se pudo eliminar el producto.");
                }
            } catch (SQLException e) {
                mensaje(Alert.AlertType.ERROR, "No se puede eliminar el producto porque está referenciado en facturas o ventas existentes.");
            }
        }
    }

    @FXML
    private void cerrar() {
        if (txtCodigo != null && txtCodigo.getScene() != null) {
            ((Stage) txtCodigo.getScene().getWindow()).close();
        }
    }

    @FXML
    private void limpiar() {
        if (txtCodigo != null) txtCodigo.clear();
        if (txtNombre != null) txtNombre.clear();
        if (txtPrecioVenta != null) txtPrecioVenta.clear();
        if (txtExistencia != null) txtExistencia.clear();
        if (cmbCategoria != null) cmbCategoria.getSelectionModel().clearSelection();
        if (chkActivo != null) chkActivo.setSelected(true);
        if (imgProducto != null) imgProducto.setImage(null);
        rutaImagen = null;
        productoEdicion = null;
        if (tblProductos != null) tblProductos.getSelectionModel().clearSelection();
    }

    private void mostrarAdvertencia(String titulo, String texto) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(texto);
        alert.showAndWait();
    }

    private void mensaje(Alert.AlertType tipo, String texto) {
        new Alert(tipo, texto, ButtonType.OK).showAndWait();
    }
}