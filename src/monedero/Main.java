package monedero;

import monedero.datos.DatosIniciales;
import monedero.servicio.SistemaMonedero;
import monedero.ui.MenuConsola;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        SistemaMonedero sistema = new SistemaMonedero();
        DatosIniciales.cargar(sistema);
        try (Scanner entrada = new Scanner(System.in)) {
            new MenuConsola(sistema, entrada).iniciar();
        }
    }
}
