/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package empleadosmanager;

/**
 *
 * @author Lourdes
 */
import java.io.*;
import java.util.Calendar;
import java.util.Date;

public class EmpleadosManager {

    private RandomAccessFile rcods, remps;

    public EmpleadosManager() {
        try {
            initializeSystem();
        } catch (IOException e) {
            System.out.println("Error al inicializar el sistema");
            e.printStackTrace();
        }
    }

    private void initializeSystem() throws IOException {
        File companyDir = new File("Company");
        companyDir.mkdir();

        rcods = new RandomAccessFile("Company/codigos.emp", "rw");
        remps = new RandomAccessFile("Company/empleados.emp", "rw");

        if (rcods.length() == 0) {
            rcods.writeInt(1);
        }
    }

    private int getCode() throws IOException {
        rcods.seek(0);
        int code = rcods.readInt();
        rcods.seek(0);
        rcods.writeInt(code + 1);
        return code;
    }

    public void addEmployee(String name, double salary) {
        try {
            int code = getCode();

            // Mover el puntero al final del archivo antes de agregar un nuevo empleado
            remps.seek(remps.length());

            remps.writeInt(code);
            remps.writeUTF(name);
            remps.writeDouble(salary);
            remps.writeLong(Calendar.getInstance().getTimeInMillis());
            remps.writeLong(0);
            createEmployeeFolders(code);
        } catch (IOException e) {
            System.out.println("Error al agregar empleado");
            e.printStackTrace();
        }
    }

    private void createEmployeeFolders(int code) throws IOException {
        File employeeDir = new File(employeeFolder(code));
        employeeDir.mkdir();
        createYearSalesFileFor(code);
    }

    private String employeeFolder(int code) {
        return "Company/empleado" + code;
    }

    private void createYearSalesFileFor(int code) throws IOException {
        String dirPadre = employeeFolder(code);
        int yearActual = Calendar.getInstance().get(Calendar.YEAR);
        String path = dirPadre + "/Ventas" + yearActual + ".emp";

        // Crear el archivo de ventas si no existe
        File salesFile = new File(path);
        if (!salesFile.exists()) {
            try (RandomAccessFile ryear = new RandomAccessFile(path, "rw")) {
                if (ryear.length() == 0) {
                    for (int mes = 0; mes < 12; mes++) {
                        ryear.writeDouble(0);
                        ryear.writeBoolean(false);
                    }
                }
            }
        }
    }

    public String obtenerListaEmpleados() {
        StringBuilder listaEmpleados = new StringBuilder();

        try {
            remps.seek(0);
            while (remps.getFilePointer() < remps.length()) {
                int code = remps.readInt();
                String name = remps.readUTF();
                double salary = remps.readDouble();
                Date FechaC = new Date(remps.readLong());
                long terminationDate = remps.readLong();

                if (terminationDate == 0) {
                    // Agregar la información del empleado a la cadena
                    listaEmpleados.append("Código: ").append(code).append("\n");
                    listaEmpleados.append("Nombre: ").append(name).append("\n");
                    listaEmpleados.append("Salario: $").append(salary).append("\n");
                    listaEmpleados.append("Fecha Contratado: ").append(FechaC).append("\n\n");
                }
            }
        } catch (IOException e) {
            System.out.println("Error al obtener la lista de empleados");
            e.printStackTrace();
        }

        return listaEmpleados.toString();
    }

    public void addSaleToEmployee(int code, double sale) {
        try {
            try (RandomAccessFile salesFile = salesFileFor(code)) {
                int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
                salesFile.seek(currentMonth * 12); // Mover el puntero al mes actual
                double currentSales = salesFile.readDouble(); // Leer las ventas actuales
                salesFile.seek(currentMonth * 12); // Mover el puntero de nuevo al mes actual
                salesFile.writeDouble(currentSales + sale); // Sumar la nueva venta a las ventas actuales
                salesFile.writeBoolean(false); // Establecer el estado de pago a falso para el mes actual
                salesFile.seek(currentMonth * 12 + 8); // Mover el puntero al estado de pago del mes actual
                salesFile.writeBoolean(false); // Asegurarse de que el estado de pago esté actualizado
            }
        } catch (IOException e) {
            System.out.println("Error al agregar venta al empleado");
            e.printStackTrace();
        }
    }

    private double montoPago(int code) {
        double salarioBase;
        double ventasTotales;

        try {
            salarioBase = getEmployeeSalary(code);
            ventasTotales = getSalesTotalForEmployee(code);
        } catch (IOException e) {
            System.out.println("Error al calcular el monto de pago");
            e.printStackTrace();
            return 0.0;
        }

        double comision = 0.1 * ventasTotales;
        return salarioBase + comision;
    }

    private double getEmployeeSalary(int code) throws IOException {
        remps.seek(0);
        while (remps.getFilePointer() < remps.length()) {
            int readCode = remps.readInt();
            String name = remps.readUTF();
            double salary = remps.readDouble();
            Date FechaC = new Date(remps.readLong());
            long terminationDate = remps.readLong();
            if (readCode == code && terminationDate == 0) {
                return salary;
            }
            remps.seek(remps.getFilePointer() + 32); // Mover al siguiente registro
        }
        throw new IllegalArgumentException("Empleado no encontrado con código: " + code);
    }

