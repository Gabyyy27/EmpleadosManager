/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EmpleadosManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author LENOVO
 */
public class Empleado_Manager {
    private RandomAccessFile rcods,remps;
    public Empleado_Manager(){
        try{
            File f=new File("Company ");
            f.mkdir();
            // instanciar los Rads dentro del folder company             
            rcods=new RandomAccessFile("Company/codigos.emp","rw");
            remps=new RandomAccessFile("Company/empleados.emp","rw");   
            // Inicializar el archivo de codifos si es nuevo
            initCode();
        }catch (IOException e){
            System.out.println(" No deberia pasar esto    ");
        }   
    }    
    private void initCode()throws IOException {
    if (rcods.length()==0){
        // 0 bytes
        rcods.writeInt(1);
        //             4bytes        
    }
    }
    private int getCode()throws IOException{
        // seek(int)
        // getFilePointer()
        rcods.seek(0);
        int code=rcods.readInt();//1
        rcods.seek(0);
        rcods.writeInt(code+1);
        return code;
    }
    public void addEmployee(String name,double salary)throws IOException{
        /*
        Formato Empleados.emp
        codigo - int 
        nombre - String 
        salario - double 
        fecha Contratacion - Fecha del momento - Long .
        fecha Despido - Fecha del Momento del despido - Long. 
        */
      // Asegurar que el puntero este al final 
      remps.seek(remps.length());
      // codigo 
      int code=getCode();
      remps.writeInt(code);
      // nombre 
      remps.writeUTF(name);
      // salario 
      remps.writeDouble(salary);
      // fecha Contratacion
      remps.writeLong(Calendar.getInstance().getTimeInMillis());
      // fecha Despedir
      remps.writeLong(0);
      //  Asegurar sus Archivos individuales.
      createYearSalesFileFor(code);
      
    }
   private String employeeFolder(int code){
       return "Company/empleado"+code;
   }
   private void createEmployeeFolders(int code)throws IOException{
       // crear Folder
       File femp=new File(employeeFolder(code));
       femp.mkdir();
       // crear los archivos de ventas del empleado
       createYearSalesFileFor(code);
   }
   private RandomAccessFile salesFileFor(int code )throws IOException{
       String dirPadre=employeeFolder(code);
       int yearActual=Calendar.getInstance().get(Calendar.YEAR);
       String path=dirPadre+"/Ventas"+yearActual+".emp";
       // se retorna con new Random Access File ()con dos campos: path y "rw"
       return new RandomAccessFile(path,"rw");
   }
   private void createYearSalesFileFor(int code)throws IOException{
       RandomAccessFile ryear= salesFileFor(code);
     /*
       Formato VentasYear.emp;
       ventas - Double
       estado - Boolean
       */          
     // inicia en 0 bytes
     if (ryear.length()==0){
         for (int mes=0;mes<12;mes++){
             ryear.writeDouble(0);
             ryear.writeBoolean(false);
             
         }         
     }             
   }
  public void listarEmpleados()throws IOException{
     rcods.seek(0);
     while (remps.getFilePointer() < remps.length()){
         int code=remps.readInt();
         String name=remps.readUTF();
         double salary=remps.readDouble(); 
         Date FechaC=new Date(remps.readLong());
         if (remps.readLong()==0){
         System.out.println("El codigo del empleado es: "+code);
         System.out.println("El nombre del Emplado es: "+name);
         System.out.println("El salario es:$. "+salary);
         System.out.println("Fecha Contratado: "+FechaC); 
        }
     }    
   }
   public void fireEmployee(int code)throws IOException{
    remps.seek(0);
    while (remps.getFilePointer() < remps.length()) {
        long currentPointer = remps.getFilePointer();
        String name = remps.readUTF();
        double salary = remps.readDouble();
        Date FechaC = new Date(remps.readLong());
        long employeeCode = remps.readLong();
        long terminationDate = remps.readLong();
        if (employeeCode == code && terminationDate == 0) {
            // Actualizar la fecha de despido
            remps.seek(currentPointer + 8); // Mover el puntero justo después del código del empleado (long)
            remps.writeLong(System.currentTimeMillis()); // Usar la fecha actual como fecha de despido
            System.out.println("Empleado con código " + code + " ha sido despedido.");
            break;
        }
    }
 }
   private void isEmployeeActive(int code)throws IOException{
      remps.seek(0);
       while (remps.getFilePointer() < remps.length()) {
        long currentPointer = remps.getFilePointer();
        String name = remps.readUTF();
        double salary = remps.readDouble();
        Date FechaC = new Date(remps.readLong());
        long employeeCode = remps.readLong();
        if (employeeCode == code) {
            System.out.println("El codigo del empleado es: " + code);
            System.out.println("El nombre del Empleado es: " + name);
            System.out.println("El salario es: $." + salary);
            System.out.println("Fecha Contratado: " + FechaC);
            remps.seek(currentPointer + 8); 
            break;
        }
   }
   }
   public void addSaleToEmployee(int code, double sale)throws IOException{
       remps.seek(0);
       
   } 
   private RandomAccessFile billsFileFor(int code) throws IOException {
        String billsFileName = "recibos_emp/recibo_" + code + ".emp";
        return new RandomAccessFile(billsFileName, "rw");
    }
    public void payEmployee(int code) throws IOException {
        remps.seek(0);
        while (remps.getFilePointer() < remps.length()) {
            long currentPointer = remps.getFilePointer();
            String name = remps.readUTF();
            double salary = remps.readDouble();
            Date FechaC = new Date(remps.readLong());
            long employeeCode = remps.readLong();
            long terminationDate = remps.readLong();
            boolean paid = remps.readBoolean();
            if (employeeCode == code && terminationDate == 0 && !paid) {
                double commission = 0.10 * getSalesTotalForEmployee(code); // Obtener la comisión del archivo de ventas
                double totalSalary = salary + commission;
                double deduction = 0.035 * totalSalary;
                remps.seek(currentPointer + 32); // Mover el puntero al booleano de pagado
                remps.writeBoolean(true);
                try (RandomAccessFile billsFile = billsFileFor(code)) {
                    billsFile.writeLong(System.currentTimeMillis()); 
                    billsFile.writeDouble(totalSalary);
                    billsFile.writeDouble(deduction);
                    billsFile.writeInt(FechaC.getYear() + 1900);
                    billsFile.writeInt(FechaC.getMonth() + 1);
                }
                // Mostrar la salida en pantalla
                System.out.println("Nombre del empleado: " + name);
                System.out.println("Total del sueldo de pago: " + totalSalary);
                break;
            }
        }
    }
  private double getSalesTotalForEmployee(int code) throws IOException {
    try (RandomAccessFile salesFile = salesFileFor(code)) {
        String dirPadre=employeeFolder(code);
       int yearActual=Calendar.getInstance().get(Calendar.YEAR);
       String path=dirPadre+"/Ventas"+yearActual+".emp";
        double totalSales = 0.0;
        while (salesFile.getFilePointer() < salesFile.length()) {
            totalSales += salesFile.readDouble(); // Lee y suma las ventas
        }
        return totalSales;
    }
} 
  public boolean isEmployeePayed(int code)throws IOException{
      remps.seek(0);
     try (RandomAccessFile salesFile = salesFileFor(code)) {
        long currentPointer = salesFile.getFilePointer();
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        salesFile.seek(0);

        for (int mes = 0; mes <= currentMonth; mes++) {
            salesFile.readDouble(); 
            if (mes == currentMonth) {
                return salesFile.readBoolean(); 
            }
        }
    }
    return false;
  }
  
}
