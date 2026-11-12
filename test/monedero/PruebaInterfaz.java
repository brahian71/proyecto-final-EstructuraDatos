package monedero;

import monedero.datos.DatosIniciales;
import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;
import monedero.modelo.Cliente;
import monedero.modelo.Frecuencia;
import monedero.modelo.TipoTransaccion;
import monedero.servicio.Reloj;
import monedero.servicio.SistemaMonedero;
import monedero.servicio.VerificadorIntegridad;
import monedero.ui.gui.VentanaLogin;
import monedero.ui.gui.VentanaPrincipal;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * Prueba automatizada de la interfaz: abre la ventana real, escribe en los
 * campos, pulsa los botones y responde los dialogos (un hilo vigila los
 * JOptionPane). Recorre inicio de sesion, operaciones, errores de monto,
 * reversion, canje, programadas, reportes, registro de clientes y la vista
 * restringida de un cliente.
 *
 * Resultado esperado: todas las comprobaciones OK, 0 FALLO.
 */
public class PruebaInterfaz {

    static final Coleccion<String> dialogos = new ListaEnlazada<>();
    static volatile int respuestaConfirmar = JOptionPane.YES_OPTION;
    static final Coleccion<String> errores = new ListaEnlazada<>();
    static int ok = 0;
    static int fallo = 0;
    static SistemaMonedero s;
    static VentanaPrincipal v;

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            synchronized (errores) {
                errores.agregar(t.getName() + ": " + e);
            }
            e.printStackTrace();
        });
        iniciarVigilante();
        s = new SistemaMonedero(new Reloj(LocalDateTime.of(2026, 10, 27, 8, 0)));
        DatosIniciales.cargar(s);
        SwingUtilities.invokeAndWait(VentanaPrincipal::aplicarAspecto);
        v = iniciarSesion("admin", "malo123", "admin", "admin123");

        JTabbedPane tabs = v.getPestanias();
        Object op = tabs.getComponentAt(1), hist = tabs.getComponentAt(2), puntos = tabs.getComponentAt(3),
                prog = tabs.getComponentAt(4), rep = tabs.getComponentAt(5), cli = tabs.getComponentAt(6);
        JComboBox<?> comboCliente = (JComboBox<?>) campo(v, "comboCliente");
        check("inicio: cliente actual es Ana", sel(comboCliente).startsWith("C001"));

        long antes = s.consultarSaldo("M001");
        elegir(campo(op, "depositoDestino"), "M001");
        escribir(campo(op, "depositoMonto"), "100.000");
        check("deposito: cotizacion muestra 10 puntos", texto(campo(op, "depositoCotizacion")).contains("Puntos: 10"));
        clic(op, "Depositar");
        check("deposito: dialogo con puntos ganados", ultimo().contains("Puntos ganados: 10"));
        check("deposito: saldo +100.000", s.consultarSaldo("M001") == antes + 100_000);
        check("deposito: campo monto limpio", ((JTextField) campo(op, "depositoMonto")).getText().isEmpty());

        elegir(campo(op, "retiroOrigen"), "M001");
        escribir(campo(op, "retiroMonto"), "999.999.999");
        clic(op, "Retirar");
        check("retiro: error de saldo insuficiente", ultimo().startsWith("No se pudo completar") && ultimo().contains("Saldo insuficiente"));
        escribir(campo(op, "retiroMonto"), "");
        clic(op, "Retirar");
        check("retiro: monto vacio da error", ultimo().contains("Escriba un monto"));
        escribir(campo(op, "retiroMonto"), "abc");
        clic(op, "Retirar");
        check("retiro: monto con letras da error", ultimo().contains("Monto no valido"));
        escribir(campo(op, "retiroMonto"), "12.5");
        clic(op, "Retirar");
        check("monto: 12.5 se rechaza", ultimo().contains("Monto no valido"));
        escribir(campo(op, "retiroMonto"), "12,500");
        clic(op, "Retirar");
        check("monto: 12,500 se rechaza", ultimo().contains("Monto no valido"));
        long antesMil = s.consultarSaldo("M001");
        escribir(campo(op, "depositoMonto"), "$1.250.000");
        clic(op, "Depositar");
        check("monto: $1.250.000 se acepta", s.consultarSaldo("M001") == antesMil + 1_250_000);
        escribir(campo(op, "depositoMonto"), "2.000.000.000");
        clic(op, "Depositar");
        check("monto: mas del maximo da error", ultimo().contains("maximo"));
        check("confirmacion: sale despues de refrescar", texto(campo(op, "proximaReversible")).contains("1.250.000"));

        long brunoAntes = s.consultarSaldo("M002");
        int avisosBruno = s.getNotificador().noLeidas("C002");
        elegir(campo(op, "transferenciaOrigen"), "M001");
        elegir(campo(op, "transferenciaDestino"), "M002");
        escribir(campo(op, "transferenciaMonto"), "50.000");
        check("transferencia: cotiza comision $450", texto(campo(op, "transferenciaCotizacion")).contains("$450"));
        long anaAntesT = s.consultarSaldo("M001");
        clic(op, "Transferir");
        check("transferencia: dialogo con comision", ultimo().contains("Comision: $450"));
        check("transferencia: Bruno recibe 50.000", s.consultarSaldo("M002") == brunoAntes + 50_000);
        check("transferencia: Ana paga 50.450", s.consultarSaldo("M001") == anaAntesT - 50_450);
        check("transferencia: Bruno tiene aviso nuevo", s.getNotificador().noLeidas("C002") == avisosBruno + 1);

        respuestaConfirmar = JOptionPane.NO_OPTION;
        clic(op, "Revertir ultima transaccion");
        check("revertir NO: no cambia nada", s.consultarSaldo("M001") == anaAntesT - 50_450);
        respuestaConfirmar = JOptionPane.YES_OPTION;
        clic(op, "Revertir ultima transaccion");
        check("revertir SI: dialogo de revertida", ultimo().contains("revertida"));
        check("revertir SI: saldo vuelve", s.consultarSaldo("M001") == anaAntesT && s.consultarSaldo("M002") == brunoAntes);

        elegir(campo(hist, "monedero"), "M001");
        check("historial: aparece REVERTIDA", tablaContiene(hist, "REVERTIDA"));

        Cliente ana = s.buscarCliente("C001");
        int disp = ana.getPuntosDisponibles();
        clic(puntos, "100 pts");
        check("canje 100: descuenta disponibles", ana.getPuntosDisponibles() == disp - 100);
        check("canje 100: dialogo con restantes", ultimo().contains(String.valueOf(disp - 100)));
        check("canje: bono deshabilitado para Ana", !boton(puntos, "1000 pts").isEnabled());

        elegir(comboCliente, "C004");
        check("cliente: cambia a Diego", s.buscarCliente("C004").equals(clienteDe(v)));
        check("canje: bono habilitado para Diego", boton(puntos, "1000 pts").isEnabled());
        long diegoAntes = s.consultarSaldo("M004");
        clic(puntos, "1000 pts");
        check("canje 1000: bono de $5.000", s.consultarSaldo("M004") == diegoAntes + 5_000);
        check("operaciones: combos con monederos de Diego", sel(campo(op, "depositoDestino")).startsWith("M004"));

        elegir(comboCliente, "C001");
        JComboBox<?> tipo = (JComboBox<?>) campo(prog, "tipo");
        SwingUtilities.invokeAndWait(() -> tipo.setSelectedItem(TipoTransaccion.DEPOSITO));
        check("programadas: deposito solo monederos propios", todosContienen(campo(prog, "destino"), "Ana"));
        SwingUtilities.invokeAndWait(() -> tipo.setSelectedItem(TipoTransaccion.TRANSFERENCIA));
        check("programadas: transferencia ofrece todos", ((JComboBox<?>) campo(prog, "destino")).getItemCount() == 6);
        check("programadas: transferencia no queda al mismo monedero", !sel(campo(prog, "origen")).equals(sel(campo(prog, "destino"))));
        String origenProg = sel(campo(prog, "origen"));
        JComboBox<?> destinoProg = (JComboBox<?>) campo(prog, "destino");
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < destinoProg.getItemCount(); i++) {
                if (destinoProg.getItemAt(i).equals(origenProg)) {
                    destinoProg.setSelectedIndex(i);
                }
            }
        });
        escribir(campo(prog, "monto"), "10.000");
        clic(prog, "Programar");
        check("programadas: transferencia al mismo monedero da error", ultimo().contains("mismo monedero"));
        JTable tablaProg = (JTable) campo(prog, "tabla");
        int filaOtro = filaCon(tablaProg, "M002");
        SwingUtilities.invokeAndWait(() -> tablaProg.setRowSelectionInterval(filaOtro, filaOtro));
        int pendAntes = s.getProgramador().cantidadPendientes();
        clic(prog, "Cancelar la seleccionada");
        check("programadas: no deja cancelar las de otro cliente", ultimo().contains("Solo puede cancelar")
                && s.getProgramador().cantidadPendientes() == pendAntes);
        SwingUtilities.invokeAndWait(() -> tipo.setSelectedItem(TipoTransaccion.DEPOSITO));
        elegir(campo(prog, "destino"), "M005");
        escribir(campo(prog, "monto"), "50.000");
        JComboBox<?> frecuencia = (JComboBox<?>) campo(prog, "frecuencia");
        SwingUtilities.invokeAndWait(() -> frecuencia.setSelectedItem(Frecuencia.SEMANAL));
        int pendientes = s.getProgramador().cantidadPendientes();
        clic(prog, "Programar");
        check("programadas: dialogo Programada", ultimo().contains("Programada P"));
        check("programadas: una mas en la cola", s.getProgramador().cantidadPendientes() == pendientes + 1);
        int fila = filaCon(tablaProg, "M005");
        SwingUtilities.invokeAndWait(() -> tablaProg.setRowSelectionInterval(fila, fila));
        clic(prog, "Cancelar la seleccionada");
        check("programadas: cancelada", s.getProgramador().cantidadPendientes() == pendientes);
        SwingUtilities.invokeAndWait(tablaProg::clearSelection);
        clic(prog, "Cancelar la seleccionada");
        check("programadas: cancelar sin seleccion avisa", ultimo().contains("Seleccione"));

        JSpinner spinner = buscar(v.getContentPane(), JSpinner.class);
        SwingUtilities.invokeAndWait(() -> spinner.setValue(31));
        clic(v.getContentPane(), "Avanzar dias");
        check("avanzar: informa procesadas", ultimo().contains("Transacciones programadas procesadas:") && !ultimo().contains("procesadas: 0"));
        clic(v.getContentPane(), "Verificar integridad");
        check("integridad: OK", ultimo().contains("Integridad: OK"));

        JComboBox<?> reporte = (JComboBox<?>) campo(rep, "reporte");
        for (int i = 0; i < 6; i++) {
            int k = i;
            int dialogosAntes = dialogos.tamanio();
            SwingUtilities.invokeAndWait(() -> reporte.setSelectedIndex(k));
            if (i == 4) {
                elegir(campo(rep, "otroCliente"), "C002");
            }
            clic(rep, "Generar");
            check("reporte " + (i + 1) + ": sin error", dialogos.tamanio() == dialogosAntes);
        }
        check("reporte 6: tiene filas", ((TableModel) campo(rep, "modelo")).getRowCount() > 0);
        SwingUtilities.invokeAndWait(() -> reporte.setSelectedIndex(3));
        clic(rep, "Generar");
        Object dibujo = campo(rep, "dibujo");
        check("reporte ciclos: queda resaltado", ((monedero.estructuras.Coleccion<?>) campo(dibujo, "resaltado")).tamanio() > 0);
        elegir(comboCliente, "C003");
        check("reportes: al cambiar de cliente se limpia", ((TableModel) campo(rep, "modelo")).getRowCount() == 0);
        elegir(comboCliente, "C001");

        Coleccion<JTextField> camposCli = buscarTodos((Container) cli, JTextField.class);
        escribir(camposCli.obtener(0), "Prueba Uno");
        escribir(camposCli.obtener(1), "pr");
        escribir(camposCli.obtener(2), "prueba123");
        clic(cli, "Registrar");
        check("clientes: usuario corto da error", ultimo().contains("al menos 3") && !sel(comboCliente).contains("Prueba Uno"));
        escribir(camposCli.obtener(1), "prueba");
        escribir(camposCli.obtener(2), "prueba123");
        clic(cli, "Registrar");
        check("clientes: registrado", ultimo().contains("registrado") && ultimo().contains("prueba"));
        check("clientes: queda seleccionado", sel(comboCliente).contains("Prueba Uno"));
        check("clientes: contrasena se borra", ((JPasswordField) camposCli.obtener(2)).getPassword().length == 0);
        check("clientes: el usuario nuevo inicia sesion", s.getUsuarios().iniciarSesion("prueba", "prueba123") != null);
        escribir(camposCli.obtener(3), "Viaje");
        clic(cli, "Crear");
        Cliente nuevo = clienteDe(v);
        check("clientes: monedero creado", nuevo.cantidadMonederos() == 2 && ultimo().contains("creado"));
        escribir(camposCli.obtener(0), "   ");
        escribir(camposCli.obtener(1), "otro");
        escribir(camposCli.obtener(2), "otro1234");
        clic(cli, "Registrar");
        check("clientes: nombre vacio da error", ultimo().startsWith("No se pudo completar") && !s.getUsuarios().existe("otro"));

        check("nuevo: boton revertir deshabilitado", !boton(op, "Revertir ultima transaccion").isEnabled());
        for (int i = 0; i < 6; i++) {
            int k = i;
            int dialogosAntes = dialogos.tamanio();
            SwingUtilities.invokeAndWait(() -> reporte.setSelectedIndex(k));
            clic(rep, "Generar");
            check("nuevo: reporte " + (k + 1) + " sin error", dialogos.tamanio() == dialogosAntes
                    || ultimo().contains("Registre otro cliente"));
        }
        elegir(campo(op, "depositoDestino"), "Viaje");
        escribir(campo(op, "depositoMonto"), "20.000");
        clic(op, "Depositar");
        check("nuevo: deposito en monedero Viaje", nuevo.saldoTotal() == 20_000);

        for (int i = 0; i < tabs.getTabCount(); i++) {
            int k = i;
            SwingUtilities.invokeAndWait(() -> {
                tabs.setSelectedIndex(k);
                v.setSize(1280, 760);
                v.validate();
                BufferedImage img = new BufferedImage(1280, 760, BufferedImage.TYPE_INT_RGB);
                v.getContentPane().printAll(img.getGraphics());
            });
        }

        clic(v.getContentPane(), "Cerrar sesion");
        check("sesion: se cierra la ventana principal", !v.isDisplayable());
        VentanaPrincipal anaV = iniciarSesion(null, null, "ana", "ana123");
        check("cliente: ventana de Ana", clienteDe(anaV).getId().equals("C001"));
        check("cliente: no ve selector de clientes", !((JComboBox<?>) campo(anaV, "comboCliente")).isVisible());
        check("cliente: no ve avanzar ni integridad", !boton(anaV.getContentPane(), "Avanzar dias").isVisible()
                && !boton(anaV.getContentPane(), "Verificar integridad").isVisible());
        JTabbedPane tabsAna = anaV.getPestanias();
        Object opAna = tabsAna.getComponentAt(1), progAna = tabsAna.getComponentAt(4), repAna = tabsAna.getComponentAt(5),
                cliAna = tabsAna.getComponentAt(6), histAna = tabsAna.getComponentAt(2);
        check("cliente: pestania Mis monederos", tabsAna.getTitleAt(6).equals("Mis monederos"));
        check("cliente: 3 reportes", ((JComboBox<?>) campo(repAna, "reporte")).getItemCount() == 3);
        check("cliente: no ve el grafo", ((Component) campo(repAna, "dibujo")).getParent() == null);
        JComboBox<?> destinoAna = (JComboBox<?>) campo(opAna, "transferenciaDestino");
        boolean ocultaAjenos = true;
        for (int i = 0; i < destinoAna.getItemCount(); i++) {
            String item = destinoAna.getItemAt(i).toString();
            if (!item.contains("Ana Gomez") && item.contains("$")) {
                ocultaAjenos = false;
            }
        }
        check("cliente: no ve saldos ajenos", ocultaAjenos && destinoAna.getItemCount() > 2);
        int programadasAna = 0;
        for (var op2 : s.getProgramador().pendientes()) {
            if (op2.getClienteId().equals("C001")) {
                programadasAna++;
            }
        }
        check("cliente: solo sus programadas", ((JTable) campo(progAna, "tabla")).getRowCount() == programadasAna);
        check("cliente: tabla con sus monederos", buscarTodos((Container) cliAna, JTable.class).obtener(0).getRowCount()
                == s.buscarCliente("C001").cantidadMonederos());
        elegir(campo(histAna, "monedero"), "M001");
        check("historial: transferencia recibida sin puntos ajenos", tablaFilaCon(histAna, "M006", "-"));
        Object ctxAna = campo(anaV, "contexto");
        Method sel = ctxAna.getClass().getDeclaredMethod("seleccionarCliente", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(ctxAna, "C002");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        check("cliente: no puede cambiar a otro cliente", clienteDe(anaV).getId().equals("C001"));
        long anaSaldo = s.consultarSaldo("M001");
        elegir(campo(opAna, "depositoDestino"), "M001");
        escribir(campo(opAna, "depositoMonto"), "10.000");
        clic(opAna, "Depositar");
        check("cliente: opera sus monederos", s.consultarSaldo("M001") == anaSaldo + 10_000);
        clic(anaV.getContentPane(), "Cerrar sesion");

        check("final: integridad", new VerificadorIntegridad(s).verificar().esValido());
        check("final: sin excepciones no capturadas", errores.estaVacia());
        System.out.println("\nDialogos vistos: " + dialogos.tamanio());
        System.out.println("Resultado: " + ok + " OK, " + fallo + " FALLO");
        System.exit(fallo == 0 ? 0 : 1);
    }

    static VentanaPrincipal iniciarSesion(String malUsuario, String malaClave, String usuario, String clave) throws Exception {
        VentanaLogin[] login = new VentanaLogin[1];
        SwingUtilities.invokeAndWait(() -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof VentanaLogin l && w.isDisplayable()) {
                    login[0] = l;
                }
            }
            if (login[0] == null) {
                login[0] = new VentanaLogin(s);
            }
        });
        if (malUsuario != null) {
            escribir(campo(login[0], "campoUsuario"), malUsuario);
            escribir(campo(login[0], "campoContrasena"), malaClave);
            clic(login[0].getContentPane(), "Ingresar");
            check("login: credenciales malas muestran error", texto(campo(login[0], "mensaje")).contains("incorrectos")
                    && login[0].isDisplayable());
            check("login: borra la contrasena", ((JPasswordField) campo(login[0], "campoContrasena")).getPassword().length == 0);
        }
        escribir(campo(login[0], "campoUsuario"), usuario);
        escribir(campo(login[0], "campoContrasena"), clave);
        clic(login[0].getContentPane(), "Ingresar");
        VentanaPrincipal[] principal = new VentanaPrincipal[1];
        SwingUtilities.invokeAndWait(() -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof VentanaPrincipal vp && w.isDisplayable()) {
                    principal[0] = vp;
                }
            }
            if (principal[0] != null) {
                principal[0].setVisible(false);
            }
        });
        check("login: " + usuario + " entra", principal[0] != null && !login[0].isDisplayable());
        return principal[0];
    }

    static void iniciarVigilante() {
        Thread t = new Thread(() -> {
            while (true) {
                for (Window w : Window.getWindows()) {
                    if (w instanceof JDialog d && d.isShowing()) {
                        JOptionPane pane = buscar(d.getContentPane(), JOptionPane.class);
                        if (pane != null && pane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
                            String msg = mensaje(pane.getMessage());
                            synchronized (dialogos) {
                                dialogos.agregar(d.getTitle() + ": " + msg);
                            }
                            int respuesta = pane.getOptionType() == JOptionPane.YES_NO_OPTION ? respuestaConfirmar : JOptionPane.OK_OPTION;
                            SwingUtilities.invokeLater(() -> pane.setValue(respuesta));
                        }
                    }
                }
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    static String mensaje(Object m) {
        if (m instanceof JScrollPane sp && sp.getViewport().getView() instanceof JTextArea ta) {
            return ta.getText();
        }
        return String.valueOf(m);
    }

    static String ultimo() {
        synchronized (dialogos) {
            return dialogos.estaVacia() ? "" : dialogos.obtener(dialogos.tamanio() - 1);
        }
    }

    static void check(String nombre, boolean cond) {
        if (cond) {
            ok++;
            System.out.println("OK    " + nombre);
        } else {
            fallo++;
            System.out.println("FALLO " + nombre + "   [ultimo dialogo: " + ultimo().replace('\n', ' ') + "]");
        }
    }

    static Object campo(Object o, String nombre) throws Exception {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(nombre);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(nombre);
    }

    static Cliente clienteDe(VentanaPrincipal ventana) throws Exception {
        Object contexto = campo(ventana, "contexto");
        Method m = contexto.getClass().getDeclaredMethod("cliente");
        m.setAccessible(true);
        return (Cliente) m.invoke(contexto);
    }

    static String sel(Object combo) {
        Object o = ((JComboBox<?>) combo).getSelectedItem();
        return o == null ? "" : o.toString();
    }

    static void elegir(Object combo, String texto) throws Exception {
        JComboBox<?> c = (JComboBox<?>) combo;
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < c.getItemCount(); i++) {
                if (c.getItemAt(i).toString().contains(texto)) {
                    c.setSelectedIndex(i);
                    return;
                }
            }
            throw new IllegalStateException("No hay opcion " + texto);
        });
    }

    static boolean todosContienen(Object combo, String texto) {
        JComboBox<?> c = (JComboBox<?>) combo;
        for (int i = 0; i < c.getItemCount(); i++) {
            if (!c.getItemAt(i).toString().contains(texto)) {
                return false;
            }
        }
        return c.getItemCount() > 0;
    }

    static void escribir(Object campo, String texto) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JTextField) campo).setText(texto));
    }

    static String texto(Object etiqueta) {
        return ((javax.swing.JLabel) etiqueta).getText();
    }

    static void clic(Object contenedor, String texto) throws Exception {
        AbstractButton b = boton(contenedor, texto);
        SwingUtilities.invokeAndWait(b::doClick);
        Thread.sleep(150);
    }

    static AbstractButton boton(Object contenedor, String texto) {
        for (AbstractButton b : buscarTodos((Container) contenedor, AbstractButton.class)) {
            if (b.getText() != null && b.getText().contains(texto)) {
                return b;
            }
        }
        throw new IllegalStateException("No hay boton " + texto);
    }

    static boolean tablaContiene(Object panel, String texto) {
        for (JTable t : buscarTodos((Container) panel, JTable.class)) {
            if (filaCon(t, texto) >= 0) {
                return true;
            }
        }
        return false;
    }

    static boolean tablaFilaCon(Object panel, String enFila, String tambien) {
        for (JTable t : buscarTodos((Container) panel, JTable.class)) {
            int f = filaCon(t, enFila);
            if (f >= 0) {
                for (int c = 0; c < t.getColumnCount(); c++) {
                    if (t.getColumnName(c).equals("Puntos")) {
                        return tambien.equals(String.valueOf(t.getValueAt(f, c)));
                    }
                }
            }
        }
        return false;
    }

    static int filaCon(JTable t, String texto) {
        for (int f = 0; f < t.getRowCount(); f++) {
            for (int c = 0; c < t.getColumnCount(); c++) {
                if (String.valueOf(t.getValueAt(f, c)).contains(texto)) {
                    return f;
                }
            }
        }
        return -1;
    }

    static <T> T buscar(Container c, Class<T> tipo) {
        Coleccion<T> todos = buscarTodos(c, tipo);
        return todos.estaVacia() ? null : todos.obtener(0);
    }

    static <T> Coleccion<T> buscarTodos(Container c, Class<T> tipo) {
        Coleccion<T> r = new ListaEnlazada<>();
        for (Component k : c.getComponents()) {
            if (tipo.isInstance(k)) {
                r.agregar(tipo.cast(k));
            }
            if (k instanceof Container cc) {
                for (T x : buscarTodos(cc, tipo)) {
                    r.agregar(x);
                }
            }
        }
        return r;
    }
}