    public void payEmployee(int code) throws IOException {
        if (isEmployeeActive(code)) {
            if (!isEmployeePayed(code)) {
                double montoPago = montoPago(code);
                crearRecibo(code, montoPago);
                marcarComoPagado(code);

                // Mostrar información
                System.out.println("Empleado: " + getEmployeeName(code));
                System.out.println("Total del sueldo de pago: $" + montoPago);
            } else {
                System.out.println("Error: El empleado ya ha sido pagado este mes.");
            }
        } else {
            System.out.println("Error: El empleado no está activo o no existe.");
        }
    }

    private void crearRecibo(int code, double montoPago) {
        try (RandomAccessFile recibosFile = recibosFilePara(code)) {
            recibosFile.writeLong(System.currentTimeMillis()); // Fecha de pago
            recibosFile.writeDouble(montoPago); // Sueldo
            recibosFile.writeDouble(0.035 * montoPago); // Deducción
            recibosFile.writeInt(Calendar.getInstance().get(Calendar.YEAR)); // Año
            recibosFile.writeInt(Calendar.getInstance().get(Calendar.MONTH)); // Mes
        } catch (IOException e) {
            System.out.println("Error al crear el recibo para el empleado");
            e.printStackTrace();
        }
    }

    private RandomAccessFile recibosFilePara(int code) throws IOException {
        String dirPadre = employeeFolder(code);
        String path = dirPadre + "/recibos.emp";
        return new RandomAccessFile(path, "rw");
    }

    private void marcarComoPagado(int code) {
        try (RandomAccessFile ventasFile = salesFileFor(code)) {
            int mesActual = Calendar.getInstance().get(Calendar.MONTH);
            ventasFile.seek(mesActual * 12 + 8); // Mover el puntero al estado de pago del mes actual
            ventasFile.writeBoolean(true); // Establecer el estado de pago a verdadero
        } catch (IOException e) {
            System.out.println("Error al marcar como pagado para el empleado");
            e.printStackTrace();
        }
    }

    public boolean isEmployeeActive(int code) throws IOException {

        remps.seek(0);
        while (remps.getFilePointer() < remps.length()) {
            long currentPointer = remps.getFilePointer();
            int readCode = remps.readInt();
            String name = remps.readUTF();
            double salary = remps.readDouble();
            Date FechaC = new Date(remps.readLong());
            long terminationDate = remps.readLong();
            if (readCode == code && terminationDate == 0) {
                System.out.println("El código del empleado es: " + code);
                System.out.println("El nombre del Empleado es: " + name);
                System.out.println("El salario es: $." + salary);
                System.out.println("Fecha Contratado: " + FechaC);
                return true;
            }
            remps.seek(currentPointer + 32); // Mover al siguiente registro
        }

        return false;
    }

    public boolean isEmployeePayed(int code) throws IOException {
        try (RandomAccessFile salesFile = salesFileFor(code)) {
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
            salesFile.seek(currentMonth * 12 + 8); // Mover el puntero al estado de pago del mes actual
            return salesFile.readBoolean(); // Leer y devolver el estado de pago
        }
    }

    private String getEmployeeName(int code) {
        try {
            remps.seek(0);
            while (remps.getFilePointer() < remps.length()) {
                int readCode = remps.readInt();
                String name = remps.readUTF();
                double salary = remps.readDouble();
                Date FechaC = new Date(remps.readLong());
                long terminationDate = remps.readLong();
                if (readCode == code && terminationDate == 0) {
                    return name;
                }
                remps.seek(remps.getFilePointer() + 32); // Mover al siguiente registro
            }
        } catch (IOException e) {
            System.out.println("Error al obtener el nombre del empleado");
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Empleado no encontrado con código: " + code);
    }

    private double getSalesTotalForEmployee(int code) {
        try (RandomAccessFile salesFile = salesFileFor(code)) {
            double totalSales = 0.0;
            while (salesFile.getFilePointer() < salesFile.length()) {
                totalSales += salesFile.readDouble();
                salesFile.readBoolean(); // Leer el estado de pago, pero no lo estamos utilizando aquí
            }
            return totalSales;
        } catch (IOException e) {
            System.out.println("Error al obtener las ventas totales del empleado");
            e.printStackTrace();
            return 0.0;
        }
    }

    private RandomAccessFile salesFileFor(int code) throws IOException {
        String dirPadre = employeeFolder(code);
        int yearActual = Calendar.getInstance().get(Calendar.YEAR);
        String path = dirPadre + "/Ventas" + yearActual + ".emp";

        // Verificar si el archivo de ventas existe, de lo contrario, crearlo
        File salesFile = new File(path);
        if (!salesFile.exists()) {
            createYearSalesFileFor(code);
        }

        // Retornar el RandomAccessFile con dos campos: path y "rw"
        return new RandomAccessFile(path, "rw");
    }

    public void fireEmployee(int code) {
        try {
            remps.seek(0);
            while (remps.getFilePointer() < remps.length()) {
                long currentPointer = remps.getFilePointer();
                int readCode = remps.readInt();
                String name = remps.readUTF();
                double salary = remps.readDouble();
                Date fechaContratacion = new Date(remps.readLong());
                long terminationDate = remps.readLong();

                if (readCode == code && terminationDate == 0) {
                    remps.seek(currentPointer + 32); // Mover al campo de fecha de terminación
                    remps.writeLong(Calendar.getInstance().getTimeInMillis()); // Establecer la fecha de terminación
                    System.out.println("Empleado despedido: " + name);
                    return;
                }

                // Mover al siguiente registro
                if (remps.getFilePointer() < remps.length()) {
                    remps.seek(currentPointer + 32);
                }
            }

            System.out.println("Error: Empleado no encontrado con código " + code);
        } catch (IOException e) {
            System.out.println("Error al despedir empleado");
            e.printStackTrace();
        }
    }
}
